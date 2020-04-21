package com.nis.modules.overview.dao;

import com.nis.modules.overview.entity.AssetStat;
import com.nis.modules.overview.entity.EndpointStat;
import com.nis.modules.overview.entity.ModuleStat;
import com.nis.modules.overview.entity.ProjectStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface OverviewDao {
    // 按告警规则维度统计告警信息的topN
    List<Map> alertRuleTopN(@Param("topN") Integer topN);

    // 按照Cabinet维度 统计资产 alert 信息 返回为 cabinetId alertHigh(数量)  alertMedium （数量） alertLow （数量）
    List<Map> assetCabAndAlertSeveritysNums(@Param("onlyInUse") Integer onlyInUse);

    // 关联并且type = per prometheus status 为可用的 prometheus 关联 dc的名称集合
    List<String> getIdcNameByPerPromeServer();

    AssetStat getAssetTotalInfo();

    List<AssetStat> getAssetStatInfoByModel();

    List<AssetStat> getAssetStatInfoByIdc();

    List<ProjectStat> getProjectStatInfo();

    List<EndpointStat> getEndpointInfoByDc();

    List<Map<String, String>> getIdcNameWithEndpointIds();

    List<Map> getAlertRuleStat();

    List<Map> getAlertStatByAssetTopN(@Param("topN") Integer topN);

    List<ModuleStat> getModuleStatInfo();

    List<ModuleStat> getEndPointNumByModule();

    List<Map> getAlertMessageStatGroupByLevel();

    List<Map> getAlertStatByModuleTopN(@Param("topN")Integer topN);
}
