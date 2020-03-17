package com.nis.modules.overview.dao;

import com.nis.modules.overview.entity.AssetStat;
import com.nis.modules.overview.entity.EndpointStat;
import com.nis.modules.overview.entity.ProjectStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface OverviewDao {

    // 获取alert message 中全部级别的信息   格式为  计数 nums 级别 severity 类型 type
    //List<Map> getAlertMessageAllSeverityInfos();

    // 获取project 没有关联alert message的数量
    //Integer getProjectNotRelationAlertCount();

    // 获取Module 没有关联alert message的数量
    //Integer getModuletNotRelationAlertCount();

    // 获取asset 没有关联alert message的数量
    //Integer getAssetNotRelationAlertCount();

    // 告警数量topN
    //List<Map> alertNumTopN(@Param("topN") Integer topN);

    //  未处理告警（按照告警级别，告警开始排序分页加载）
    // List<Map> alertUnprocessed(@Param("pageNo") Integer pageNo, @Param("pageSize") Integer pageSize);

    // 资产型号统计  返回为 modelId alertHigh(数量)  alertMedium （数量） alertLow （数量）
    //List<Map> assetModelAndAlertSeveritysNums(@Param("onlyInUse") Integer onlyInUse);

    // 资产型号统计中 assetping 可达状态统计 返回为 modelId pingActive (可达数量)  pingInactive （不可达数量）
    //List<Map> assetModelAndPingActivesNums(@Param("onlyInUse") Integer onlyInUse);


    // 按照dc维度统计资产assetping 可达状态统计 返回为 idcId pingActive (可达数量)  pingInactive （不可达数量）
    //List<Map> assetDcAndPingActivesNums(@Param("onlyInUse") Integer onlyInUse);

    // 告警规则统计各个级别的数量 返回  high medium  low （数量）
    //Map alertRuleStat();

    // 按照DC维度 统计prome server 状态 返回  idcId prometheusServerActive prometheusServerInactive (数量)
    // List<Map> promDCAndActiveNums();

    // 获取 idc 下的 endpoint  格式为 idcId endpoints(1,2,...)
    //List<Map> getIdcAndEndpointIds();


    // 按告警规则维度统计告警信息的topN
    List<Map> alertRuleTopN(@Param("topN") Integer topN);

    // 按照DC维度 统计资产alert 信息 返回为 idcId alertHigh(数量)  alertMedium （数量） alertLow （数量）
    List<Map> assetDcAndAlertSeveritysNums(@Param("onlyInUse") Integer onlyInUse);

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
}
