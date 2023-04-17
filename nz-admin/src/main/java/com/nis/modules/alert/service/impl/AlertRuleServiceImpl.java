package com.nis.modules.alert.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.smartvalidate.utils.RegexUtil;
import com.nis.common.utils.*;
import com.nis.common.utils.Constant.AlertRuleType;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertNotifyMethod;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleImportEnum;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertNotifyMethodService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("alertRuleService")
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleDao, AlertRuleEntity> implements AlertRuleService {

    private static final Log log = Log.get();

    @Autowired
    private AlertRuleDao alertRuleDao;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private AlertNotifyMethodService methodService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private SysUserStarredService sysUserStarredService;

    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;
    
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String receiverStr = (String) params.get("receiver");
        List<String> receivers = new ArrayList<>();
        if (StringUtils.isNotEmpty(receiverStr)) {
            receivers = Arrays.asList(receiverStr.split(","));
        }

        // 当前登录用户
        params.put("currentLoginUserID", ShiroUtils.getUserId().intValue());

        IPage page = new Query(AlertRuleEntity.class).getPage(params);
        List<AlertRuleEntity> ruleEntityList = alertRuleDao.queryList(page, params, receivers);

        // 查询结果不为空时处理返回结果
        if(CollectionUtils.isNotEmpty(ruleEntityList)){
            this.handleResuleList(ruleEntityList);
        }

        page.setRecords(ruleEntityList);
        return new PageUtils(page);
    }

    private void handleResuleList(List<AlertRuleEntity> ruleEntityList) {
        // 获取所有用户
        List<SysUserEntity> userList = sysUserService.list();
        Map<Long, SysUserEntity> userIdAndEntityMap = userList.stream().collect(Collectors.toMap(SysUserEntity::getId, Function.identity()));

        // method
        List<AlertNotifyMethod> methodList = methodService.list();
        Map<Integer, AlertNotifyMethod> methodIdAndEntitiyMap = methodList.stream().collect(Collectors.toMap(AlertNotifyMethod::getId, Function.identity()));

        ruleEntityList.stream().forEach(alertRule -> {
            String receiver = alertRule.getReceiver();
            if (StringUtils.isNotEmpty(receiver)) {
                List<SysUserEntity> list = new ArrayList<>();
                Arrays.asList(receiver.split(",")).stream().forEach(id -> {
                    SysUserEntity userEntity = userIdAndEntityMap.get(Long.valueOf(id));
                    list.add(userEntity);
                });
                alertRule.setReceivers(list);
            }
            String method = alertRule.getMethod();
            if (StringUtils.isNotEmpty(method)) {
                List<AlertNotifyMethod> list = new ArrayList<>();
                Arrays.asList(method.split(",")).stream().forEach(id -> {
                    AlertNotifyMethod notifyMethod = methodIdAndEntitiyMap.get(Integer.valueOf(id));
                    list.add(notifyMethod);
                });
                alertRule.setMethods(list);
            }
        });
    }

    public boolean checkName(AlertRuleEntity alertRule) {
        List<AlertRuleEntity> list = this.list(new QueryWrapper<AlertRuleEntity>().lambda()
                .eq(AlertRuleEntity::getName, alertRule.getName()));

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list.size() == 1 && alertRule.getId() != null && list.get(0).getId().intValue() == alertRule.getId().intValue())) {
            return true;
        }
        return false;
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
        Map<Long, String> userIdAndName = userEntityList.stream().collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getName));

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
    public Integer saveOrUpdateAlertRule(AlertRuleEntity alertRule) {
        // 校验参数
        this.validateParam(alertRule);

        // 校验名称重复
        if (!this.checkName(alertRule)) {
            throw new NZException(RCode.ALERTRULE_NAME_DUPLICATE);
        }

        // 校验关联用户
        String receiver = alertRule.getReceiver();
        if (StringUtils.isNotEmpty(receiver)) {
            this.validateReceiver(receiver);
            String method = alertRule.getMethod();
            ValidateUtils.is(method).notNull(RCode.ALERTRULE_METHOD_ISNULL);
        }

        // 设置 interval timeout 值
        if (alertRule.getInr() != null && alertRule.getInr() < 15) {
            Integer defaultScrapeInterval = Integer.valueOf(sysConfigService.getValue(Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
            alertRule.setInr(defaultScrapeInterval);
        }

        if (alertRule.getTimeout() != null && alertRule.getTimeout() < 15) {
            Integer defaultScrapeTimeout = Integer.valueOf(sysConfigService.getValue(Constant.SYSCONFIG_KEY_SCRAPETIMEOUT));
            alertRule.setTimeout(defaultScrapeTimeout);
        }

        // 入库
        if (alertRule.getId() == null) {
            // 默认开启
            if (alertRule.getState() == null) {
                alertRule.setState(1);
            }
            this.save(alertRule);
        } else {
            // 校验内置规则
            AlertRuleEntity ruleEntity = this.getById(alertRule.getId());
            if (ruleEntity != null && ruleEntity.getBuildIn().equals(1)) {
                throw new NZException(RCode.ALERTRULE_BUILDIN_CAN_NOT_UPDATE);
            }

            this.updateById(alertRule);
        }
        return alertRule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer[] ids) {
        ValidateUtils.is(ids).notNull(RCode.ALERTMSG_IDS_ISNULL);
        // 校验内置
        List<Integer> idList = Arrays.asList(ids);
        this.validateBulitinRule(idList);
        // 删除
        this.removeByIds(idList);

        // 删除关联的告警消息
        alertMessageService.removeByRuleIds(Arrays.asList(ids));

        // 删除用户收藏数据
        sysUserStarredService.remove(
                new LambdaQueryWrapper<SysUserStarredEntity>()
                        .eq(SysUserStarredEntity::getType, "alertRule")
                        .in(SysUserStarredEntity::getTid, idList)
        );
    }


    @Override
    public void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_ALERTRULE_EXPORT_HEADER);
        String fileName = "AlertRuleTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
    }

    @Override
    public Map importAlertRuleData(MultipartFile multipartFile, Map<String, Object> params) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_ALERTRULE_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        // 表头信息 国际化之后的
        List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

        String fileExtension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
        params.put("fileExtension", fileExtension);

        // 返回统计信息
        Map statMap = this.saveRulesFromImportDataList(importDataList, headerInfos, params);
        return statMap;
    }

    private Map saveRulesFromImportDataList(List<Map<Integer, String>> importDataList, List<String> headerInfos, Map<String, Object> params) {
        String seq = StrUtil.uuid();
        params.put("seq", seq);
        List<AlertNotifyMethod> methodList = methodService.list();
        List<SysUserEntity> userList = sysUserService.list();

        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");
        // 遇到错误是否继续导入，可选值： 0：不继续    1：继续    默认值：1
        String ignoreError = Tool.ObjectUtil.defaultIfEmpty((String) params.get("ignoreError"), "1");
        // 创建导入校验对象
        ImportValidateUtil validateUtil = ImportValidateUtil.builder()
                .existed(existed).headerInfos(headerInfos)
                .build();
        Integer addNum = 0, updateNum = 0;
        // 记录导入失败内容
        List<Map<Integer, String>> failContentList = ListUtil.list(true);
        for (int i = 0; i < importDataList.size(); i++) {
            int lineNum = i + 1;
            Map<Integer, String> dataMap = importDataList.get(i);

            String name = dataMap.get(AlertRuleImportEnum.NAME.getIndex());
            validateUtil.validateRequired(name, lineNum, AlertRuleImportEnum.NAME.getIndex());
            boolean indexDuplicate = validateUtil.validateIndexDuplicate("alert_rule", "name", name, lineNum, AlertRuleImportEnum.NAME.getIndex());
            // 索引列重复 & 忽略标识，提示忽略本行
            if (BooleanUtil.and(indexDuplicate, StrUtil.equals("2", existed))) {
                validateUtil.ignoreThisRow(lineNum, AlertRuleImportEnum.NAME.getIndex());
                failContentList.add(dataMap);
                continue;
            }
            String interval = dataMap.get(AlertRuleImportEnum.INTERVAL.getIndex());
            Integer intervalIntVal = validateUtil.validateInteger(interval, lineNum, AlertRuleImportEnum.INTERVAL.getIndex());

            String timeout = dataMap.get(AlertRuleImportEnum.TIMEOUT.getIndex());
            Integer timeoutIntVal = validateUtil.validateInteger(timeout, lineNum, AlertRuleImportEnum.TIMEOUT.getIndex());

            String type = dataMap.get(AlertRuleImportEnum.TYPE.getIndex());
            validateUtil.validateRequired(type, lineNum, AlertRuleImportEnum.TYPE.getIndex());
            validateUtil.validateEnumValArr(type, Tool.ListUtil.of("1", "2", "3"), lineNum, AlertRuleImportEnum.TYPE.getIndex());

            String expr = dataMap.get(AlertRuleImportEnum.EXPR.getIndex());
            validateUtil.validateRequired(expr, lineNum, AlertRuleImportEnum.EXPR.getIndex());

            String unit = dataMap.get(AlertRuleImportEnum.UNIT.getIndex());
            String last = dataMap.get(AlertRuleImportEnum.LAST.getIndex());
            String condition = dataMap.get(AlertRuleImportEnum.CONDITION.getIndex());
            if (StrUtil.equals("3", type)) {
                validateUtil.validateRegex(expr, "^(\\d{1,9}\\.){0,}[\\d]+$", lineNum, AlertRuleImportEnum.EXPR.getIndex());
            } else {
                List<String> unitEnumList = Tool.ListUtil.toList(Constant.ALERT_RULE_UNIT_MAP.keySet());
                validateUtil.validateRequired(unit, lineNum, AlertRuleImportEnum.UNIT.getIndex());
                validateUtil.validateEnumValArr(unit, unitEnumList, lineNum, AlertRuleImportEnum.UNIT.getIndex());

                validateUtil.validateRequired(last, lineNum, AlertRuleImportEnum.LAST.getIndex());
                validateUtil.validateDouble(last, lineNum, AlertRuleImportEnum.LAST.getIndex());
            }

            // condition 必填 & jsonArr 格式
            validateUtil.validateRequired(condition, lineNum, AlertRuleImportEnum.CONDITION.getIndex());
            validateUtil.validateJsonArray(condition, lineNum, AlertRuleImportEnum.CONDITION.getIndex());

            String extLabels = dataMap.get(AlertRuleImportEnum.EXT_LABELS.getIndex());
            validateUtil.validateJsonArray(extLabels, lineNum, AlertRuleImportEnum.EXT_LABELS.getIndex());

            String summary = dataMap.get(AlertRuleImportEnum.SUMMARY.getIndex());
            validateUtil.validateRequired(summary, lineNum, AlertRuleImportEnum.SUMMARY.getIndex());

            String receiver = dataMap.get(AlertRuleImportEnum.RECEIVER.getIndex());
            String method = dataMap.get(AlertRuleImportEnum.NOTICES_METHOD.getIndex());
            if (StrUtil.isNotEmpty(receiver)) {
                validateUtil.validateRequired(method, lineNum, AlertRuleImportEnum.NOTICES_METHOD.getIndex());

                List<String> list = StrUtil.split(receiver, ",").stream().distinct().collect(Collectors.toList());
                List<String> receiverUserNameList = userList.stream().map(SysUserEntity::getName).distinct().filter(list::contains).collect(Collectors.toList());

                if (ObjectUtil.notEqual(list.size(), receiverUserNameList.size()))
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_RELATED_OBJ_NOT_EXIST.setParam(AlertRuleImportEnum.RECEIVER.getIndex()));
                receiver = userList.stream().filter(user -> list.contains(user.getName())).map(user -> user.getId().toString()).collect(Collectors.joining(","));
            }

            if (StrUtil.isNotEmpty(method)) {
                List<String> list = StrUtil.split(method, ",").stream().distinct().collect(Collectors.toList());
                List<String> receiverMethodNameList = methodList.stream().map(AlertNotifyMethod::getName).distinct().filter(list::contains).collect(Collectors.toList());

                if (ObjectUtil.notEqual(list.size(), receiverMethodNameList.size()))
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_RELATED_OBJ_NOT_EXIST.setParam(AlertRuleImportEnum.NOTICES_METHOD.getIndex()));
                method = methodList.stream().filter(m -> list.contains(m.getName())).map(m -> m.getId().toString()).collect(Collectors.joining(","));
            }

            String state = dataMap.get(AlertRuleImportEnum.STATE.getIndex());
            state = StrUtil.emptyToDefault(state, "1");
            validateUtil.validateEnumValArr(state, Tool.ListUtil.of("0", "1"), lineNum, AlertRuleImportEnum.STATE.getIndex());

            String autoExpired = dataMap.get(AlertRuleImportEnum.AUTO_EXPIRED.getIndex());
            autoExpired = StrUtil.emptyToDefault(autoExpired, "1");
            validateUtil.validateEnumValArr(autoExpired, Tool.ListUtil.of("0", "1"), lineNum, AlertRuleImportEnum.AUTO_EXPIRED.getIndex());

            // 默认开启 进行校验 day start end format
            String schedEnable = dataMap.get(AlertRuleImportEnum.SCHED_ENABLE.getIndex());
            schedEnable = StrUtil.emptyToDefault(schedEnable, "1");
            validateUtil.validateEnumValArr(schedEnable, Tool.ListUtil.of("0", "1"), lineNum, AlertRuleImportEnum.SCHED_ENABLE.getIndex());

            String notifyActive = dataMap.get(AlertRuleImportEnum.NOTIFY_ACTIVE.getIndex());
            notifyActive = StrUtil.emptyToDefault(notifyActive, "1");
            validateUtil.validateEnumValArr(notifyActive, Tool.ListUtil.of("0", "1"), lineNum, AlertRuleImportEnum.NOTIFY_ACTIVE.getIndex());

            String notifyExpired = dataMap.get(AlertRuleImportEnum.NOTIFY_EXPIRED.getIndex());
            notifyExpired = StrUtil.emptyToDefault(notifyExpired, "1");
            validateUtil.validateEnumValArr(notifyExpired, Tool.ListUtil.of("0", "1"), lineNum, AlertRuleImportEnum.NOTIFY_EXPIRED.getIndex());

            String schedDays = dataMap.get(AlertRuleImportEnum.SCHED_DAYS.getIndex());
            String schedStime = dataMap.get(AlertRuleImportEnum.SCHED_STIME.getIndex());
            String schedEtime = dataMap.get(AlertRuleImportEnum.SCHED_ETIME.getIndex());
            schedDays = StrUtil.emptyToDefault(schedDays, "1,2,3,4,5,6,7");
            schedStime = StrUtil.emptyToDefault(schedStime, "00:00");
            schedEtime = StrUtil.emptyToDefault(schedEtime, "23:59");
            if(schedStime.length() < 5) schedStime = "0" + schedStime;
            if(schedEtime.length() < 5) schedEtime = "0" + schedEtime;

            StrUtil.split(schedDays, ",").stream().distinct().forEach(s -> {
                validateUtil.validateRegex(s, "^[1-7]$", lineNum, AlertRuleImportEnum.SCHED_DAYS.getIndex());
            });

            validateUtil.validateRegex(schedStime, "^(?:[01]\\d|2[0-3])(?::[0-5]\\d)$", lineNum, AlertRuleImportEnum.SCHED_STIME.getIndex());
            validateUtil.validateRegex(schedEtime, "^(?:[01]\\d|2[0-3])(?::[0-5]\\d)$", lineNum, AlertRuleImportEnum.SCHED_ETIME.getIndex());

            Date schedStimeDate = validateUtil.validateDateFormat(schedStime, "HH:mm", lineNum, AlertRuleImportEnum.SCHED_STIME.getIndex());
            Date schedEtimeDate = validateUtil.validateDateFormat(schedEtime, "HH:mm", lineNum, AlertRuleImportEnum.SCHED_ETIME.getIndex());
            if (ObjectUtil.isAllNotEmpty(schedStimeDate, schedEtimeDate)) {// 结束时间应该晚于开始时间
                if (DateUtil.compare(schedStimeDate, schedEtimeDate) > 0)
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(AlertRuleImportEnum.SCHED_ETIME.getIndex()));
            }

            List<Map<String, String>> errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
            if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
                // 结束本次导入，自导入行开始 到结尾数据行，提示错误信息
                for (int j = i; j < importDataList.size(); j++) {
                    validateUtil.recordImportAttrErr(j + 1, RCode.IMPORT_ERROR_STOP);
                    failContentList.add(importDataList.get(j));
                }
                break;
            }

            String description = dataMap.get(AlertRuleImportEnum.DESCRIPTION.getIndex());
            String trbShotStr = dataMap.get(AlertRuleImportEnum.TROUBLE_SHOOTING.getIndex());
            if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
                try {
                    AlertRuleEntity ruleEntity = new AlertRuleEntity();
                    ruleEntity.setName(name);
                    ruleEntity.setExpr(expr);
                    ruleEntity.setUnit(Constant.ALERT_RULE_UNIT_MAP.get(StrUtil.emptyToDefault(unit, "none")));
                    ruleEntity.setLast(StrUtil.isNotEmpty(last) ? Double.valueOf(last).intValue() : null);
                    ruleEntity.setCondition(condition);
                    ruleEntity.setExtLabels(extLabels);
                    ruleEntity.setSummary(summary);
                    ruleEntity.setDescription(description);
                    ruleEntity.setReceiver(receiver);
                    ruleEntity.setMethod(method);
                    ruleEntity.setState(Integer.valueOf(state));
                    ruleEntity.setAutoExpired(Integer.valueOf(autoExpired));
                    ruleEntity.setSchedEnable(Integer.valueOf(schedEnable));
                    ruleEntity.setSchedDays(schedDays);
                    ruleEntity.setSchedStime(schedStime);
                    ruleEntity.setSchedEtime(schedEtime);
                    ruleEntity.setNotifyActive(Integer.valueOf(notifyActive));
                    ruleEntity.setNotifyExpired(Integer.valueOf(notifyExpired));
                    ruleEntity.setTrbShot(trbShotStr);
                    ruleEntity.setType(Integer.valueOf(type));
                    ruleEntity.setInr(intervalIntVal);
                    ruleEntity.setTimeout(timeoutIntVal);

                    this.saveOrUpdateRowData(ruleEntity, params);
                    // save or update
                    Object saveFlag = params.get("save");
                    if (ObjectUtil.isNotNull(saveFlag)) {
                        addNum++;
                    } else {
                        updateNum++;
                    }
                } catch (Exception e) {
                    failContentList.add(dataMap);
                    validateUtil.recordImportAttrErr(lineNum, RCode.ERROR);
                    log.error("Asset import roll back the transaction failure", e);
                }
            } else {
                failContentList.add(dataMap);
            }
        }
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_ALERTRULE_EXPORT_HEADER);
        String failedContentFileBase64Str = basicImportAndExportServices.getFailedContentFileBase64Str(failContentList, sysHeaderJsonStr, MapUtil.getStr(params, "fileExtension"));
        //  返回统计信息
        Map<Object, Object> statMap = Tool.MapUtil.builder()
                .put("seq", seq)
                .put("totalNum", importDataList.size())
                .put("successNum", addNum + updateNum)
                .put("failNum", failContentList.size())
                .put("addNum", addNum)
                .put("updateNum", updateNum)
                .put("failDetail", validateUtil.getImportErrMsg())
                .put("failContent", failedContentFileBase64Str)
                .build();
        return statMap;
    }

    private void saveOrUpdateRowData(AlertRuleEntity rule, Map<String, Object> params) {
        MapUtil.removeAny(params, "save", "update");
        String seq = params.get("seq").toString();
        rule.setSeq(seq);

        Integer inr = rule.getInr();
        if (ObjectUtil.isNotEmpty(inr)) {
            if (inr < 15) {
                inr = Integer.valueOf(sysConfigService.getValue(Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
            }
            rule.setInr(inr);
        }
        Integer timeout = rule.getTimeout();
        if (ObjectUtil.isNotEmpty(timeout)) {
            if (timeout < 15) {
                timeout = Integer.valueOf(sysConfigService.getValue(Constant.SYSCONFIG_KEY_SCRAPETIMEOUT));
            }
            rule.setTimeout(timeout);
        }

        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore   在此只需要处理更新即可
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");
        AlertRuleEntity oldRule = this.getOne(new LambdaQueryWrapper<AlertRuleEntity>().eq(AlertRuleEntity::getName, rule.getName()));
        if (StrUtil.equals("1", existed) && oldRule != null) {
            params.put("update", true);
            // update
            rule.setId(oldRule.getId());

            this.updateById(rule);
        } else {
            params.put("save", true);
            // insert
            this.save(rule);
        }
    }

    @Override
    public void export(HttpServletResponse response, Map<String, Object> params) throws IOException {
        String maxLine = sysConfigService.getValue(Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE);
        long exportMaxLine = Integer.parseInt(StrUtil.emptyToDefault(maxLine, "10000"));
        // 导出最大条数限制
        Object limit = params.get(Constant.LIMIT) == null ? Constant.PAGESIZE : params.get(Constant.LIMIT);
        Long pageSize = Long.parseLong(limit.toString());
        if (pageSize == -1) {
            // 导出所有直接限制 pageSize
            params.put(Constant.PAGE, "1");
            params.put(Constant.LIMIT, Long.toString(exportMaxLine));
        } else if (pageSize > exportMaxLine) {
            // 分页超过最大限制 则设置最大限制为 系统配置值，和 pageSize == -1 判断区别在于 pageNo
            params.put(Constant.LIMIT, Long.toString(exportMaxLine));
        }

        // 导出查询与列表查询一致
        PageUtils page = this.queryPage(params);
        List<AlertRuleEntity> list = (List<AlertRuleEntity>) page.getList();

        List<List<String>> exportDataList = this.handleExportDataList(list);

        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_ALERTRULE_EXPORT_HEADER);
        String fileName = "AlterRules";
        String formatStr = params.get("format").toString();
        basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
    }

    @Override
    public void cancelImport(String seq) {
        this.remove(new QueryWrapper<AlertRuleEntity>().lambda().eq(AlertRuleEntity::getSeq, seq));
    }

    private List<List<String>> handleExportDataList(List<AlertRuleEntity> list) {
        List<List<String>> exportDataList = new ArrayList<>();

        List<SysUserEntity> sysUserList = sysUserService.list();
        Map<Long, String> userIdAndNameMap = sysUserList.stream().collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getName));
        List<AlertNotifyMethod> methodList = methodService.list();
        Map<Integer, String> methodIdAndNameMap = methodList.stream().collect(Collectors.toMap(AlertNotifyMethod::getId, AlertNotifyMethod::getName));
        for (AlertRuleEntity rule : list) {
            // 内置告警不导出
            if (rule.getBuildIn() != null && rule.getBuildIn().equals(1)) continue;
            List<String> tempList = new ArrayList<>();
            tempList.add(rule.getName());
            tempList.add(ObjectUtil.defaultIfNull(rule.getType(), "").toString());
            tempList.add(ObjectUtil.defaultIfNull(rule.getInr(), "").toString());
            tempList.add(ObjectUtil.defaultIfNull(rule.getTimeout(), "").toString());
            tempList.add(rule.getExpr());
            tempList.add(rule.getCondition());
            tempList.add(StrUtil.nullToDefault(rule.getExtLabels(), ""));
            tempList.add(getUnitDescrByVal(rule.getUnit()));
            tempList.add(ObjectUtil.defaultIfNull(rule.getLast(),"").toString());
            tempList.add(rule.getSummary());
            tempList.add(rule.getDescription());
            String receiveIds = rule.getReceiver();
            if (StringUtils.isNotEmpty(receiveIds)) {
                String receiveNames = "";
                for (String id : Arrays.asList(receiveIds.split(","))) {
                    receiveNames += userIdAndNameMap.get(Long.valueOf(id)) + ",";
                }
                if (StringUtils.isNotEmpty(receiveNames)) {
                    receiveNames = receiveNames.substring(0, receiveNames.length() - 1);
                }
                tempList.add(receiveNames);
            } else {
                tempList.add(StrUtil.EMPTY);
            }

            String methodIds = rule.getMethod();
            if (StringUtils.isNotEmpty(methodIds)) {
                String methodNames = "";
                for (String id : Arrays.asList(methodIds.split(","))) {
                    methodNames += methodIdAndNameMap.get(Integer.valueOf(id)) + ",";
                }
                if (StringUtils.isNotEmpty(methodNames)) {
                    methodNames = methodNames.substring(0, methodNames.length() - 1);
                }
                tempList.add(methodNames);
            } else {
                tempList.add(StrUtil.EMPTY);
            }
            tempList.add(ObjectUtil.defaultIfNull(rule.getState(),"").toString());
            tempList.add(ObjectUtil.defaultIfNull(rule.getAutoExpired(),"").toString());
            tempList.add(ObjectUtil.defaultIfNull(rule.getSchedEnable(),"").toString());
            tempList.add(rule.getSchedDays());
            tempList.add(rule.getSchedStime());
            tempList.add(rule.getSchedEtime());
            tempList.add(ObjectUtil.defaultIfNull(rule.getNotifyActive(),"").toString());
            tempList.add(ObjectUtil.defaultIfNull(rule.getNotifyExpired(),"").toString());
            tempList.add(StrUtil.emptyToDefault(rule.getTrbShot(), ""));
            exportDataList.add(tempList);
        }
        return exportDataList;
    }

    private String getUnitDescrByVal(Integer unitVal) {
        String unitDescr = null;
        for (Map.Entry<String, Integer> entry : Constant.ALERT_RULE_UNIT_MAP.entrySet()) {
            if (entry.getValue().equals(unitVal)) {
                unitDescr = entry.getKey();
                break;
            }
        }
        return unitDescr;
    }

    private void validateParam(AlertRuleEntity alertRule) {
        Integer type = alertRule.getType();
        List<Integer> typeEnumList = Arrays.asList(AlertRuleType.values()).stream().map(AlertRuleType::getValue).collect(Collectors.toList());
        if (!typeEnumList.contains(type)) {
            throw new NZException(RCode.ALERTRULE_TYPE_INCORRECT);
        }

        switch (AlertRuleType.getRuleType(type)) {
            case METRICS:
            case LOGS: {
                Integer unit = alertRule.getUnit();
                ValidateUtils.is(unit).notNull(RCode.ALERTRULE_UNIT_ISNULL);
                List<Integer> unitList = new ArrayList(Constant.ALERT_RULE_UNIT_MAP.values());
                if (unit < Collections.min(unitList) || unit > Collections.max(unitList)) {
                    throw new NZException(RCode.ALERTRULE_UNIT_INVALIDE);
                }
                break;
            }
            case SNMP_TRAP: {
                String oid = alertRule.getExpr();
                if (!RegexUtil.test("^(\\d{1,9}\\.){0,}[\\d]+$", oid)) {
                    throw new NZException(RCode.ALERTRULE_OIDFORMAT_ERROR);
                }
                break;
            }
            default:
                break;
        }

        // 校验 method 是否存在
        String method = alertRule.getMethod();
        if (StringUtils.isNotEmpty(method)) {
            List<AlertNotifyMethod> methodList = methodService.list();
            List<Integer> methodIdList = methodList.stream().map(AlertNotifyMethod::getId).collect(Collectors.toList());

            Arrays.asList(method.split(",")).stream().forEach(id -> {
                try {
                    Integer aint = Integer.valueOf(id);
                    if (!methodIdList.contains(aint)) {
                        // method 不存在
                        throw new NZException(RCode.ALERTRULE_METHOD_NOTFOUND);
                    }
                } catch (NumberFormatException e) {
                    log.error(e);
                    throw new NZException(RCode.ALERTRULE_METHOD_NOTFOUND);
                }
            });
        }

        Integer state = alertRule.getState();
        if (state != null) {
            List<Integer> switchList = Arrays.asList(new Integer[]{1, 0});
            if (!switchList.contains(state)) {
                throw new NZException(RCode.ALERTRULE_STATE_INCORRECT);
            }
        }

        // 校验 开关相关参数是否正确
        List<Integer> enableList = Arrays.asList(new Integer[]{0, 1});
        Integer autoExpired = alertRule.getAutoExpired();
        if (autoExpired != null && !enableList.contains(autoExpired)) {
            throw new NZException(RCode.ALERTRULE_AUTOEXPIRED_INCORRECT);
        }
        Integer notifyActive = alertRule.getNotifyActive();
        if (notifyActive != null && !enableList.contains(notifyActive)) {
            throw new NZException(RCode.ALERTRULE_NOTIFYACTIVE_INCORRECT);
        }

        Integer notifyExpired = alertRule.getNotifyExpired();
        if (notifyExpired != null && !enableList.contains(notifyExpired)) {
            throw new NZException(RCode.ALERTRULE_NOTIFYEXPIRED_INCORRECT);
        }

        Integer schedEnable = alertRule.getSchedEnable();
        if (schedEnable != null) {
            if (!enableList.contains(schedEnable)) {
                throw new NZException(RCode.ALERTRULE_SCHEDENABLE_INCORRECT);
            }
            if (1 == schedEnable) {
                // 校验 schedDays schedStime schedEtime
                ValidateUtils.is(alertRule.getSchedDays()).notNull(RCode.ALERTRULE_SCHEDDAYS_ISNULL)
                        .and(alertRule.getSchedStime()).notNull(RCode.ALERTRULE_SCHEDSTIME_ISNULL)
                        .and(alertRule.getSchedEtime()).notNull(RCode.ALERTRULE_SCHEDETIME_ISNULL);
                List<String> tempList = Arrays.asList(alertRule.getSchedDays().split(","));
                for (String s : tempList) {
                    if (!RegexUtil.test("^[1-7]$", s)) {
                        throw new NZException(RCode.ALERTRULE_SCHEDDAYS_INCORRECT);
                    }
                }

                // 时间格式
                DateFormat formatter = new SimpleDateFormat("HH:mm");
                if (!RegexUtil.test("^(?:[01]\\d|2[0-3])(?::[0-5]\\d)$", alertRule.getSchedStime())) {
                    throw new NZException(RCode.ALERTRULE_SCHEDSTIME_INCORRECT);
                }
                if (!RegexUtil.test("^(?:[01]\\d|2[0-3])(?::[0-5]\\d)$", alertRule.getSchedEtime())) {
                    throw new NZException(RCode.ALERTRULE_SCHEDETIME_INCORRECT);
                }
                try {
                    Date schedStimeDate = formatter.parse(alertRule.getSchedStime());
                    Date schedEtimeDate = formatter.parse(alertRule.getSchedEtime());
                    // 结束时间应该晚于开始时间
                    if (DateUtil.compare(schedStimeDate, schedEtimeDate) > 0) {
                        throw new NZException(RCode.ALERTRULE_SCHEDETIME_ERROR);
                    }
                } catch (ParseException e) {
                    log.error(e);
                }
            }
        }

        // condition
        String condition = alertRule.getCondition();
        boolean isConditionJsonArray = JSONUtil.isTypeJSONArray(condition);
        if (BooleanUtil.negate(isConditionJsonArray)) {
            throw new NZException(RCode.ALERTRULE_CONDITION_FORMAT);
        }
        List<AlertRuleEntity.ConditionItem> conditionItems = alertRule.getConditionItems();
        ValidateUtils.is(conditionItems).notNull(RCode.ALERTRULE_CONDITION_ISNULL);

        for (AlertRuleEntity.ConditionItem item : conditionItems) {
            ValidateUtils.is(item.getId()).notNull(RCode.ALERTSEVERITY_ID_ISNULL)
                    .and(item.getWeight()).notNull(RCode.ALERTRULE_CONDITION_ID_ISNULL)
                    .and(item.getOperator()).notNull(RCode.ALERTRULE_CONDITION_OPERATOR_ISNULL)
                    .and(item.getValue()).notNull(RCode.ALERTRULE_CONDITION_VALUE_ISNULL);
            if (ObjectUtil.equal(type, AlertRuleType.SNMP_TRAP.getValue())) {
                ValidateUtils.is(item.getOperator()).equalsIgnoreCase("=~", RCode.ALERTRULE_SNMPTRAP_OPERATOR_ERROR);
            } else {
                List<String> operatorEnumList = Arrays.asList(new String[]{"==", "!=", ">", "<", ">=", "<="});
                if (!operatorEnumList.contains(item.getOperator())) {
                    throw new NZException(RCode.ALERTRULE_OPERATOR_INVALIDE);
                }
            }
        }

        // extLabels
        String extLabels = alertRule.getExtLabels();
        if (StrUtil.isNotEmpty(extLabels)) {
            boolean isExtLabelsJsonArray = JSONUtil.isTypeJSONArray(condition);
            if (BooleanUtil.negate(isExtLabelsJsonArray)) {
                throw new NZException(RCode.ALERTRULE_EXTLABELS_FORMAT);
            }
        }
    }

	@Override
	public AlertRuleEntity queryAlertRule(Integer id) {
		AlertRuleEntity alertRule = alertRuleDao.selectById(id);
        ValidateUtils.is(alertRule).notNull(RCode.ALERTRULE_NOT_EXIST);
		
        List<AlertMessageEntity> alertMessageList = alertMessageService.list(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getRuleId, id));
        alertRule.setAlertNum(alertMessageList.size());
        List<Map<String, Object>> alert = alertMessageService.getAlertsStatBySeverity("rule",id);
        alertRule.setAlert(alert);

        // 获取所有用户
        List<SysUserEntity> userList = sysUserService.list();
        Map<Long, String> userIdAndName = userList.stream().collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getName));

        String receiver = alertRule.getReceiver();
        if (StringUtils.isNotEmpty(receiver)) {
            List<SysUserEntity> list = new ArrayList<>();
            Arrays.asList(receiver.split(",")).stream().forEach(userId -> {
                SysUserEntity userEntity = new SysUserEntity();
                userEntity.setId(Long.valueOf(userId));
                userEntity.setName(userIdAndName.get(Long.valueOf(userId)));
                list.add(userEntity);
            });
            alertRule.setReceivers(list);
        }

        // 收藏标识 0：未收藏 1：已收藏
        List<SysUserStarredEntity> tUserStatList = sysUserStarredService.list(new LambdaQueryWrapper<SysUserStarredEntity>()
                .select(SysUserStarredEntity::getId)
                .eq(SysUserStarredEntity::getTid, alertRule.getId())
                .eq(SysUserStarredEntity::getType, "alertRule")
                .eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId())
                .last("limit 1")
        );
        alertRule.setStarred(CollUtil.isEmpty(tUserStatList) ? 0 : 1);
		return alertRule;
	}
}
