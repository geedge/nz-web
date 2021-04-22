package com.nis.modules.endpoint.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/monitor")
public class MonitorEndpointController {

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    /**
     * 详情
     *
     * @param id
     * @return
     */
    @GetMapping("/endpoint/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:info"})
    public R queryEndpointEntity(@PathVariable("id") Integer id) {
        return R.ok(monitorEndpointService.queryEndpointEntity(id));
    }

    @GetMapping("/endpoint")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:list"})
    public R queryEndpointPage(@RequestParam Map<String, Object> params) {
        PageUtils page = monitorEndpointService.queryPage(params);
        return R.ok(page);
    }

    /**
     * endpoint tree 三级列表 project -> module -> endpoint
     *
     * @param name
     * @return
     */
    @GetMapping("/endpoint/tree")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:list"})
    public R queryEndpointTree(String name) {
        Map map = new HashMap(2);
        map.put("list", monitorEndpointService.queryEndpointTree(name));
        return R.ok(map);
    }

    @PostMapping("/endpoint")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:save"})
    public R saveEndpoint(@RequestBody List<MonitorEndpoint> endpointList) {
        monitorEndpointService.saveOrUpdateBatchEndpointList(endpointList);
        return R.ok();
    }

    @PutMapping("/endpoint")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:update"})
    public R modify(@RequestBody List<MonitorEndpoint> endpointList) {
        for (MonitorEndpoint endpointEntity : endpointList) {
            ValidateUtils.is(endpointEntity.getId()).notNull(RCode.ENDPOINT_ID_ISNULL);
        }
        monitorEndpointService.saveOrUpdateBatchEndpointList(endpointList);
        return R.ok();
    }

    @DeleteMapping("/endpoint")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:delete"})
    public R remove(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ENDPOINT_ID_ISNULL);

        monitorEndpointService.deleteEndpointByIds(Arrays.asList(ids));
        return R.ok();
    }

    @GetMapping("/endpoint/template")
    @RequiresPermissions({"endpoint:import:template"})
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        if (ToolUtil.isEmpty(language)) throw new NZException(RCode.ENDPOINT_EXPORT_HEADER_LANGUAGE_ISNULL);

        monitorEndpointService.getExportTemplateByLanguage(response, language);
    }

    /**
     * 撤销导入操作
     *
     * @param seq
     * @return
     */
    @DeleteMapping("/endpoint/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:cancel:import"})
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.ENDPOINT_CANCELIMPORTSEQ_ISNULL);

        monitorEndpointService.cancelImport(seq);
        return R.ok();
    }

    @PostMapping("/endpoint/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:import"})
    public R importEndpoint(MultipartFile excelFile, String language) {
        ValidateUtils.is(excelFile).notNull(RCode.ENDPOINT_IMPORT_FILE_ISNULL)
                .and(language).notNull(RCode.ENDPOINT_EXPORT_HEADER_LANGUAGE_ISNULL);

        Map map = monitorEndpointService.importEndpoint(excelFile, language);
        return R.ok(map);
    }

    /**
     * 批量导出endpoint
     *
     * @param response
     * @param params
     */
    @GetMapping("/endpoint/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:export"})
    public void exportEndpointsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        monitorEndpointService.export(response, params);
    }

}

