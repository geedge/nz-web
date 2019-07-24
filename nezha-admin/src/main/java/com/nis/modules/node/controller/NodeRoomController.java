package com.nis.modules.node.controller;

import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.node.entity.NodeRoomEntity;
import com.nis.modules.node.service.NodeRoomService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * 机房 controller
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@RestController
@RequestMapping("/room")
@Api(tags="机房node-room")
public class NodeRoomController {

    @Autowired
    private NodeRoomService nodeRoomService;

    @ApiOperation("机房列表查询")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = nodeRoomService.queryPage(params);
        return R.ok().put("page", page);
    }

    @ApiOperation("机房详情查询")
    @ApiImplicitParam(name = "id", value = "id", required = true, paramType = "query", dataType = "Long")
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public R detail(@RequestParam("id") Long id) {
        NodeRoomEntity nodeRoom = nodeRoomService.detail(id);
        return R.ok(nodeRoom);
    }


    @ApiOperation("机房新增")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public R save(@RequestBody NodeRoomEntity nodeRoom) {
        nodeRoomService.saveNodeRoom(nodeRoom);
        return R.ok();
    }



    @ApiOperation("机房修改")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public R update(@RequestBody NodeRoomEntity nodeRoom) {
        nodeRoomService.updateNodeRoom(nodeRoom);
        return R.ok();
    }




    @ApiOperation("机房删除")
    @ApiImplicitParam(name = "id", value = "id", required = true, paramType = "query", dataType = "Long")
    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public R delete(@RequestParam(value = "id") Long id) {
        nodeRoomService.delete(id);
        return null;
    }

    @ApiOperation("机房名称校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "id", paramType = "query", dataType = "Long"),
            @ApiImplicitParam(name = "name", value = "name", required = true, paramType = "query", dataType = "String")
    })
    @RequestMapping(value = "/roomNameCheck", method = RequestMethod.GET)
    public R roomNameCheck(@RequestParam(value = "id", required = false) Long id, @RequestParam("name") String name) {
        if (nodeRoomService.roomNameCheck(id, name)) {
            return R.ok();
        }
        return R.error(RCode.NODEROOM_NAME_DUPLICATE);// 机房名称重复
    }
}
