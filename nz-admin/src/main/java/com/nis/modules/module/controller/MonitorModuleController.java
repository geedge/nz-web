package com.nis.modules.module.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

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
    public R remove(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.MODULE_ID_ISNULL);
        monitorModuleService.removeModules(ids);
        return R.ok();
    }


    @GetMapping("/module/template")
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        if (ToolUtil.isEmpty(language)) throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ISNULL);

        monitorModuleService.getExportTemplateByLanguage(response, language);
    }


    @PostMapping("/module/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.MODULE)
    @RequiresPermissions({"module:save"})
    public R importModulesFromExcel(MultipartFile excelFile, String language) {
        ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL)
                .and(language).notNull(RCode.EXCELFILE_HEADER_LANGUAGE_ISNULL);

        Map info = monitorModuleService.importDatacentersFromExcel(excelFile, language);
        return R.ok(info);
    }

    @GetMapping("/module/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.MODULE)
    public void exportModules(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        monitorModuleService.export(response, params);
    }


    @DeleteMapping("/module/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.MODULE)
    public R cancelImport(@RequestParam(value = "seq", required = false) String seq) {
        ValidateUtils.is(seq).notNull(RCode.MODULE_SEQ_ISNULL);

        monitorModuleService.cancelImport(seq);
        return R.ok();
    }


}
