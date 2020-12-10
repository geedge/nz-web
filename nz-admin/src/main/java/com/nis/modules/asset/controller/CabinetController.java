package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.service.CabinetService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * 机柜  controller
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@RestController
@RequestMapping("/cabinet")
public class CabinetController {

    @Autowired
    private CabinetService cabinetService;

    /**
     * 机柜请求列表
     *
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:list"})
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = cabinetService.queryPage(params);
        return R.ok(page);
    }

    /**
     * 保存
     *
     * @param cabinet
     * @return
     */
    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:save"})
    public R save(@RequestBody Cabinet cabinet) {
        ValidateUtils.is(cabinet.getName()).notNull(RCode.IDC_CABINETNAME_ISNULL)
                .and(cabinet.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(cabinet.getY()).min(1, RCode.CABINET_Y_MINIMUM)
                .and(cabinet.getX()).min(1, RCode.CABINET_X_MINIMUM);

        cabinetService.saveOrUpdateCabinet(cabinet);
        return R.ok();
    }

    /**
     * 修改
     *
     * @param cabinet
     * @return
     */
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:update"})
    public R update(@RequestBody Cabinet cabinet) {
        ValidateUtils.is(cabinet.getId()).notNull(RCode.IDC_CABINETID_ISNULL)
                .and(cabinet.getName()).notNull(RCode.IDC_CABINETNAME_ISNULL)
                .and(cabinet.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(cabinet.getY()).min(1, RCode.CABINET_Y_MINIMUM)
                .and(cabinet.getX()).min(1, RCode.CABINET_X_MINIMUM);

        cabinetService.saveOrUpdateCabinet(cabinet);
        return R.ok();
    }

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:delete"})
    public R delete(@RequestParam Integer ... ids) {
        ValidateUtils.is(ids).notNull(RCode.IDC_CABINETID_ISNULL);

        cabinetService.delete(ids);
        return R.ok();
    }

    /**
     * 机柜u位查询
     * @param id
     * @return
     */
    @GetMapping("/u")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:u:info"})
    public R getUInfo(Integer id) {
        ValidateUtils.is(id).notNull(RCode.IDC_CABINETID_ISNULL);

        Map uInfo = cabinetService.getUInfo(id);
        return R.ok(uInfo);
    }
}