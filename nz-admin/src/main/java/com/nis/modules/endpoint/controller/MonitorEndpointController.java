package com.nis.modules.endpoint.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.AssetFieldValue;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


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
    public R queryEndpointEntity(@PathVariable("id") Integer id) {
        return R.ok(monitorEndpointService.queryEndpointEntity(id));
    }

    @GetMapping("/endpoint")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ENDPOINT)
    public R queryEndpointPage(@RequestParam Map<String, Object> params) {
        Map result = monitorEndpointService.queryPage(params);
        Object page = result.get("page");
        R r =new R();
        r.put("data", page);
		r.put("time", new Date());
		Object statFlag = params.getOrDefault("statistics", 0);
        if(Tool.ObjectUtil.equal("1", String.valueOf(statFlag))) {
        	Map<String, List> endpointGroupBy = monitorEndpointService.queryEndpointGroupBy(params);
        	r.put("statistics", endpointGroupBy);
        }
        return r;
    }

    /**
     * endpoint tree 三级列表 project -> module -> endpoint
     *
     * @param name
     * @return
     */
    @GetMapping("/endpoint/tree")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ENDPOINT)
    public R queryEndpointTree(String name) {
        Map map = new HashMap(2);
        map.put("list", monitorEndpointService.queryEndpointTree(name));
        return R.ok(map);
    }

    @PostMapping("/endpoint")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:save"})
    public R saveEndpoint(@RequestBody MonitorEndpoint endpoint) {
        ValidateUtils.is(endpoint.getName()).notNull(RCode.ENDPOINT_NAME_ISNULL).maxLength(128)
                .and(endpoint.getModuleId()).notNull(RCode.ENDPOINT_MODULEID_ISNULL)
                .and(endpoint.getAssetId()).notNull(RCode.ENDPOINT_ASSETID_ISNULL);

        return R.ok().putData("id", monitorEndpointService.saveEndpoint(endpoint));
    }

    @PutMapping("/endpoint")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:update"})
    public R modify(@RequestBody MonitorEndpoint endpoint) {
        ValidateUtils.is(endpoint.getId()).notNull(RCode.ENDPOINT_ID_ISNULL)
                .and(endpoint.getName()).notNull(RCode.ENDPOINT_NAME_ISNULL).maxLength(128)
                .and(endpoint.getModuleId()).notNull(RCode.ENDPOINT_MODULEID_ISNULL)
                .and(endpoint.getAssetId()).notNull(RCode.ENDPOINT_ASSETID_ISNULL);

        return R.ok().putData("id", monitorEndpointService.updateEndpoint(endpoint));
    }

    @PostMapping("/endpoint/bulk")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:save"})
    public R saveBatchEndpoint(@RequestBody List<MonitorEndpoint> endpointList) {
        return R.ok(monitorEndpointService.saveBatchEndpoint(endpointList));
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
    public void getExportTemplateByLanguage(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String language = request.getHeader(Constant.LANGUAGE);
        String format = request.getHeader("format");
        format = Tool.StrUtil.emptyToDefault(format, "1");

        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        // 校验 FORMAT
        if (!Arrays.asList(Constant.IMPORTFILE_TYPE_INTVALUES).contains(format)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }

        monitorEndpointService.getTemplateByLanguage(response, format);
    }

    /**
     * 撤销导入操作
     *
     * @param seq
     * @return
     */
    @DeleteMapping("/endpoint/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ENDPOINT)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);

        monitorEndpointService.cancelImport(seq);
        return R.ok();
    }

    @PostMapping("/endpoint/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.ENDPOINT)
    @RequiresPermissions({"endpoint:save"})
    public R importEndpointsData(MultipartFile excelFile, HttpServletRequest request) throws IOException {
        String language = request.getHeader(Constant.LANGUAGE);
        ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);
        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }

        Map map = monitorEndpointService.importEndpointData(excelFile);
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
    public void exportEndpointsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
        Object format = params.get("format");
        String formatStr = format == null ? "1" : format.toString();
        // 校验 FORMAT
        if (!Arrays.asList(Constant.IMPORTFILE_TYPE_INTVALUES).contains(formatStr)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }
        params.put("format", formatStr);

        monitorEndpointService.export(response, params);
    }

    @RequestMapping(value = "/endpoint/render", method = {RequestMethod.POST, RequestMethod.PUT})
    public R render(@RequestBody Map<String, String> params) {
        ValidateUtils.is(params.get("moduleIds")).notNull(RCode.MODULE_ID_ISNULL)
                .and(params.get("assetIds")).notNull(RCode.ASSET_ID_ISNULL);

        return R.ok().putData("list", monitorEndpointService.render(params));
    }

    /**
     * endpoint配置同步接口
     */
    @PutMapping( "/endpoint/syncTmpl")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ENDPOINT)
    public R syncTmpl(@RequestBody Map<String, List<Integer>> params) {
        ValidateUtils.notAllNull(RCode.ENDPOINT_MODULE_ASSET_ID_ISALLNULL, params.get("moduleIds"), params.get("endpointIds"), params.get("assetIds"));
        monitorEndpointService.syncTmpl(params.get("moduleIds"), params.get("endpointIds"), params.get("assetIds"));
        return R.ok();
    }

    /**
     * endpoint 批量修改
     */
    @PutMapping( "/endpoint/bulk")
    @RequiresPermissions({"endpoint:update"})
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ENDPOINT)
    public R bulk(@RequestBody Map<String, Object> params) {
        ValidateUtils.is(params.get("editType")).notNull(RCode.ENDPOINT_EDITTYPE_ISNULL)
                .and(params.get("ids")).notNull(RCode.ENDPOINT_ID_ISNULL);
        monitorEndpointService.bulkEdit(params);
        return R.ok();
    }

    /**
     * endpoint 错误诊断
     */
    @GetMapping( "/endpoint/diagnose/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ENDPOINT)
    public R diagnose(@PathVariable("id") Integer id) {
        ValidateUtils.is(id).notNull(RCode.ENDPOINT_ID_ISNULL);
        return R.ok(monitorEndpointService.diagnose(id));
    }

}

