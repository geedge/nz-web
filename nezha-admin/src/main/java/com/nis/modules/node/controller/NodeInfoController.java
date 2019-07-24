package com.nis.modules.node.controller;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.mission.utils.ApiJsonObject;
import com.nis.modules.mission.utils.ApiJsonProperty;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.node.service.NodeInfoService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * 节点信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@RestController
@RequestMapping("/node")
@Api(tags="节点node-info")
public class NodeInfoController {

    @Autowired
    private NodeInfoService nodeInfoService;

    /**
     * 列表`
     */
    @ApiOperation("节点列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = nodeInfoService.queryPage(params);
        return R.ok().put("data", page);
    }

    /**
     * 节点详情查询
     *
     * @param uuid
     * @return
     */
    @ApiOperation("节点详情查询")
    @ApiImplicitParam(name = "uuid", value = "uuid", required = true, paramType = "query", dataType = "Long")
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public R detail(@RequestParam("uuid") Long uuid) {
        NodeInfoEntity nodeInfoDetail = nodeInfoService.detail(uuid);
        return R.ok().put("data", nodeInfoDetail);
    }

    /**
     * 节点新增
     *
     * @param nodeInfoEntity
     * @return
     */
    @ApiOperation("节点新增")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public R save(@RequestBody NodeInfoEntity nodeInfoEntity) {
        ValidateUtils.check(nodeInfoEntity.getIp()).ip(RCode.NODE_IPADDRESS_FORMAT);
        nodeInfoService.saveNodeInfo(nodeInfoEntity);
        return R.ok();
    }

    /**
     * 节点修改
     *
     * @param nodeInfoEntity
     * @return
     */
    @ApiOperation("节点修改")
    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    public R update(@RequestBody NodeInfoEntity nodeInfoEntity) {
        ValidateUtils.check(nodeInfoEntity.getIp()).ip(RCode.NODE_IPADDRESS_FORMAT);
        nodeInfoService.updateNodeInfo(nodeInfoEntity);
        return R.ok();
    }


    /**
     * IP校验  是否合法 唯一
     *
     * @param uuid
     * @param ip
     * @return
     */
    @ApiOperation("IP地址校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "uuid", value = "uuid", required = false, paramType = "query", dataType = "Long"),
            @ApiImplicitParam(name = "ip", value = "ip", required = true, paramType = "query", dataType = "String")
    })
    @RequestMapping(value = "ipCheck", method = RequestMethod.GET)
    public R ipCheck(@RequestParam(value = "uuid", required = false) Long uuid, @RequestParam(value = "ip") String ip) {
        ValidateUtils.check(ip).ip(RCode.NODE_IPADDRESS_FORMAT);
        if (nodeInfoService.ipCheck(uuid, ip)) {
            return R.ok();
        }
        return R.error(RCode.NODE_IPADDRESS_DUPLICATE);//地址重复
    }

    /**
     * 节点名称校验
     *
     * @param uuid
     * @param name
     * @return
     */
    @ApiOperation("name唯一校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "uuid", value = "uuid", required = false, paramType = "query", dataType = "Long"),
            @ApiImplicitParam(name = "name", value = "name", required = true, paramType = "query", dataType = "String")
    })
    @RequestMapping(value = "nodeNameCheck", method = RequestMethod.GET)
    public R nodeNameCheck(@RequestParam(value = "uuid", required = false) Long uuid, @RequestParam(value = "name") String name) {
        if (nodeInfoService.nodeNameCheck(uuid, name)) {
            return R.ok();
        }
        return R.error(RCode.NODE_NAME_DUPLICATE);//名称重复
    }

    /**
     * 节点状态变更
     * (上线/下线/删除)
     *
     * @param uuids
     * @param state
     * @return
     */
    @ApiOperation("节点状态变更")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "uuids", value = "uuids", required = true, paramType = "query", dataType = "Long", allowMultiple = true),
            @ApiImplicitParam(name = "state", value = "state", required = true, paramType = "query", dataType = "String")
    })
    @RequestMapping(value = "changeState", method = RequestMethod.PUT)
    public R changeState(@RequestParam("uuids") Long[] uuids, @RequestParam("state") String state) {
        nodeInfoService.changeState(uuids, state);
        return R.ok();
    }

}
