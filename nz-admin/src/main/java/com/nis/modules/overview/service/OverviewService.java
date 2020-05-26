package com.nis.modules.overview.service;

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
    
    Set<Integer> getEndpointStatus();
}
