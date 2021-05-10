package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.AssetBrand;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.service.AssetBrandService;
import com.nis.modules.asset.service.AssetModelService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 资产品牌、型号 controller
 *
 * @author shizhendong
 * @date 2021-03-16
 */
@RestController
@RequestMapping
public class AssetBrandModelController {

    @Autowired
    private AssetBrandService brandService;

    @Autowired
    private AssetModelService modelService;

    @GetMapping("/asset/brand/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSETBRAND)
    public R queryBrandInfo(@PathVariable("id") Integer id) {
        AssetBrand brand = brandService.queryBrandInfo(id);
        return R.ok(brand);
    }

    @GetMapping("/asset/brand")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSETBRAND)
    public R queryBrandList(@RequestParam Map<String, Object> params) {
        PageUtils page = brandService.queryPage(params);
        return R.ok(page);
    }

    @PostMapping("/asset/brand")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ASSETBRAND)
    @RequiresPermissions({"asset:brand:save"})
    public R saveAssetBrand(@RequestBody AssetBrand brand) {
        ValidateUtils.is(brand.getName()).notNull(RCode.ASSET_BRAND_NAME_ISNULL);

        brandService.saveBrand(brand);
        return R.ok();
    }

    @PutMapping("/asset/brand")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ASSETBRAND)
    @RequiresPermissions({"asset:brand:update"})
    public R editAssetBrand(@RequestBody AssetBrand brand) {
        ValidateUtils.is(brand.getId()).notNull(RCode.ASSET_BRAND_ID_ISNULL)
                .and(brand.getName()).notNull(RCode.ASSET_BRAND_NAME_ISNULL);

        brandService.updateBrand(brand);
        return R.ok();
    }

    @DeleteMapping("/asset/brand")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ASSETBRAND)
    @RequiresPermissions({"asset:brand:delete"})
    public R deleteAssetBrands(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_BRAND_ID_ISNULL);

        brandService.delete(ids);
        return R.ok();
    }

    @GetMapping("/asset/model/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.MODEL)
    public R queryModelInfo(@PathVariable("id") Integer id) {
        AssetModel model = modelService.queryModelInfo(id);
        return R.ok(model);
    }

    @GetMapping("/asset/model")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.MODEL)
    public R queryModelList(@RequestParam Map<String, Object> params) {
        PageUtils page = modelService.queryPage(params);
        return R.ok(page);
    }

    @PostMapping("/asset/model")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.MODEL)
    @RequiresPermissions({"asset:model:save"})
    public R saveAssetModel(@RequestBody AssetModel model) {
        ValidateUtils.is(model.getName()).notNull(RCode.ASSET_MODELNAME_ISNULL)
                .and(model.getBrandId()).notNull(RCode.ASSET_MODEL_BRANDID_ISNULL);

        modelService.saveModel(model);
        return R.ok();
    }

    @PutMapping("/asset/model")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.MODEL)
    @RequiresPermissions({"asset:model:update"})
    public R editAssetModel(@RequestBody AssetModel model) {
        ValidateUtils.is(model.getId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(model.getName()).notNull(RCode.ASSET_MODELNAME_ISNULL)
                .and(model.getBrandId()).notNull(RCode.ASSET_MODEL_BRANDID_ISNULL);

        modelService.updateModel(model);
        return R.ok();
    }

    @DeleteMapping("/asset/model")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.MODEL)
    @RequiresPermissions({"asset:model:delete"})
    public R deleteAssetModels(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_MODELID_ISNULL);

        modelService.delete(ids);
        return R.ok();
    }

}
