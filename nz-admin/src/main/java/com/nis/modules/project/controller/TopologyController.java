package com.nis.modules.project.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.project.entity.TopoIcon;
import com.nis.modules.project.service.TopoIconService;
import com.nis.modules.sys.controller.AbstractController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class TopologyController extends AbstractController {

    @Autowired
    private TopoIconService topoIconService;

    /**
     * unit 查询
     */
    @GetMapping("/topology/unit")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TOPO)
    public R queryUnit(String name) {
        List<String> result = topoIconService.queryUnitList(name);
        return R.ok(result);
    }


    /**
     * unit 修改
     */
    @PutMapping("/topology/unit")
    @RequiresPermissions({"project:topo:icon:save"})
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.TOPO)
    public R updateUnit(@RequestBody(required = false) Map<String, String> requestBody) {
        // validate param
        requestBody = Tool.MapUtil.defaultIfEmpty(requestBody, Tool.MapUtil.empty());

        String oldUnitName = requestBody.get("old");
        String newUnitName = requestBody.get("new");

        ValidateUtils.is(oldUnitName).notNull(RCode.PROJECT_TOPOICON_UNIT_ISNULL)
                .and(newUnitName).notNull(RCode.PROJECT_TOPOICON_UNIT_ISNULL).maxLength(64);

        topoIconService.renameUnit(oldUnitName, newUnitName);
        return R.ok();
    }


    /**
     * unit 删除
     */
    @DeleteMapping("/topology/unit")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TOPO)
    @RequiresPermissions({"project:topo:icon:delete"})
    public R deleteUnit(String unit) {
        ValidateUtils.is(unit).notNull(RCode.PROJECT_TOPOICON_UNIT_ISNULL);

        topoIconService.deleteIconByUnit(unit);
        return R.ok();
    }


    /**
     * icon 查询
     */
    @GetMapping("/topology/icon")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TOPO)
    public R queryIcon(@RequestParam Map<String, Object> params) {
        Map result = new HashMap(2);
        List<TopoIcon> topoIcons = topoIconService.queryIconList(params);
        result.put("list", topoIcons);
        return R.ok(result);
    }

    /**
     * icon 新增
     */
    @PostMapping("/topology/icon")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.TOPO)
    @RequiresPermissions({"project:topo:icon:save"})
    public R saveIcon(TopoIcon icon, @RequestParam(value = "file", required = false) MultipartFile file) {
        ValidateUtils.is(icon.getName()).notNull(RCode.PROJECT_TOPOICON_NAME_ISNULL).maxLength(64)
                .and(icon.getUnit()).notNull(RCode.PROJECT_TOPOICON_UNIT_ISNULL).maxLength(64)
                .and(file).notNull(RCode.PROJECT_TOPOICON_FILE_ISNULL);

        Integer iconId = topoIconService.saveTopoIcon(icon, file);
        return R.ok().putData("id", iconId);
    }

    /**
     * icon 删除
     */
    @DeleteMapping("/topology/icon")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.TOPO)
    @RequiresPermissions({"project:topo:icon:delete"})
    public R removeIcon(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.PROJECT_ID_ISNULL);

        topoIconService.removeIcons(ids);
        return R.ok();
    }


    /**
     * icon 图片下载
     *
     * @param base64 1: base64格式  0：非base64格式
     */
    @GetMapping("/topology/icon/{base64}/{unit}/{name}")
    public void downloadIconFile(
            @PathVariable(value = "base64") Integer base64,
            @PathVariable(value = "unit") String unit,
            @PathVariable(value = "name") String name,
            HttpServletResponse response) {

        topoIconService.downloadIconFile(unit, name, base64, response);
    }


}
