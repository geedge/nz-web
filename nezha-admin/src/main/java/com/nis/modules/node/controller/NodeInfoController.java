package com.nis.modules.node.controller;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.node.entity.AvailCabinetEntity;
import com.nis.modules.node.entity.NodeGroupRelEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.node.service.NodeInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
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
    public R detail(Long uuid) {
        ValidateUtils.is(uuid).notNull(RCode.NODE_UUID_ISNULL);

        NodeInfoEntity nodeInfoDetail = nodeInfoService.detail(uuid);
        return R.ok(nodeInfoDetail);
    }

    @ApiOperation("节点新增")
    @PostMapping("/save")
    public R save(@RequestBody NodeInfoEntity nodeInfoEntity) {
        ValidateUtils.is(nodeInfoEntity).notNull(RCode.NOT_NULL_ERROR)
                .and(nodeInfoEntity.getIp()).notNull(RCode.NODE_IPADDRESS_ISNULL).ip(RCode.NODE_IPADDRESS_FORMAT)
                .and(nodeInfoEntity.getName()).notNull(RCode.NODE_NAME_ISNULL)
                .and(nodeInfoEntity.getType()).notNull(RCode.NODE_TYPE_ISNULL);

        nodeInfoService.saveNodeInfo(nodeInfoEntity);
        return R.ok();
    }

    @ApiOperation("节点修改")
    @PutMapping("/update")
    public R update(@RequestBody NodeInfoEntity nodeInfoEntity) {
        ValidateUtils.is(nodeInfoEntity).notNull(RCode.NOT_NULL_ERROR)
                .and(nodeInfoEntity.getUuid()).notNull(RCode.NODE_UUID_ISNULL)
                .and(nodeInfoEntity.getIp()).notNull(RCode.NODE_IPADDRESS_ISNULL).ip(RCode.NODE_IPADDRESS_FORMAT)
                .and(nodeInfoEntity.getName()).notNull(RCode.NODE_NAME_ISNULL)
                .and(nodeInfoEntity.getType()).notNull(RCode.NODE_TYPE_ISNULL);

        nodeInfoService.updateNodeInfo(nodeInfoEntity);
        return R.ok();
    }


    @ApiOperation("IP地址校验")
    @GetMapping("/ipCheck")
    public R ipCheck(Long uuid, String ip) {
        ValidateUtils.is(ip).notNull(RCode.NODE_IPADDRESS_ISNULL).and(ip).ip(RCode.NODE_IPADDRESS_FORMAT);

        return nodeInfoService.ipCheck(uuid, ip);
    }

    @ApiOperation("name唯一校验")
    @GetMapping("/nodeNameCheck")
    public R nodeNameCheck(Long uuid, String name) {
        ValidateUtils.is(name).notNull(RCode.NODE_NAME_ISNULL);

        if (nodeInfoService.nodeNameCheck(uuid, name)) {
            return R.ok();
        }
        return R.error(RCode.NODE_NAME_DUPLICATE);//名称重复
    }

    @ApiOperation("节点状态变更")
    @PutMapping("/changeState")
    public R changeState(Long[] uuids, String state) {
        ValidateUtils.is(uuids).notNull(RCode.NODE_UUID_ISNULL).and(state).notNull(RCode.NODE_STATE_ISNULL);

        nodeInfoService.changeState(uuids, state);
        return R.ok();
    }

    @ApiOperation("查询可用机柜")
    @GetMapping("/selectCab")
    public R selectCab(Long uuid, Integer uSize) {
        ValidateUtils.is(uSize).notNull(RCode.NODE_USIZE_ISNULL);
        if (uSize < 1) {
            throw new NZException(RCode.NODE_USIZE_NOTTRUE); // u位大小值不正确
        }

        List<AvailCabinetEntity> availCabinetEntities = nodeInfoService.selectCab(uuid, uSize);
        Map<String, List<AvailCabinetEntity>> map = new HashMap();
        map.put("list", availCabinetEntities);
        return R.ok(map);
    }


    /**
     * 根据节点UUID 查询所属节点组集合
     * @param uuid
     * @return
     */
    @GetMapping("/getGroupsById")
    public R getGroupsById(Long uuid){
        ValidateUtils.is(uuid).notNull(RCode.NODE_UUID_ISNULL);
        return R.ok(nodeInfoService.getGroupsById(uuid));
    }
}
