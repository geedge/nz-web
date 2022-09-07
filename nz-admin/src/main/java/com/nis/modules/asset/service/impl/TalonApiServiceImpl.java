package com.nis.modules.asset.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.util.TalonUtil;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.TalonApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * nz talon api service impl
 *
 * @author shizhendong
 * @date 2022-09-07
 */
@Service
public class TalonApiServiceImpl implements TalonApiService {

    @Autowired
    private AssetAssetService assetService;

    @Override
    public JSONObject oshiInfo(Integer assetId) {
        AssetAsset asset = assetService.getById(assetId);
        ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);

        // ip:port
        String targetUrl = String.format("%s:%s", asset.getManageIp(), asset.getClientPort());
        String targetToken = asset.getClientToken();
        String path = "/oshi/info";
        Map requestParam = Tool.MapUtil.empty();

        String object = TalonUtil.getForObject(targetUrl, targetToken, path, requestParam, String.class);
        return JSONObject.parseObject(object);
    }

    @Override
    public JSONObject oshiProcess(Integer assetId) {
        AssetAsset asset = assetService.getById(assetId);
        ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);

        // ip:port
        String targetUrl = String.format("%s:%s", asset.getManageIp(), asset.getClientPort());
        String targetToken = asset.getClientToken();
        String path = "/oshi/process";
        Map requestParam = Tool.MapUtil.empty();

        String object = TalonUtil.getForObject(targetUrl, targetToken, path, requestParam, String.class);
        return JSONObject.parseObject(object);
    }

    @Override
    public JSONObject oshiNetstat(Integer assetId) {
        AssetAsset asset = assetService.getById(assetId);
        ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);

        // ip:port
        String targetUrl = String.format("%s:%s", asset.getManageIp(), asset.getClientPort());
        String targetToken = asset.getClientToken();
        String path = "/oshi/netstat";
        Map requestParam = Tool.MapUtil.empty();

        String object = TalonUtil.getForObject(targetUrl, targetToken, path, requestParam, String.class);
        return JSONObject.parseObject(object);
    }

}
