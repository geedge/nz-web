package com.nis.modules.asset.controller;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.asset.service.TalonApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asset OSHI
 *
 * @author shizhendong
 * @date 2022-09-07
 */
@RestController
@RequestMapping
public class AssetOSHIController {

    @Autowired
    private TalonApiService talonApiService;

    /**
     * 基础信息
     *
     * @param assetId
     * @return
     */
    @GetMapping("/asset/oshi/info")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.HARDWARE_ASSET)
    public R oshiInfo(@RequestParam(value = "id", required = false) Integer assetId) {
        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL);

        JSONObject jsonObject = talonApiService.oshiInfo(assetId);
        return R.ok(jsonObject.get("data"));
    }

    /**
     * 进程信息
     *
     * @param assetId
     * @return
     */
    @GetMapping("/asset/oshi/process")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.HARDWARE_ASSET)
    public R oshiProcess(@RequestParam(value = "id", required = false) Integer assetId) {
        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL);

        JSONObject jsonObject = talonApiService.oshiProcess(assetId);
        return R.ok(jsonObject.get("data"));
    }

    /**
     * 网络连接信息
     *
     * @param assetId
     * @return
     */
    @GetMapping("/asset/oshi/netstat")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.HARDWARE_ASSET)
    public R oshiNetstat(@RequestParam(value = "id", required = false) Integer assetId) {
        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL);

        JSONObject jsonObject = talonApiService.oshiNetstat(assetId);
        return R.ok(jsonObject.get("data"));
    }

}
