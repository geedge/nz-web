package com.nis.modules.node.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.node.entity.AvailCabinetEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.node.service.NodeInfoService;
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
 * 节点信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@RestController
@RequestMapping("/node")
@Api(tags = "节点node-info")
public class NodeInfoController {

    @Autowired
    private NodeInfoService nodeInfoService;

    @ApiOperation("节点列表")
    @GetMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = nodeInfoService.queryPage(params);
        return R.ok(page);
    }

    @ApiOperation("节点详情查询")
    @GetMapping("/detail")
    public R detail(@RequestParam("uuid") Long uuid) {
        NodeInfoEntity nodeInfoDetail = nodeInfoService.detail(uuid);
        return R.ok().put("data", nodeInfoDetail);
    }

    @ApiOperation("节点新增")
    @PostMapping("/save")
    public R save(@RequestBody NodeInfoEntity nodeInfoEntity) {
        ValidateUtils.check(nodeInfoEntity.getIp()).ip(RCode.NODE_IPADDRESS_FORMAT);
        nodeInfoService.saveNodeInfo(nodeInfoEntity);
        return R.ok();
    }

    @ApiOperation("节点修改")
    @PutMapping("/update")
    public R update(@RequestBody NodeInfoEntity nodeInfoEntity) {
        ValidateUtils.check(nodeInfoEntity.getIp()).ip(RCode.NODE_IPADDRESS_FORMAT);
        nodeInfoService.updateNodeInfo(nodeInfoEntity);
        return R.ok();
    }


    @ApiOperation("IP地址校验")
    @GetMapping("/ipCheck")
    public R ipCheck(@RequestParam(value = "uuid", required = false) Long uuid, @RequestParam(value = "ip") String ip) {
        ValidateUtils.check(ip).ip(RCode.NODE_IPADDRESS_FORMAT);
        if (nodeInfoService.ipCheck(uuid, ip)) {
            return R.ok();
        }
        return R.error(RCode.NODE_IPADDRESS_DUPLICATE);//地址重复
    }

    @ApiOperation("name唯一校验")
    @GetMapping("/nodeNameCheck")
    public R nodeNameCheck(@RequestParam(value = "uuid", required = false) Long uuid, @RequestParam(value = "name") String name) {
        if (nodeInfoService.nodeNameCheck(uuid, name)) {
            return R.ok();
        }
        return R.error(RCode.NODE_NAME_DUPLICATE);//名称重复
    }

    @ApiOperation("节点状态变更")
    @PutMapping("/changeState")
    public R changeState(@RequestParam("uuids") Long[] uuids, @RequestParam("state") String state) {
        nodeInfoService.changeState(uuids, state);
        return R.ok();
    }

    @ApiOperation("查询可用机柜")
    @GetMapping("/selectCab")
    public R selectCab(@RequestParam(value = "uuid", required = false) Long uuid, @RequestParam("uSize") Integer uSize) {
        List<AvailCabinetEntity> availCabinetEntities = nodeInfoService.selectCab(uuid, uSize);
        Map<String, List<AvailCabinetEntity>> map = new HashMap();
        map.put("list", availCabinetEntities);
        return R.ok(map);
    }

}
