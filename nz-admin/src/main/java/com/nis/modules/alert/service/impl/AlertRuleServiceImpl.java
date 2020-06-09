package com.nis.modules.alert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.AlertNameMapUserDTO;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleStat;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.dao.AssetDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.module.dao.ModuleDao;
import com.nis.modules.module.entity.Module;
import com.nis.modules.project.dao.ProjectDao;
import com.nis.modules.project.entity.Project;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("alertRuleService")
@Slf4j
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleDao, AlertRuleEntity> implements AlertRuleService {

    @Autowired
    private AlertRuleDao alertRuleDao;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private YamlUtil yamlUtil;

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private ModuleDao moduleDao;

    @Autowired
    private AssetDao assetDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String receiverStr = (String) params.get("receiver");
        List<String> receivers = new ArrayList<>();
        if (StringUtils.isNotEmpty(receiverStr)) {
            receivers = Arrays.asList(receiverStr.split(","));
        }

        IPage<AlertRuleEntity> page = new Query<AlertRuleEntity>().getPage(params);
        List<AlertRuleEntity> ruleEntityList = alertRuleDao.queryList(page, params, receivers);

        // 获取所有用户
        List<SysUserEntity> userList = sysUserService.list();
        Map<Long, String> userIdAndName = userList.stream().collect(Collectors.toMap(SysUserEntity::getUserId, SysUserEntity::getUsername));

        ruleEntityList.stream().forEach(alertRule -> {
            String receiver = alertRule.getReceiver();
            if (StringUtils.isNotEmpty(receiver)) {
                List<SysUserEntity> list = new ArrayList<>();
                Arrays.asList(receiver.split(",")).stream().forEach(id -> {
                    SysUserEntity userEntity = new SysUserEntity();
                    userEntity.setUserId(Long.valueOf(id));
                    userEntity.setUserName(userIdAndName.get(Long.valueOf(id)));
                    list.add(userEntity);
                });
                alertRule.setReceivers(list);
            }
        });

        page.setRecords(ruleEntityList);
        return new PageUtils(page);
    }

    public boolean checkName(AlertRuleEntity alertRule) {
        List<AlertRuleEntity> list = this.list(new QueryWrapper<AlertRuleEntity>().lambda()
                .eq(AlertRuleEntity::getAlertName, alertRule.getAlertName()));

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list.size() == 1 && alertRule.getId() != null && list.get(0).getId().intValue() == alertRule.getId().intValue())) {
            return true;
        }
        return false;
    }

    @Override
    public List<AlertNameMapUserDTO> queryUserByRuleId(Set<Integer> ruleIds) {
        List<AlertNameMapUserDTO> results = this.baseMapper.queryUserByRuleIds(ruleIds);
        return results;
    }

    /**
     * 查询当前规则是否为内置规则
     */
    @Override
    public void validateBulitinRule(List<Integer> ids) {
        List<AlertRuleEntity> objs = alertRuleDao.selectBatchIds(ids);
        for (AlertRuleEntity obj : objs) {
            if (obj != null && obj.getBuildIn().equals(1)) {
                throw new NZException(RCode.ALERTRULE_BUILDIN_CAN_NOT_REMOVE);
            }
        }
    }

    public void validateReceiver(String receiver) {

        List<SysUserEntity> userEntityList = sysUserService.list();
        Map<Long, String> userIdAndName = userEntityList.stream().collect(Collectors.toMap(SysUserEntity::getUserId, SysUserEntity::getUsername));

        Arrays.asList(receiver.split(",")).stream().forEach(id -> {
            try {
                Long aLong = Long.valueOf(id);
                if (ToolUtil.isEmpty(userIdAndName.get(aLong))) {
                    // 用户不存在
                    throw new NZException("System user not found with ID value " + aLong, RCode.SYS_USER_NOT_EXIST.getCode());
                }
            } catch (NumberFormatException e) {
                log.error("告警通知用户id值格式有误,当前为 " + id + ", 应该为数值格式");
                throw new NZException(RCode.ALERTRULE_RECEIVER_FORMAT);
            }
        });
    }

    @Override
    public void saveOrUpdateAlertRule(AlertRuleEntity alertRule) {
        // 校验参数
        this.validateParam(alertRule);

        // 校验名称重复
        if (!this.checkName(alertRule)) {
            throw new NZException(RCode.ALERTRULE_NAME_DUPLICATE);
        }


        // 校验expr
        boolean checkRule = false;
        try {
            checkRule = yamlUtil.checkRule(YamlUtil.alertRuleEntityToYamlMap(alertRule));
        } catch (Exception e) {
            log.error("expr校验失败", e);
        } finally {
            if (!checkRule) {
                throw new NZException(RCode.ALERTRULE_EXPR_ERROR);
            }
        }

        // 校验关联用户
        String receiver = alertRule.getReceiver();
        if (StringUtils.isNotEmpty(receiver)) {
            this.validateReceiver(receiver);
        }

        // 入库
        if (alertRule.getId() == null) {
            this.save(alertRule);
        } else {
            // 校验内置规则
            AlertRuleEntity ruleEntity = this.getById(alertRule.getId());
            if (ruleEntity != null && ruleEntity.getBuildIn().equals(1)) {
                throw new NZException(RCode.ALERTRULE_BUILDIN_CAN_NOT_UPDATE);
            }

            this.updateById(alertRule);
        }
    }

    @Override
    public void delete(Integer[] ids) {
        ValidateUtils.is(ids).notNull(RCode.ALERTMSG_IDS_ISNULL);
        // 校验内置
        this.validateBulitinRule(Arrays.asList(ids));
        // 删除
        this.removeByIds(Arrays.asList(ids));
    }

    @Override
    public Map getAlertStatById(Integer id) {
        Map resultData = new HashMap();

        // 1. project 统计
        List<Project> projects = projectDao.selectList(null);
        Map<Integer, String> projectIdAndNameMap = projects.stream().collect(Collectors.toMap(Project::getId, Project::getName));

        List<AlertRuleStat> ruleStatGooupByProject = alertRuleDao.getRuleStatGooupByProject(id);
        this.handleRuleStatName(ruleStatGooupByProject, projectIdAndNameMap);

        resultData.put("project", ruleStatGooupByProject);

        // 2. module 统计
        List<Module> modules = moduleDao.selectList(null);
        Map<Integer, String> moduleIdAndNameMap = modules.stream().collect(Collectors.toMap(Module::getId, Module::getName));

        List<AlertRuleStat> ruleStatGooupByModule = alertRuleDao.getRuleStatGooupByModule(id);
        this.handleRuleStatName(ruleStatGooupByModule, moduleIdAndNameMap);

        resultData.put("module", ruleStatGooupByModule);

        // 3. asset 统计
        List<Asset> assets = assetDao.selectList(null);
        Map<Integer, String> assetIdAndHostMap = assets.stream().collect(Collectors.toMap(Asset::getId, Asset::getHost));

        List<AlertRuleStat> ruleStatGooupByAsset = alertRuleDao.getRuleStatGooupByAsset(id);
        this.handleRuleStatName(ruleStatGooupByAsset, assetIdAndHostMap);

        resultData.put("asset", ruleStatGooupByAsset);

        // 4. endpoint 统计
        List<AlertRuleStat> ruleStatGooupByEndpoint = alertRuleDao.getRuleStatGooupByEndpoint(id);
        resultData.put("endpoint", ruleStatGooupByEndpoint);

        return resultData;
    }

    private void handleRuleStatName(List<AlertRuleStat> ruleStats, Map<Integer, String> pojoIdAndNameMap) {
        ruleStats.stream().forEach(stat -> {
            stat.setName(pojoIdAndNameMap.get(stat.getId()));
        });
    }

    private void validateParam(AlertRuleEntity alertRule) {
        String operator = alertRule.getOperator();
        Integer unit = alertRule.getUnit();
        ValidateUtils.is(alertRule.getAlertName()).notNull(RCode.ALERTRULE_NAME_ISNULL)
                .and(alertRule.getExpr()).notNull(RCode.ALERTRULE_EXPR_ISNULL)
                .and(alertRule.getLast()).notNull(RCode.ALERTRULE_LAST_ISNULL).min(1, RCode.ALERTRULE_LAST_ISNOTPOSITIVE)
                .and(alertRule.getSeverity()).notNull(RCode.ALERTRULE_SEVERITY_ISNULL)
                .and(alertRule.getSummary()).notNull(RCode.ALERTRULE_SUMMARY_ISNULL)
                .and(operator).notNull(RCode.ALERTRULE_OPERATOR_ISNULL)
                .and(alertRule.getThreshold()).notNull(RCode.ALERTRULE_THRESHOLD_ISNULL)
                .and(unit).notNull(RCode.ALERTRULE_UNIT_ISNULL);

        operator = operator.replaceAll(" ", "");
        alertRule.setOperator(operator);

        if(!Constant.RULE_OPERATORS.contains(operator)){
            throw new NZException(RCode.ALERTRULE_OPERATOR_INVALIDE);
        }

        if(unit < 1 || unit > 33){
            throw new NZException(RCode.ALERTRULE_UNIT_INVALIDE);
        }
    }
}