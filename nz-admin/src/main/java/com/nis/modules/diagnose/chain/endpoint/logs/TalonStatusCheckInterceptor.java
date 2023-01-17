package com.nis.modules.diagnose.chain.endpoint.logs;


import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * TalonStatusCheckInterceptor
 * 检查 NZ-Talon 组件状态
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Component
public class TalonStatusCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static final String TALON_AUTH_SUCCESS = "TRUE";
    private static final String COMPONENT_AVAILABLE_STATUS = "UP";

    private static AssetAssetService assetService;

    @Autowired
    public void initBean(AssetAssetService assetService) {
        TalonStatusCheckInterceptor.assetService = assetService;
    }

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[TalonStatusCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            MonitorEndpoint endpoint = (MonitorEndpoint) chain.object();

            // check Talon Status
            List<Map<Object, Object>> diagnoseMsgList = this.checkTalonStatus(endpoint.getAssetId());

            // update check msg
            DiagnoseClient.addDiagnoseMsg(diagnoseMsgList);
        } else {
            // 未检查
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "logs.diagnose.endpoint.item.talon")
                    .put("state", DiagnoseState.NOT_CHECKED.getValue())
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        }
    }

    /**
     * check Talon Status
     *
     * @param assetId
     * @return
     */
    private List<Map<Object, Object>> checkTalonStatus(Integer assetId) {
        log.info("[checkTalonStatus] [check logs endpoint state] [assetId: {}]", assetId);
        AssetAsset asset = assetService.getById(assetId);

        String resultJsonStr = Tool.StrUtil.EMPTY_JSON;
        try {
            UrlBuilder urlBuilder = UrlBuilder.create().setScheme("http")
                    .setHost(asset.getManageIp()).setPort(asset.getClientPort())
                    .appendPath("nz-talon").appendPath("healthy");
            resultJsonStr = AgentUtil.requestGet(urlBuilder.build(), asset.getClientToken(), String.class);
        } catch (Exception e) {
            log.error(e, "[checkTalonStatus] [request talon healthy api error] [asset: {}]", Tool.JSONUtil.toJsonStr(asset));
        }
        log.info("[checkTalonStatus] [talon healthy api] [result: {}]", resultJsonStr);

        List<Map<Object, Object>> componentsStatusList = Tool.ListUtil.list(true);

        Map resultObj = JSONObject.parseObject(resultJsonStr, Map.class);
        if (Tool.BooleanUtil.or(
                Tool.MapUtil.isEmpty(resultObj),
                Tool.ObjectUtil.notEqual(RCode.SUCCESS.getCode(), resultObj.get("code")))) {
            // talon error
            componentsStatusList.add(Tool.MapUtil.builder()
                    .put("item", "logs.diagnose.endpoint.item.talon")
                    .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                    .put("resolution", "diagnose.endpoint.resolution.talon")
                    .map());
        } else {
            Object auth = JSONPath.read(resultJsonStr, "data.auth");
            if (Tool.ObjectUtil.notEqual(TALON_AUTH_SUCCESS, auth)) {
                componentsStatusList.add(Tool.MapUtil.builder()
                        .put("item", "diagnose.endpoint.item.talon.auth")
                        .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                        .put("resolution", "diagnose.endpoint.resolution.talon.auth")
                        .map());
            }

            Object promtail = JSONPath.read(resultJsonStr, "data.promtail");
            if (Tool.ObjectUtil.notEqual(COMPONENT_AVAILABLE_STATUS, promtail)) {
                componentsStatusList.add(Tool.MapUtil.builder()
                        .put("item", "diagnose.endpoint.item.talon.promtail")
                        .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                        .put("resolution", "diagnose.endpoint.resolution.talon.promtail")
                        .map());
            }
        }

        // 所有检查通过 PASS
        if (Tool.CollUtil.isEmpty(componentsStatusList)) {
            componentsStatusList.add(Tool.MapUtil.builder()
                    .put("item", "logs.diagnose.endpoint.item.talon")
                    .put("state", DiagnoseState.PASS.getValue())
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map());
        }
        return componentsStatusList;
    }

}
