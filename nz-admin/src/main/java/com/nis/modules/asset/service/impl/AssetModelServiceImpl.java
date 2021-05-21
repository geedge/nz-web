package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.dao.AssetModelDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetBrand;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetBrandService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.service.VisualChartService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产型号 service 实现类
 *
 * @author shizhendong
 * @date 2021-03-16
 */
@Service
public class AssetModelServiceImpl extends ServiceImpl<AssetModelDao, AssetModel> implements AssetModelService {

    @Autowired
    private AssetAssetService assetAssetService;

    @Autowired
    private AssetBrandService brandService;

    @Autowired
    private AssetModelDao assetModelDao;
    
    @Autowired
    private VisualChartService visualChartService;

    @Autowired
    private AssetAssetDao assetDao;

    @Override
    public AssetModel queryModelInfo(Integer id) {
        AssetModel model = this.getById(id);
        if (ToolUtil.isEmpty(model)) {
            throw new NZException(RCode.ASSET_MODEL_NOTFOUND);
        }
        AssetBrand brand = brandService.getById(model.getBrandId());
        String chartIds = model.getChartIds();
    	if(!StrUtil.isBlank(chartIds)) {
    		List<VisualChart> charts = visualChartService.list(new QueryWrapper<VisualChart>().lambda().in(VisualChart::getId, Arrays.asList(chartIds.split(","))));
        	model.setCharts(charts);
    	}
    	//获取资产数量
    	List<AssetAsset> list = assetAssetService.list(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getModelId, model.getId()));
    	model.setAssetNum(list.size());
        model.setBrand(brand);
        return model;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage page = new Query(AssetModel.class).getPage(params);
        String ids = (String) params.get("ids");
        if(StrUtil.isNotBlank(ids)) {
        	List<String> listIds = Arrays.asList(ids.split(","));
        	params.put("ids",listIds);
        }
        String brandIds = (String) params.get("brandIds");
        if(StrUtil.isNotBlank(brandIds)) {
        	List<String> brandListIds = Arrays.asList(brandIds.split(","));
        	params.put("brandIds",brandListIds);
        }
        List<AssetModel> modelList = assetModelDao.queryList(page, params);
        for(AssetModel model : modelList) {
        	String chartIds = model.getChartIds();
        	if(!StrUtil.isBlank(chartIds)) {
        		List<VisualChart> charts = visualChartService.list(new QueryWrapper<VisualChart>().lambda().in(VisualChart::getId, Arrays.asList(chartIds.split(","))));
            	model.setCharts(charts);
        	}
        }
        page.setRecords(modelList);
        return new PageUtils(page);
    }

    private void validateAssetModelInfo(AssetModel model) {
        // 1. brand 是否存在
        AssetBrand brand = brandService.getById(model.getBrandId());
        if (ToolUtil.isEmpty(brand)) {
            throw new NZException(RCode.ASSET_BRAND_NOT_FOUND);
        }

        // 2. 名称重复
        List<AssetModel> list = this.list(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getBrandId, model.getBrandId()).eq(AssetModel::getName, model.getName()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(model.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.ASSET_MODELNAME_DUPLICATE);
        }
        
        // 3. 校验chart是否为模板且存在
        String chartIds = model.getChartIds();
        if(StrUtil.isNotBlank(chartIds)) {
        	List<String> chartids = Arrays.asList(chartIds.split(","));
        	List<VisualChart> charts = visualChartService.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getPanelId, 0).in(VisualChart::getId, chartids));
        	if(!ToolUtil.equals(charts.size(), chartids.size())) {
        		throw new NZException(RCode.ASSET_MODEL_CHART_ERROR);
        	}
        }
    }

    @Override
    public void saveModel(AssetModel model) {
        // 1. 校验
        this.validateAssetModelInfo(model);

        // 保存
        this.save(model);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModel(AssetModel model) {
        // 1. 校验
        this.validateAssetModelInfo(model);

        AssetModel oldModel = this.getById(model.getId());

        // 保存
        this.updateById(model);

        // 如果修改了Model, 处理关联 charts 变化
        if (!oldModel.getBrandId().equals(model.getBrandId())) {
            assetDao.updateBrandIdByModelId(model.getId(), model.getBrandId());
        }
    }

    @Override
    public void delete(Integer... ids) {
        // 1. 已被 asset 引用的型号信息不允许删除
        List<AssetAsset> assetList = assetAssetService.list();
        List<Integer> assetUseModelIdList = assetList.stream().map(AssetAsset::getModelId).distinct().collect(Collectors.toList());

        assetUseModelIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(assetUseModelIdList)) {
            throw new NZException("These models have been used by assets and cannot be deleted. Details: " + assetUseModelIdList.toString(), RCode.ASSET_MODELRELATED_ASSET.getCode());
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }
}
