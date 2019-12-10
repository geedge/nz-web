package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Map;


/**
 * 资产  controller
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@RestController
@RequestMapping("/asset")
public class AssetController {

    @Autowired
    private AssetService assetService;

    /**
     * 资产请求列表
     *
     * @param params
     * @return
     */
    @GetMapping
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = assetService.queryPage(params);
        return R.ok(page);
    }

    /**
     * 保存
     *
     * @param asset
     * @return
     */
    @PostMapping
    @SysLog("save asset")
    public R save(@RequestBody Asset asset) {
        ValidateUtils.is(asset.getSn()).notNull(RCode.ASSET_SN_ISNULL)
                .and(asset.getModelId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(asset.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(asset.getHost()).notNull(RCode.ASSET_HOST_ISNULL).ip(RCode.ASSET_HOST_FORMAT);

        // 购买时间格式校验
        if (StringUtils.isNotEmpty(asset.getPurchaseDate())) {
            String format = "yyyy-MM-dd";
            ValidateUtils.is(asset.getPurchaseDate()).date(format, RCode.ASSET_PURCHASEDATE_FORMAT);

            SimpleDateFormat sd = new SimpleDateFormat(format);
            try {
                sd.setLenient(false);
                sd.parse(asset.getPurchaseDate());
            } catch (Exception e) {
                // 日期不合法
                throw new NZException(RCode.ASSET_PURCHASEDATE_UNAVAILABLE);
            }
        } else {
            asset.setPurchaseDate(null);
        }

        assetService.saveOrUpdateAsset(asset);
        return R.ok();
    }

    /**
     * 修改
     *
     * @param asset
     * @return
     */
    @PutMapping
    @SysLog("update asset")
    public R update(@RequestBody Asset asset) {
        ValidateUtils.is(asset.getId()).notNull(RCode.ASSET_ID_ISNULL)
                .and(asset.getSn()).notNull(RCode.ASSET_SN_ISNULL)
                .and(asset.getModelId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(asset.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(asset.getHost()).notNull(RCode.ASSET_HOST_ISNULL).ip(RCode.ASSET_HOST_FORMAT);

        // 购买时间格式校验
        if (StringUtils.isNotEmpty(asset.getPurchaseDate())) {
            String format = "yyyy-MM-dd";
            ValidateUtils.is(asset.getPurchaseDate()).date(format, RCode.ASSET_PURCHASEDATE_FORMAT);

            SimpleDateFormat sd = new SimpleDateFormat(format);
            try {
                sd.setLenient(false);
                sd.parse(asset.getPurchaseDate());
            } catch (Exception e) {
                // 日期不合法
                throw new NZException(RCode.ASSET_PURCHASEDATE_UNAVAILABLE);
            }
        }

        assetService.saveOrUpdateAsset(asset);
        return R.ok();
    }

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog("delete asset")
    public R delete(String ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_ID_ISNULL);

        assetService.delete(ids);
        return R.ok();
    }
}