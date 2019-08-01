package com.nis.modules.node.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.node.entity.AvailCabinetEntity;
import com.nis.modules.node.entity.NodeCabinetEntity;
import com.nis.modules.node.service.NodeCabinetService;
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

    /**
     * 列表
     */
    @ApiOperation("机柜列表查询")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = nodeCabinetService.queryPage(params);
        return R.ok(page);
    }

    @ApiOperation("机柜详情查询")
    @ApiImplicitParam(name = "id", value = "id", required = true, paramType = "query", dataType = "Long")
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public R detail(@RequestParam("id") Long id) {
        NodeCabinetEntity detail = nodeCabinetService.detail(id);
        return R.ok(detail);
    }

    @ApiOperation("机柜新增")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public R save(@RequestBody NodeCabinetEntity nodeCabinet) {
        nodeCabinetService.saveNodeCabinet(nodeCabinet);
        return R.ok();
    }

    @ApiOperation("机柜修改")
    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    public R update(@RequestBody NodeCabinetEntity nodeCabinet) {
        nodeCabinetService.updateNodeCabinet(nodeCabinet);
        return R.ok();
    }

    @ApiOperation("机柜删除")
    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public R delete(@RequestParam("id") Long id) {
        nodeCabinetService.delete(id);
        return R.ok();
    }

    @ApiOperation("机柜名称校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "id", paramType = "query", dataType = "Long"),
            @ApiImplicitParam(name = "name", value = "name", required = true, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "roomId", value = "roomId", required = true, paramType = "query", dataType = "Long")
    })
    @RequestMapping(value = "/cabNameCheck", method = RequestMethod.GET)
    public R cabNameCheck(@RequestParam(value = "id", required = false) Long id, @RequestParam("name") String name, @RequestParam("roomId") Long roomId) {
        if (nodeCabinetService.cabNameCheck(id, name, roomId)) {
            return R.ok();
        }
        return R.error(RCode.NODECABINET_NAME_DUPLICATE);// 机柜名称重复错误
    }


    @ApiOperation("校验U位数量是否可用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "id", required = true, paramType = "query", dataType = "Long"),
            @ApiImplicitParam(name = "ubit", value = "ubit", required = true, paramType = "query", dataType = "Long")
    })
    @RequestMapping(value = "/cabUbitCheck", method = RequestMethod.GET)
    public R cabUbitCheck(@RequestParam("id") Long id, @RequestParam("ubit") Integer ubit) {
        if (nodeCabinetService.cabUbitCheck(id, ubit)) {
            return R.ok();
        }
        return R.error(RCode.NODECABINET_UBIT_OCCUPIED); // 机柜U位数量不可用
    }


    @ApiOperation("查询机柜下可选的机房及行号列号")
    @ApiImplicitParam(name = "id", value = "id", paramType = "query", dataType = "Long")
    @RequestMapping(value = "/optionalRoom", method = RequestMethod.GET)
    public R optionalRoom(@RequestParam(value = "id", required = false) Long id) {
        List<AvailCabinetEntity> availCabinetEntities = nodeCabinetService.optionalRoom(id);
        Map map = new HashMap();
        map.put("list", availCabinetEntities);
        return R.ok().put("data", map);
    }

}
