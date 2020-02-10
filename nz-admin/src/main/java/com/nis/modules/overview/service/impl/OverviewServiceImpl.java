package com.nis.modules.overview.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.AssetPingService;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.asset.service.ModelService;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.overview.dao.OverviewDao;
import com.nis.modules.overview.service.OverviewService;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.service.impl.PromserverProxyServiceImpl;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("overviewService")
public class OverviewServiceImpl implements OverviewService {
    private static Logger logger = LoggerFactory.getLogger(OverviewServiceImpl.class);

    @Autowired
    private OverviewDao overviewDao;

    @Autowired
    private AssetService assetService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private IdcService idcService;

    @Autowired
    private AssetPingService assetPingService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private PromserverProxyServiceImpl promserverProxyService;

    @Value("${nezha.promApis.query}")
    private String query;


    @Override
    public Map byTypeStat() {
        Map resultMap = new HashMap();

        Map projectInfo = new HashMap();
        Map moduleInfo = new HashMap();
        Map assetInfo = new HashMap();

        List<Map> alertMessageAllSeverityInfos = overviewDao.getAlertMessageAllSeverityInfos();

        // 当前没有任何计数信息
        if (CollectionUtils.isEmpty(alertMessageAllSeverityInfos)) {
            return null;
        }

        String nums, severity = null;
        Integer type = null;
        Map<String, String> tempMap;
        for (Map entity : alertMessageAllSeverityInfos) {
            tempMap = new HashMap<>();

            nums = entity.get("nums").toString();
            severity = entity.get("severity").toString();
            type = Integer.parseInt(entity.get("type").toString());

            tempMap.put(severity, nums);

            if (Constant.AlertType.ALERT_TYPE_PROJECT.getCode().equals(type)) {
                projectInfo.putAll(tempMap);
            } else if (Constant.AlertType.ALERT_TYPE_MODULE.getCode().equals(type)) {
                moduleInfo.putAll(tempMap);
            } else if (Constant.AlertType.ALERT_TYPE_ASSET.getCode().equals(type)) {
                assetInfo.putAll(tempMap);
            }
        }

        this.addLackSeverity(projectInfo);
        this.addLackSeverity(moduleInfo);
        this.addLackSeverity(assetInfo);

        projectInfo.put("none", overviewDao.getProjectNotRelationAlertCount());
        moduleInfo.put("none", overviewDao.getModuletNotRelationAlertCount());
        assetInfo.put("none", overviewDao.getAssetNotRelationAlertCount());

        // 可达
        int pingActive = assetPingService.count(new QueryWrapper<AssetPing>().eq("status", 1));
        // 不可达
        int pingInactive = assetPingService.count(new QueryWrapper<AssetPing>().eq("status", 0));
        assetInfo.put("pingActive", pingActive);
        assetInfo.put("pingInactive", pingInactive);

        resultMap.put("project", projectInfo);
        resultMap.put("module", moduleInfo);
        resultMap.put("asset", assetInfo);

        return resultMap;
    }

    // 如果没有该级别的 默认为0
    public void addLackSeverity(Map map) {
        if (map.get(Constant.ALERT_SEVERITY_HIGH) == null) {
            map.put(Constant.ALERT_SEVERITY_HIGH, 0);
        }
        if (map.get(Constant.ALERT_SEVERITY_MEDIUM) == null) {
            map.put(Constant.ALERT_SEVERITY_MEDIUM, 0);
        }
        if (map.get(Constant.ALERT_SEVERITY_LOW) == null) {
            map.put(Constant.ALERT_SEVERITY_LOW, 0);
        }
    }

    @Override
    public Map scrapeModuleStat() {
        Map resultMap = new HashMap();

        Map prometheusServer = new HashMap();
        Map endpoint = new HashMap();

        int active = promserverService.count(new QueryWrapper<Promserver>().eq("status", 1));
        int inactive = promserverService.count(new QueryWrapper<Promserver>().eq("status", 0));

        prometheusServer.put("active", active);
        prometheusServer.put("inactive", inactive);

        // endpoint 状态查询  返回 可用的endpoint id集合
        Set<Integer> endPointIds = this.getEndpointStatus();
        // 当前库中的数据
        List<Endpoint> endpoints = endpointService.list();
        List<Integer> dataBaseEndpointIds = new ArrayList<>();
        for (Endpoint entity : endpoints) {
            dataBaseEndpointIds.add(entity.getId());
        }

        int endpointActive = 0;
        for (Integer id : endPointIds) {
            if (dataBaseEndpointIds.contains(id)) {
                endpointActive++;
            }
        }
        endpoint.put("active", endpointActive);
        endpoint.put("inactive", dataBaseEndpointIds.size() - endpointActive);

        resultMap.put("prometheusServer", prometheusServer);
        resultMap.put("endpoint", endpoint);
        return resultMap;
    }

    // 请求prometheus 然后返回状态可用的endpoint ids
    public Set<Integer> getEndpointStatus() {
        Set<Integer> endPointIds = new HashSet<>();

        // 1. 查询 global  prometheus 信息 global 会汇总up时序数据
        List<Promserver> promservers = promserverService.list(new QueryWrapper<Promserver>().eq("type", 1));
        String url;
        Map result;
        for (Promserver promserver : promservers) {
            // 2. 查询 up
            url = "http://" + promserver.getHost() + ":" + promserver.getPort();// ip+port
            url += query + "?query=up";// url

            result = promserverProxyService.requestPromAPI(url);
            // 查询成功
            if (result != null && "success".equals(result.get("status"))) {
                Map data = (Map) result.get("data");
                JSONArray array = (JSONArray) data.get("result");

                JSONObject obj = null;
                JSONArray timeAndState = null;
                Map metricLabels;
                String job = null;
                String[] split = null;

                for (int i = 0; i < array.size(); i++) {
                    obj = (JSONObject) array.get(i);
                    timeAndState = (JSONArray) obj.get("value");

                    if ("1".equals(timeAndState.get(1).toString())) {
                        metricLabels = (Map) obj.get("metric");
                        job = metricLabels.get("job").toString();

                        if (job.contains("_")) {
                            // job 标签的格式为 ed_endpointId
                            split = job.split("_");
                            endPointIds.add(Integer.parseInt(split[1]));
                        }
                    }
                }
            } else {// 查询失败
                logger.error("query endpoint status failed ,prometheus server is {}", promserver);
            }
        }
        return endPointIds;
    }

    @Override
    public Map alertStat() {
        Map resultMap = new HashMap();

        Map high = new HashMap();
        Map medium = new HashMap();
        Map low = new HashMap();

        // 格式为 key: count-type value: 告警级别
        List<Map> alertMessageAllSeverityInfos = overviewDao.getAlertMessageAllSeverityInfos();

        // 当前没有任何计数信息
        if (CollectionUtils.isEmpty(alertMessageAllSeverityInfos)) {
            return null;
        }

        String severity, nums = null;
        Integer type = null;
        for (Map entity : alertMessageAllSeverityInfos) {
            severity = entity.get("severity").toString();
            nums = entity.get("nums").toString();
            type = Integer.parseInt(entity.get("type").toString());

            switch (severity) {
                case Constant.ALERT_SEVERITY_HIGH:
                    high.put(Constant.AlertType.getAlertType(type).getType(), nums);
                    break;
                case Constant.ALERT_SEVERITY_MEDIUM:
                    medium.put(Constant.AlertType.getAlertType(type).getType(), nums);
                    break;
                case Constant.ALERT_SEVERITY_LOW:
                    low.put(Constant.AlertType.getAlertType(type).getType(), nums);
                    break;
            }
        }

        this.addAlertType(high);
        this.addAlertType(medium);
        this.addAlertType(low);

        resultMap.put(Constant.ALERT_SEVERITY_HIGH, high);
        resultMap.put(Constant.ALERT_SEVERITY_MEDIUM, medium);
        resultMap.put(Constant.ALERT_SEVERITY_LOW, low);

        return resultMap;
    }

    // 如果没有该类型计数的 默认为0
    public void addAlertType(Map map) {
        if (map.get(Constant.AlertType.ALERT_TYPE_PROJECT.getType()) == null) {
            map.put(Constant.AlertType.ALERT_TYPE_PROJECT.getType(), 0);
        }
        if (map.get(Constant.AlertType.ALERT_TYPE_MODULE.getType()) == null) {
            map.put(Constant.AlertType.ALERT_TYPE_MODULE.getType(), 0);
        }
        if (map.get(Constant.AlertType.ALERT_TYPE_ASSET.getType()) == null) {
            map.put(Constant.AlertType.ALERT_TYPE_ASSET.getType(), 0);
        }
    }

    @Override
    public List<Map> alertNumTopN(Integer topN) {
        List<Map> list = overviewDao.alertNumTopN(topN);
        if (CollectionUtils.isNotEmpty(list)) {

            // 补充 alert type 关联的名称 (name) project module  (host) asset
            this.addAlertTypeNameFromRelation(list);
        }
        return list;
    }

    // 补充 alert type 关联的名称 (name) project module  (host) asset
    public void addAlertTypeNameFromRelation(List<Map> list) {
        List<Asset> assets = assetService.list();
        List<Project> projects = projectService.list();
        List<Module> modules = moduleService.list();

        // 准备数据 下方会替换返回结果中 name 的值
        Map assetIdAndHost = new HashMap();
        Map projectIdAndName = new HashMap();
        Map moduleIdAndName = new HashMap();

        for (Asset asset : assets) {
            assetIdAndHost.put(asset.getId(), asset.getHost());
        }
        for (Project project : projects) {
            projectIdAndName.put(project.getId(), project.getName());
        }
        for (Module module : modules) {
            moduleIdAndName.put(module.getId(), module.getName());
        }

        Integer type, linkId = null;
        for (Map map : list) {
            type = Integer.parseInt(map.get("type").toString());
            linkId = Integer.parseInt(map.get("linkId").toString());

            // 替换 name 的值
            if (Constant.AlertType.ALERT_TYPE_PROJECT.getCode().equals(type)) {
                map.put("name", projectIdAndName.get(linkId));
            }
            if (Constant.AlertType.ALERT_TYPE_MODULE.getCode().equals(type)) {
                map.put("name", moduleIdAndName.get(linkId));
            }
            if (Constant.AlertType.ALERT_TYPE_ASSET.getCode().equals(type)) {
                map.put("name", assetIdAndHost.get(linkId));
            }
        }
    }

    @Override
    public List<Map> alertRuleTopN(Integer topN) {
        List<Map> list = overviewDao.alertRuleTopN(topN);
        return list;
    }

    @Override
    public List<Map> alertUnprocessed(Integer pageNo, Integer pageSize) {
        if (pageNo == null) {
            pageNo = 1;
        }
        if (pageSize == null) {
            pageSize = Integer.parseInt(String.valueOf(Constant.PAGESIZE));
        }

        List<Map> list = overviewDao.alertUnprocessed((pageNo - 1) * pageSize, pageSize);

        if (CollectionUtils.isNotEmpty(list)) {
            List<AlertRuleEntity> alertRules = alertRuleService.list();
            Map ruleIdAndName = new HashMap();
            for (AlertRuleEntity rule : alertRules) {
                ruleIdAndName.put(rule.getId(), rule.getAlertName());
            }

            Integer type, ruleId = null;

            // 补充 alert type 关联的名称 (name) project module  (host) asset
            this.addAlertTypeNameFromRelation(list);

            for (Map entity : list) {
                type = Integer.parseInt(entity.get("type").toString());
                ruleId = Integer.parseInt(entity.get("ruleId").toString());

                // 替换 type 内容
                if (Constant.AlertType.ALERT_TYPE_PROJECT.getCode().equals(type)) {
                    entity.put("type", Constant.AlertType.ALERT_TYPE_PROJECT.getType());
                }
                if (Constant.AlertType.ALERT_TYPE_MODULE.getCode().equals(type)) {
                    entity.put("type", Constant.AlertType.ALERT_TYPE_MODULE.getType());
                }
                if (Constant.AlertType.ALERT_TYPE_ASSET.getCode().equals(type)) {
                    entity.put("type", Constant.AlertType.ALERT_TYPE_ASSET.getType());
                }

                // 替换alert rule name
                entity.put("alertName", ruleIdAndName.get(ruleId));
            }
        }
        return list;
    }

    @Override
    public List<Map> assetModelStat(Integer onlyInUse) {
        // 资产型号告警级别统计  返回为 modelId alertHigh(数量)  alertMedium （数量） alertLow （数量）
        List<Map> list = overviewDao.assetModelAndAlertSeveritysNums(onlyInUse);
        // 资产型号统计中 assetping 可达状态统计 返回为 modelId pingActive (可达数量)  pingInactive （不可达数量）
        List<Map> pingActives = overviewDao.assetModelAndPingActivesNums(onlyInUse);

        List<Model> models = modelService.list();
        Map<Integer, String> modelIdAndName = new HashMap();
        for (Model model : models) {
            modelIdAndName.put(model.getId(), model.getName());
        }

        for (int i = 0; i < list.size(); i++) {
            // 因为 型号与告警级别 和 型号与ping状态都按照module字段排序 所以顺序一致
            // 补充 型号名称
            list.get(i).put("model", modelIdAndName.get(Integer.parseInt(list.get(i).get("modelId").toString())));
            // 补充 ping 状态计数
            list.get(i).put("pingActive", pingActives.get(i).get("pingActive"));
            list.get(i).put("pingInactive", pingActives.get(i).get("pingInactive"));
        }

        // 不为空 型号多于查出集合  证明有没有关联的  补充默认值
        if (CollectionUtils.isNotEmpty(models) && list.size() < models.size()) {
            // 数据库中的型号ids
            List<Integer> modelIds = new ArrayList<>();
            // 通过sql查询出来的型号ids
            List<Integer> queryModelIds = new ArrayList<>();

            for (Map.Entry<Integer, String> entry : modelIdAndName.entrySet()) {
                modelIds.add(entry.getKey());
            }
            for (Map entity : list) {
                queryModelIds.add(Integer.parseInt(entity.get("modelId").toString()));
            }

            Map defaultValueMap;
            for (Integer id : modelIds) {
                if (!queryModelIds.contains(id)) {
                    defaultValueMap = new HashMap();
                    // 如果不包含则补充默认值
                    defaultValueMap.put("model", modelIdAndName.get(id));
                    defaultValueMap.put("pingActive", 0);
                    defaultValueMap.put("pingInactive", 0);
                    defaultValueMap.put("alertHigh", 0);
                    defaultValueMap.put("alertMedium", 0);
                    defaultValueMap.put("alertLow", 0);

                    // 添加到返回结果中
                    list.add(defaultValueMap);
                }
            }
        }
        return list;
    }

    @Override
    public List<Map> assetDCStat(Integer onlyInUse) {
        // 按照DC 维度统计asset信息 返回 idcId alertHigh(数量)  alertMedium （数量） alertLow （数量）
        List<Map> list = overviewDao.assetDcAndAlertSeveritysNums(onlyInUse);
        // 按照DC维度 assetping 可达状态统计 返回为 idcId pingActive (可达数量)  pingInactive （不可达数量）
        List<Map> pingActives = overviewDao.assetDcAndPingActivesNums(onlyInUse);

        List<Idc> idcs = idcService.list();
        Map<Integer, String> idcIdAndName = new HashMap<>();
        for (Idc idc : idcs) {
            idcIdAndName.put(idc.getId(), idc.getName());
        }

        for (int i = 0; i < list.size(); i++) {
            // 补充idc 名称
            list.get(i).put("dc", idcIdAndName.get(Integer.parseInt(list.get(i).get("idcId").toString())));
            // 补充 ping 状态计数
            list.get(i).put("pingActive", pingActives.get(i).get("pingActive"));
            list.get(i).put("pingInactive", pingActives.get(i).get("pingInactive"));
        }

        // 不为空 型号多于查出集合  证明有没有关联的  补充默认值
        if (CollectionUtils.isNotEmpty(idcs) && list.size() < idcs.size()) {
            // 数据库中的 idc ids
            List<Integer> idcIds = new ArrayList<>();
            // 通过sql查询出来的idc ids
            List<Integer> queryIdcIds = new ArrayList<>();

            for (Map.Entry<Integer, String> entry : idcIdAndName.entrySet()) {
                idcIds.add(entry.getKey());
            }
            for (Map entity : list) {
                queryIdcIds.add(Integer.parseInt(entity.get("idcId").toString()));
            }

            Map defaultValueMap;
            for (Integer id : idcIds) {
                if (!queryIdcIds.contains(id)) {
                    defaultValueMap = new HashMap();
                    // 如果不包含则补充默认值
                    defaultValueMap.put("dc", idcIdAndName.get(id));
                    defaultValueMap.put("pingActive", 0);
                    defaultValueMap.put("pingInactive", 0);
                    defaultValueMap.put("alertHigh", 0);
                    defaultValueMap.put("alertMedium", 0);
                    defaultValueMap.put("alertLow", 0);

                    // 添加到返回结果中
                    list.add(defaultValueMap);
                }
            }
        }
        return list;
    }

    @Override
    public Map alertRuleStat() {
        Map map = overviewDao.alertRuleStat();
        return map;
    }

    @Override
    public List<Map> promDCStat() {
        // 按照DC维度 统计prome server 状态 返回  idcId prometheusServerActive prometheusServerInactive (数量)
        List<Map> list = overviewDao.promDCAndActiveNums();

        List<Idc> idcs = idcService.list();
        Map<Integer, String> idcIdAndName = new HashMap<>();
        for (Idc idc : idcs) {
            idcIdAndName.put(idc.getId(), idc.getName());
        }


        Integer idcId;
        for (int i = 0; i < list.size(); i++) {
            idcId = Integer.parseInt(list.get(i).get("idcId").toString());

            // 补充idc 名称
            list.get(i).put("dc", idcIdAndName.get(idcId));
        }

        // 补充 没有关联 prome server 的dc 数据
        if (CollectionUtils.isNotEmpty(idcs) && list.size() < idcs.size()) {
            // 数据库中的 idc ids
            List<Integer> idcIds = new ArrayList<>();
            // 通过sql查询出来的idc ids
            List<Integer> queryIdcIds = new ArrayList<>();

            for (Map.Entry<Integer, String> entry : idcIdAndName.entrySet()) {
                idcIds.add(entry.getKey());
            }
            for (Map entity : list) {
                queryIdcIds.add(Integer.parseInt(entity.get("idcId").toString()));
            }

            Map defaultValueMap;
            for (Integer id : idcIds) {
                if (!queryIdcIds.contains(id)) {
                    defaultValueMap = new HashMap();
                    // 如果不包含则补充默认值
                    defaultValueMap.put("dc", idcIdAndName.get(id));
                    defaultValueMap.put("idcId", id);
                    defaultValueMap.put("prometheusServerActive", 0);
                    defaultValueMap.put("prometheusServerInactive", 0);

                    // 添加到返回结果中
                    list.add(defaultValueMap);
                }
            }
        }

        // 补充 endpoint up down数据
        // 获取 idc 下的 endpoint  格式为 idcId endpoints(1,2,...)
        List<Map> idcAndEndpointIds = overviewDao.getIdcAndEndpointIds();

        // 记录 idcId 和 关联的endpoint idlist
        Map<Integer, List<Integer>> idcWithEndpointList = new HashMap();
        String endpointIds;
        String[] split;
        List<Integer> tempIds;
        for (Map map : idcAndEndpointIds) {
            tempIds = new ArrayList<>();
            endpointIds = map.get("endpointIds").toString();
            split = endpointIds.split(",");
            for (String id : split) {
                tempIds.add(Integer.parseInt(id));
            }

            idcWithEndpointList.put(Integer.parseInt(map.get("idcId").toString()), tempIds);
        }

        // 请求prometheus 查询可用endpoint id 集合
        Set<Integer> endpointStatusIds = this.getEndpointStatus();

        for (int i = 0; i < list.size(); i++) {
            int endpointUp = 0;
            idcId = Integer.parseInt(list.get(i).get("idcId").toString());

            // 数据库中 idc关联的endpoint id 集合
            List<Integer> idcRelationEndpointids = idcWithEndpointList.get(idcId);

            // 如果存在没有关联dc的endpoint数据 补充默认值
            if (CollectionUtils.isEmpty(idcRelationEndpointids)) {

                list.get(i).put("endpointUp", 0);
                list.get(i).put("endpointDown", 0);
                break;
            }

            for (Integer id : idcRelationEndpointids) {
                if (endpointStatusIds.contains(id)) {
                    endpointUp++;
                }
            }

            // 补充endpoint up down 状态计数
            list.get(i).put("endpointUp", endpointUp);
            list.get(i).put("endpointDown", idcRelationEndpointids.size() - endpointUp);
        }

        return list;
    }

    @Override
    public Map sysUserStat() {
        // 当前可用用户
        List<SysUserEntity> list = sysUserService.list(new QueryWrapper<SysUserEntity>().eq("status", 1));

        Map map = new HashMap();
        map.put("totalUser", list.size());
        map.put("loginUser", Constant.loginUsers.size());
        return map;
    }
}
