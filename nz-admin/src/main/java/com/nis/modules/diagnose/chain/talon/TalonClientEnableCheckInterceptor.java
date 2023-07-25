package com.nis.modules.diagnose.chain.talon;

import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;

import java.util.Map;

/**
 * TalonClientEnableCheckInterceptor
 * 检查  asset talon 是否开启
 *
 * @author shuai
 * @date 2023/7/25
 */
public class TalonClientEnableCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    @Override
    public void intercept(Chain chain) {

        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[TalonClientEnableCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        AssetAsset asset = (AssetAsset) chain.object();
        Integer clientEnable = asset.getClientEnable();

        log.info("[checkTalonEnable] [asset id: {}] [enable: {}]", asset.getId(), clientEnable);

        Map<Object, Object> resultMap = Tool.MapUtil.builder()
                .put("item", "diagnose.talon.item.clientEnable")
                .put("resolution", Tool.StrUtil.EMPTY)
                .map();

        if (continueDiagnosis){

            if (Tool.ObjectUtil.equal(0, clientEnable)) {
                // DID NOT PASS
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                // set resolution
                resultMap.put("resolution", "diagnose.talon.resolution.clientEnable");

                // stopDiagnose
                chain.stopDiagnose();
            } else {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);

        }else {
            resultMap.put("state",DiagnoseState.NOT_CHECKED);
            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);

        }
        // next chain
        chain.proceed(asset);
    }
}
