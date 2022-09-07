package com.nis.modules.asset.service;

import com.alibaba.fastjson.JSONObject;

/**
 * nz talon api service
 *
 * @author shizhendong
 * @date 2022-09-07
 */
public interface TalonApiService {

    /**
     * talon /oshi/info
     *
     * @param assetId
     * @return
     */
    JSONObject oshiInfo(Integer assetId);

    /**
     * talon /oshi/process
     *
     * @param assetId
     * @return
     */
    JSONObject oshiProcess(Integer assetId);

    /**
     * talon /oshi/netstat
     *
     * @param assetId
     * @return
     */
    JSONObject oshiNetstat(Integer assetId);

}
