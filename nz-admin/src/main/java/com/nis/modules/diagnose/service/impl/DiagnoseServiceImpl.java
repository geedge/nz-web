package com.nis.modules.diagnose.service.impl;

import cn.hutool.log.Log;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.diagnose.chain.DiagnoseClient;
import com.nis.modules.diagnose.chain.asset.AgentStateCheckOfPingInterceptor;
import com.nis.modules.diagnose.chain.asset.AssetNetworkCheckInterceptor;
import com.nis.modules.diagnose.chain.asset.AssetStateConfCheckInterceptor;
import com.nis.modules.diagnose.chain.asset.BlackboxConfigCheckInterceptor;
import com.nis.modules.diagnose.service.DiagnoseService;
import com.nis.modules.sys.service.SysI18nService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * DiagnoseServiceImpl 故障排查
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@Service
public class DiagnoseServiceImpl implements DiagnoseService {

    private static final Log log = Log.get();

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private SysI18nService sysI18nService;

    @Override
    public List<Map<Object, Object>> assetErrorDiagnosis(Integer assetId, String language) {
        try {
            AssetAsset asset = assetService.getById(assetId);
            ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);

            // diagnose client. set diagnose chain
            DiagnoseClient diagnoseClient = new DiagnoseClient.Builder()
                    .addInterceptor(new AssetStateConfCheckInterceptor())
                    .addInterceptor(new AssetNetworkCheckInterceptor())
                    .addInterceptor(new AgentStateCheckOfPingInterceptor())
                    .addInterceptor(new BlackboxConfigCheckInterceptor())
                    .build();

            // diagnose
            diagnoseClient.diagnose(asset);

            // Diagnose Msg List
            List<Map<Object, Object>> diagnoseMsgList = this.queryDiagnoseMsgByLanguage(language);
            return diagnoseMsgList;
        } finally {
            DiagnoseClient.cleanDiagnoseMsg();
        }
    }

    /**
     * query Diagnose Msg By Language
     *
     * @param language
     * @return
     */
    private List<Map<Object, Object>> queryDiagnoseMsgByLanguage(String language) {
        List<Map<Object, Object>> diagnoseMsgList = DiagnoseClient.queryDiagnoseMsg();

        for (Map<Object, Object> map : diagnoseMsgList) {
            String itemCode = map.get("item").toString();
            String itemValue = sysI18nService.queryValue(itemCode, language);
            map.put("item", itemValue);

            String resolutionCode = map.get("resolution").toString();
            String resolutionValue = sysI18nService.queryValue(resolutionCode, language);
            map.put("resolution", resolutionValue);
        }

        return diagnoseMsgList;
    }
}
