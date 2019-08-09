package com.nis.modules.node.controller;

import com.nis.common.smartvalidate.ValidateUtils;
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
@Api(tags = "机房node-room")
public class NodeRoomController {

    @Autowired
    private NodeRoomService nodeRoomService;

    @ApiOperation("机房列表查询")
    @GetMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = nodeRoomService.queryPage(params);
        return R.ok().put("page", page);
    }

    @ApiOperation("机房详情查询")
    @GetMapping("/detail")
    public R detail(@RequestParam("id") Long id) {
        ValidateUtils.is(id).notNull(RCode.NODEROOM_ID_ISNULL);

        NodeRoomEntity nodeRoom = nodeRoomService.detail(id);
        return R.ok(nodeRoom);
    }


    @ApiOperation("机房新增")
    @PostMapping("/save")
    public R save(@RequestBody NodeRoomEntity nodeRoom) {
        ValidateUtils.is(nodeRoom.getName()).notNull(RCode.NODEROOM_NAME_ISNULL)
                .and(nodeRoom.getRoomRows()).notNull(RCode.NODEROOM_ROWS_ISNULL).min(1,RCode.NODEROOM_ROWS_MINNUM)
                .and(nodeRoom.getRoomCols()).notNull(RCode.NODEROOM_COLS_ISNULL).min(1,RCode.NODEROOM_COLS_MINNUM)
                .and(nodeRoom.getAddr()).notNull(RCode.NODEROOM_ADDR_ISNULL);

        nodeRoomService.saveNodeRoom(nodeRoom);
        return R.ok();
    }


    @ApiOperation("机房修改")
    @PutMapping("/update")
    public R update(@RequestBody NodeRoomEntity nodeRoom) {
        ValidateUtils.is(nodeRoom.getName()).notNull(RCode.NODEROOM_NAME_ISNULL)
                .and(nodeRoom.getRoomRows()).notNull(RCode.NODEROOM_ROWS_ISNULL).min(1,RCode.NODEROOM_ROWS_MINNUM)
                .and(nodeRoom.getRoomCols()).notNull(RCode.NODEROOM_COLS_ISNULL).min(1,RCode.NODEROOM_COLS_MINNUM)
                .and(nodeRoom.getAddr()).notNull(RCode.NODEROOM_ADDR_ISNULL);

        nodeRoomService.updateNodeRoom(nodeRoom);
        return R.ok();
    }


    @ApiOperation("机房删除")
    @DeleteMapping("/delete")
    public R delete(@RequestParam(value = "ids") Long[] ids) {
        ValidateUtils.is(ids).notNull(RCode.NODEROOM_ID_ISNULL);

        nodeRoomService.delete(ids);
        return R.ok();
    }

    @ApiOperation("机房名称校验")
    @GetMapping("/roomNameCheck")
    public R roomNameCheck(@RequestParam(value = "id", required = false) Long id, @RequestParam("name") String name) {
        ValidateUtils.is(name).notNull(RCode.NODEROOM_NAME_ISNULL);

        if (nodeRoomService.roomNameCheck(id, name)) {
            return R.ok();
        }
        return R.error(RCode.NODEROOM_NAME_DUPLICATE);// 机房名称重复
    }

    @ApiOperation("机房修改时行列校验")
    @GetMapping("/roomSizeCheck")
    public R roomSizeCheck(@RequestParam("id") Long id, @RequestParam("roomRows") Integer roomRows, @RequestParam("roomCols") Integer roomCols) {
        ValidateUtils.is(id).notNull(RCode.NODEROOM_ID_ISNULL)
                .and(roomRows).notNull(RCode.NODEROOM_ROWS_ISNULL)
                .and(roomCols).notNull(RCode.NODEROOM_COLS_ISNULL);

        nodeRoomService.roomSizeCheck(id, roomRows, roomCols);
        return R.ok();
    }

}
