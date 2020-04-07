package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
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
    @SysLog("save model")
    public R save(@RequestBody Model model) {
        ValidateUtils.is(model.getName()).notNull(RCode.ASSET_MODELNAME_ISNULL)
                .and(model.getVendorCode()).notNull(RCode.ASSET_VENDOR_ISNULL)
                .and(model.getTypeCode()).notNull(RCode.ASSET_TYPE_ISNULL);

        Integer usize = model.getUsize();
        if(usize != null){
            ValidateUtils.is(usize).min(1, RCode.ASSET_MODEL_USIZEMINIMUM);
        }

        // 校验名称重复
        if (!modelService.checkName(model.getId(), model.getName())) {
            // 资产型号名称重复
            throw new NZException(RCode.ASSET_MODELNAME_DUPLICATE);
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
    @SysLog("update model")
    public R update(@RequestBody Model model) {
        ValidateUtils.is(model.getId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(model.getName()).notNull(RCode.ASSET_MODELNAME_ISNULL)
                .and(model.getVendorCode()).notNull(RCode.ASSET_VENDOR_ISNULL)
                .and(model.getTypeCode()).notNull(RCode.ASSET_TYPE_ISNULL);

        Integer usize = model.getUsize();
        if(usize != null){
            ValidateUtils.is(usize).min(1, RCode.ASSET_MODEL_USIZEMINIMUM);
        }

        // 校验名称重复
        if (!modelService.checkName(model.getId(), model.getName())) {
            // 资产型号名称重复
            throw new NZException(RCode.ASSET_MODELNAME_DUPLICATE);
        }

        modelService.checkUsize(model);

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
    @SysLog("delete model")
    public R delete(@RequestParam Integer ... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_MODELID_ISNULL);

        modelService.delete(ids);
        return R.ok();
    }
}