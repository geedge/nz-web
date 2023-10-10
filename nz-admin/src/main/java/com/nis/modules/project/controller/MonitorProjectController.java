package com.nis.modules.project.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.entity.TopoIcon;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.project.service.TopoIconService;
import com.nis.modules.project.service.TopoService;
import com.nis.modules.sys.controller.AbstractController;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitor")
public class MonitorProjectController extends AbstractController {

    @Autowired
    private TopoService topoService;

    @Autowired
    private TopoIconService topoIconService;

    @Autowired
    private MonitorProjectService monitorProjectService;

    /**
     * 详情
     *
     * @param id
     * @return
     */
    @GetMapping("/project/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.PROJECT)
    public R queryProjectEntity(@PathVariable("id") Integer id) {
        return R.ok(monitorProjectService.queryProjectEntity(id));
    }

    /**
     * project 查询
     *
     * @param params
     * @return
     */
    @GetMapping("/project")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.PROJECT)
    public R queryProjectPage(@RequestParam Map<String, Object> params) {
        PageUtils page = monitorProjectService.queryPage(params);
        return R.ok(page);
    }

    /**
     * 新增 project
     *
     * @param project
     * @return
     */
    @PostMapping("/project")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.PROJECT)
    @RequiresPermissions({"project:save"})
    public R saveProject(@RequestBody MonitorProject project) {
        ValidateUtils.is(project.getName()).notNull(RCode.PROJECT_NAME_ISNULL).maxLength(64)
        .and(project.getRemark()).maxLength(256);

        Map map = new HashMap(2);
        map.put("id", monitorProjectService.saveProject(project));
        return R.ok(map);
    }

    /**
     * 修改 project
     *
     * @param project
     * @return
     */
    @PutMapping("/project")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.PROJECT)
    @RequiresPermissions({"project:update"})
    public R editProject(@RequestBody MonitorProject project) {
        ValidateUtils.is(project.getId()).notNull(RCode.PROJECT_ID_ISNULL)
                .and(project.getName()).notNull(RCode.PROJECT_NAME_ISNULL).maxLength(64)
                .and(project.getRemark()).maxLength(256);

        Map map = new HashMap(2);
        map.put("id", monitorProjectService.updateProject(project));
        return R.ok(map);
    }

    /**
     * project 删除
     * @param ids
     * @return
     */
    @DeleteMapping("/project")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.PROJECT)
    @RequiresPermissions({"project:delete"})
    public R deleteProjects(@RequestParam(required = false, defaultValue = "0") Integer force, Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.PROJECT_ID_ISNULL);

        return monitorProjectService.delete(force, ids);
    }

    /**
     * icon unit 查询
     */
    @GetMapping("/project/topo/iconUnit")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TOPO)
    public R queryTopoUnit(String name) {
        List<String> result = topoIconService.queryUnitList(name);
        return R.ok(result);
    }

    /**
     * topo icon unit 删除
     *
     * @param unit
     * @return
     */
    @DeleteMapping("/project/topo/iconUnit")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TOPO)
    @RequiresPermissions({"project:topo:icon:delete"})
    public R deleteTopoUnit(String unit) {
        ValidateUtils.is(unit).notNull(RCode.PROJECT_TOPOICON_UNIT_ISNULL);

        topoIconService.deleteIconByUnit(unit);
        return R.ok();
    }


    /**
     * topo icon查询
     *
     * @param params
     * @return
     */
    @GetMapping("/project/topo/icon")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.PROJECT)
    public R queryIconList(@RequestParam Map<String, Object> params) {
        Map result = new HashMap(2);
        List<TopoIcon> topoIcons = topoIconService.queryIconList(params);
        result.put("list", topoIcons);
        return R.ok(result);
    }

    /**
     * topo icon新增
     *
     * @param icon
     * @param file
     * @return
     */
    @PostMapping("/project/topo/icon")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.PROJECT)
    @RequiresPermissions({"project:topo:icon:save"})
    public R save(TopoIcon icon, @RequestParam(value = "file", required = false) MultipartFile file) {
        ValidateUtils.is(icon.getName()).notNull(RCode.PROJECT_TOPOICON_NAME_ISNULL).maxLength(64)
                .and(icon.getUnit()).notNull(RCode.PROJECT_TOPOICON_UNIT_ISNULL).maxLength(64)
                .and(file).notNull(RCode.PROJECT_TOPOICON_FILE_ISNULL);

        Integer iconId = topoIconService.saveTopoIcon(icon, file);
        Map result = new HashMap(2);
        result.put("id", iconId);
        return R.ok(result);
    }


    /**
     * topo icon删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/project/topo/icon")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.PROJECT)
    @RequiresPermissions({"project:topo:icon:delete"})
    public R removeIcons(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.PROJECT_ID_ISNULL);

        topoIconService.removeIcons(ids);
        return R.ok();
    }

    /**
     * icon图标预览
     *
     * @param id
     * @param base64    1: base64格式  0：非base64格式
     * @param response
     */
    @GetMapping("/project/topo/icon/{id}/{base64}")
    public void downloadIconFile(@PathVariable("id") Integer id, @PathVariable("base64") Integer base64, HttpServletResponse response) {
        ValidateUtils.is(id).notNull(RCode.PROJECT_ICONID_ISNULL);

        topoIconService.downloadIconFileById(id, base64,response);
    }


    /**
     * 系统topo查询
     *
     * @param projectId
     */
    @GetMapping("/project/topo")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TOPO)
    public R queryTopo(Integer projectId) {
        ValidateUtils.is(projectId).notNull(RCode.PROJECT_ID_ISNULL);

        Map result = new HashMap(2);
        result.put("topo", topoService.queryTopoByProjectId(projectId));
        return R.ok(result);
    }

    /**
     * 系统topo编辑
     *
     * @param params
     * @return
     */
    @PutMapping("/project/topo")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.TOPO)
    @RequiresPermissions({"project:topo:update"})
    public R updateProjectTopo(@RequestBody Map<String, Object> params) {
        Integer projectId = (Integer) params.get("projectId");
        String topoConfig = (String) params.get("topo");

        ValidateUtils.is(projectId).notNull(RCode.PROJECT_ID_ISNULL)
                .and(topoConfig).notNull(RCode.PROJECT_TOPOCONFIG_ISNULL);

        topoService.updateProjectTopo(projectId, topoConfig);
        return R.ok();
    }


    @PostMapping("/project/topo/{projectId}/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.TOPO)
    @RequiresPermissions({"project:topo:update"})
    public R topoImport(@PathVariable(required = false) Integer projectId,
                        @RequestBody(required = false) Map<String, Object> requestBody) {
        requestBody = Tool.MapUtil.defaultIfEmpty(requestBody, Tool.MapUtil.empty());

        Map topo = (Map) requestBody.getOrDefault("topo", Tool.MapUtil.empty());

        List icons = (List) requestBody.getOrDefault("icons", Tool.ListUtil.empty());
        List<TopoIcon> topoIcons = JSONObject.parseArray(JSONUtil.toJsonStr(icons), TopoIcon.class);

        // validate param
        ValidateUtils.is(projectId).notNull(RCode.PROJECT_ID_ISNULL)
                .and(topo).notNull(RCode.PROJECT_TOPO_CONF_ISNULL)
                .and(topoIcons).notNull(RCode.PROJECT_ICON_ISNULL);

        MonitorProject project = monitorProjectService.getById(projectId);
        ValidateUtils.is(project).notNull(RCode.PROJECT_NOTFOUND);

        for (TopoIcon icon : topoIcons) {
            String type = icon.getType();
            ValidateUtils.is(icon.getName()).notNull(RCode.PROJECT_TOPOICON_NAME_ISNULL).maxLength(64)
                    .and(icon.getUnit()).notNull(RCode.PROJECT_TOPOICON_UNIT_ISNULL).maxLength(64)
                    .and(icon.getBytes()).notNull(RCode.PROJECT_TOPOICON_FILE_ISNULL)
                    .and(type).notNull(RCode.PROJECT_ICON_TYPE_ISNULL);

            if (Tool.BooleanUtil.negate(StrUtil.equalsAnyIgnoreCase(type, "jpg", "jpeg", "png", "gif", "svg"))) {
                throw new NZException(RCode.PROJECT_TOPOICON_FILETYPE_ERROR);
            }
        }

        topoService.topoImport(projectId, topo, topoIcons);
        return R.ok();
    }

    @GetMapping("/project/topo/{projectId}/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.TOPO)
    public void topoExport(@PathVariable(required = false) Integer projectId,
                           HttpServletResponse response) {
        ValidateUtils.is(projectId).notNull(RCode.PROJECT_ID_ISNULL);

        topoService.topoExport(projectId, response);
    }

}
