package com.nis.modules.node.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.node.entity.AvailCabinetEntity;
import com.nis.modules.node.entity.NodeCabinetEntity;
import com.nis.modules.node.service.NodeCabinetService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 机柜信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@RestController
@RequestMapping("/cab")
@Api(tags = "机柜node-cabinet")
public class NodeCabinetController {

    @Autowired
    private NodeCabinetService nodeCabinetService;

    @ApiOperation("机柜列表查询")
    @GetMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = nodeCabinetService.queryPage(params);
        return R.ok(page);
    }

    @ApiOperation("机柜详情查询")
    @GetMapping(value = "/detail")
    public R detail(Long id) {
        ValidateUtils.is(id).notNull(RCode.NODECABINET_ID_ISNULL);

        NodeCabinetEntity detail = nodeCabinetService.detail(id);
        return R.ok(detail);
    }

    @ApiOperation("机柜新增")
    @PostMapping("/save")
    public R save(@RequestBody NodeCabinetEntity nodeCabinet) {
        ValidateUtils.is(nodeCabinet).notNull(RCode.NOT_NULL_ERROR)
                .and(nodeCabinet.getName()).notNull(RCode.NODECABINET_NAME_ISNULL)
                .and(nodeCabinet.getRoomId()).notNull(RCode.NODEROOM_ID_ISNULL)
                .and(nodeCabinet.getUbit()).notNull(RCode.NODECABINET_UBIT_ISNULL).min(1, RCode.NODECABINET_UBIT_MINNUM);

        nodeCabinetService.saveNodeCabinet(nodeCabinet);
        return R.ok();
    }

    @ApiOperation("机柜修改")
    @PutMapping("/update")
    public R update(@RequestBody NodeCabinetEntity nodeCabinet) {
        ValidateUtils.is(nodeCabinet).notNull(RCode.NOT_NULL_ERROR)
                .and(nodeCabinet.getId()).notNull(RCode.NODECABINET_ID_ISNULL)
                .and(nodeCabinet.getName()).notNull(RCode.NODECABINET_NAME_ISNULL)
                .and(nodeCabinet.getRoomId()).notNull(RCode.NODEROOM_ID_ISNULL)
                .and(nodeCabinet.getUbit()).notNull(RCode.NODECABINET_UBIT_ISNULL).min(1, RCode.NODECABINET_UBIT_MINNUM);

        nodeCabinetService.updateNodeCabinet(nodeCabinet);
        return R.ok();
    }

    @ApiOperation("机柜删除")
    @DeleteMapping("/delete")
    public R delete(Long[] ids) {
        ValidateUtils.is(ids).notNull(RCode.NODECABINET_ID_ISNULL);

        nodeCabinetService.delete(ids);
        return R.ok();
    }

    @ApiOperation("机柜名称校验")
    @GetMapping("/cabNameCheck")
    public R cabNameCheck(Long id, String name, Long roomId) {
        ValidateUtils.is(name).notNull(RCode.NODECABINET_NAME_ISNULL)
                .and(roomId).notNull(RCode.NODEROOM_ID_ISNULL);

        if (nodeCabinetService.cabNameCheck(id, name, roomId)) {
            return R.ok();
        }
        return R.error(RCode.NODECABINET_NAME_DUPLICATE);// 机柜名称重复错误
    }


    @ApiOperation("校验U位数量是否可用")
    @GetMapping("/cabUbitCheck")
    public R cabUbitCheck(Long id, Integer ubit) {
        ValidateUtils.is(id).notNull(RCode.NODECABINET_ID_ISNULL)
                .and(ubit).notNull(RCode.NODECABINET_UBIT_ISNULL).min(1, RCode.NODECABINET_UBIT_MINNUM);

        return nodeCabinetService.cabUbitCheck(id, ubit);
    }


    @ApiOperation("查询机柜下可选的机房及行号列号")
    @GetMapping("/optionalRoom")
    public R optionalRoom(Long id) {
        List<AvailCabinetEntity> availCabinetEntities = nodeCabinetService.optionalRoom(id);
        Map map = new HashMap();
        map.put("list", availCabinetEntities);
        return R.ok(map);
    }

}
