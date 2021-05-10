package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.DcService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * 数据中心  controller
 *
 * @author shizhendong
 * @date 2021-4-6
 */
@RestController
@RequestMapping("/dc")
public class DcController {

    @Autowired
    private DcService dcService;

    @Autowired
    private CabinetService cabinetService;

    /**
     * dc详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.DATACENTER)
    public R queryDcDetails(@PathVariable("id") Integer id) {
        return R.ok(dcService.queryDcDetails(id));
    }

    /**
     * 数据中心请求列表
     *
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.DATACENTER)
    public R queryDcPage(@RequestParam Map<String, Object> params) {
        PageUtils page = dcService.queryPage(params);
        return R.ok(page);
    }


    /**
     * 保存
     *
     * @param dc
     * @return
     */
    @PostMapping
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.DATACENTER)
    @RequiresPermissions({"dc:save"})
    public R saveDc(@RequestBody Dc dc) {
        ValidateUtils.is(dc.getName()).notNull(RCode.DC_NAME_ISNULL)
                .and(dc.getState()).notNull(RCode.DC_STATE_ISNULL)
                .and(dc.getY()).min(1, RCode.DC_Y_MINIMUM)
                .and(dc.getX()).min(1, RCode.DC_X_MINIMUM);

        return R.ok().putData("id", dcService.saveDc(dc));
    }

    /**
     * 修改
     *
     * @param dc
     * @return
     */
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.DATACENTER)
    @RequiresPermissions({"dc:update"})
    public R updateDc(@RequestBody Dc dc) {
        ValidateUtils.is(dc.getId()).notNull(RCode.DC_ID_ISNULL)
                .and(dc.getName()).notNull(RCode.DC_NAME_ISNULL)
                .and(dc.getState()).notNull(RCode.DC_STATE_ISNULL)
                .and(dc.getY()).min(1, RCode.DC_Y_MINIMUM)
                .and(dc.getX()).min(1, RCode.DC_X_MINIMUM);

        return R.ok().putData("id", dcService.updateDc(dc));
    }


    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.DATACENTER)
    @RequiresPermissions({"dc:delete"})
    public R deleteDcByIds(@RequestParam Integer ... ids) {
        ValidateUtils.is(ids).notNull(RCode.DC_ID_ISNULL);

        dcService.delete(ids);
        return R.ok();
    }


    /**
     * 机柜详情
     *
     * @param id
     * @return
     */
    @GetMapping("/cabinet/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.CABINET)
    public R queryCabinetDetails(@PathVariable("id") Integer id) {
        return R.ok(cabinetService.queryCabinetDetails(id));
    }


    /**
     * 机柜请求列表
     *
     * @param params
     * @return
     */
    @GetMapping("/cabinet")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.CABINET)
    public R queryCabinetPage(@RequestParam Map<String, Object> params) {
        PageUtils page = cabinetService.queryCabinetPage(params);
        return R.ok(page);
    }


    /**
     * 保存
     *
     * @param cabinet
     * @return
     */
    @PostMapping("/cabinet")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:save"})
    public R saveCabinet(@RequestBody Cabinet cabinet) {
        ValidateUtils.is(cabinet.getName()).notNull(RCode.DC_CABINETNAME_ISNULL)
                .and(cabinet.getDcId()).notNull(RCode.DC_ID_ISNULL)
                .and(cabinet.getY()).min(1, RCode.CABINET_Y_MINIMUM)
                .and(cabinet.getX()).min(1, RCode.CABINET_X_MINIMUM);

        return R.ok().putData("id", cabinetService.saveCabinet(cabinet));
    }


    /**
     * 修改
     *
     * @param cabinet
     * @return
     */
    @PutMapping("/cabinet")
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:update"})
    public R updateCabinet(@RequestBody Cabinet cabinet) {
        ValidateUtils.is(cabinet.getId()).notNull(RCode.DC_CABINETID_ISNULL)
                .and(cabinet.getName()).notNull(RCode.DC_CABINETNAME_ISNULL)
                .and(cabinet.getDcId()).notNull(RCode.DC_ID_ISNULL)
                .and(cabinet.getY()).min(1, RCode.CABINET_Y_MINIMUM)
                .and(cabinet.getX()).min(1, RCode.CABINET_X_MINIMUM);

        return R.ok().putData("id", cabinetService.updateCabinet(cabinet));
    }



    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/cabinet")
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:delete"})
    public R deleteCabinetByIds(@RequestParam Integer ... ids) {
        ValidateUtils.is(ids).notNull(RCode.DC_CABINETID_ISNULL);

        cabinetService.deleteByIds(ids);
        return R.ok();
    }



    /**
     * 机柜u位查询
     * @param id
     * @return
     */
    @GetMapping("/cabinet/u")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.CABINET)
    public R getCabinetUsizeDetails(Integer id) {
        ValidateUtils.is(id).notNull(RCode.DC_CABINETID_ISNULL);

        Map uInfo = cabinetService.getCabinetUsizeDetails(id);
        return R.ok(uInfo);
    }



}
