package com.nis.modules.overview.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.ModelService;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.overview.dao.OverviewDao;
import com.nis.modules.overview.entity.AssetStat;
import com.nis.modules.overview.entity.DcStat;
import com.nis.modules.overview.entity.EndpointStat;
import com.nis.modules.overview.entity.ModuleStat;
import com.nis.modules.overview.entity.ProjectStat;
import com.nis.modules.overview.service.OverviewService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.service.impl.PromserverProxyServiceImpl;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;

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
    private AlertRuleService alertRuleService;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private PromserverProxyServiceImpl promserverProxyService;

    @Autowired
    private SysDictService sysDictService;

    @Autowired
    private CabinetService cabinetService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Value("${nezha.promApis.query}")
    private String query;

    @Override
    public List<Map> alertRuleTopN(Integer topN) {
        List<Map> list = overviewDao.alertRuleTopN(topN);
        return list;
    }

    @Override
    public Map getAssetStat() {
        // 统计总数
        AssetStat assetTotalInfo = overviewDao.getAssetTotalInfo();

        // 根据型号维度统计信息
        List<AssetStat> assetStatInfoByModel = overviewDao.getAssetStatInfoByModel();
        Map<Integer, AssetStat> modelIdAndStat = new HashMap<>();
        for (AssetStat baseStat : assetStatInfoByModel) {
            modelIdAndStat.put(baseStat.getId(), baseStat);
        }


        // 根据dc维度统计信息
        List<AssetStat> assetStatInfoByIdc = overviewDao.getAssetStatInfoByIdc();
        // 去掉 total 为 0 的数据
        for (int i = 0; i < assetStatInfoByIdc.size(); i++) {
            if(assetStatInfoByIdc.get(i).getTotal() < 1){
                assetStatInfoByIdc.remove(i);
                i--;
            }
        }

        // 处理type为维度统计asset信息
        List<AssetStat> assetStatInfoByType = new ArrayList<>();

        Map<String, Object> map = new HashMap<>(2);
        map.put("type", Constant.DICT_ASSET_MODEL_TYPE); // 字典表中资产类型 type 值 assetType
        List<SysDictEntity> assetTypes = sysDictService.queryList(map);

        List<Model> models = modelService.list();

        AssetStat baseStat = null;
        for (SysDictEntity entity : assetTypes) {
            baseStat = new AssetStat();
            baseStat.setName(entity.getValue());

            for (Model model : models) {
                if (entity.getCode().equals(model.getTypeCode())) {
                    AssetStat baseStat1 = modelIdAndStat.get(model.getId());
                    if (baseStat1 != null) {
                        baseStat.setTotal((baseStat.getTotal()) + baseStat1.getTotal());
                        baseStat.setInStock(baseStat.getInStock() + baseStat1.getInStock());
                        baseStat.setOutStock(baseStat.getOutStock() + baseStat1.getOutStock());
                        baseStat.setPingUp(baseStat.getPingUp() + baseStat1.getPingUp());
                        baseStat.setPingDown(baseStat.getPingDown() + baseStat1.getPingDown());
                        baseStat.setAlertTotal(baseStat.getAlertTotal() + baseStat1.getAlertTotal());
                        baseStat.setAlertHigh(baseStat.getAlertHigh() + baseStat1.getAlertHigh());
                        baseStat.setAlertMedium(baseStat.getAlertMedium() + baseStat1.getAlertMedium());
                        baseStat.setAlertLow(baseStat.getAlertLow() + baseStat1.getAlertLow());
                    }
                }
            }

            if(baseStat.getTotal() > 0){
                assetStatInfoByType.add(baseStat);
            }
        }

        // 返回结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("totalStat", assetTotalInfo);
        resultData.put("typeStat", assetStatInfoByType);
        resultData.put("modelStat", assetStatInfoByModel);
        resultData.put("dcStat", assetStatInfoByIdc);
        return resultData;
    }

    @Override
    public Map getProjectStat() {
        Map resultData = new HashMap();

        List<ProjectStat> projectStatInfo = overviewDao.getProjectStatInfo();
        resultData.put("total", projectStatInfo.size());
        resultData.put("projectStat", projectStatInfo);

        return resultData;
    }

    @Override
    public Map getEndpointStat() {
        // 返回信息包含 name promUp promDown promTotal endPointTotal
        List<EndpointStat> endpointInfoByDc = overviewDao.getEndpointInfoByDc();

        // 补充 endpointTotal endpointUp endpointDown 信息
        Map<String, List<Integer>> idcNameWithEndpointIdsMap = new HashMap<>();

        List<Map<String, String>> idcNameWithEndpointIds = overviewDao.getIdcNameWithEndpointIds();
        String idcName, endpointIds;
        List<Integer> endpoindIdList;
        for (Map<String, String> entity : idcNameWithEndpointIds) {
            endpoindIdList = new ArrayList<>();
            idcName = entity.get("name");
            endpointIds = entity.get("endpointIds");
            if (StringUtils.isNotEmpty(endpointIds)) {
                for (String str : endpointIds.split(",")) {
                    endpoindIdList.add(Integer.parseInt(str));
                }
            }
            // 添加至map中
            idcNameWithEndpointIdsMap.put(idcName, endpoindIdList);
        }

        // 服务健康的endpoint id列表
        Set<Integer> endpointUpIds = this.getEndpointStatus();

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

    @Override
    public Map getDatacenterStat() {
        List<DcStat> dcStatList = new ArrayList<>();
        // 根据dc维度统计信息
        List<AssetStat> assetStatInfoByIdc = overviewDao.getAssetStatInfoByIdc();

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

        int cabinetTotal = cabinetService.count();

        Map resultData = new HashMap();
        resultData.put("dcTotal", dcStatList.size());
        resultData.put("cabinetTotal", cabinetTotal);
        resultData.put("dcStat", dcStatList);

        return resultData;
    }

    @Override
    public Map getAlertRuleStat() {
        int alertRuleTotal = alertRuleService.count();
        List<Map> alertRuleStat = overviewDao.getAlertRuleStat();

        Map resultData = new HashMap();
        resultData.put("alertRuleTotal", alertRuleTotal);
        resultData.put("alertRule", alertRuleStat);

        return resultData;
    }

    @Override
    public List<Map> alertStatByAssetTopN(Integer top) {
        List<Map> alertStatByAssetTopN = overviewDao.getAlertStatByAssetTopN(top);
        return alertStatByAssetTopN;
    }

    @Override
    public Map getModuleStat() {
        Map resultData = new HashMap();

        List<ModuleStat> moduleStatInfo = overviewDao.getModuleStatInfo();

        // module id 和 包含的endpoint计数
        List<ModuleStat> endPointNumByModule = overviewDao.getEndPointNumByModule();
        Map<Integer, Integer> moduleIdWithEndpointNumMap = endPointNumByModule.stream().collect(Collectors.toMap(ModuleStat::getId, ModuleStat::getEndpointNum));

        // 补充 endpointNum
        Integer endPointNum;
        for (ModuleStat moduleStat : moduleStatInfo) {
            endPointNum = moduleIdWithEndpointNumMap.get(moduleStat.getId());
            if (ToolUtil.isNotEmpty(endPointNum)) {
                moduleStat.setEndpointNum(endPointNum);
            }
        }

        int moduleCount = moduleService.count();

        resultData.put("moduleStat", moduleStatInfo);
        resultData.put("total", moduleCount);

        return resultData;
    }

    @Override
    public Map alertMessageStat() {
        Map resultData = new HashMap();

        List<Map> byLevel = overviewDao.getAlertMessageStatGroupByLevel();
        int alertMessageTotal = alertMessageService.count(new QueryWrapper<AlertMessageEntity>().eq("state", 1));

        resultData.put("alertMessageTotal", alertMessageTotal);
        resultData.put("alertMessage", byLevel);

        return resultData;
    }

    @Override
    public List<Map> alertStatByModuleTopN(Integer top) {
        List<Map> alertStatByModuleTopN = overviewDao.getAlertStatByModuleTopN(top);
        return alertStatByModuleTopN;
    }

    // 请求prometheus 然后返回状态可用的endpoint ids
    public Set<Integer> getEndpointStatus() {
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
