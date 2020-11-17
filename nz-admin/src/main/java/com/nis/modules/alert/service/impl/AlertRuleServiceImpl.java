package com.nis.modules.alert.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleStat;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.dao.AssetDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.endpoint.dao.EndpointDao;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.module.dao.ModuleDao;
import com.nis.modules.module.entity.Module;
import com.nis.modules.project.dao.ProjectDao;
import com.nis.modules.project.entity.Project;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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

    @Autowired
    private AlertMessageDao alertMessageDao;

    @Autowired
    private EndpointDao endpointDao;

    @Autowired
    private SysConfigService sysConfigService;

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

        // 处理告警消息数量
        this.handleAlertMessageNumForRules(ruleEntityList);

        page.setRecords(ruleEntityList);
        return new PageUtils(page);
    }

    private void handleAlertMessageNumForRules(List<AlertRuleEntity> rules) {
        List<AlertMessageEntity> alerts = alertMessageDao.getAlertsStatColumn();
        Map<Integer, List<AlertMessageEntity>> ruleIdAndAlertsMap = alerts.stream().collect(Collectors.groupingBy(AlertMessageEntity::getRuleId));
        List<AlertMessageEntity> messageEntities;
        for (AlertRuleEntity entity : rules) {
            messageEntities = ruleIdAndAlertsMap.get(entity.getId());
            entity.setAlertNum(CollectionUtils.isEmpty(messageEntities) ? 0 : messageEntities.size());
        }
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
            log.error(String.format("expr校验失败,rule信息是：%s,错误信息是：%s", alertRule.toString(), e.getMessage()), e);
        }

        if (!checkRule) {
            throw new NZException(RCode.ALERTRULE_EXPR_ERROR);
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
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer[] ids) {
        ValidateUtils.is(ids).notNull(RCode.ALERTMSG_IDS_ISNULL);
        // 校验内置
        List<Integer> idList = Arrays.asList(ids);
        this.validateBulitinRule(idList);
        // 删除
        this.removeByIds(idList);

        // 删除关联的告警消息
        alertMessageDao.delete(new QueryWrapper<AlertMessageEntity>().lambda().in(AlertMessageEntity::getRuleId, idList));
    }

    @Override
    public Map getAlertStatById(Integer id) {
        Map resultData = new HashMap();

        // 1. project 统计
        List<AlertRuleStat> ruleStatGooupByProject = alertRuleDao.getRuleStatGooupByProject(id);
        if (CollectionUtils.isNotEmpty(ruleStatGooupByProject)) {

            // 查询 module 统计信息
            List<AlertRuleStat> ruleStatGooupByModule = alertRuleDao.getRuleStatGooupByModule(id);

            // 处理 project 名称
            List<Project> projects = projectDao.selectList(null);
            Map<Integer, String> projectIdAndNameMap = projects.stream().collect(Collectors.toMap(Project::getId, Project::getName));
            this.handleRuleStatName(ruleStatGooupByProject, projectIdAndNameMap);


            // 如果 module 统计信息不为空
            if (CollectionUtils.isNotEmpty(ruleStatGooupByModule)) {

                // endpoint 统计
                List<AlertRuleStat> ruleStatGooupByEndpoint = alertRuleDao.getRuleStatGooupByEndpoint(id);

                // 处理 module 名称
                List<Integer> projectIds = ruleStatGooupByProject.stream().map(AlertRuleStat::getId).collect(Collectors.toList());
                List<Module> modules = moduleDao.selectList(new QueryWrapper<Module>().lambda().in(Module::getProjectId, projectIds));
                Map<Integer, String> moduleIdAndNameMap = modules.stream().collect(Collectors.toMap(Module::getId, Module::getName));
                this.handleRuleStatName(ruleStatGooupByModule, moduleIdAndNameMap);

                // endpoint 统计不为空
                if (CollectionUtils.isNotEmpty(ruleStatGooupByEndpoint)) {

                    Map<Integer, AlertRuleStat> endpointStatMap = new HashMap<>();
                    for (AlertRuleStat stat : ruleStatGooupByEndpoint) {
                        endpointStatMap.put(stat.getId(), stat);
                    }

                    // 查询 endpoint 并根据 moduleId 分组
                    List<Integer> moduleIds = ruleStatGooupByModule.stream().map(AlertRuleStat::getId).collect(Collectors.toList());
                    List<Endpoint> endpoints = endpointDao.selectList(new QueryWrapper<Endpoint>().lambda().in(Endpoint::getModuleId, moduleIds));
                    Map<Integer, List<Endpoint>> moduleIdAndEndpoints = endpoints.stream().collect(Collectors.groupingBy(Endpoint::getModuleId));

                    List<Endpoint> endpointsByModuleId;
                    List<Integer> endPointIds;
                    List<AlertRuleStat> list;
                    AlertRuleStat endpointStat;
                    for (AlertRuleStat moduleStat : ruleStatGooupByModule) {
                        // 取出 moudle 对应的 endpoints
                        endpointsByModuleId = moduleIdAndEndpoints.get(moduleStat.getId());
                        if (CollectionUtils.isNotEmpty(endpointsByModuleId)) {
                            endPointIds = endpointsByModuleId.stream().map(Endpoint::getId).collect(Collectors.toList());
                            list = new ArrayList<>();
                            for (Integer endpointId : endPointIds) {
                                endpointStat = endpointStatMap.get(endpointId);
                                if (ToolUtil.isNotEmpty(endpointStat)) {
                                    list.add(endpointStat);
                                }
                            }
                            if (CollectionUtils.isNotEmpty(list)) {
                                moduleStat.setEndpoint(list.stream().sorted((a1, a2) -> a2.getNums().compareTo(a1.getNums())).collect(Collectors.toList()));
                            }
                        }
                    }
                }

                // 处理 project 对应 module 信息
                Map<Integer, AlertRuleStat> moduleStatMap = new HashMap<>();
                for (AlertRuleStat stat : ruleStatGooupByModule) {
                    moduleStatMap.put(stat.getId(), stat);
                }

                Map<Integer, List<Module>> projectIdAndModules = modules.stream().collect(Collectors.groupingBy(Module::getProjectId));

                List<Module> modulesByProjectId;
                List<Integer> moduleIds;
                List<AlertRuleStat> list;
                AlertRuleStat moduleStat;
                for (AlertRuleStat projectStat : ruleStatGooupByProject) {
                    modulesByProjectId = projectIdAndModules.get(projectStat.getId());
                    if (CollectionUtils.isNotEmpty(modulesByProjectId)) {
                        moduleIds = modulesByProjectId.stream().map(Module::getId).collect(Collectors.toList());
                        list = new ArrayList<>();
                        for (Integer moduleId : moduleIds) {
                            moduleStat = moduleStatMap.get(moduleId);
                            if (ToolUtil.isNotEmpty(moduleStat)) {
                                list.add(moduleStat);
                            }
                        }
                        if (CollectionUtils.isNotEmpty(list)) {
                            projectStat.setModule(list.stream().sorted((a1, a2) -> a2.getNums().compareTo(a1.getNums())).collect(Collectors.toList()));
                        }
                    }
                }
            }

            resultData.put("project", ruleStatGooupByProject);
        } else {
            resultData.put("project", Collections.EMPTY_LIST);
        }

        // asset 统计
        List<Asset> assets = assetDao.selectList(null);
        Map<Integer, String> assetIdAndHostMap = assets.stream().collect(Collectors.toMap(Asset::getId, Asset::getHost));

        List<AlertRuleStat> ruleStatGooupByAsset = alertRuleDao.getRuleStatGooupByAsset(id);
        this.handleRuleStatName(ruleStatGooupByAsset, assetIdAndHostMap);

        resultData.put("asset", ruleStatGooupByAsset);
        return resultData;
    }

    @Override
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ALERTRULE_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        switch (language) {
            case "en": {
                exportHeaderMap = (Map) map.get("en");
                break;
            }
            case "cn": {
                exportHeaderMap = (Map) map.get("cn");
                break;
            }
            case "ru": {
                exportHeaderMap = (Map) map.get("ru");
                break;
            }
            default: {
                throw new NZException(RCode.ALERTRULE_EXPORT_HEADER_LANGUAGE_ERROR);
            }
        }
        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "AlertRule", true);
            String fileName = "AlertRuleExportTemplate.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("告警规则模板生成失败，错误信息是：" + e.getMessage(), e);
        } catch (Exception e) {
            log.error("获取告警规则导入模板失败，表头语言是：" + language + " ，错误信息是：" + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        } catch (Exception e) {
            log.error("解析导入告警规则文件格式有误", e);
            throw new NZException(RCode.ASSET_IMPORTFILE_FAILED);
        } finally {
            if (work != null) {
                try {
                    work.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        List<String> alertNameList = alertList.stream().map(AlertRuleEntity::getAlertName).collect(Collectors.toList());
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
            String expr = ExcelUtils.getCellValue(row.getCell(1));
            if (StringUtils.isEmpty(expr)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1),RCode.ALERTRULE_EXPR_ISNULL.getMsg());
            }
            String operator = ExcelUtils.getCellValue(row.getCell(2));
            if (StringUtils.isEmpty(operator)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.ALERTRULE_OPERATOR_ISNULL.getMsg());
            } else {
                operator = operator.replaceAll(" ", "");
                if (!Constant.RULE_OPERATORS.contains(operator)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.ALERTRULE_OPERATOR_INVALIDE.getMsg());
                }
            }
            String threshold = ExcelUtils.getCellValue(row.getCell(3));
            if (StringUtils.isEmpty(threshold)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ALERTRULE_THRESHOLD_ISNULL.getMsg());
            } else {
                try {
                    Double.valueOf(threshold);
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ALERTRULE_THRESHOLD_FORMAT.getMsg());
                }
            }
            String unit = ExcelUtils.getCellValue(row.getCell(4));
            if (StringUtils.isEmpty(unit)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ALERTRULE_UNIT_ISNULL.getMsg());
            } else {
                Integer integer = Constant.ALERT_RULE_UNIT_MAP.get(unit);
                if (ToolUtil.isEmpty(integer)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ALERTRULE_UNIT_INVALIDE.getMsg());
                }
            }
            String last = ExcelUtils.getCellValue(row.getCell(5));
            if (StringUtils.isEmpty(last)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.ALERTRULE_LAST_ISNULL.getMsg());
            } else {
                try {
                    Double.valueOf(last).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.ALERTRULE_LAST_ISNOTPOSITIVE.getMsg());
                }
            }
            String severity = ExcelUtils.getCellValue(row.getCell(6));
            if (StringUtils.isEmpty(severity)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.ALERTRULE_SEVERITY_ISNULL.getMsg());
            }else{
                if(!Constant.ALERT_SEVERITY_HIGH.equals(severity) && !Constant.ALERT_SEVERITY_MEDIUM.equals(severity) && !Constant.ALERT_SEVERITY_LOW.equals(severity)){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.ALERTRULE_SEVERITY_INCORRECT.getMsg());
                }
            }
            String summary = ExcelUtils.getCellValue(row.getCell(7));
            if (StringUtils.isEmpty(summary)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(7), RCode.ALERTRULE_SUMMARY_ISNULL.getMsg());
            }
            String description = ExcelUtils.getCellValue(row.getCell(8));
            if (StringUtils.isEmpty(description)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ALERTRULE_DESCRIPTION_ISNULL.getMsg());
            }
            String receiver = ExcelUtils.getCellValue(row.getCell(9));
            if (StringUtils.isNotEmpty(receiver)) {
                if (CollectionUtils.isNotEmpty(this.getNonExistentUsers(receiver, userList))) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(9), RCode.ALERTRULE_RECEIVER_NOTFOUND.getMsg());
                } else {
                    receiver = this.getUserIdsByUserNames(receiver, userList);
                }
            }
            if (saveFlag) {
                AlertRuleEntity ruleEntity = new AlertRuleEntity();
                ruleEntity.setAlertName(alertName);
                ruleEntity.setExpr(expr);
                ruleEntity.setOperator(operator);
                ruleEntity.setThreshold(Double.valueOf(threshold));
                ruleEntity.setUnit(Constant.ALERT_RULE_UNIT_MAP.get(unit));
                ruleEntity.setLast(Double.valueOf(last).intValue());
                ruleEntity.setSeverity(severity);
                ruleEntity.setSummary(summary);
                ruleEntity.setDescription(description);
                ruleEntity.setReceiver(receiver);
                ruleEntity.setSeq(seqUUID);
                try {
                    boolean b = yamlUtil.checkRule(YamlUtil.alertRuleEntityToYamlMap(ruleEntity));
                    if (!b) {
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.ALERTRULE_EXPR_ERROR.getMsg());
                        continue;
                    }
                } catch (Exception e) {
                    log.error(String.format("expr校验失败,rule信息是：%s,错误信息是：%s", ruleEntity.toString(), e.getMessage()), e);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.ALERTRULE_EXPR_ERROR.getMsg());
                }
                try {
                    this.save(ruleEntity);
                    alertNameList.add(alertName);
                    successNum++;
                } catch (Exception e) {
                    log.error(String.format("rule导入保存失败,rule信息是：%s,错误信息是：%s", ruleEntity.toString(), e.getMessage()), e);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", RCode.ALERTRULE_IMPORT_ERROR.getMsg());
                }
            }
        }
        return successNum;
    }

    private String getUserIdsByUserNames(String receiver, List<SysUserEntity> userList) {
        Map<String, Long> userNameAndIdMap = userList.stream().collect(Collectors.toMap(SysUserEntity::getUsername, SysUserEntity::getUserId));
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
        List<String> usernames = userList.stream().map(SysUserEntity::getUsername).collect(Collectors.toList());
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
            workbook = ExcelUtils.createExcel07((Map) map.get(language), sheetName, false);
            SXSSFSheet sheet = workbook.getSheet(sheetName);

            // 填充数据
            this.fillData(list, 1, sheet, workbook);

            String fileName = "AlterRuleExport.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("告警规则导出文件生成失败，错误信息是：" + e.getMessage(), e);
        } catch (Exception e) {
            log.error("告警规则导出失败，参数是：" + params.toString() + " ，错误信息是：" + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void cancelImport(String seq) {
        this.remove(new QueryWrapper<AlertRuleEntity>().lambda().eq(AlertRuleEntity::getSeq, seq));
    }

    private void fillData(List<AlertRuleEntity> list, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook) {
        List<SysUserEntity> sysUserList = sysUserService.list();
        Map<Long, String> userIdAndNameMap = sysUserList.stream().collect(Collectors.toMap(SysUserEntity::getUserId, SysUserEntity::getUsername));
        Row row;
        String receiveIds, receiveNames;
        for (AlertRuleEntity rule : list) {
            // 内置告警不导出
            if (rule.getBuildIn() != null && rule.getBuildIn().equals(1)) continue;
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rule.getAlertName());
            row.createCell(1).setCellValue(rule.getExpr());
            row.createCell(2).setCellValue(rule.getOperator());
            row.createCell(3).setCellValue(rule.getThreshold() == null ? "" : rule.getThreshold() + "");
            row.createCell(4).setCellValue(getUnitDescrByVal(rule.getUnit()));
            row.createCell(5).setCellValue(rule.getLast());
            row.createCell(6).setCellValue(rule.getSeverity());
            row.createCell(7).setCellValue(rule.getSummary());
            row.createCell(8).setCellValue(rule.getDescription());
            receiveIds = rule.getReceiver();
            if (StringUtils.isNotEmpty(receiveIds)) {
                receiveNames = "";
                for (String id : Arrays.asList(receiveIds.split(","))) {
                    receiveNames += userIdAndNameMap.get(Long.valueOf(id)) + ",";
                }
                if (StringUtils.isNotEmpty(receiveNames)) {
                    receiveNames = receiveNames.substring(0, receiveNames.length() - 1);
                }
                row.createCell(9).setCellValue(receiveNames);
            }

            // 设置单元格样式
            ExcelUtils.setDataCellStyle(workbook, row, 9);
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

        List<Integer> unitList = new ArrayList(Constant.ALERT_RULE_UNIT_MAP.values());
        if (unit < Collections.min(unitList) || unit > Collections.max(unitList)) {
            throw new NZException(RCode.ALERTRULE_UNIT_INVALIDE);
        }
    }
}