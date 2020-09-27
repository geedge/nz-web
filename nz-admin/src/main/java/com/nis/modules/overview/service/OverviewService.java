package com.nis.modules.overview.service;

import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.overview.entity.AssetStat;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OverviewService {

    List<Map> alertRuleTopN(Integer topN);

    Map getAssetStat();

    Map getProjectStat();

    Map getEndpointStat();

    Map getDatacenterStat();

    Map getAlertRuleStat();

    List<Map> alertStatByAssetTopN(Integer top);

    Map getModuleStat();

    Map alertMessageStat();

    List<Map> alertStatByModuleTopN(Integer top);
    
    Set<Integer> getUpStatusEndpointIds();

    List<AssetStat> getAssetStatByDc(List<Asset> assetList, List<AssetPing> assetPingList, List<AlertMessageEntity> alertList);

    List<AssetStat> getAssetStatByCabinet();
}
