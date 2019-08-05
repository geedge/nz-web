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
    @GetMapping("/list")
    public R list() {
        List<SystemNodeGroupEntity> list = nodegroupInfoService.getSysNodeGroups();
        Map<String, List<SystemNodeGroupEntity>> map = new HashMap();
        map.put("list", list);
        return R.ok().put("data", map);
    }

    @ApiOperation(" 节点组详情查询")
    @GetMapping("/detail")
    public R detail(@RequestParam("id") Integer id) {
        NodeGroupInfoEntity nodeGroup = nodegroupInfoService.detail(id);
        return R.ok().put("data", nodeGroup);
    }


    @ApiOperation(" 节点组新增")
    @PostMapping("/save")
    public R save(@RequestBody NodeGroupInfoEntity nodegroupInfo) {
        nodegroupInfoService.saveNodeGroupInfo(nodegroupInfo);
        return R.ok();
    }

    @ApiOperation(" 节点组删除")
    @DeleteMapping("/delete")
    public R delete(@RequestParam("id") Long id) {
        nodegroupInfoService.delete(id);
        return R.ok();
    }

    @ApiOperation(" 节点组修改")
    @PutMapping("/update")
    public R update(@RequestBody NodeGroupInfoEntity nodegroupInfo) {
        nodegroupInfoService.updateNodeGroupInfo(nodegroupInfo);
        return R.ok();
    }


    @ApiOperation(" 节点组名称校验")
    @GetMapping("/groupNameCheck")
    public R groupNameCheck(@RequestParam(value = "id", required = false) Long id, @RequestParam("name") String name, @RequestParam("pid") Long pid) {
        if (nodegroupInfoService.groupNameCheck(id, name, pid)) {
            return R.ok();
        }
        return R.error(RCode.NODEGROUP_NAME_DUPLICATE);//节点组名称重复错误
    }

    @ApiOperation(" 节点组移除节点")
    @DeleteMapping("/deleteNodeRel")
    public R deleteNodeRel(@RequestParam(value = "id") Long id, @RequestParam("uuids") Long[] uuids) {
        nodegroupInfoService.deleteNodeRel(id, uuids);
        return R.ok();
    }

    @ApiOperation("节点组分配节点 建立关系")
    @PostMapping("/saveNodeRel")
    public R saveNodeRel(@RequestParam(value = "id") Long id, @RequestParam("uuids") Long[] uuids) {
        nodegroupInfoService.saveNodeRel(id, uuids);
        return R.ok();
    }

    @ApiOperation("可分配节点查询")
    @GetMapping("/assList")
    public R assList(@RequestParam Map<String, Object> params) {
        PageUtils page = nodegroupInfoService.assList(params);
        return R.ok(page);
    }
}
