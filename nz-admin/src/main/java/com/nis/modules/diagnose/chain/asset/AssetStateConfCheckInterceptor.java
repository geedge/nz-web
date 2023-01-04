package com.nis.modules.diagnose.chain.asset;


import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetStateConf;
import com.nis.modules.asset.service.AssetStateConfService;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AssetStateConfCheckInterceptor
 * 检查 Asset state_conf 是否支持 ping
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class AssetStateConfCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static AssetStateConfService assetStateConfService;

    @Autowired
    public void initBean(AssetStateConfService assetStateConfService) {
        AssetStateConfCheckInterceptor.assetStateConfService = assetStateConfService;
    }

    @Override
    public void intercept(Chain chain) {
        // process object
        AssetAsset asset = (AssetAsset) chain.object();

        AssetStateConf stateConf = assetStateConfService.getById(asset.getStateId());
        Integer ping = stateConf.getPing();
        log.info("[checkAssetStateConf] [asset id: {}] [state.ping: {}]", asset.getId(), ping);

        Map<Object, Object> resultMap = Tool.MapUtil.builder()
                .put("item", "diagnose.asset.item.stateconf")
                .put("resolution", Tool.StrUtil.EMPTY)
                .map();

        if (Tool.ObjectUtil.equal(0, ping)) {
            // DID NOT PASS
            resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
            // set resolution
            resultMap.put("resolution", "diagnose.asset.resolution.stateconf");

            // stopDiagnose
            chain.stopDiagnose();
        } else {
            // PASS
            resultMap.put("state", DiagnoseState.PASS.getValue());
        }

        // update check msg
        DiagnoseClient.addDiagnoseMsg(resultMap);

        // next chain
        chain.proceed(asset);
    }

}
