package com.nis.modules.node.controller;

import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.node.entity.NodegroupInfoEntity;
import com.nis.modules.node.entity.SystemNodeGroupEntity;
import com.nis.modules.node.service.NodegroupInfoService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
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
public class NodegroupInfoController {

    @Autowired
    private NodegroupInfoService nodegroupInfoService;

    /**
     * 列表
     */
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
    public R detail(@RequestParam Integer id) {
        NodegroupInfoEntity nodeGroup = nodegroupInfoService.detail(id);
        return R.ok().put("data", nodeGroup);
    }


    @ApiOperation(" 节点组新增")
    @RequestMapping(value = "save", method = RequestMethod.POST)
    public R save(@RequestBody NodegroupInfoEntity nodegroupInfo) {
        nodegroupInfoService.saveNodeGroupInfo(nodegroupInfo);
        return R.ok();
    }

    @ApiOperation(" 节点组修改")
    @RequestMapping(value = "/update",method = RequestMethod.PUT)
    public R update(@RequestBody NodegroupInfoEntity nodegroupInfo){
        nodegroupInfoService.updateNodeGroupInfo(nodegroupInfo);
        return null;
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

}
