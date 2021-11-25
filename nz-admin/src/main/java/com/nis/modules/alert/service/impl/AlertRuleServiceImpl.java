package com.nis.modules.alert.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.additional.query.impl.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.smartvalidate.utils.RegexUtil;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertNotifyMethod;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertSeverityConf;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertNotifyMethodService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.alert.service.AlertSeverityService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
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

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String receiverStr = (String) params.get("receiver");
        List<String> receivers = new ArrayList<>();
        if (StringUtils.isNotEmpty(receiverStr)) {
            receivers = Arrays.asList(receiverStr.split(","));
        }

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
    }


    @Override
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ALERTRULE_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        if(map.get(language) != null){
            exportHeaderMap = (Map) map.get(language);
        }else{
            throw new NZException(RCode.ALERTRULE_EXPORT_HEADER_LANGUAGE_ERROR);
        }
        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "AlertRule", true);
            String fileName = "AlertRuleExportTemplate.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("告警规则模板生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            log.error("获取告警规则导入模板失败，表头语言是：" + language + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
    }

    @Override
    public Map importAlertFromExcelFile(MultipartFile excelFile, String language) {
        if (!ExcelUtils.validateFileType(excelFile.getOriginalFilename())) {
            throw new NZException(RCode.ALERTRULE_IMPORTFILE_TYPE);
        }

        Workbook work = null;
        try {
            work = ExcelUtils.getWork(excelFile.getOriginalFilename(), excelFile.getInputStream());
        } catch (IOException e) {
            log.error("解析导入告警规则文件格式有误"+e);
            throw new NZException(RCode.ASSET_IMPORTFILE_FAILED);
        } finally {
            if (work != null) {
                IOUtils.closeQuietly(work);
            }
        }

        Sheet sheet = work.getSheetAt(0);
        // 表头校验
        SysConfigEntity config = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ALERTRULE_EXPORT_HEADER));
        HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
        boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), map.values());
        if(!headerFlag){
            throw new NZException(RCode.ASSET_EXPORT_HEADER_TEMPLATE_ERROR);
        }

        Map<String, String> exportHeader = (Map) map.get(language);
        List<String> headerInfos = new ArrayList(exportHeader.keySet());

        // 预先查询出校验数据 避免循环查表
        List<AlertRuleEntity> alertList = this.list();
        List<String> alertNameList = alertList.stream().map(AlertRuleEntity::getName).collect(Collectors.toList());
        List<SysUserEntity> userList = sysUserService.list();

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();
        // 保存并返回成功条数
        int successNum = this.saveAlertDataFromSheet(sheet, alertNameList, failDetails, userList, headerInfos,seqUUID);
        int totalNum = sheet.getLastRowNum() - sheet.getFirstRowNum();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    private int saveAlertDataFromSheet(Sheet sheet, List<String> alertNameList, List<Map> failDetails, List<SysUserEntity> userList, List<String> headerInfos, String seqUUID) {
        int successNum = 0;
        List<AlertSeverityConf> severityConfs = alertSeverityService.list();
        List<AlertNotifyMethod> methodList = methodService.list();
        Row row;
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i);
            if (row == null) continue;
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            String alertName = ExcelUtils.getCellValue(row.getCell(0));
            if (StringUtils.isEmpty(alertName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.ALERTRULE_NAME_ISNULL.getMsg());
            } else {
                if (!this.checkImportAlertNameDuplicate(alertName, alertNameList)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.ALERTRULE_NAME_DUPLICATE.getMsg());
                }
            }

            String typeStr = ExcelUtils.getCellValue(row.getCell(1));
            if (StrUtil.isEmpty(typeStr)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.ALERTRULE_TYPE_ISNULL.getMsg());
            } else {
                List<String> typeList = Arrays.asList(new String[]{"1", "2", "3"});
                if (!typeList.contains(typeStr)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.ALERTRULE_TYPE_INCORRECT.getMsg());
                }
            }
            String intervalStr = ExcelUtils.getCellValue(row.getCell(2));
            String timeoutStr = ExcelUtils.getCellValue(row.getCell(3));

            String expr = ExcelUtils.getCellValue(row.getCell(4));
            String operator = ExcelUtils.getCellValue(row.getCell(5));
            String threshold = ExcelUtils.getCellValue(row.getCell(6));
            String unit = ExcelUtils.getCellValue(row.getCell(7));
            String last = ExcelUtils.getCellValue(row.getCell(8));
            if (saveFlag) {
                if (StringUtils.isEmpty(expr)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ALERTRULE_EXPR_ISNULL.getMsg());
                }

                if (StrUtil.equals("3", typeStr)) {
                    if (!RegexUtil.test("^(\\d{1,9}\\.){0,}[\\d]+$", expr)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ALERTRULE_OIDFORMAT_ERROR.getMsg());
                    }
                } else {
                    if (StringUtils.isEmpty(threshold)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.ALERTRULE_THRESHOLD_ISNULL.getMsg());
                    } else {
                        try {
                            Double.valueOf(threshold);
                        } catch (NumberFormatException e) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.ALERTRULE_THRESHOLD_FORMAT.getMsg());
                        }
                    }
                    if (StringUtils.isEmpty(unit)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(7), RCode.ALERTRULE_UNIT_ISNULL.getMsg());
                    } else {
                        Integer integer = Constant.ALERT_RULE_UNIT_MAP.get(unit);
                        if (ToolUtil.isEmpty(integer)) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(7), RCode.ALERTRULE_UNIT_INVALIDE.getMsg());
                        }
                    }
                    if (StringUtils.isEmpty(operator)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.ALERTRULE_OPERATOR_ISNULL.getMsg());
                    } else {
                        operator = operator.replaceAll(" ", "");
                        if (!Constant.RULE_OPERATORS.contains(operator)) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.ALERTRULE_OPERATOR_INVALIDE.getMsg());
                        }
                    }
                    if (StringUtils.isEmpty(last)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ALERTRULE_LAST_ISNULL.getMsg());
                    } else {
                        try {
                            Double.valueOf(last).intValue();
                        } catch (NumberFormatException e) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ALERTRULE_LAST_ISNOTPOSITIVE.getMsg());
                        }
                    }
                }
            }
            String severity = ExcelUtils.getCellValue(row.getCell(9));
            AlertSeverityConf alertSeverityConf=null;
            if (StringUtils.isEmpty(severity)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(9), RCode.ALERTRULE_SEVERITY_ISNULL.getMsg());
            }else{
                alertSeverityConf = severityConfs.stream().filter(t -> Tool.StrUtil.equals(severity, t.getName())).findAny().orElse(null);
                if(ToolUtil.isEmpty(alertSeverityConf)){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(9), RCode.ALERTRULE_SEVERITY_INCORRECT.getMsg());
                }
            }
            String summary = ExcelUtils.getCellValue(row.getCell(10));
            if (StringUtils.isEmpty(summary)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(10), RCode.ALERTRULE_SUMMARY_ISNULL.getMsg());
            }
            String description = ExcelUtils.getCellValue(row.getCell(11));
            String receiver = ExcelUtils.getCellValue(row.getCell(12));
            String method = ExcelUtils.getCellValue(row.getCell(13));
            if (StringUtils.isNotEmpty(receiver)) {
                if (CollectionUtils.isNotEmpty(this.getNonExistentUsers(receiver, userList))) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(12), RCode.ALERTRULE_RECEIVER_NOTFOUND.getMsg());
                } else {
                    receiver = this.getUserIdsByUserNames(receiver, userList);
                }
                if(StringUtils.isEmpty(method)){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(13), RCode.ALERTRULE_METHOD_ISNULL.getMsg());
                }
            }

            if (StringUtils.isNotEmpty(method)) {
                // 校验 method 是否存在
                if (CollectionUtils.isNotEmpty(this.getNonExistentMethods(method, methodList))) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(12), RCode.ALERTRULE_METHOD_NOTFOUND.getMsg());
                } else {
                    // 获取 method ids
                    method = this.getMethodIdsByUserNames(method, methodList);
                }
            }

            String stateStr = ExcelUtils.getCellValue(row.getCell(14));
            if (StrUtil.isNotEmpty(stateStr)) {
                if (!StrUtil.equals("1", stateStr) && !StrUtil.equals("0", stateStr)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(14), RCode.ALERTRULE_STATE_INCORRECT.getMsg());
                }
            }

            String autoExpiredStr = ExcelUtils.getCellValue(row.getCell(15));
            // 默认开启 进行校验 day start end format
            String schedEnableStr = ExcelUtils.getCellValue(row.getCell(16));
            schedEnableStr = StrUtil.emptyToDefault(schedEnableStr, "1");
            String schedDaysStr = ExcelUtils.getCellValue(row.getCell(17));
            schedDaysStr = StrUtil.emptyToDefault(schedDaysStr, "1,2,3,4,5,6,7");
            String schedStimeStr = ExcelUtils.getCellValue(row.getCell(18));
            schedStimeStr = StrUtil.emptyToDefault(schedStimeStr, "00:00");
            String schedEtimeStr = ExcelUtils.getCellValue(row.getCell(19));
            schedEtimeStr = StrUtil.emptyToDefault(schedEtimeStr, "23:59");
            String notifyActiveStr = ExcelUtils.getCellValue(row.getCell(20));
            String notifyExpiredStr = ExcelUtils.getCellValue(row.getCell(21));
            String trbShotStr = ExcelUtils.getCellValue(row.getCell(22));

            List<String> enableList = Arrays.asList(new String[]{"0", "1"});
            if (StrUtil.isNotEmpty(autoExpiredStr) && !enableList.contains(autoExpiredStr)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(15), RCode.ALERTRULE_AUTOEXPIRED_INCORRECT.getMsg());
            }

            if (StrUtil.isNotEmpty(notifyActiveStr) && !enableList.contains(notifyActiveStr)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(20), RCode.ALERTRULE_NOTIFYACTIVE_INCORRECT.getMsg());
            }

            if (StrUtil.isNotEmpty(notifyExpiredStr) && !enableList.contains(notifyExpiredStr)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(21), RCode.ALERTRULE_NOTIFYEXPIRED_INCORRECT.getMsg());
            }

            if (!enableList.contains(schedEnableStr)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(16), RCode.ALERTRULE_SCHEDENABLE_INCORRECT.getMsg());
            }

            if (StrUtil.isNotEmpty(schedDaysStr)) {
                List<String> tempList = Arrays.asList(schedDaysStr.split(","));
                for (String s : tempList) {
                    if (!RegexUtil.test("^[1-7]$", s)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(17), RCode.ALERTRULE_SCHEDDAYS_INCORRECT.getMsg());
                    }
                }
            }

            if (!RegexUtil.test("^(?:[01]\\d|2[0-3])(?::[0-5]\\d)$", schedStimeStr)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(18), RCode.ALERTRULE_SCHEDSTIME_INCORRECT.getMsg());
            }
            if (!RegexUtil.test("^(?:[01]\\d|2[0-3])(?::[0-5]\\d)$", schedEtimeStr)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(19), RCode.ALERTRULE_SCHEDETIME_INCORRECT.getMsg());
            }
            try {
                DateFormat formatter = new SimpleDateFormat("HH:mm");
                Date schedStimeDate = formatter.parse(schedStimeStr);
                Date schedEtimeDate = formatter.parse(schedEtimeStr);
                // 结束时间应该晚于开始时间
                if (DateUtil.compare(schedStimeDate, schedEtimeDate) > 0) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(19), RCode.ALERTRULE_SCHEDETIME_ERROR.getMsg());
                }
            } catch (ParseException e) {
                log.error(e);
            }

            if (saveFlag) {
                AlertRuleEntity ruleEntity = new AlertRuleEntity();
                ruleEntity.setName(alertName);
                ruleEntity.setExpr(expr);
                ruleEntity.setOperator(operator);
                ruleEntity.setThreshold(StrUtil.isNotEmpty(threshold) ? Double.valueOf(threshold) : null);
                ruleEntity.setUnit(Constant.ALERT_RULE_UNIT_MAP.get(StrUtil.emptyToDefault(unit,"none")));
                ruleEntity.setLast(StrUtil.isNotEmpty(last) ? Double.valueOf(last).intValue() : null);
                ruleEntity.setSeverityId(alertSeverityConf.getId());
                ruleEntity.setSummary(summary);
                ruleEntity.setDescription(description);
                ruleEntity.setReceiver(receiver);
                ruleEntity.setMethod(method);
                ruleEntity.setSeq(seqUUID);
                ruleEntity.setState(Integer.valueOf(StrUtil.emptyToDefault(stateStr, "1")));

                ruleEntity.setAutoExpired(Integer.valueOf(StrUtil.emptyToDefault(autoExpiredStr, "1")));
                ruleEntity.setSchedEnable(Integer.valueOf(schedEnableStr));
                ruleEntity.setSchedDays(schedDaysStr);
                ruleEntity.setSchedStime(schedStimeStr);
                ruleEntity.setSchedEtime(schedEtimeStr);
                ruleEntity.setNotifyActive(Integer.valueOf(StrUtil.emptyToDefault(notifyActiveStr, "1")));
                ruleEntity.setNotifyExpired(Integer.valueOf(StrUtil.emptyToDefault(notifyExpiredStr, "1")));
                ruleEntity.setTrbShot(trbShotStr);

                ruleEntity.setType(Integer.valueOf(typeStr));
                if (StrUtil.isNotEmpty(intervalStr)) {
                    Integer intervalVal = Integer.valueOf(intervalStr);
                    if (intervalVal < 15) {
                        intervalVal = Integer.valueOf(sysConfigService.getValue(Constant.SYSCONFIG_KEY_SCRAPEINTERVAL));
                    }
                    ruleEntity.setInr(intervalVal);
                }

                if (StrUtil.isNotEmpty(timeoutStr)) {
                    Integer timeoutVal = Integer.valueOf(timeoutStr);
                    if (timeoutVal < 15) {
                        timeoutVal = Integer.valueOf(sysConfigService.getValue(Constant.SYSCONFIG_KEY_SCRAPETIMEOUT));
                    }
                    ruleEntity.setTimeout(timeoutVal);
                }
                // 仅metrics类型需要校验rule表达式  logs和snmp trap暂不支持
                if (StrUtil.equals("1", typeStr)) {
                    try {
                        boolean b = yamlUtil.checkRule(YamlUtil.alertRuleEntityToYamlMap(ruleEntity));
                        if (!b) {
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ALERTRULE_EXPR_ERROR.getMsg());
                            continue;
                        }
                    } catch (IOException e) {
                        log.error(String.format("expr校验失败,rule信息是：%s,错误信息是：%s", ruleEntity.toString(), e));
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ALERTRULE_EXPR_ERROR.getMsg());
                    }
                }
                try {
                    this.save(ruleEntity);
                    alertNameList.add(alertName);
                    successNum++;
                } catch (RuntimeException e) {
                    log.error(String.format("rule导入保存失败,rule信息是：%s,错误信息是：%s", ruleEntity.toString(), e));
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", RCode.ALERTRULE_IMPORT_ERROR.getMsg());
                }
            }
        }
        return successNum;
    }

    private List<String> getNonExistentMethods(String method, List<AlertNotifyMethod> methodList) {
        List<String> methodNames = methodList.stream().map(AlertNotifyMethod::getName).collect(Collectors.toList());
        List<String> list = new ArrayList<>(Arrays.asList(method.split(",")));
        Iterator<String> iterator = list.iterator();
        String next;
        while (iterator.hasNext()) {
            next = iterator.next();
            if (methodNames.contains(next)) {
                iterator.remove();
            }
        }
        return list;
    }

    private String getMethodIdsByUserNames(String method, List<AlertNotifyMethod> methodList) {
        Map<String, Integer> nameAndIdMap = methodList.stream().collect(Collectors.toMap(AlertNotifyMethod::getName, AlertNotifyMethod::getId));
        List<String> list = Arrays.asList(method.split(","));
        String methodIds = "";
        for (String name : list) {
            methodIds += nameAndIdMap.get(name) + ",";
        }
        if (StringUtils.isNotEmpty(methodIds)) {
            methodIds = methodIds.substring(0, methodIds.length() - 1);
        }
        return methodIds;
    }

    private String getUserIdsByUserNames(String receiver, List<SysUserEntity> userList) {
        Map<String, Long> userNameAndIdMap = userList.stream().collect(Collectors.toMap(SysUserEntity::getName, SysUserEntity::getId));
        List<String> list = Arrays.asList(receiver.split(","));
        String receiverIds = "";
        for (String s : list) {
            receiverIds += userNameAndIdMap.get(s) + ",";
        }
        if (StringUtils.isNotEmpty(receiverIds)) {
            receiverIds = receiverIds.substring(0, receiverIds.length() - 1);
        }
        return receiverIds;
    }

    private List<String> getNonExistentUsers(String receiver, List<SysUserEntity> userList) {
        List<String> usernames = userList.stream().map(SysUserEntity::getName).collect(Collectors.toList());
        List<String> list = new ArrayList<>(Arrays.asList(receiver.split(",")));
        Iterator<String> iterator = list.iterator();
        String next;
        while (iterator.hasNext()) {
            next = iterator.next();
            if (usernames.contains(next)) {
                iterator.remove();
            }
        }
        return list;
    }

    private void addFailDetail(int i, List<Map> failDetails, String headerInfo, String msg) {
        Map failDetail = new HashMap(4);
        failDetail.put("lineNo", i);
        failDetail.put("errorMsg", headerInfo + ": " + msg);
        failDetails.add(failDetail);
    }

    public boolean checkImportAlertNameDuplicate(String alertName, List<String> alertNameList) {
        if (ToolUtil.isNotEmpty(alertNameList)) {
            // 不唯一
            if (alertNameList.contains(alertName)) {
                return false;
            } else {
                //  唯一
                return true;
            }
        }
        return true;
    }

    @Override
    public void export(HttpServletResponse response, Map<String, Object> params) {
    	SysConfigEntity maxLine = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE));
        long exportMaxLine = 10000;
		// 导出最大条数限制
        if(ObjectUtil.isNotEmpty(maxLine) && ObjectUtil.isNotEmpty(maxLine.getParamValue())) {
        	exportMaxLine = Integer.parseInt(maxLine.getParamValue());
        }
        Long pageSize = Long.parseLong((String)params.get(Constant.LIMIT));
		if(ObjectUtil.isEmpty(pageSize) || pageSize==-1 || pageSize>exportMaxLine) {
			params.put(Constant.LIMIT,Long.toString(exportMaxLine));
		}
        // 导出查询与列表查询一致
        PageUtils page = this.queryPage(params);
        List<AlertRuleEntity> list = (List<AlertRuleEntity>) page.getList();

        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ALERTRULE_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        // 添加标题行 默认语言 en
        String language = (String) params.get("language");
        language = ToolUtil.isEmpty(language) ? "en" : language;

        SXSSFWorkbook workbook = null;
        try {
            String sheetName = "AlterRule";
            workbook = ExcelUtils.createExcel07((Map) map.get(language), sheetName, true);
            SXSSFSheet sheet = workbook.getSheet(sheetName);

            // 填充数据
            this.fillData(list, 1, sheet, workbook);

            String fileName = "AlterRuleExport.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("告警规则导出文件生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            log.error("告警规则导出失败，参数是：" + params.toString() + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
    }

    @Override
    public void cancelImport(String seq) {
        this.remove(new QueryWrapper<AlertRuleEntity>().lambda().eq(AlertRuleEntity::getSeq, seq));
    }

    private void fillData(List<AlertRuleEntity> list, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook) {
        List<SysUserEntity> sysUserList = sysUserService.list();
        Map<Long, String> userIdAndNameMap = sysUserList.stream().collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getName));
        List<AlertNotifyMethod> methodList = methodService.list();
        Map<Integer, String> methodIdAndNameMap = methodList.stream().collect(Collectors.toMap(AlertNotifyMethod::getId, AlertNotifyMethod::getName));
        Row row;
        String receiveIds, receiveNames;
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        for (AlertRuleEntity rule : list) {
            // 内置告警不导出
            if (rule.getBuildIn() != null && rule.getBuildIn().equals(1)) continue;
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rule.getName());
            row.createCell(1).setCellValue(rule.getType());
            row.createCell(2).setCellValue(rule.getInr());
            row.createCell(3).setCellValue(rule.getTimeout());
            row.createCell(4).setCellValue(rule.getExpr());
            row.createCell(5).setCellValue(rule.getOperator());
            row.createCell(6).setCellValue(rule.getThreshold() == null ? "" : rule.getThreshold() + "");
            row.createCell(7).setCellValue(getUnitDescrByVal(rule.getUnit()));
            row.createCell(8).setCellValue(rule.getLast());
            row.createCell(9).setCellValue(rule.getSeverity().getName());
            row.createCell(10).setCellValue(rule.getSummary());
            row.createCell(11).setCellValue(rule.getDescription());
            receiveIds = rule.getReceiver();
            if (StringUtils.isNotEmpty(receiveIds)) {
                receiveNames = "";
                for (String id : Arrays.asList(receiveIds.split(","))) {
                    receiveNames += userIdAndNameMap.get(Long.valueOf(id)) + ",";
                }
                if (StringUtils.isNotEmpty(receiveNames)) {
                    receiveNames = receiveNames.substring(0, receiveNames.length() - 1);
                }
                row.createCell(12).setCellValue(receiveNames);
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
                row.createCell(13).setCellValue(methodNames);
            }
            row.createCell(14).setCellValue(rule.getState());
            row.createCell(15).setCellValue(rule.getAutoExpired());
            row.createCell(16).setCellValue(rule.getSchedEnable());
            row.createCell(17).setCellValue(rule.getSchedDays());
            row.createCell(18).setCellValue(rule.getSchedStime());
            row.createCell(19).setCellValue(rule.getSchedEtime());
            row.createCell(20).setCellValue(rule.getNotifyActive());
            row.createCell(21).setCellValue(rule.getNotifyExpired());
            row.createCell(22).setCellValue(StrUtil.emptyToDefault(rule.getTrbShot(), ""));

            // 设置单元格样式
            ExcelUtils.setDataCellStyle(dataStyle, row, 22);
        }
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
		
		int alertNum = alertMessageService.count(new QueryWrapper<AlertMessageEntity>().lambda().eq(AlertMessageEntity::getRuleId,id));
		alertRule.setAlertNum(alertNum);
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
		}
		
		return alertRule;
	}
}
