package com.nis.modules.overview.service;

import java.util.List;
import java.util.Map;

public interface OverviewService {

    /*Map byTypeStat();

    Map scrapeModuleStat();

    Map alertStat();

    List<Map> alertNumTopN(Integer topN);

    List<Map> alertUnprocessed(Integer pageNo, Integer pageSize);

    List<Map> assetModelStat(Integer onlyInUse);

    List<Map> assetDCStat(Integer onlyInUse);

    Map alertRuleStat();

    List<Map> promDCStat();

    Map sysUserStat();*/

    List<Map> alertRuleTopN(Integer topN);

    Map getAssetStat();

    Map getProjectStat();

    Map getEndpointStat();

    Map getDatacenterStat();

    Map getAlertRuleStat();

    List<Map> alertStatByAssetTopN(Integer top);
}
