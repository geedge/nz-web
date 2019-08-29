package com.nis.modules.node.controller;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.service.NodeGroupInfoService;
import com.nis.modules.sys.entity.SystemInfoEntity;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 节点组信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@RestController
@RequestMapping("/nodegroup")
@Api(tags = "节点组node-group")
class NodeGroupInfoController {

    @Autowired
    private NodeGroupInfoService nodegroupInfoService;

    @ApiOperation("节点组列表")
    @GetMapping("/list")
    public R list() {
        List<SystemInfoEntity> list = nodegroupInfoService.getSysNodeGroups();
        Map<String, List<SystemInfoEntity>> map = new HashMap();
        map.put("list", list);
        return R.ok(map);
    }

    @ApiOperation(" 节点组详情查询")
    @GetMapping("/detail")
    public R detail(@RequestParam(value = "id", required = false) Long id) {
        ValidateUtils.is(id).notNull(RCode.NODEGROUP_GROUPID_ISNULL);

        NodeGroupInfoEntity nodeGroup = nodegroupInfoService.detail(id);
        return R.ok(nodeGroup);
    }


    @ApiOperation(" 节点组新增")
    @PostMapping("/save")
    public R save(@RequestBody NodeGroupInfoEntity nodegroupInfo) {
        ValidateUtils.is(nodegroupInfo).notNull(RCode.NOT_NULL_ERROR)
                .and(nodegroupInfo.getName()).notNull(RCode.NODEGROUP_NAME_ISNULL)
                .and(nodegroupInfo.getPid()).notNull(RCode.NODEGROUP_PID_ISNULL);

        nodegroupInfoService.saveNodeGroupInfo(nodegroupInfo);
        return R.ok();
    }

    @ApiOperation(" 节点组删除")
    @DeleteMapping("/delete")
    public R delete(@RequestParam(value = "id", required = false) Long id) {
        ValidateUtils.is(id).notNull(RCode.NODEGROUP_GROUPID_ISNULL);

        nodegroupInfoService.delete(id);
        return R.ok();
    }

    @ApiOperation(" 节点组修改")
    @PutMapping("/update")
    public R update(@RequestBody NodeGroupInfoEntity nodegroupInfo) {
        ValidateUtils.is(nodegroupInfo).notNull(RCode.NOT_NULL_ERROR)
                .and(nodegroupInfo.getName()).notNull(RCode.NODEGROUP_NAME_ISNULL);
        if (nodegroupInfo != null) {
            if (nodegroupInfo.getSystemId() != null) {
                throw new NZException(RCode.NODEGROUP_SYSTEMID_NOT_MODIFIED);
            }
            if (nodegroupInfo.getPid() != null) {
                throw new NZException(RCode.NODEGROUP_PARENTID_NOT_MODIFIED);
            }
            if (nodegroupInfo.getPids() != null || StringUtils.isNotBlank(nodegroupInfo.getPids())) {
                throw new NZException(RCode.NODEGROUP_PARENTIDS_NOT_MODIFIED);
            }
        }
        nodegroupInfoService.updateNodeGroupInfo(nodegroupInfo);
        return R.ok();
    }


    @ApiOperation(" 节点组名称校验")
    @GetMapping("/groupNameCheck")
    public R groupNameCheck(@RequestParam(value = "id", required = false) Long id, @RequestParam(value = "name", required = false) String name, @RequestParam(value = "pid", required = false) Long pid) {
        ValidateUtils.is(name).notNull(RCode.NODEGROUP_NAME_ISNULL).and(pid).notNull(RCode.NODEGROUP_PID_ISNULL);

        if (nodegroupInfoService.groupNameCheck(id, name, pid)) {
            return R.ok();
        }
        return R.error(RCode.NODEGROUP_NAME_DUPLICATE);//节点组名称重复错误
    }

    @ApiOperation(" 节点组移除节点")
    @DeleteMapping("/deleteNodeRel")
    public R deleteNodeRel(@RequestParam(value = "id", required = false) Long id, @RequestParam(value = "uuids", required = false) Long[] uuids) {
        ValidateUtils.is(id).notNull(RCode.NODEGROUP_GROUPID_ISNULL)
                .and(uuids).notNull(RCode.NODE_UUID_ISNULL);

        nodegroupInfoService.deleteNodeRel(id, uuids);
        return R.ok();
    }

    @ApiOperation("节点组分配节点 建立关系")
    @PostMapping("/saveNodeRel")
    public R saveNodeRel(@RequestParam(value = "id", required = false) Long id, @RequestParam(value = "uuids", required = false) Long[] uuids) {
        ValidateUtils.is(id).notNull(RCode.NODEGROUP_GROUPID_ISNULL)
                .and(uuids).notNull(RCode.NODE_UUID_ISNULL);

        nodegroupInfoService.saveNodeRel(id, uuids);
        return R.ok();
    }

    @ApiOperation("可分配节点查询")
    @GetMapping("/assList")
    public R assList(@RequestParam Map<String, Object> params) {
        ValidateUtils.is(params).notNull(RCode.NOT_NULL_ERROR);
        if (params.get("systemId") == null || StringUtils.isBlank(params.get("systemId").toString())) {
            throw new NZException(RCode.NODEGROUP_SYSTEMID_ISNULL);
        }
        if (params.get("groupId") == null || StringUtils.isBlank(params.get("groupId").toString())) {
            throw new NZException(RCode.NODEGROUP_GROUPID_ISNULL);
        }

        PageUtils page = nodegroupInfoService.assList(params);
        return R.ok(page);
    }
}
