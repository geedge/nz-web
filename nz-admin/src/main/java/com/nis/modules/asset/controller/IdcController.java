/*
package com.nis.modules.asset.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.entity.IdcTraffic;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.asset.service.IdcTrafficService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


*/
/**
 * 数据中心  controller
 *
 * @author shizhendong
 * @date 2019-11-21
 *//*

@RestController
@RequestMapping("/idc")
public class IdcController {

    @Autowired
    private IdcService idcService;

    @Autowired
    private IdcTrafficService idcTrafficService;

    */
/**
     * 数据中心请求列表
     *
     * @param params
     * @return
     *//*

    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.DATACENTER)
    @RequiresPermissions({"idc:list"})
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = idcService.queryPage(params);
        return R.ok(page);
    }

    */
/**
     * 保存
     *
     * @param idc
     * @return
     *//*

    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.DATACENTER)
    @RequiresPermissions({"idc:save"})
    public R save(@RequestBody Idc idc) {
        ValidateUtils.is(idc.getName()).notNull(RCode.DC_NAME_ISNULL)
                .and(idc.getY()).min(1, RCode.DC_Y_MINIMUM)
                .and(idc.getX()).min(1, RCode.DC_X_MINIMUM);

        idcService.saveOrUpdateIdc(idc);
        return R.ok();
    }

    */
/**
     * 修改
     *
     * @param idc
     * @return
     *//*

    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.DATACENTER)
    @RequiresPermissions({"idc:update"})
    public R update(@RequestBody Idc idc) {
        ValidateUtils.is(idc.getId()).notNull(RCode.DC_ID_ISNULL)
                .and(idc.getName()).notNull(RCode.DC_NAME_ISNULL)
                .and(idc.getY()).min(1, RCode.DC_Y_MINIMUM)
                .and(idc.getX()).min(1, RCode.DC_X_MINIMUM);

        idcService.saveOrUpdateIdc(idc);
        return R.ok();
    }


    */
/**
     * 删除
     *
     * @param ids
     * @return
     *//*

    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.DATACENTER)
    @RequiresPermissions({"idc:delete"})
    public R delete(@RequestParam Integer ... ids) {
        ValidateUtils.is(ids).notNull(RCode.DC_ID_ISNULL);

        idcService.delete(ids);
        return R.ok();
    }


    @GetMapping("/trafficSetting")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.DATACENTER)
    @RequiresPermissions({"idc:taffic:list"})
    public R queryTrafficSetting(@RequestParam Map<String, Object> params) {
        PageUtils page = idcTrafficService.queryTrafficSetting(params);
        return R.ok(page);
    }

    @PutMapping("/trafficSetting")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.DATACENTER)
    @RequiresPermissions({"idc:taffic:update"})
    public R updateTrafficSetting(@RequestBody Map params) {
        Integer idcId = (Integer) params.get("idcId");
        List<IdcTraffic> setting = JSONArray.parseArray(JSON.toJSONString(params.get("setting")), IdcTraffic.class);

        idcTrafficService.validateTrafficInfo(idcId, setting);

        idcTrafficService.updateTraffic(idcId, setting);
        return R.ok();
    }
}
*/
