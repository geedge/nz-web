package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.AssetBrand;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.service.AssetBrandService;
import com.nis.modules.asset.service.AssetModelService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
    public R deleteAssetBrands(@RequestParam(required = false, defaultValue = "0") Integer force, Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_BRAND_ID_ISNULL);

        List<Map> delRelObjectMsgList = brandService.delete(force, ids);
        switch (force) {
            case 0: {// 0：存在关联数据时，删除失败
                if (Tool.CollUtil.isNotEmpty(delRelObjectMsgList)) {
                    return R.error(RCode.ASSET_BRAND_RELOBJ_CAN_NOT_REMOVE).putData("list", delRelObjectMsgList);
                }
            }
            case 1: // 1：存在关联数据时，同时删除关联数据
            default:
                return R.ok().putData("list", delRelObjectMsgList);
        }
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
        ValidateUtils.is(model.getName()).notNull(RCode.ASSET_MODELNAME_ISNULL).maxLength(64)
                .and(model.getBrandId()).notNull(RCode.ASSET_MODEL_BRANDID_ISNULL)
                .and(model.getRemark()).maxLength(256)
                .and(model.getTypeId()).notNull(RCode.ASSET_TYPE_ID_ISNULL);

        Integer id = modelService.saveModel(model);
        return R.ok().putData("id", id);
    }

    @PutMapping("/asset/model")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.MODEL)
    @RequiresPermissions({"asset:model:update"})
    public R editAssetModel(@RequestBody AssetModel model) {
        ValidateUtils.is(model.getId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(model.getName()).notNull(RCode.ASSET_MODELNAME_ISNULL).maxLength(64)
                .and(model.getBrandId()).notNull(RCode.ASSET_MODEL_BRANDID_ISNULL)
                .and(model.getRemark()).maxLength(256)
                .and(model.getTypeId()).notNull(RCode.ASSET_TYPE_ID_ISNULL);

        Integer id = modelService.updateModel(model);
        return R.ok().putData("id", id);
    }

    @DeleteMapping("/asset/model")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.MODEL)
    @RequiresPermissions({"asset:model:delete"})
    public R deleteAssetModels(@RequestParam(required = false, defaultValue = "0") Integer force, Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_MODELID_ISNULL);

        List<Map> delRelObjectMsgList = modelService.delete(force, ids);
        switch (force) {
            case 0: {// 0：存在关联数据时，删除失败
                if (Tool.CollUtil.isNotEmpty(delRelObjectMsgList)) {
                    return R.error(RCode.ASSET_MODEL_RELOBJ_CAN_NOT_REMOVE).putData("list", delRelObjectMsgList);
                }
            }
            case 1: // 1：存在关联数据时，同时删除关联数据
            default:
                return R.ok().putData("list", delRelObjectMsgList);
        }
    }


    @GetMapping("/asset/model/template")
    public void getExportTemplateByLanguage(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String language = request.getHeader(Constant.LANGUAGE);
        String format = request.getHeader("format");
        format = Tool.StrUtil.emptyToDefault(format, "1");

        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        // 校验 FORMAT
        if (!Arrays.asList(Constant.IMPORTFILE_TYPE_INTVALUES).contains(format)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }

        modelService.getTemplateByLanguage(response, format);
    }


    @PostMapping("/asset/model/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.MODEL)
    @RequiresPermissions({"asset:model:save"})
    public R importAssetModelData(@RequestParam(value = "file") MultipartFile excelFile, HttpServletRequest request, @RequestParam Map<String, Object> params) throws IOException {
        String language = request.getHeader(Constant.LANGUAGE);
        ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);
        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }

        Map info = modelService.importAssetModelData(excelFile, params);
        return R.ok(info);
    }

    @GetMapping("/asset/model/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.MODEL)
    public void exportAssetModels(HttpServletResponse response, @RequestParam Map<String, Object> params)  throws IOException {
        Object format = params.get("format");
        String formatStr = format == null ? "1" : format.toString();
        // 校验 FORMAT
        if (!Arrays.asList(Constant.IMPORTFILE_TYPE_INTVALUES).contains(formatStr)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }
        params.put("format", formatStr);

        modelService.export(response, params);
    }

    @DeleteMapping("/asset/model/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.MODEL)
    public R cancelImport(@RequestParam(value = "seq", required = false) String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);

        modelService.cancelImport(seq);
        return R.ok();
    }

}
