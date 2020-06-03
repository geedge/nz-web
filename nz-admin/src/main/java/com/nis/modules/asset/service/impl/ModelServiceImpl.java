package com.nis.modules.asset.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.ModelDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.ModelService;
import com.nis.modules.panel.dao.PanelDao;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 资产型号    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("modelService")
@Slf4j
public class ModelServiceImpl extends ServiceImpl<ModelDao, Model> implements ModelService {

    // 资产  service
    @Autowired
    private AssetService assetService;

    // 字典  service
    @Autowired
    private SysDictService sysDictService;

    // 资产型号 dao
    @Autowired
    private ModelDao modelDao;

    @Autowired
    private PanelDao panelDao;

    @Autowired
    private ChartService chartService;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        IPage<Model> page = new Query<Model>().getPage(params);

        List<Model> modelList = modelDao.queryList(page, params);

        String stat = null;
        if (ToolUtil.isNotEmpty(params)) {
            stat = (String) params.get("stat");
        }
        // 返回不含统计信息
        if (!"1".equals(stat)) {
            for (Model model : modelList) {
                model.setAssetStat(null);
            }
        }

        page.setRecords(modelList);

        return new PageUtils(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateModel(Model model) {
        // 1.校验资产型号 厂商 类型 有效性
        this.checkAssetVendorAndTypeExist(model);

        // 2. 校验是否存在该型号
        Model entity = this.getOne(new QueryWrapper<Model>().lambda().eq(Model::getName, model.getName()).eq(Model::getTypeCode, model.getTypeCode()).eq(Model::getVendorCode, model.getVendorCode()));
        if (entity != null && !entity.getId().equals(model.getId())) {
            throw new NZException(RCode.ASSET_MODEL_EXISTS);
        }

        // 3.入库
        if (ToolUtil.isEmpty(model.getId())) {
            this.save(model);

            // 新增时需要新增 panel chart
            this.saveModelPanelAndChart(model);
        } else {
            this.updateById(model);
        }
    }

    @Override
    public void delete(Integer... ids) {
        // 1. 校验已存在资产是否关联型号
        List<Asset> assets = assetService.list();
        Set<Integer> set = assets.stream().map(Asset::getModelId).collect(Collectors.toSet());

        Map<Integer, String> modelIdAndNameMap = this.list().stream().collect(Collectors.toMap(Model::getId, Model::getName));

        for (Integer id : ids) {
            if (id == null) {
                continue;
            }

            if (set.contains(id)) {
                // 当前资产类型中关联着资产 不可删除
                throw new NZException(modelIdAndNameMap.get(id) + " related assets , not deletable", RCode.ASSET_MODELRELATED_ASSET.getCode());
            }
        }

        // 2. 删除
        this.removeByIds(Arrays.asList(ids));

        // 3. 删除关联 panel charts chartElement
        List<Panel> panels = panelDao.selectList(new QueryWrapper<Panel>().lambda().eq(Panel::getType, "model").in(Panel::getLink, ids));
        Set<Integer> panelIds = panels.stream().map(Panel::getId).collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(panelIds)) {
            panelDao.deleteBatchIds(panelIds);
            Integer[] removePanelIds = panelIds.stream().toArray(n -> new Integer[n]);
            chartService.delChartsByPanelId(removePanelIds);
        }
    }

    /**
     * 名称唯一校验
     *
     * @param id
     * @param name
     * @return
     */
    @Override
    public boolean checkName(Integer id, String name) {
        List<Model> list = this.list(new QueryWrapper<Model>().lambda().eq(Model::getName, name));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
    }

    @Override
    public void checkUsize(Model model) {
        Model oldModel = this.getById(model.getId());
        // 如果之前 model usize为空 则不进行后续验证
        if(ToolUtil.isEmpty(oldModel.getUsize())){
            return;
        }

        List<Asset> assets = assetService.list(new QueryWrapper<Asset>().lambda().eq(Asset::getModelId, model.getId()));

        // 去除没有位置信息的asset
        assets = assets.stream().filter(asset -> asset.getCabinetStart() != null && asset.getCabinetEnd() != null ? true : false).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(assets)) {
            // 有关联的情况下 修改usize值不能大于当前设备的最小U

            Asset tempAsset = assets.get(0);
            int minUsize = tempAsset.getCabinetEnd() - tempAsset.getCabinetStart();


            Integer start, end;
            for (Asset asset : assets) {
                start = asset.getCabinetStart();
                end = asset.getCabinetEnd();
                if (end - start < minUsize) {
                    minUsize = end - start;
                }
            }

            if(model.getUsize() > minUsize){
                throw new NZException(RCode.ASSET_MODEL_USIZEERROR);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncChart(Integer modelId, Integer assetId) {
        if (modelId != null && assetId != null) {
            Asset asset = assetService.getById(assetId);
            if (ToolUtil.isEmpty(asset)) {
                throw new NZException(RCode.ASSET_NOT_EXIST);
            }
            if (!modelId.equals(asset.getModelId())) {
                throw new NZException(RCode.ASSET_MODELID_INCONSISTENT);
            }

            this.updateAssetChartsThroughRelatedModel(assetId);
        } else if (modelId == null && assetId != null) {
            Asset asset = assetService.getById(assetId);
            if (ToolUtil.isEmpty(asset)) {
                throw new NZException(RCode.ASSET_NOT_EXIST);
            }

            this.updateAssetChartsThroughRelatedModel(assetId);
        } else if (modelId != null && assetId == null) {

            this.updateuAllAssetChartsUnderCurrentModel(modelId);
        } else {
            // modelId 与 assetId 不能同时为空
            throw new NZException(RCode.ASSET_MODEL_PARAM_ISNULL);
        }
    }

    // 全量更新此 asset 的 chart，与对应型号的chart 保持一致
    private void updateAssetChartsThroughRelatedModel(Integer assetId) {
        Asset asset = assetService.getById(assetId);
        chartService.updateAssetChartsThroughRelatedModel(asset);
    }

    // 更新所有型号为 modelId 的asset chart
    private void updateuAllAssetChartsUnderCurrentModel(Integer modelId) {
        List<Asset> assetList = assetService.list(new QueryWrapper<Asset>().lambda().eq(Asset::getModelId, modelId));
        if (CollectionUtils.isEmpty(assetList)) {
            return;
        }

        chartService.updateuAllAssetChartsUnderCurrentModel(modelId, assetList);
    }

    /**
     * 资产类型有效性校验
     */
    public void checkAssetVendorAndTypeExist(Model model) {
        List<SysDictEntity> dictEntities = sysDictService.list(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getDelFlag, 0));

        List<SysDictEntity> assetTypes = new ArrayList<>();
        List<SysDictEntity> vendors = new ArrayList<>();

        dictEntities.stream().forEach(dict -> {
            switch (dict.getType()) {
                case Constant.DICT_ASSET_MODEL_TYPE: {
                    assetTypes.add(dict);
                    break;
                }
                case Constant.DICT_ASSET_MODEL_VENDOR: {
                    vendors.add(dict);
                    break;
                }
                default:
            }
        });

        List<Integer> typeCodeList = assetTypes.stream().map(SysDictEntity::getCode).collect(Collectors.toList());
        if (!typeCodeList.contains(model.getTypeCode())) {
            // 资产类型未找到
            throw new NZException(RCode.ASSET_MODELTYPE_NOTFOUND);
        }

        List<Integer> vendorCodeList = vendors.stream().map(SysDictEntity::getCode).collect(Collectors.toList());
        if (!vendorCodeList.contains(model.getVendorCode())) {
            // 厂商未找到
            throw new NZException(RCode.ASSET_VENDOR_NOTFOUND);
        }
    }


    public void saveModelPanelAndChart(Model model) {
        // 配置信息获取
        SysConfigEntity modelConfig = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MODEL_CHART_TPL));
        if (ToolUtil.isEmpty(modelConfig)) {
            log.error(String.format("获取model配置信息失败%s", Constant.SYSCONFIG_KEY_MODEL_CHART_TPL));
            throw new NZException(RCode.SYS_CONFIG_MODELCHARTTPL_NOTFOUND);
        }
        // chart config
        Chart chartConfig = JSONObject.parseObject(modelConfig.getParamValue(), Chart.class);


        Panel panel = new Panel();
        panel.setType("model");
        panel.setLink(model.getId());
        panel.setCreateBy(ShiroUtils.getUserId().intValue());

        panel.setBuildIn(chartConfig.getBuildIn() == null ? 0 : chartConfig.getBuildIn());
        // 固定格式 panel name format is mode_panel_link
        panel.setName("model_panel_" + model.getId());
        panelDao.insert(panel);


        Chart target = new Chart();
        BeanUtils.copyProperties(chartConfig, target);
        // 暂时以model name作为chart title
        target.setTitle(ToolUtil.isEmpty(target.getTitle()) ? model.getName() + " Chart" : target.getTitle());
        target.setPanelId(panel.getId());
        target.setCreateAt(new Date());
        target.setParam(ToolUtil.isNotEmpty(target.getParam()) ? JSONObject.toJSONString(target.getParam()) : null);
        chartService.saveChart(target);
    }
}