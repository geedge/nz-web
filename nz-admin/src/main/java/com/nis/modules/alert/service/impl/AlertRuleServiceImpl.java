package com.nis.modules.alert.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.smartvalidate.utils.RegexUtil;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.*;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertNotifyMethodService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.alert.service.AlertSeverityService;
import com.nis.modules.stat.service.StatService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
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

    private static Log log = Log.get();

    @Autowired
    private AlertRuleDao alertRuleDao;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private YamlUtil yamlUtil;

    @Autowired
    private AlertNotifyMethodService methodService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AlertSeverityService alertSeverityService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private StatService statService;

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


        // 仅metrics类型需要校验rule表达式  logs和snmp trap暂不支持
        if (1 == alertRule.getType()) {
            // 校验expr
            boolean checkRule = false;
            try {
                checkRule = yamlUtil.checkRule(YamlUtil.alertRuleEntityToYamlMap(alertRule));
            } catch (IOException e) {
                log.error(String.format("expr校验失败,rule信息是：%s,错误信息是：%s", alertRule.toString(), e));
            }

            if (!checkRule) {
                throw new NZException(RCode.ALERTRULE_EXPR_ERROR);
            }
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

        String seq = UUID.randomUUID().toString();
        params.put("seq", seq);

        // 表头信息 国际化之后的
        List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

        int totalNum = importDataList.size();
        List<Map<String, String>> failDetailList = this.saveRulesFromImportDataList(importDataList, headerInfos, params);

        // 从错误信息中获取失败导入行数量
        int failNum = (int) failDetailList.stream().map(map -> Integer.valueOf(map.get("lineNo"))).distinct().count();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seq);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", totalNum - failNum);
        resultMap.put("failNum", failNum);
        resultMap.put("failDetail", failDetailList);
        return resultMap;
    }

    private List<Map<String, String>> saveRulesFromImportDataList(List<Map<Integer, String>> importDataList, List<String> headerInfos, Map<String, Object> params) {
        List<AlertNotifyMethod> methodList = methodService.list();
        List<SysUserEntity> userList = sysUserService.list();

        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");
        // 创建导入校验对象
        ImportValidateUtil validateUtil = ImportValidateUtil.builder()
                .existed(existed).headerInfos(headerInfos)
                .build();
        for (int i = 0; i < importDataList.size(); i++) {
            int lineNum = i + 1;
            Map<Integer, String> dataMap = importDataList.get(i);

            String name = dataMap.get(AlertRuleImportEnum.NAME.getIndex());
            validateUtil.validateRequired(name, lineNum, AlertRuleImportEnum.NAME.getIndex());
            boolean indexDuplicate = validateUtil.validateIndexDuplicate("alert_rule", "name", name, lineNum, AlertRuleImportEnum.NAME.getIndex());
            // 索引列重复 & 忽略标识，提示忽略本行
            if (BooleanUtil.and(indexDuplicate, StrUtil.equals("2", existed))) {
                validateUtil.ignoreThisRow(lineNum, AlertRuleImportEnum.NAME.getIndex());
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

            String operator = dataMap.get(AlertRuleImportEnum.OPERATOR.getIndex());
            operator = operator.replaceAll(" ", "");
            String threshold = dataMap.get(AlertRuleImportEnum.THRESHOLD.getIndex());
            String unit = dataMap.get(AlertRuleImportEnum.UNIT.getIndex());
            String last = dataMap.get(AlertRuleImportEnum.LAST.getIndex());
            if (StrUtil.equals("3", type)) {
                validateUtil.validateRegex(expr, "^(\\d{1,9}\\.){0,}[\\d]+$", lineNum, AlertRuleImportEnum.EXPR.getIndex());
            } else {
                validateUtil.validateRequired(threshold, lineNum, AlertRuleImportEnum.THRESHOLD.getIndex());
                validateUtil.validateDouble(threshold, lineNum, AlertRuleImportEnum.THRESHOLD.getIndex());

                List<String> unitEnumList = Tool.ListUtil.toList(Constant.ALERT_RULE_UNIT_MAP.keySet());
                validateUtil.validateRequired(unit, lineNum, AlertRuleImportEnum.UNIT.getIndex());
                validateUtil.validateEnumValArr(unit, unitEnumList, lineNum, AlertRuleImportEnum.UNIT.getIndex());

                validateUtil.validateRequired(operator, lineNum, AlertRuleImportEnum.OPERATOR.getIndex());
                validateUtil.validateEnumValArr(operator, Constant.RULE_OPERATORS, lineNum, AlertRuleImportEnum.OPERATOR.getIndex());

                validateUtil.validateRequired(last, lineNum, AlertRuleImportEnum.LAST.getIndex());
                validateUtil.validateDouble(last, lineNum, AlertRuleImportEnum.LAST.getIndex());
            }

            String severity = dataMap.get(AlertRuleImportEnum.SEVERITY.getIndex());
            validateUtil.validateRequired(severity, lineNum, AlertRuleImportEnum.SEVERITY.getIndex());
            validateUtil.validateRelObjExits("alert_severity_conf", "name", severity, false, lineNum, AlertRuleImportEnum.SEVERITY.getIndex());

            String summary = dataMap.get(AlertRuleImportEnum.SUMMARY.getIndex());
            validateUtil.validateRequired(summary, lineNum, AlertRuleImportEnum.SUMMARY.getIndex());

            String receiver = dataMap.get(AlertRuleImportEnum.RECEIVER.getIndex());
            String method = dataMap.get(AlertRuleImportEnum.NOTICES_METHOD.getIndex());
            if (StrUtil.isNotEmpty(receiver)) {
                validateUtil.validateRequired(method, lineNum, AlertRuleImportEnum.NOTICES_METHOD.getIndex());

                List<String> list = Arrays.stream(StrUtil.split(receiver, ",")).distinct().collect(Collectors.toList());
                List<String> receiverUserNameList = userList.stream().map(SysUserEntity::getName).distinct().filter(list::contains).collect(Collectors.toList());

                if (ObjectUtil.notEqual(list.size(), receiverUserNameList.size()))
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_RELATED_OBJ_NOT_EXIST.setParam(AlertRuleImportEnum.RECEIVER.getIndex()));
                receiver = userList.stream().filter(user -> list.contains(user.getName())).map(user -> user.getId().toString()).collect(Collectors.joining(","));
            }

            if (StrUtil.isNotEmpty(method)) {
                List<String> list = Arrays.stream(StrUtil.split(method, ",")).distinct().collect(Collectors.toList());
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

            Arrays.stream(StrUtil.split(schedDays, ",")).distinct().forEach(s -> {
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
            if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
                AlertSeverityConf alertSeverityConf = alertSeverityService.getOne(new LambdaQueryWrapper<AlertSeverityConf>().eq(AlertSeverityConf::getName, severity));

                String description = dataMap.get(AlertRuleImportEnum.DESCRIPTION.getIndex());
                String trbShotStr = dataMap.get(AlertRuleImportEnum.TROUBLE_SHOOTING.getIndex());
                AlertRuleEntity ruleEntity = new AlertRuleEntity();
                ruleEntity.setName(name);
                ruleEntity.setExpr(expr);
                ruleEntity.setOperator(operator);
                ruleEntity.setThreshold(StrUtil.isNotEmpty(threshold) ? Double.valueOf(threshold) : null);
                ruleEntity.setUnit(Constant.ALERT_RULE_UNIT_MAP.get(StrUtil.emptyToDefault(unit, "none")));
                ruleEntity.setLast(StrUtil.isNotEmpty(last) ? Double.valueOf(last).intValue() : null);
                ruleEntity.setSeverityId(alertSeverityConf.getId());
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
                // 仅metrics类型需要校验rule表达式  logs和snmp trap暂不支持
                if (StrUtil.equals("1", type)) {
                    try {
                        boolean checkRule = yamlUtil.checkRule(YamlUtil.alertRuleEntityToYamlMap(ruleEntity));
                        if (!checkRule) {
                            validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(AlertRuleImportEnum.EXPR.getIndex()));
                        }
                    } catch (Exception e) {
                        log.error(e, "expr validate error,rule info: {}", JSONObject.toJSONString(ruleEntity));
                        validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(AlertRuleImportEnum.EXPR.getIndex()));
                    }
                }

                // 遇到错误是否继续导入，可选值： 0：不继续    1：继续    默认值：1
                String ignoreError = Tool.ObjectUtil.defaultIfEmpty((String) params.get("ignoreError"), "1");

                errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
                if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
                    break;
                }

                if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
                    try {
                        this.saveOrUpdateRowData(ruleEntity, params);
                    } catch (Exception e) {
                        validateUtil.recordImportAttrErr(lineNum, RCode.ERROR);
                        log.error("Asset import roll back the transaction failure", e);
                    }
                }
            }
        }
        return validateUtil.getImportErrMsg();
    }

    private void saveOrUpdateRowData(AlertRuleEntity rule, Map<String, Object> params) {
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
            // update
            rule.setId(oldRule.getId());

            this.updateById(rule);
        } else {
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
        String receiveIds, receiveNames;
        for (AlertRuleEntity rule : list) {
            // 内置告警不导出
            if (rule.getBuildIn() != null && rule.getBuildIn().equals(1)) continue;
            List<String> tempList = new ArrayList<>();
            tempList.add(rule.getName());
            tempList.add(ObjectUtil.defaultIfNull(rule.getType(), "").toString());
            tempList.add(ObjectUtil.defaultIfNull(rule.getInr(), "").toString());
            tempList.add(ObjectUtil.defaultIfNull(rule.getTimeout(), "").toString());
            tempList.add(rule.getExpr());
            tempList.add(rule.getOperator());
            tempList.add(rule.getThreshold() == null ? "" : rule.getThreshold() + "");
            tempList.add(getUnitDescrByVal(rule.getUnit()));
            tempList.add(ObjectUtil.defaultIfNull(rule.getLast(),"").toString());
            tempList.add(rule.getSeverity().getName());
            tempList.add(rule.getSummary());
            tempList.add(rule.getDescription());
            receiveIds = rule.getReceiver();
            if (StringUtils.isNotEmpty(receiveIds)) {
                receiveNames = "";
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
        String operator = alertRule.getOperator();
        Integer unit = alertRule.getUnit();
        ValidateUtils.is(alertRule.getName()).notNull(RCode.ALERTRULE_NAME_ISNULL).maxLength(64)
                .and(alertRule.getExpr()).notNull(RCode.ALERTRULE_EXPR_ISNULL).maxLength(512)
                .and(alertRule.getLast()).notNull(RCode.ALERTRULE_LAST_ISNULL).min(1, RCode.ALERTRULE_LAST_ISNOTPOSITIVE)
                .and(alertRule.getSeverityId()).notNull(RCode.ALERTRULE_SEVERITY_ISNULL)
                .and(alertRule.getSummary()).notNull(RCode.ALERTRULE_SUMMARY_ISNULL).maxLength(512)
                .and(alertRule.getDescription()).maxLength(512)
                .and(alertRule.getType()).notNull(RCode.ALERTRULE_TYPE_ISNULL);

        List<Integer> typeList = Arrays.asList(new Integer[]{1, 2, 3});
        if (!typeList.contains(alertRule.getType())) {
            throw new NZException(RCode.ALERTRULE_TYPE_INCORRECT);
        }

        // snmp trap type
        if (3 == alertRule.getType()) {
            String oid = alertRule.getExpr();
            if (!RegexUtil.test("^(\\d{1,9}\\.){0,}[\\d]+$", oid)) {
                throw new NZException(RCode.ALERTRULE_OIDFORMAT_ERROR);
            }
        } else {
            // 除 snmp trap 类型外，threshold unit 必填
            ValidateUtils.is(alertRule.getThreshold()).notNull(RCode.ALERTRULE_THRESHOLD_ISNULL).maxLength(20)
                    .and(unit).notNull(RCode.ALERTRULE_UNIT_ISNULL)
                    .and(operator).notNull(RCode.ALERTRULE_OPERATOR_ISNULL);

            List<Integer> unitList = new ArrayList(Constant.ALERT_RULE_UNIT_MAP.values());
            if (unit < Collections.min(unitList) || unit > Collections.max(unitList)) {
                throw new NZException(RCode.ALERTRULE_UNIT_INVALIDE);
            }

            operator = operator.replaceAll(" ", "");
            alertRule.setOperator(operator);

            if (!Constant.RULE_OPERATORS.contains(operator)) {
                throw new NZException(RCode.ALERTRULE_OPERATOR_INVALIDE);
            }
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
    }

	@Override
	public AlertRuleEntity queryAlertRule(Integer id) {
		AlertRuleEntity alertRule = alertRuleDao.selectById(id);

        List<AlertMessageEntity> alertMessageList = alertMessageService.list(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getRuleId, id));
        alertRule.setAlertNum(alertMessageList.size());

        Map<Integer, Long> ruleIdAndAlertCountMap = alertMessageList.parallelStream().collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
        List<Map<String, Object>> alert = statService.getSeverityStatByRuleMsgCount(Tool.MapUtil.empty(), ruleIdAndAlertCountMap);
        alertRule.setAlert(alert);

		if(ObjectUtil.isNotEmpty(alertRule)) {
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

            // 当前用户收藏状态  1：收藏  0：未收藏
            int ruleStarredCount = sysUserStarredService.count(
                    new LambdaQueryWrapper<SysUserStarredEntity>()
                            .eq(SysUserStarredEntity::getType, "alertRule")
                            .eq(SysUserStarredEntity::getTid, alertRule.getId())
                            .eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId().intValue()));

            alertRule.setStarred(ruleStarredCount == 0 ? 0 : 1);
        }

		return alertRule;
	}

    /**
     * 通过 ruleIds 返回  告警级别  对应 ruleId map
     * k: severityName
     * v: ruleIds
     *
     * @param ids
     * @return
     */
    @Override
    public Map<String, List<Integer>> getSeverityNameAndRuleIdsMap(Integer... ids) {
        List<AlertSeverityConf> severityConfList = alertSeverityService.list();
        Map<Integer, String> severityIdAndNameMap = severityConfList.stream().collect(Collectors.toMap(AlertSeverityConf::getId, AlertSeverityConf::getName));

        List<AlertRuleEntity> ruleList = this.list(new LambdaQueryWrapper<AlertRuleEntity>().in(AlertRuleEntity::getId, Arrays.asList(ids)));
        Map<Integer, List<AlertRuleEntity>> severityIdAndRules = ruleList.stream().collect(Collectors.groupingBy(AlertRuleEntity::getSeverityId));

        Map<String, List<Integer>> severityNameAndRuleIdsMap = new HashMap<>(8);
        for (Map.Entry<Integer, String> entry : severityIdAndNameMap.entrySet()) {
            Integer severityId = entry.getKey();
            String severityName = entry.getValue();
            List<AlertRuleEntity> rules = Tool.ObjectUtil.isNull(severityIdAndRules.get(severityId)) ? new ArrayList<>() : severityIdAndRules.get(severityId);
            List<Integer> ruleIds = rules.stream().map(AlertRuleEntity::getId).collect(Collectors.toList());
            severityNameAndRuleIdsMap.put(severityName, ruleIds);
        }
        return severityNameAndRuleIdsMap;
    }
}
