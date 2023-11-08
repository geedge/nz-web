package com.nis.modules.module.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/monitor")
public class MonitorModuleController {

    @Autowired
    private MonitorModuleService monitorModuleService;

    /**
     * 详情
     *
     * @param id
     * @return
     */
    @GetMapping("/module/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.MODULE)
    public R queryModuleEntity(@PathVariable("id") Integer id) {
        return R.ok(monitorModuleService.queryModuleInfo(id));
    }

    /**
     * 列表
     *
     * @param params
     * @return
     */
    @GetMapping("/module")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.MODULE)
    public R queryModulePage(@RequestParam Map<String, Object> params) {
        PageUtils page = monitorModuleService.queryPage(params);
        return R.ok(page);
    }

    /**
     * module tree 两级列表 project -> module
     *
     * @param name
     * @return
     */
    @GetMapping("/module/tree")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.MODULE)
    public R queryModuleTree(String name) {
        Map map = new HashMap(2);
        map.put("list", monitorModuleService.queryModuleTree(name));
        return R.ok(map);
    }

    @PostMapping("/module")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.MODULE)
    @RequiresPermissions({"module:save"})
    public R saveModule(@RequestBody MonitorModule module) {
        ValidateUtils.is(module.getProjectId()).notNull(RCode.MODULE_PROJECTID_ISNULL)
                .and(module.getName()).notNull(RCode.MODULE_NAME_ISNULL).maxLength(64)
                .and(module.getEndpointNameTmpl()).notNull(RCode.MODULE_ENDPOINTNAMETMPL_ISNULL)
                .and(module.getConfigs()).notNull(RCode.MODULE_CONFIGS_ISNULL)
                .and(module.getRemark()).maxLength(256);

        Map map = new HashMap(2);
        map.put("id", monitorModuleService.saveModule(module));
        return R.ok(map);
    }

    @PutMapping("/module")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.MODULE)
    @RequiresPermissions({"module:update"})
    public R editModule(@RequestBody MonitorModule module) {
        ValidateUtils.is(module.getId()).notNull(RCode.MODULE_ID_ISNULL)
                .and(module.getProjectId()).notNull(RCode.MODULE_PROJECTID_ISNULL)
                .and(module.getName()).notNull(RCode.MODULE_NAME_ISNULL).maxLength(64)
                .and(module.getEndpointNameTmpl()).notNull(RCode.MODULE_ENDPOINTNAMETMPL_ISNULL)
                .and(module.getConfigs()).notNull(RCode.MODULE_CONFIGS_ISNULL)
                .and(module.getRemark()).maxLength(256);

        Map map = new HashMap(2);
        map.put("id", monitorModuleService.updateModule(module));
        return R.ok(map);
    }


    @DeleteMapping("/module")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.MODULE)
    @RequiresPermissions({"module:delete"})
    public R remove(@RequestParam(required = false ,defaultValue = "0") Integer force, Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.MODULE_ID_ISNULL);

        return monitorModuleService.removeModules(force, ids);
    }


    @GetMapping("/module/template")
    @SysLog(operation = OperationEnum.DOWNLOAD, type = TypeEnum.MODULE)
    public void getTemplateByLanguage(HttpServletResponse response,
                                      @RequestParam(required = false, defaultValue = "en") String language,
                                      @RequestParam(required = false, defaultValue = "1") String format) throws IOException {

        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(format)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }

        monitorModuleService.getTemplateByLanguage(response, format);
    }


    @PostMapping("/module/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.MODULE)
    @RequiresPermissions({"module:save"})
    public R importModules(@RequestParam(value = "file") MultipartFile excelFile, HttpServletRequest request, @RequestParam Map<String, Object> params) throws IOException {
        String language = request.getHeader(Constant.LANGUAGE);
        ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);
        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }

        Map info = monitorModuleService.importModuleData(excelFile, params);
        return R.ok(info);
    }

    @GetMapping("/module/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.MODULE)
    public void exportModules(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
        Object format = params.get("format");
        String formatStr = format == null ? "1" : format.toString();
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(formatStr)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }
        params.put("format", formatStr);

        monitorModuleService.export(response, params);
    }


    @DeleteMapping("/module/cancelImport")
    @SysLog(operation = OperationEnum.CANCEL, type = TypeEnum.MODULE)
    public R cancelImport(@RequestParam(value = "seq", required = false) String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);

        monitorModuleService.cancelImport(seq);
        return R.ok();
    }

    /**
     * Module 配置同步更新 dashboard，endpoint
     *
     * @return
     */
    @RequestMapping(value = "/module/sync", method = {RequestMethod.POST, RequestMethod.PUT})
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.MODULE)
    public R syncModuleEndpointAndDashboard(@RequestBody(required = false) Map<String, Object> requestParam) {
        // validate param
        requestParam = Tool.MapUtil.defaultIfEmpty(requestParam, Tool.MapUtil.empty());

        List<Integer> syncModuleIdList = (List<Integer>) requestParam.get("ids");
        ValidateUtils.is(syncModuleIdList).notNull(RCode.MODULE_ID_ISNULL);

        Integer isSyncEndpoint = Tool.MapUtil.getInt(requestParam, "endpoint", 0);
        Integer isSyncDashboard = Tool.MapUtil.getInt(requestParam, "dashboard", 0);

        monitorModuleService.syncModuleEndpointAndDashboard(syncModuleIdList, isSyncEndpoint, isSyncDashboard);
        return R.ok();
    }


}
