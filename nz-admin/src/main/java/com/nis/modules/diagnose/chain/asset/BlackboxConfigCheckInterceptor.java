package com.nis.modules.diagnose.chain.asset;


import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * BlackboxConfigCheckInterceptor
 * 检查 是否下发 Asset ping 配置 blackbox_exporter 配置
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class BlackboxConfigCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();


    private static PromApiService promApiService;

    @Autowired
    public void initBean(PromApiService promApiService) {
        BlackboxConfigCheckInterceptor.promApiService = promApiService;
    }

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[BlackboxConfigCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            AssetAsset asset = (AssetAsset) chain.object();

            // query Has Results
            boolean queryHasResults = this.checkBlackboxConfig(asset);

            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "diagnose.asset.item.blackbox")
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            if (queryHasResults) {
                // PASS
                resultMap.put("state", DiagnoseState.PASS.getValue());
            } else {
                // DID NOT PASS
                resultMap.put("state", DiagnoseState.DID_NOT_PASS.getValue());
                // set resolution
                resultMap.put("resolution", "diagnose.asset.resolution.blackbox");

                // stopDiagnose
                chain.stopDiagnose();
            }

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        } else {

            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "diagnose.asset.item.blackbox")
                    .put("state", DiagnoseState.NOT_CHECKED.getValue())
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        }

        chain.proceed(chain.object());
    }

    /**
     * check Black box Config
     *
     * @param asset
     * @return
     */
    private boolean checkBlackboxConfig(AssetAsset asset) {
        String queryExpr = String.format("probe_success{host='%s',module='ping'}", asset.getManageIp());

        log.info("[checkBlackboxConfig] [query prom api] [expr: {}]", queryExpr);
        JSONObject jsonObject = promApiService.query(queryExpr, null, null);
        Object read = JSONPath.read(Tool.JSONUtil.toJsonStr(jsonObject), "data.result");
        List list = (List) Tool.ObjectUtil.defaultIfNull(read, Tool.ListUtil.empty());

        log.info("[checkBlackboxConfig] [probe_success result size: {}]", list.size());
        return Tool.CollUtil.isNotEmpty(list);
    }


}
