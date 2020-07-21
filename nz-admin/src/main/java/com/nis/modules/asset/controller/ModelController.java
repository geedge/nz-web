package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * 资产型号  controller
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@RestController
@RequestMapping("/model")
public class ModelController {

    @Autowired
    private ModelService modelService;

    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.MODEL)
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = modelService.queryPage(params);
        return R.ok(page);
    }

    /**
     * 保存
     *
     * @param model
     * @return
     */
    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.MODEL)
    public R save(@RequestBody Model model) {
        ValidateUtils.is(model.getName()).notNull(RCode.ASSET_MODELNAME_ISNULL)
                .and(model.getVendorCode()).notNull(RCode.ASSET_VENDOR_ISNULL)
                .and(model.getTypeCode()).notNull(RCode.ASSET_TYPE_ISNULL);

        Integer usize = model.getUsize();
        if(usize != null){
            ValidateUtils.is(usize).min(1, RCode.ASSET_MODEL_USIZEMINIMUM);
        }

        modelService.saveOrUpdateModel(model);
        return R.ok();
    }


    /**
     * 修改
     *
     * @param model
     * @return
     */
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.MODEL)
    public R update(@RequestBody Model model) {
        ValidateUtils.is(model.getId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(model.getName()).notNull(RCode.ASSET_MODELNAME_ISNULL)
                .and(model.getVendorCode()).notNull(RCode.ASSET_VENDOR_ISNULL)
                .and(model.getTypeCode()).notNull(RCode.ASSET_TYPE_ISNULL);

        Integer usize = model.getUsize();
        if(usize != null){
            ValidateUtils.is(usize).min(1, RCode.ASSET_MODEL_USIZEMINIMUM);
        }

        modelService.saveOrUpdateModel(model);
        return R.ok();
    }

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.MODEL)
    public R delete(@RequestParam Integer ... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_MODELID_ISNULL);

        modelService.delete(ids);
        return R.ok();
    }


    /**
     * 模板图表更新
     *
     * @param params
     * @return
     */
    @PutMapping("/syncChart")
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.MODEL)
    public R syncChart(@RequestBody Map<String, Object> params) {
        modelService.syncChart(params);
        return R.ok();
    }
}