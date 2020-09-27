package com.nis.modules.overview.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.asset.dao.*;
import com.nis.modules.asset.entity.*;
import com.nis.modules.asset.service.ModelService;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.dao.ModuleDao;
import com.nis.modules.module.entity.Module;
import com.nis.modules.overview.dao.OverviewDao;
import com.nis.modules.overview.entity.AssetStat;
import com.nis.modules.overview.entity.*;
import com.nis.modules.overview.service.OverviewService;
import com.nis.modules.project.dao.ProjectDao;
import com.nis.modules.project.entity.Project;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.service.impl.PromserverProxyServiceImpl;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("overviewService")
public class OverviewServiceImpl implements OverviewService {
    private static Logger logger = LoggerFactory.getLogger(OverviewServiceImpl.class);

    @Autowired
    private OverviewDao overviewDao;

    @Autowired
    private ModelService modelService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private AlertRuleDao alertRuleDao;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private PromserverProxyServiceImpl promserverProxyService;

    @Autowired
    private CabinetDao cabinetDao;

    @Autowired
    private ModuleDao moduleDao;

    @Autowired
    private AlertMessageDao alertMessageDao;

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private AssetPingDao assetPingDao;

    @Autowired
    private IdcDao idcDao;

    @Autowired
    private ModelDao modelDao;

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Value("${nezha.promApis.query}")
    private String query;

    @Override
    public List<Map> alertRuleTopN(Integer top) {
        List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
        Map<Integer, List<AlertMessageEntity>> ruleIdAndAlertsMap = alertList.stream().collect(Collectors.groupingBy(AlertMessageEntity::getRuleId));

        Map<Integer, AlertRuleEntity> ruleIdAndEntityMap = new HashMap<>();
        List<AlertRuleEntity> rules = alertRuleDao.getAlertRulesStatColumn();
        for (AlertRuleEntity rule : rules) {
            ruleIdAndEntityMap.put(rule.getId(), rule);
        }

        List<Map.Entry<Integer, Integer>> list = this.getAlertMessageTop(ruleIdAndAlertsMap, top);

        List<Map> resultData = new ArrayList<>(top);
        Map map;
        AlertRuleEntity ruleEntity;
        for (Map.Entry<Integer, Integer> entry : list) {
            ruleEntity = ruleIdAndEntityMap.get(entry.getKey());
            if (ToolUtil.isEmpty(ruleEntity)) continue;

            map = new HashMap();
            map.put("id", ruleEntity.getId());
            map.put("alertName", ruleEntity.getAlertName());
            map.put("level", ruleEntity.getSeverity());
            map.put("nums", entry.getValue());

            resultData.add(map);
        }
        return resultData;
    }

    @Override
    public Map getAssetStat() {
        List<Asset> assetList = assetDao.getAssetsStatColumn();
        List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();

        List<AssetPing> assetPingList = Collections.emptyList();
        SysConfigEntity pingSwitchConfig = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_PING_SWITCH));
        if (pingSwitchConfig != null && Constant.ASSET_PING_OPEN_SWITCH.equals(pingSwitchConfig.getParamValue())) {
            assetPingList = assetPingDao.selectList(null);
        }

        // 过滤掉没有 asset 的 idc
        List<AssetStat> assetStatByDc = this.getAssetStatByDc(assetList, assetPingList, alertList);
        assetStatByDc = assetStatByDc.stream().filter(assetStat -> ToolUtil.isNotEmpty(assetStat.getTotal()) && assetStat.getTotal() > 0).collect(Collectors.toList());

        // 返回结果
        Map<String, Object> resultData = new HashMap<>();
        List<AlertMessageEntity> alertListByAsset = alertList.stream().filter(alert -> ToolUtil.isNotEmpty(alert.getAssetId())).collect(Collectors.toList());
        resultData.put("totalStat", this.getAssetStat(assetList, assetPingList, alertListByAsset));
        resultData.put("dcStat", assetStatByDc);
        resultData.put("modelStat", this.getAssetStatByModel(assetList, assetPingList, alertListByAsset));
        resultData.put("typeStat", this.getAssetStatByAssetType(assetList, assetPingList, alertList));
        return resultData;
    }

    private List<AssetStat> getAssetStatByAssetType(List<Asset> assetList, List<AssetPing> assetPingList, List<AlertMessageEntity> alertList) {
        Map<Integer, List<Asset>> modelIdAndAssetsMap = assetList.stream().collect(Collectors.groupingBy(Asset::getModelId));

        // assetType名称 和 关联的 modelids
        List<Map> assetTypeNameAndModelIds = modelDao.getModelIdsGroupByType();
        String modelIds;
        List<Asset> tempAssetList;
        List<AssetPing> tempAssetPing;
        List<AlertMessageEntity> tempAlertList;
        AssetStat assetStat;
        List<AssetStat> typeStats = new ArrayList<>();
        for (Map map : assetTypeNameAndModelIds) {
            modelIds = (String) map.get("modelIds");
            if (StringUtils.isEmpty(modelIds)) continue;

            tempAssetList = new ArrayList<>();
            for (String modelId : Arrays.asList(modelIds.split(","))) {
                List<Asset> assets = modelIdAndAssetsMap.get(Integer.valueOf(modelId));
                if (CollectionUtils.isNotEmpty(assets)) {
                    tempAssetList.addAll(assets);
                }
            }
            // 只统计包含 asset 的 assetType
            if (CollectionUtils.isEmpty(tempAssetList)) continue;

            tempAlertList = this.getAlertsByAsset(tempAssetList, alertList);
            tempAssetPing = this.getAssetPingByAsset(tempAssetList, assetPingList);

            assetStat = this.getAssetStat(tempAssetList, tempAssetPing, tempAlertList);
            assetStat.setName(map.get("name").toString());

            typeStats.add(assetStat);
        }
        return typeStats;
    }

    private List<AssetStat> getAssetStatByModel(List<Asset> assetList, List<AssetPing> assetPingList, List<AlertMessageEntity> alertList) {
        List<Model> models = modelService.list();
        Map<Integer, List<Asset>> modelIdAndAssetListMap = assetList.stream().collect(Collectors.groupingBy(Asset::getModelId));
        List<AssetStat> modelStats = new ArrayList<>();
        AssetStat assetStat;
        List<Asset> tempAssets;
        List<AssetPing> tempAssetPing;
        List<AlertMessageEntity> tempAlertList;
        for (Model model : models) {
            tempAssets = modelIdAndAssetListMap.get(model.getId());
            if (CollectionUtils.isEmpty(tempAssets)) continue;

            tempAlertList = this.getAlertsByAsset(tempAssets, alertList);
            tempAssetPing = this.getAssetPingByAsset(tempAssets, assetPingList);

            assetStat = this.getAssetStat(tempAssets, tempAssetPing, tempAlertList);
            assetStat.setId(model.getId());
            assetStat.setName(model.getName());

            modelStats.add(assetStat);
        }

        return modelStats;
    }

    @Override
    public List<AssetStat> getAssetStatByCabinet() {
        List<Cabinet> cabinetList = cabinetDao.selectList(null);
        List<Asset> assetList = assetDao.getAssetsStatColumn();
        assetList = assetList.stream().filter(asset -> ToolUtil.isNotEmpty(asset.getCabinetId())).collect(Collectors.toList());
        Map<Integer, List<Asset>> cabientIdAndAssetListMap = assetList.stream().collect(Collectors.groupingBy(Asset::getCabinetId));

        List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();

        List<AssetStat> cabinetStats = new ArrayList<>();

        AssetStat assetStat;
        List<Asset> tempAssets;
        List<AlertMessageEntity> tempAlertList;
        for (Cabinet cabinet : cabinetList) {
            tempAssets = cabientIdAndAssetListMap.get(cabinet.getId());
            if (CollectionUtils.isEmpty(tempAssets)) continue;

            tempAlertList = this.getAlertsByAsset(tempAssets, alertList);
            assetStat = this.getAssetStat(tempAssets, Collections.emptyList(), tempAlertList);
            assetStat.setId(cabinet.getId());

            cabinetStats.add(assetStat);
        }
        return cabinetStats;
    }

    @Override
    public List<AssetStat> getAssetStatByDc(List<Asset> assetList, List<AssetPing> assetPingList, List<AlertMessageEntity> alertList) {
        List<Idc> idcList = idcDao.selectList(null);
        Map<Integer, List<Asset>> idcIdAndAssetListMap = assetList.stream().collect(Collectors.groupingBy(Asset::getIdcId));
        List<AlertMessageEntity> alertListByDc = alertList.stream().filter(alert -> ToolUtil.isNotEmpty(alert.getDcId())).collect(Collectors.toList());
        List<AssetStat> dcStats = new ArrayList<>();
        AssetStat assetStat;
        List<Asset> tempAssets;
        List<AssetPing> tempAssetPing;
        List<AlertMessageEntity> tempAlertList;
        for (Idc idc : idcList) {
            tempAssets = idcIdAndAssetListMap.get(idc.getId());
            tempAssets = CollectionUtils.isEmpty(tempAssets) ? Collections.emptyList() : tempAssets;

            tempAlertList = alertListByDc.stream().filter(alert -> idc.getId().equals(alert.getDcId())).collect(Collectors.toList());
            tempAssetPing = this.getAssetPingByAsset(tempAssets, assetPingList);

            assetStat = this.getAssetStat(tempAssets, tempAssetPing, tempAlertList);
            assetStat.setId(idc.getId());
            assetStat.setName(idc.getName());

            dcStats.add(assetStat);
        }
        return dcStats;
    }

    private List<AlertMessageEntity> getAlertsByAsset(List<Asset> assetList, List<AlertMessageEntity> alertList) {
        List<Integer> assetIdList = assetList.stream().map(Asset::getId).collect(Collectors.toList());
        List<AlertMessageEntity> alertMessageList = alertList.stream().filter(alert -> assetIdList.contains(alert.getAssetId())).collect(Collectors.toList());
        return alertMessageList;
    }

    private List<AssetPing> getAssetPingByAsset(List<Asset> assetList, List<AssetPing> assetPingList) {
        List<String> hostList = assetList.stream().map(Asset::getHost).collect(Collectors.toList());
        List<AssetPing> assetPings = assetPingList.stream().filter(assetPing -> hostList.contains(assetPing.getHost())).collect(Collectors.toList());
        return assetPings;
    }

    private AssetStat getAssetStat(List<Asset> assetList, List<AssetPing> assetPingList, List<AlertMessageEntity> alertList) {
        AssetStat assetStat = new AssetStat();
        List<AssetPing> assetPingByAsset = this.getAssetPingByAsset(assetList, assetPingList);

        long inStock = assetList.stream().filter(asset -> asset.getState().equals(1)).count();
        long outStock = assetList.stream().filter(asset -> asset.getState().equals(2)).count();
        long pingUp = assetPingByAsset.stream().filter(assetPing -> assetPing.getStatus().equals(1)).count();
        long pingDown = assetPingByAsset.stream().filter(assetPing -> assetPing.getStatus().equals(0)).count();
        long alertHigh = alertList.stream().filter(alert -> Constant.ALERT_SEVERITY_HIGH.equals(alert.getSeverity())).count();
        long alertMedium = alertList.stream().filter(alert -> Constant.ALERT_SEVERITY_MEDIUM.equals(alert.getSeverity())).count();
        long alertLow = alertList.stream().filter(alert -> Constant.ALERT_SEVERITY_LOW.equals(alert.getSeverity())).count();

        assetStat.setTotal(assetList.size());
        assetStat.setInStock(inStock);
        assetStat.setOutStock(outStock);
        assetStat.setPingUp(pingUp);
        assetStat.setPingDown(pingDown);
        assetStat.setAlertTotal(alertList.size());
        assetStat.setAlertHigh(alertHigh);
        assetStat.setAlertMedium(alertMedium);
        assetStat.setAlertLow(alertLow);
        return assetStat;
    }

    @Override
    public Map getProjectStat() {
        Map resultData = new HashMap();

        List<Project> projects = projectDao.selectList(null);
        List<Module> moduleList = moduleDao.getModulesStatColumn();
        List<Endpoint> endpointList = endpointService.list();
        List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();

        Map<Integer, List<Module>> projectIdAndModuleListMap = moduleList.stream().collect(Collectors.groupingBy(Module::getProjectId));
        Map<Integer, List<Endpoint>> moduleIdAndEndpointListMap = endpointList.stream().collect(Collectors.groupingBy(Endpoint::getModuleId));

        List<Module> tempModuelList;
        List<Endpoint> tempEndpoints;
        ProjectStat projectStat;
        List<ProjectStat> projectStats = new ArrayList<>();
        for (Project project : projects) {
            tempModuelList = projectIdAndModuleListMap.get(project.getId());
            tempModuelList = CollectionUtils.isEmpty(tempModuelList) ? Collections.emptyList() : tempModuelList;

            int endpointNum = 0;
            for (Module module : tempModuelList) {
                tempEndpoints = moduleIdAndEndpointListMap.get(module.getId());
                endpointNum += CollectionUtils.isNotEmpty(tempEndpoints) ? tempEndpoints.size() : 0;
            }

            long alertNum = alertList.stream().filter(alert -> project.getId().equals(alert.getProjectId())).count();
            projectStat = new ProjectStat();
            projectStat.setId(project.getId());
            projectStat.setName(project.getName());
            projectStat.setModuleNum(tempModuelList.size());
            projectStat.setEndpointNum(endpointNum);
            projectStat.setAlertNum(Integer.valueOf(String.valueOf(alertNum)));

            projectStats.add(projectStat);
        }

        resultData.put("total", projects.size());
        resultData.put("projectStat", projectStats);

        return resultData;
    }

    @Override
    public Map getEndpointStat() {
        // 返回信息包含 name promUp promDown promTotal endPointTotal
        List<EndpointStat> endpointInfoByDc = overviewDao.getEndpointInfoByDc();

        // 补充 endpointTotal endpointUp endpointDown 信息
        Map<String, List<Integer>> idcNameWithEndpointIdsMap = this.getDcNameWithEndpointInfo();

        // 服务健康的endpoint id列表
        Set<Integer> endpointUpIds = this.getUpStatusEndpointIds();

        for (EndpointStat stat : endpointInfoByDc) {
            List<Integer> list = idcNameWithEndpointIdsMap.get(stat.getName());
            int endpointUp = 0;
            int total = list.size();
            if (CollectionUtils.isNotEmpty(list)) {
                for (Integer id : list) {
                    if (endpointUpIds.contains(id)) {
                        endpointUp++;
                    }
                }
            }
            stat.setEndpointTotal(total);
            stat.setEndpointUp(endpointUp);
            stat.setEndpointDown(total - endpointUp);
        }

        int count = endpointService.count();
        Map resultData = new HashMap();
        resultData.put("total", count);
        resultData.put("up", endpointUpIds.size());
        resultData.put("down", count - endpointUpIds.size());
        resultData.put("dcStat", endpointInfoByDc);

        return resultData;
    }

    private Map<String, List<Integer>> getDcNameWithEndpointInfo() {
        List<Idc> idcList = idcDao.selectList(null);
        List<Asset> assetList = assetDao.getAssetsStatColumn();
        List<Endpoint> endpointList = endpointService.list();

        Map<Integer, List<Asset>> idcIdAndAssetListMap = assetList.stream().collect(Collectors.groupingBy(Asset::getIdcId));
        Map<Integer, List<Endpoint>> assetIdAndEndpointListMap = endpointList.stream().collect(Collectors.groupingBy(Endpoint::getAssetId));

        Map<String, List<Integer>> idcNameWithEndpointIdsMap = new HashMap<>();
        List<Asset> tempAssets;
        List<Endpoint> tempEndpoints;
        List<Integer> endpointIdsByDc;
        for (Idc idc : idcList) {
            tempAssets = idcIdAndAssetListMap.get(idc.getId());
            if (CollectionUtils.isEmpty(tempAssets)) {
                idcNameWithEndpointIdsMap.put(idc.getName(), Collections.emptyList());
                continue;
            }

            endpointIdsByDc = new ArrayList<>();
            for (Asset entity : tempAssets) {
                tempEndpoints = assetIdAndEndpointListMap.get(entity.getId());
                if (CollectionUtils.isEmpty(tempEndpoints)) continue;

                endpointIdsByDc.addAll(tempEndpoints.stream().map(Endpoint::getId).collect(Collectors.toList()));
            }

            idcNameWithEndpointIdsMap.put(idc.getName(), endpointIdsByDc);
        }
        return idcNameWithEndpointIdsMap;
    }

    @Override
    public Map getDatacenterStat() {
        List<DcStat> dcStatList = new ArrayList<>();
        // 根据dc维度统计信息
        List<Asset> assetList = assetDao.getAssetsStatColumn();
        List<AssetPing> assetPingList = assetPingDao.selectList(null);
        List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();

        List<AssetStat> assetStatInfoByIdc = this.getAssetStatByDc(assetList, assetPingList, alertList);

        // endpoint stat 统计信息
        Map endpointStatInfoMap = this.getEndpointStat();
        Map<String, EndpointStat> dcNameWithEndpointStatMap = new HashMap<>();
        List<EndpointStat> endpointInfoByDc = (List<EndpointStat>) endpointStatInfoMap.get("dcStat");

        for (EndpointStat stat : endpointInfoByDc) {
            dcNameWithEndpointStatMap.put(stat.getName(), stat);
        }

        DcStat dcStat;
        EndpointStat endpointStat;
        for (AssetStat assetStat : assetStatInfoByIdc) {
            dcStat = new DcStat(assetStat.getTotal(), assetStat.getInStock(), assetStat.getOutStock(), assetStat.getPingUp(), assetStat.getPingDown(), assetStat.getAlertTotal(), assetStat.getAlertHigh(), assetStat.getAlertMedium(), assetStat.getAlertLow());

            endpointStat = dcNameWithEndpointStatMap.get(assetStat.getName());
            dcStat.setName(endpointStat.getName());
            dcStat.setPromTotal(endpointStat.getPromTotal());
            dcStat.setPromUp(endpointStat.getPromUp());
            dcStat.setPromDown(endpointStat.getPromDown());
            dcStat.setEndpointTotal(endpointStat.getEndpointTotal());
            dcStat.setEndpointUp(endpointStat.getEndpointUp());
            dcStat.setEndpointDown(endpointStat.getEndpointDown());
            dcStat.setId(endpointStat.getId());

            dcStatList.add(dcStat);
        }

        int cabinetTotal = cabinetDao.selectCount(null);

        Map resultData = new HashMap();
        resultData.put("dcTotal", dcStatList.size());
        resultData.put("cabinetTotal", cabinetTotal);
        resultData.put("dcStat", dcStatList);

        return resultData;
    }

    @Override
    public Map getAlertRuleStat() {
        List<AlertRuleEntity> ruleList = alertRuleDao.getAlertRulesStatColumn();
        List<AlertMessageEntity> alertMessageList = alertMessageDao.getAlertsStatColumn();
        Map<Integer, List<AlertMessageEntity>> ruleIdAndMessageList = alertMessageList.stream().collect(Collectors.groupingBy(AlertMessageEntity::getRuleId));

        Map<String, List<AlertRuleEntity>> ruleLevelAndRulesMap = ruleList.stream().collect(Collectors.groupingBy(AlertRuleEntity::getSeverity));

        List<Map> resultMap = new ArrayList<>();
        Map map;
        List<AlertRuleEntity> rules;
        List<AlertMessageEntity> alerts;

        for (Map.Entry<String, List<AlertRuleEntity>> entry : ruleLevelAndRulesMap.entrySet()) {
            map = new HashMap();
            rules = entry.getValue();
            rules = CollectionUtils.isEmpty(rules) ? Collections.emptyList() : rules;

            int alertNum = 0;
            for (AlertRuleEntity entity : rules) {
                alerts = ruleIdAndMessageList.get(entity.getId());
                alertNum += CollectionUtils.isNotEmpty(alerts) ? alerts.size() : 0;
            }

            map.put("level", entry.getKey());
            map.put("ruleNum", rules.size());
            map.put("alertNum", alertNum);

            resultMap.add(map);
        }

        Map resultData = new HashMap();
        resultData.put("alertRuleTotal", ruleList.size());
        resultData.put("alertRule", resultMap);
        return resultData;
    }

    @Override
    public List<Map> alertStatByAssetTopN(Integer top) {
        List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
        alertList = alertList.stream().filter(alert -> ToolUtil.isNotEmpty(alert.getAssetId())).collect(Collectors.toList());

        List<Asset> assetList = assetDao.getAssetsStatColumn();
        Map<Integer, String> assetIdAndHostMap = assetList.stream().collect(Collectors.toMap(Asset::getId, Asset::getHost));

        Map<Integer, List<AlertMessageEntity>> assetIdAndAlertsMap = alertList.stream().collect(Collectors.groupingBy(AlertMessageEntity::getAssetId));
        List<Map.Entry<Integer, Integer>> list = this.getAlertMessageTop(assetIdAndAlertsMap, top);

        List<Map> alertStatByAssetTopN = new ArrayList<>(top);
        Map map;
        String host;
        for (Map.Entry<Integer, Integer> entry : list) {
            host = assetIdAndHostMap.get(entry.getKey());
            if (StringUtils.isEmpty(host)) continue;

            map = new HashMap();
            map.put("host", host);
            map.put("nums", entry.getValue());

            alertStatByAssetTopN.add(map);
        }
        return alertStatByAssetTopN;
    }

    private List<Map.Entry<Integer, Integer>> getAlertMessageTop(Map<Integer, List<AlertMessageEntity>> objIdAndAlertsMap, Integer top) {
        Map<Integer, Integer> assetIdAndAlertNum = new HashMap<>();
        for (Map.Entry<Integer, List<AlertMessageEntity>> entry : objIdAndAlertsMap.entrySet()) {
            int size = entry.getValue().size();
            assetIdAndAlertNum.put(entry.getKey(), size);
        }

        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(assetIdAndAlertNum.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                int compare = (o1.getValue()).compareTo(o2.getValue());
                return -compare;
            }
        });

        if (list.size() > top) {
            list = list.subList(0, top);
        }
        return list;
    }

    @Override
    public Map getModuleStat() {
        Map resultData = new HashMap();

        List<Module> moduleList = moduleDao.getModulesStatColumn();
        List<Endpoint> endpointList = endpointService.list();
        List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();

        Map<Integer, List<Endpoint>> moduleIdAndEndpointListMap = endpointList.stream().collect(Collectors.groupingBy(Endpoint::getModuleId));

        List<ModuleStat> moduleStats = new ArrayList<>();
        ModuleStat moduleStat;
        List<Endpoint> endpoints;
        for (Module module : moduleList) {
            moduleStat = new ModuleStat();
            endpoints = moduleIdAndEndpointListMap.get(module.getId());
            endpoints = CollectionUtils.isEmpty(endpoints) ? Collections.emptyList() : endpoints;

            long alertNum = alertList.stream().filter(alert -> module.getId().equals(alert.getModuleId())).count();

            moduleStat.setId(module.getId());
            moduleStat.setName(module.getName());
            moduleStat.setEndpointNum(endpoints.size());
            moduleStat.setAlertNum(Integer.valueOf(String.valueOf(alertNum)));

            moduleStats.add(moduleStat);
        }

        resultData.put("moduleStat", moduleStats);
        resultData.put("total", moduleList.size());
        return resultData;
    }

    @Override
    public Map alertMessageStat() {
        List<AlertMessageEntity> alertMessageList = alertMessageDao.getAlertsStatColumn();

        List<Map> byLevelAlertList = new ArrayList<>();
        long alertHigh = alertMessageList.stream().filter(alert -> Constant.ALERT_SEVERITY_HIGH.equals(alert.getSeverity())).count();
        long alertMedium = alertMessageList.stream().filter(alert -> Constant.ALERT_SEVERITY_MEDIUM.equals(alert.getSeverity())).count();
        long alertLow = alertMessageList.stream().filter(alert -> Constant.ALERT_SEVERITY_LOW.equals(alert.getSeverity())).count();

        Map map = new HashMap(4);
        map.put("level", Constant.ALERT_SEVERITY_HIGH);
        map.put("alertNum", alertHigh);
        byLevelAlertList.add(map);

        map = new HashMap(4);
        map.put("level", Constant.ALERT_SEVERITY_MEDIUM);
        map.put("alertNum", alertMedium);
        byLevelAlertList.add(map);

        map = new HashMap(4);
        map.put("level", Constant.ALERT_SEVERITY_LOW);
        map.put("alertNum", alertLow);
        byLevelAlertList.add(map);

        Map resultData = new HashMap(4);

        resultData.put("alertMessageTotal", alertMessageList.size());
        resultData.put("alertMessage", byLevelAlertList);

        return resultData;
    }

    @Override
    public List<Map> alertStatByModuleTopN(Integer top) {
        List<Module> moduleList = moduleDao.getModulesStatColumn();
        Map<Integer, String> moduleIdAndNameMap = moduleList.stream().collect(Collectors.toMap(Module::getId, Module::getName));
        List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
        alertList = alertList.stream().filter(alert -> ToolUtil.isNotEmpty(alert.getModuleId())).collect(Collectors.toList());

        Map<Integer, List<AlertMessageEntity>> moduleIdAndAlertsMap = alertList.stream().collect(Collectors.groupingBy(AlertMessageEntity::getModuleId));
        List<Map.Entry<Integer, Integer>> list = this.getAlertMessageTop(moduleIdAndAlertsMap, top);

        List<Map> resultData = new ArrayList<>(top);
        Map map;
        String moduleName;
        for (Map.Entry<Integer, Integer> entry : list) {
            moduleName = moduleIdAndNameMap.get(entry.getKey());
            if (ToolUtil.isEmpty(moduleName)) continue;

            map = new HashMap();
            map.put("module", moduleName);
            map.put("nums", entry.getValue());

            resultData.add(map);
        }
        return resultData;
    }

    // 请求prometheus 然后返回状态可用的endpoint ids
    @Override
    public Set<Integer> getUpStatusEndpointIds() {
        Set<Integer> endPointIds = new HashSet<>();

        // 1. 查询 global  prometheus 信息 global 会汇总up时序数据
        List<Promserver> promservers = promserverService.list(new QueryWrapper<Promserver>().eq("type", 1).eq("status", 1));
        String url = "/api/v1/query?query=up";
        Map result = null;
        Map data;
        JSONArray resultArray;
        JSONObject obj = null;
        List<String> timeAndState = null;
        String endPointValue = null;
        Integer endpointId = null;
        Map<String,String> metricLabels;
        for (Promserver promserver : promservers) {
        	//查询up
            try {
				result = promserverProxyService.promRequest(url, "GET", null, promserver.getId(), null, null, null);
			} catch (Exception e1) {
				logger.error("request error", e1);
			}
            // 查询成功
            if (result != null && "success".equals(result.get("status"))) {
                data = (Map) result.get("data");
                resultArray = (JSONArray) data.get("result");
                for (int i = 0; i < resultArray.size(); i++) {
                    obj = (JSONObject) resultArray.get(i);
                    timeAndState = (List<String>) obj.get("value");
                    if ("1".equals(timeAndState.get(1))) {
                        metricLabels = (Map) obj.get("metric");
                        endPointValue = metricLabels.get(Constant.PROMSERVERUP_ENDPOINT_LABEL);
                        if(StringUtils.isNotEmpty(endPointValue)){
                            try {
                                endpointId = Integer.parseInt(endPointValue);
                            } catch (NumberFormatException e) {
                                endpointId = null;
                                logger.error("The prometheus server is {}, the value of endpoint value error {}", promserver, endPointValue);
                            }
                            if(endpointId != null){
                                endPointIds.add(endpointId);
                            }
                        }else{
                            logger.debug("The prometheus server is {} And the {} label's job label does not contain endpoint id", promserver, obj);
                        }
                    }
                }
            } else {// 查询失败
                logger.error("query endpoint status failed ,prometheus server is {}", promserver);
            }
        }
        return endPointIds;
    }
}