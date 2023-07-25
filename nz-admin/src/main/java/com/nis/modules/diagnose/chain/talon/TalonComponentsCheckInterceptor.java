package com.nis.modules.diagnose.chain.talon;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.DiagnoseInterceptor;

import java.util.List;
import java.util.Map;

/**
 * TalonComponentsCheckInterceptor
 * 检查  talon components state
 *
 * @author shuai
 * @date 2023/7/25
 */
public class TalonComponentsCheckInterceptor implements DiagnoseInterceptor {

    private static final Log log = Log.get();

    private static final String TALON_AUTH_SUCCESS = "TRUE";
    private static final String COMPONENT_AVAILABLE_STATUS = "UP";

    @Override
    public void intercept(Chain chain) {
        boolean continueDiagnosis = chain.isContinueDiagnosis();
        log.info("[TalonComponentsCheckInterceptor] [continue diagnosis: {}]", continueDiagnosis);

        if (continueDiagnosis) {
            // process object
            AssetAsset asset = (AssetAsset) chain.object();

            // check Components Status
            List<Map<Object, Object>> diagnoseMsgList = this.checkComponentsStatus(asset);

            // update check msg
            DiagnoseClient.addDiagnoseMsg(diagnoseMsgList);
        } else {
            // 未检查
            Map<Object, Object> resultMap = Tool.MapUtil.builder()
                    .put("item", "diagnose.talon.item.components")
                    .put("state", DiagnoseState.NOT_CHECKED.getValue())
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map();

            // update check msg
            DiagnoseClient.addDiagnoseMsg(resultMap);
        }
    }

    private List<Map<Object, Object>> checkComponentsStatus(AssetAsset asset) {
        String result = Tool.StrUtil.EMPTY_JSON;

        List<Map<Object, Object>> componentsStatusList = Tool.ListUtil.list(true);
        UrlBuilder urlBuilder = UrlBuilder.create().setScheme("http").setHost(asset.getManageIp()).setPort(asset.getClientPort()).appendPath("nz-talon").appendPath("healthy");
        try {
            result = AgentUtil.requestGet(urlBuilder.build(), asset.getClientToken(), String.class);
        } catch (Exception e) {
            log.error(e, "[checkComponentsStatus] [request talon healthy api error] [Host: {}]", asset.getManageIp());
        }

        R parseObject = JSONObject.parseObject(result, R.class);
        log.info("[checkComponentsStatus] [talon healthy api] [result: {}]", parseObject);

        if (Tool.BooleanUtil.or(
                Tool.MapUtil.isEmpty(parseObject),
                Tool.ObjectUtil.notEqual(RCode.SUCCESS.getCode(), parseObject.get("code")))) {
            // nztalon  error
            componentsStatusList.add(Tool.MapUtil.builder()
                    .put("item", "diagnose.talon.item.nztalon")
                    .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                    .put("resolution", "diagnose.talon.resolution.nztalon")
                    .map());
        } else {
            // 认证检查
            Object auth = JSONPath.read(result, "data.auth");
            if (Tool.ObjectUtil.notEqual(TALON_AUTH_SUCCESS, auth)) {
                componentsStatusList.add(Tool.MapUtil.builder()
                        .put("item", "diagnose.talon.item.auth")
                        .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                        .put("resolution", "diagnose.talon.resolution.auth")
                        .map());
            }
            // promtail 状态检查
            Object promtail = JSONPath.read(result, "data.promtail");
            if (Tool.ObjectUtil.notEqual(COMPONENT_AVAILABLE_STATUS, promtail)) {
                componentsStatusList.add(Tool.MapUtil.builder()
                        .put("item", "diagnose.talon.item.promtail")
                        .put("state", DiagnoseState.DID_NOT_PASS.getValue())
                        .put("resolution", "diagnose.talon.resolution.promtail")
                        .map());
            }
        }
        // 所有检查通过 PASS
        if (Tool.CollUtil.isEmpty(componentsStatusList)) {
            componentsStatusList.add(Tool.MapUtil.builder()
                    .put("item", "diagnose.talon.item.components")
                    .put("state", DiagnoseState.PASS.getValue())
                    .put("resolution", Tool.StrUtil.EMPTY)
                    .map());
        }
        return componentsStatusList;
    }
}
