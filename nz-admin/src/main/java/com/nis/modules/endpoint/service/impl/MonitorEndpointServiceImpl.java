package com.nis.modules.endpoint.service.impl;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.endpoint.dao.MonitorEndpointDao;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.panel.service.ExpreTmplService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
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
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MonitorEndpointServiceImpl extends ServiceImpl<MonitorEndpointDao, MonitorEndpoint> implements MonitorEndpointService {

    private static Log log = Log.get();

    @Autowired
    private AssetService assetService;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private AlertMessageDao alertMessageDao;

    @Autowired
    private MonitorEndpointDao monitorEndpointDao;

    @Autowired
    private MonitorModuleService monitorModuleService;

    @Autowired
    private MonitorProjectService monitorProjectService;

    @Autowired
    private ExpreTmplService expreTmplService;

    @Override
    public MonitorEndpoint queryEndpointEntity(Integer id) {
        MonitorEndpoint endpoint = this.getById(id);
        if (ToolUtil.isEmpty(endpoint)) {
            throw new NZException(RCode.ENDPOINT_NOTFOUND);
        }
        MonitorModule module = monitorModuleService.getById(endpoint.getModuleId());
        endpoint.setModule(module);
        MonitorProject project = monitorProjectService.getById(module.getProjectId());
        endpoint.setProject(project);
        Asset asset = assetService.getById(endpoint.getAssetId());
        endpoint.setAsset(asset);

        List<AlertMessageEntity> alerts = alertMessageDao.getAlertsStatColumn();
        alerts = alerts.stream().filter(alert -> ToolUtil.isNotEmpty(alert.getEndpointId())).collect(Collectors.toList());
        Map<Integer, List<AlertMessageEntity>> endpointIdAndAlersMap = alerts.stream().collect(Collectors.groupingBy(AlertMessageEntity::getEndpointId));

        List<AlertMessageEntity> tempAlert = endpointIdAndAlersMap.get(endpoint.getId());
        if (CollectionUtils.isEmpty(tempAlert)) {
            endpoint.setAlertNum(0);
        } else {
            endpoint.setAlertNum(tempAlert.size());
        }
        return endpoint;
    }

    private void handleEndpointsAlertNum(List<MonitorEndpoint> endpointList) {
        if (CollectionUtils.isEmpty(endpointList)) return;

        List<AlertMessageEntity> alerts = alertMessageDao.getAlertsStatColumn();
        alerts = alerts.stream().filter(alert -> ToolUtil.isNotEmpty(alert.getEndpointId())).collect(Collectors.toList());
        Map<Integer, List<AlertMessageEntity>> endpointIdAndAlersMap = alerts.stream().collect(Collectors.groupingBy(AlertMessageEntity::getEndpointId));

        List<AlertMessageEntity> tempAlert;
        for (MonitorEndpoint endpoint : endpointList) {
            tempAlert = endpointIdAndAlersMap.get(endpoint.getId());
            if (CollectionUtils.isEmpty(tempAlert)) {
                endpoint.setAlertNum(0);
            } else {
                endpoint.setAlertNum(tempAlert.size());
            }
        }
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MonitorEndpoint> page = new Query<MonitorEndpoint>().getPage(params);
        List<MonitorEndpoint> datas = monitorEndpointDao.queryPage(page, params);

        // endpoint 告警数量
        this.handleEndpointsAlertNum(datas);

        page.setRecords(datas);
        return new PageUtils(page);
    }

    @Override
    public List<MonitorProject> queryEndpointTree(String name) {
        List<MonitorEndpoint> endpointList = this.list(new LambdaQueryWrapper<MonitorEndpoint>().like(StringUtils.isNotEmpty(name), MonitorEndpoint::getName, name));
        if (CollectionUtils.isEmpty(endpointList)) {
            return Collections.emptyList();
        }
        List<Integer> moduleIdList = endpointList.stream().map(MonitorEndpoint::getModuleId).distinct().collect(Collectors.toList());
        List<MonitorModule> moduleList = monitorModuleService.list(new LambdaQueryWrapper<MonitorModule>().in(MonitorModule::getId, moduleIdList));

        List<Integer> projectIdList = moduleList.stream().map(MonitorModule::getProjectId).distinct().collect(Collectors.toList());
        List<MonitorProject> projectList = monitorProjectService.list(new LambdaQueryWrapper<MonitorProject>().in(MonitorProject::getId, projectIdList));


        for (MonitorProject project : projectList) {
            List<MonitorModule> projectModules = new ArrayList<>();

            for (MonitorModule module : moduleList) {
                List<MonitorEndpoint> moduleEndpoints = new ArrayList();
                for (MonitorEndpoint endpoint : endpointList) {
                    if (endpoint.getModuleId().equals(module.getId())) {
                        moduleEndpoints.add(endpoint);
                    }
                }
                module.setChildren(moduleEndpoints);

                if (module.getProjectId().equals(project.getId())) {
                    projectModules.add(module);
                }
            }

            project.setChildren(projectModules);
        }
        return projectList;
    }

    private void validateParam(List<MonitorEndpoint> endpointList) {
        List<MonitorModule> moduleList = monitorModuleService.list();
        List<Integer> moduleIdList = moduleList.stream().map(MonitorModule::getId).collect(Collectors.toList());

        List<Asset> assetList = assetService.list();
        List<Integer> assetIdList = assetList.stream().map(Asset::getId).collect(Collectors.toList());

        List<Integer> enabledList = Arrays.asList(new Integer[]{1, 0});
        //String name;
        Integer moduleId, assetId, enabled;
        for (MonitorEndpoint endpoint : endpointList) {
            moduleId = endpoint.getModuleId();
            assetId = endpoint.getAssetId();

            // 1. 非空校验
            ValidateUtils.is(moduleId).notNull(RCode.ENDPOINT_MODULEID_ISNULL)
                    .and(assetId).notNull(RCode.ENDPOINT_ASSETID_ISNULL)
                    .and(endpoint.getPort()).notNull(RCode.ENDPOINT_PORT_ISNULL);

            // 2. module 是否存在
            if (!moduleIdList.contains(moduleId)) {
                throw new NZException(RCode.MODULE_NOTFOUND);
            }

            // 3. asset 是否存在
            if (!assetIdList.contains(assetId)) {
                throw new NZException(RCode.ASSET_NOT_EXIST);
            }

            // 4. enabled 值是否有误
            enabled = endpoint.getEnabled();
            if (enabled != null && !enabledList.contains(enabled)) {
                throw new NZException(RCode.ENDPOINT_ENABLED_INVALIDE);
            }

            // 5. port 校验
            if (endpoint.getPort() != null) {
                boolean flag = CommonUtils.checkPort(endpoint.getPort().toString());
                if (!flag) {
                    throw new NZException(RCode.ENDPOINT_PORT_ERROR);
                }
            }

            // 6. configs 校验
            if (endpoint.getConfigs() != null) {
                try {
                    JSONObject.parseObject(endpoint.getConfigs(), Map.class);
                } catch (JSONException e) {
                    throw new NZException(RCode.ENDPOINT_CONFIGS_FORMAT);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateBatchEndpointList(List<MonitorEndpoint> endpointList) {
        // 校验
        this.validateParam(endpointList);

        List<MonitorModule> moduleList = monitorModuleService.list();
        Map<Integer, MonitorModule> moduleIdAndModuleInfoMap = moduleList.stream().collect(Collectors.toMap(MonitorModule::getId, Function.identity()));

        List<Asset> assetList = assetService.list();
        Map<Integer, String> assetIdAndHostMap = assetList.stream().collect(Collectors.toMap(Asset::getId, Asset::getHost));

        List<MonitorEndpoint> endpoints = this.list();
        Map<Integer, List<MonitorEndpoint>> moduleIdAndEndpointsMap = new HashMap<>();
        for (MonitorModule module : moduleList) {
            List<MonitorEndpoint> moduleEndpoints = endpoints.stream().filter(endpoint -> endpoint.getModuleId().equals(module.getId())).collect(Collectors.toList());
            moduleIdAndEndpointsMap.put(module.getId(), ToolUtil.isEmpty(moduleEndpoints) ? new ArrayList<>() : moduleEndpoints);
        }

        MonitorModule module;
        for (MonitorEndpoint endpoint : endpointList) {
            module = moduleIdAndModuleInfoMap.get(endpoint.getModuleId());
            if (ToolUtil.isEmpty(endpoint.getPort())) {
                endpoint.setPort(module.getPort());
            }
            if (ToolUtil.isEmpty(endpoint.getHost())) {
                endpoint.setHost(assetIdAndHostMap.get(endpoint.getAssetId()));
            }
            if (ToolUtil.isEmpty(endpoint.getEnabled())) {
                endpoint.setEnabled(1);
            }
            if (ToolUtil.isEmpty(endpoint.getConfigs())) {
                endpoint.setConfigs(module.getConfigs());
            }
            // 名称为空 则按照 module endpoint 名称模板生成对应名称
            if (ToolUtil.isEmpty(endpoint.getName())) {
                String endpointNameTmpl = module.getEndpointNameTmpl();
                Map varsVal = new HashMap(4);
                varsVal.put("asset.host", assetIdAndHostMap.get(endpoint.getAssetId()));
                varsVal.put("module.name", module.getName());
                String generateEndpointName = expreTmplService.renderExpression(endpointNameTmpl, varsVal);
                endpoint.setName(generateEndpointName);
            }

            // 校验名称是否重复
            List<MonitorEndpoint> monitorEndpointList = moduleIdAndEndpointsMap.get(endpoint.getModuleId());
            MonitorEndpoint monitorEndpoint = monitorEndpointList.stream().filter(entity -> entity.getName().equals(endpoint.getName()) && !entity.getId().equals(endpoint.getId())).findAny().orElse(null);
            if (ToolUtil.isNotEmpty(monitorEndpoint)) {
                throw new NZException(RCode.ENDPOINT_NAME_DUPLICATE);
            }
            monitorEndpointList.add(endpoint);
            moduleIdAndEndpointsMap.put(endpoint.getModuleId(), monitorEndpointList);

            String hash = endpoint.getModuleId() + endpoint.getAssetId() + endpoint.getHost() + endpoint.getPort() + endpoint.getConfigs();
            endpoint.setHash(DigestUtils.md5DigestAsHex(hash.getBytes()));

            // 保存 endpoint
            this.saveOrUpdate(endpoint);
        }
    }

    @Override
    public void deleteEndpointByIds(List<Integer> ids) {
        // 删除 endpoint
        this.removeByIds(ids);
    }

    @Override
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        SysConfigEntity configEntity = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MONITORENDPOINT_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        if (map.get(language) != null) {
            exportHeaderMap = (Map) map.get(language);
        } else {
            throw new NZException(RCode.ENDPOINT_EXPORT_HEADER_LANGUAGE_ERROR);
        }
        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "Endpoint", true);
            String fileName = "EndpointExportTemplate.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("endpoint 模板生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            log.error("获取 endpoint 导入模板失败，表头语言是：" + language + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
    }

    @Override
    public void cancelImport(String seq) {
        // 撤销导入
        this.remove(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getSeq, seq));
    }

    @Override
    public Map importEndpoint(MultipartFile excelFile, String language) {
        Workbook work = ExcelUtils.getWorkBookFromMultipartFile(excelFile);

        Sheet sheet = work.getSheetAt(0);

        // 表头校验
        SysConfigEntity config = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MONITORENDPOINT_EXPORT_HEADER));
        HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
        boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), map.values());
        if (!headerFlag) {
            throw new NZException(RCode.IMPORT_EXCELFILE_HEADER_TEMPLATE_ERROR);
        }

        Map<String, String> exportHeader = (Map) map.get(language);
        List<String> headerInfos = new ArrayList(exportHeader.keySet());
        // 预先查询出校验数据 避免循环查表
        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = this.saveTmplsDataFromSheet(sheet, failDetails, headerInfos, seqUUID);
        int totalNum = sheet.getLastRowNum() - sheet.getFirstRowNum();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    @Override
    public void export(HttpServletResponse response, Map<String, Object> params) {
        IPage<MonitorEndpoint> page = new Query<MonitorEndpoint>().getPage(params);
        List<MonitorEndpoint> list = monitorEndpointDao.queryPage(page, params);

        SXSSFWorkbook workbook = null;
        try {
            int rowNum = 0;
            SysConfigEntity configEntity = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MONITORENDPOINT_EXPORT_HEADER));
            Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);

            // 添加标题行 默认语言 en
            String language = (String) params.get("language");
            language = ToolUtil.isEmpty(language) ? "en" : language;
            Map<String, String> exportHeader = (Map) map.get(language);

            String sheetName = "Endpoints";
            workbook = ExcelUtils.createExcel07(exportHeader, sheetName, false);
            SXSSFSheet sheet = workbook.getSheet(sheetName);
            CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);

            // 添加数据
            Row row1;
            for (MonitorEndpoint endpoint : list) {
                row1 = sheet.createRow(++rowNum);

                MonitorProject project = endpoint.getProject();
                MonitorModule module = endpoint.getModule();
                Asset asset = endpoint.getAsset();

                // project name
                row1.createCell(0).setCellValue(project.getName());
                // module name
                row1.createCell(1).setCellValue(module.getName());
                // name
                row1.createCell(2).setCellValue(endpoint.getName());
                // asset name
                row1.createCell(3).setCellValue(asset.getName());
                // port
                row1.createCell(4).setCellValue(endpoint.getPort());
                // config
                row1.createCell(5).setCellValue(endpoint.getConfigs());
                // host
                row1.createCell(6).setCellValue(endpoint.getHost());
                // enabled
                row1.createCell(7).setCellValue(endpoint.getEnabled());

                // 设置单元格样式
                ExcelUtils.setDataCellStyle(dataStyle, row1, 7);
            }

            // 响应为流
            String fileName = "EndpointsExport.xlsx"; // 默认名称
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error(e);
        } catch (RuntimeException e) {
            log.error(e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
    }

    private int saveTmplsDataFromSheet(Sheet sheet, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
        List<MonitorProject> projectList = monitorProjectService.list();
        Map<String, Integer> projectNameAndIdMap = projectList.stream().collect(Collectors.toMap(MonitorProject::getName, MonitorProject::getId));

        List<MonitorModule> moduleList = monitorModuleService.list();
        Map<String, MonitorModule> monitorModuleMap = moduleList.stream().collect(Collectors.toMap(MonitorModule::getName, Function.identity()));

        List<MonitorEndpoint> endpoints = this.list();
        Map<Integer, List<String>> moduleIdAndEndpointNamesMap = new HashMap<>();
        for (MonitorModule module : moduleList) {
            List<String> endpointNames = endpoints.stream().filter(endpoint -> endpoint.getModuleId().equals(module.getId())).map(MonitorEndpoint::getName).collect(Collectors.toList());
            moduleIdAndEndpointNamesMap.put(module.getId(), ToolUtil.isEmpty(endpointNames) ? new ArrayList<>() : endpointNames);
        }

        List<Asset> assets = assetService.list();
        Map<String, Integer> assetHostAndIdMap = assets.stream().collect(Collectors.toMap(Asset::getHost, Asset::getId));
        Map<String, Integer> assetNameAndIdMap = assets.stream().collect(Collectors.toMap(Asset::getName, Asset::getId));
        Map<Integer, String> assetIdAndHostMap = assets.stream().collect(Collectors.toMap(Asset::getId, Asset::getHost));


        int successNum = 0;
        Row row;
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i);
            if (row == null) continue;

            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            Integer projectId = null;
            String projectName = ExcelUtils.getCellValue(row.getCell(0));
            if (StringUtils.isEmpty(projectName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.PROJECT_NAME_ISNULL.getMsg());
            } else {
                projectId = projectNameAndIdMap.get(projectName);
                if (projectId == null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.PROJECT_NOTFOUND.getMsg());
                }
            }
            MonitorModule module = null;
            String moduleName = ExcelUtils.getCellValue(row.getCell(1));
            if (StringUtils.isEmpty(moduleName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.MODULE_NAME_ISNULL.getMsg());
            } else {
                module = monitorModuleMap.get(moduleName);
                if (module == null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.MODULE_NOTFOUND.getMsg());
                } else {
                    if (projectId != null && !module.getProjectId().equals(projectId)) {
                        module = null;
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.MODULE_NOTEXIS_PROJECT.getMsg());
                    }
                }
            }

            String assetName = ExcelUtils.getCellValue(row.getCell(3));
            String host = ExcelUtils.getCellValue(row.getCell(6));
            // asset name \ host 不可同时为空
            if (StringUtils.isEmpty(assetName) && StringUtils.isEmpty(host)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ENDPOINT_ASSET_NAME_HOST_ISNULL.getMsg());
            } else if (StringUtils.isNotEmpty(assetName) && StringUtils.isNotEmpty(host)) {
                // 都不为空 校验 name 是否可以找到 asset , host 格式是否正确
                if (!CommonUtils.checkIp(host)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.ASSET_HOST_FORMAT.getMsg());
                }
                if (ToolUtil.isEmpty(assetNameAndIdMap.get(assetName))) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ASSET_NOT_EXIST.getMsg());
                }
            } else if (StringUtils.isNotEmpty(assetName) && StringUtils.isEmpty(host)) {
                if (ToolUtil.isEmpty(assetNameAndIdMap.get(assetName))) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ASSET_NOT_EXIST.getMsg());
                }
            } else if (StringUtils.isEmpty(assetName) && StringUtils.isNotEmpty(host)) {
                if (!CommonUtils.checkIp(host)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.ASSET_HOST_FORMAT.getMsg());
                }
                if (ToolUtil.isEmpty(assetHostAndIdMap.get(host))) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.ASSET_NOT_EXIST.getMsg());
                }
            }

            String port = ExcelUtils.getCellValue(row.getCell(4));
            if (StringUtils.isNotEmpty(port)) {
                try {
                    Double aDouble = Double.valueOf(port);
                    if (!CommonUtils.checkPort(String.valueOf(aDouble.intValue()))) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.MODULE_PORT_ERROR.getMsg());
                    }
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.MODULE_PORT_ERROR.getMsg());
                }
            }
            String configs = ExcelUtils.getCellValue(row.getCell(5));
            if (StringUtils.isNotEmpty(configs)) {
                try {
                    JSONObject.parseObject(configs, Map.class);
                } catch (JSONException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ENDPOINT_CONFIGS_FORMAT.getMsg());
                }
            }

            String enabled = ExcelUtils.getCellValue(row.getCell(7));
            if (StringUtils.isNotEmpty(enabled)) {
                if (!"1".equals(enabled) && !"0".equals(enabled)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(7), RCode.ENDPOINT_ENABLED_INVALIDE.getMsg());
                }
            }

            if (saveFlag) {
                Integer assetIdByName = assetNameAndIdMap.get(assetName);
                Integer assetIdByHost = assetHostAndIdMap.get(host);

                MonitorEndpoint monitorEndpoint = new MonitorEndpoint();

                // endpoint asset_id endpoint_host
                if (StringUtils.isNotEmpty(assetName) && StringUtils.isNotEmpty(host)) {
                    monitorEndpoint.setAssetId(assetIdByName);
                    monitorEndpoint.setHost(host);
                } else if (StringUtils.isNotEmpty(assetName) && StringUtils.isEmpty(host)) {
                    monitorEndpoint.setAssetId(assetIdByName);
                    monitorEndpoint.setHost(assetIdAndHostMap.get(assetIdByName));
                } else if (StringUtils.isEmpty(assetName) && StringUtils.isNotEmpty(host)) {
                    monitorEndpoint.setAssetId(assetIdByHost);
                    monitorEndpoint.setHost(assetIdAndHostMap.get(assetIdByHost));
                }

                String name = ExcelUtils.getCellValue(row.getCell(2));
                if (StringUtils.isEmpty(name)) {
                    String endpointNameTmpl = module.getEndpointNameTmpl();
                    Map varsVal = new HashMap(4);
                    varsVal.put("asset.host", assetIdAndHostMap.get(monitorEndpoint.getAssetId()));
                    varsVal.put("module.name", module.getName());
                    name = expreTmplService.renderExpression(endpointNameTmpl, varsVal);
                }

                List<String> monitorEndpointNameList = moduleIdAndEndpointNamesMap.get(module.getId());
                String tempName = name;
                String monitorEndpointName = monitorEndpointNameList.stream().filter(s -> s.equals(tempName)).findAny().orElse(null);
                if (ToolUtil.isNotEmpty(monitorEndpointName)) {
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.ENDPOINT_NAME_DUPLICATE.getMsg());
                    continue;
                }


                if (StringUtils.isEmpty(configs)) {
                    configs = module.getConfigs();
                }

                monitorEndpoint.setName(name);
                monitorEndpoint.setConfigs(configs);
                monitorEndpoint.setModuleId(module.getId());
                monitorEndpoint.setPort(ToolUtil.isEmpty(port) ? module.getPort() : Double.valueOf(port).intValue());
                monitorEndpoint.setEnabled(ToolUtil.isEmpty(enabled) ? 1 : Double.valueOf(enabled).intValue());
                monitorEndpoint.setSeq(seqUUID);

                String hash = module.getId() + monitorEndpoint.getAssetId() + monitorEndpoint.getHost() + monitorEndpoint.getPort() + monitorEndpoint.getConfigs();
                monitorEndpoint.setHash(DigestUtils.md5DigestAsHex(hash.getBytes()));

                try {
                    this.save(monitorEndpoint);
                    successNum++;

                    monitorEndpointNameList.add(name);
                    moduleIdAndEndpointNamesMap.put(module.getId(), monitorEndpointNameList);
                } catch (RuntimeException e) {
                    log.error(String.format("endpoint 导入保存失败,表达式信息是：%s,错误信息是：%s", monitorEndpoint.toString(), e));
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", RCode.ENDPOINT_IMPORT_ERROR.getMsg());
                }
            }
        }
        return successNum;
    }

    private void addFailDetail(int i, List<Map> failDetails, String headerInfo, String msg) {
        Map failDetail = new HashMap(4);
        failDetail.put("lineNo", i);
        failDetail.put("errorMsg", headerInfo + ": " + msg);
        failDetails.add(failDetail);
    }
}
