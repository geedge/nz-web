package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.SoftwareAsset;
import com.nis.modules.asset.entity.SoftwareType;
import com.nis.modules.asset.service.SoftwareAssetService;
import com.nis.modules.asset.service.SoftwareTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Asset Software Controller
 *
 * @author shizhendong
 * @date 2023-08-10
 */
@RestController
@RequestMapping
public class AssetSoftwareController {


    @Autowired
    private SoftwareAssetService softwareAssetService;

    @Autowired
    private SoftwareTypeService softwareTypeService;


    /**
     * query software asset info
     *
     * @param id
     * @return
     */
    @GetMapping("/asset/software/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SOFTWARE_ASSET)
    public R querySoftwareAssetInfo(@PathVariable("id") Integer id) {
        return R.ok(softwareAssetService.querySoftwareAssetInfo(id));
    }


    /**
     * query software asset page
     *
     * @param params
     * @return
     */
    @GetMapping("/asset/software")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SOFTWARE_ASSET)
    public R querySoftwareAssetPage(@RequestParam Map<String, Object> params) {
        PageUtils page = softwareAssetService.querySoftwareAssetPage(params);
        return R.ok(page);
    }


    /**
     * save or update software asset
     *
     * @param softwareAsset
     * @return
     */
    @RequestMapping(value = "/asset/software", method = {RequestMethod.POST, RequestMethod.PUT})
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.SOFTWARE_ASSET)
    public R saveOrUpdateSoftwareAsset(@RequestBody SoftwareAsset softwareAsset) {
        ValidateUtils.is(softwareAsset.getName()).notNull(RCode.SOFTWARE_ASSET_NAME_ISNULL);

        return R.ok().putData("id", softwareAssetService.saveOrUpdateSoftwareAsset(softwareAsset));
    }


    /**
     * delete software asset
     *
     * @param force
     * @param ids
     * @return
     */
    @DeleteMapping("/asset/software")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.SOFTWARE_ASSET)
    public R deleteSoftwareAsset(@RequestParam(required = false, defaultValue = "0") Integer force, Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.SOFTWARE_ASSET_ID_ISNULL);

        return softwareAssetService.deleteSoftwareAsset(force, Tool.ListUtil.list(true, ids));
    }


    /**
     * query software type info
     *
     * @param id
     * @return
     */
    @GetMapping("/asset/software/type/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SOFTWARE_TYPE)
    public R querySoftwareTypeInfo(@PathVariable("id") Integer id) {
        return R.ok(softwareTypeService.querySoftwareTypeInfo(id));
    }

    /**
     * query software type page
     *
     * @param params
     * @return
     */
    @GetMapping("/asset/software/type")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SOFTWARE_TYPE)
    public R querySoftwareTypePage(@RequestParam Map<String, Object> params) {
        PageUtils page = softwareTypeService.querySoftwareTypePage(params);
        return R.ok(page);
    }


    /**
     * save or update software type
     *
     * @param softwareType
     * @return
     */
    @RequestMapping(value = "/asset/software/type", method = {RequestMethod.POST, RequestMethod.PUT})
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.SOFTWARE_TYPE)
    public R saveOrUpdateSoftwareType(@RequestBody SoftwareType softwareType) {
        ValidateUtils.is(softwareType.getName()).notNull(RCode.SOFTWARE_TYPE_NAME_ISNULL);

        return R.ok().putData("id", softwareTypeService.saveOrUpdateSoftwareType(softwareType));
    }


    /**
     * delete software type
     *
     * @param force
     * @param ids
     * @return
     */
    @DeleteMapping("/asset/software/type")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.SOFTWARE_TYPE)
    public R deleteSoftwareType(@RequestParam(required = false, defaultValue = "0") Integer force, Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.SOFTWARE_TYPE_ID_ISNULL);

        return softwareTypeService.deleteSoftwareType(force, Tool.ListUtil.list(true, ids));
    }

}
