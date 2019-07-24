package com.nis.modules.node.controller;

import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.SystemNodeGroupEntity;
import com.nis.modules.node.service.NodeGroupInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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
@Api(tags="节点组node-group")
class NodeGroupInfoController {

    @Autowired
    private NodeGroupInfoService nodegroupInfoService;

    @ApiOperation("节点组列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public R list() {
        List<SystemNodeGroupEntity> list = nodegroupInfoService.getSysNodeGroups();
        Map<String, List<SystemNodeGroupEntity>> map = new HashMap();
        map.put("list", list);
        return R.ok().put("data", map);
    }

    @ApiOperation(" 节点组详情查询")
    @ApiImplicitParam(name = "id", value = "id", required = true, paramType = "query", dataType = "Integer")
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public R detail(@RequestParam("id") Integer id) {
        NodeGroupInfoEntity nodeGroup = nodegroupInfoService.detail(id);
        return R.ok().put("data", nodeGroup);
    }


    @ApiOperation(" 节点组新增")
    @RequestMapping(value = "save", method = RequestMethod.POST)
    public R save(@RequestBody NodeGroupInfoEntity nodegroupInfo) {
        nodegroupInfoService.saveNodeGroupInfo(nodegroupInfo);
        return R.ok();
    }

    @ApiOperation(" 节点组删除")
    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public R delete(@RequestParam("id") Long id) {
        nodegroupInfoService.delete(id);
        return null;
    }

    @ApiOperation(" 节点组修改")
    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    public R update(@RequestBody NodeGroupInfoEntity nodegroupInfo) {
        nodegroupInfoService.updateNodeGroupInfo(nodegroupInfo);
        return R.ok();
    }


    @ApiOperation(" 节点组名称校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "id", paramType = "query", dataType = "Long"),
            @ApiImplicitParam(name = "name", value = "name", required = true, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "pid", value = "pid", required = true, paramType = "query", dataType = "Long")
    })
    @RequestMapping(value = "/groupNameCheck", method = RequestMethod.GET)
    public R groupNameCheck(@RequestParam(value = "id", required = false) Long id, @RequestParam("name") String name, @RequestParam("pid") Long pid) {
        if (nodegroupInfoService.groupNameCheck(id, name, pid)) {
            return R.ok();
        }
        return R.error(RCode.NODEGROUP_NAME_DUPLICATE);//节点组名称重复错误
    }

    @ApiOperation(" 节点组移除节点")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "id", required = true, paramType = "query", dataType = "Long"),
            @ApiImplicitParam(name = "uuids", value = "uuids", required = true, paramType = "query", dataType = "Long", allowMultiple = true)
    })
    @RequestMapping(value = "/deleteNodeRel", method = RequestMethod.DELETE)
    public R deleteNodeRel(@RequestParam(value = "id") Long id, @RequestParam("uuids") Long[] uuids) {
        nodegroupInfoService.deleteNodeRel(id, uuids);
        return R.ok();
    }

    @ApiOperation("节点组分配节点 建立关系")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "id", required = true, paramType = "query", dataType = "Long"),
            @ApiImplicitParam(name = "uuids", value = "uuids", required = true, paramType = "query", dataType = "Long", allowMultiple = true)
    })

    @RequestMapping(value = "/saveNodeRel", method = RequestMethod.POST)
    public R saveNodeRel(@RequestParam(value = "id") Long id, @RequestParam("uuids") Long[] uuids) {
        nodegroupInfoService.saveNodeRel(id, uuids);
        return R.ok();
    }

    @ApiOperation("可分配节点查询")
    @RequestMapping(value = "/assList", method = RequestMethod.GET)
    public R assList(@RequestParam Map<String, Object> params) {
        PageUtils page = nodegroupInfoService.assList(params);
        return R.ok(page);
    }
}
