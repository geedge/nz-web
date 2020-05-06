package com.nis.modules.alert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.AlertNameMapUserDTO;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
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

    @Override
    public boolean checkName(AlertRuleEntity alertRule) {
        List<AlertRuleEntity> list = this.list(new QueryWrapper<AlertRuleEntity>()
                .eq("alert_name", alertRule.getAlertName()));

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

    @Override
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
}