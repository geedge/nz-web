package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.dao.AssetModelDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.AssetBrand;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.service.AssetBrandService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.asset.service.AssetService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private AssetService assetService;

    @Autowired
    private AssetBrandService brandService;

    @Autowired
    private AssetModelDao assetModelDao;

    @Override
    public AssetModel queryModelInfo(Integer id) {
        AssetModel model = this.getById(id);
        if (ToolUtil.isEmpty(model)) {
            throw new NZException(RCode.ASSET_MODEL_NOTFOUND);
        }
        AssetBrand brand = brandService.getById(model.getBrandId());
        model.setBrand(brand);
        return model;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AssetModel> page = new Query<AssetModel>().getPage(params);
        List<AssetModel> modelList = assetModelDao.queryList(page, params);
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
    }

    @Override
    public void saveModel(AssetModel model) {
        // 1. 校验
        this.validateAssetModelInfo(model);

        // 保存
        this.save(model);
    }


    @Override
    public void updateModel(AssetModel model) {
        // 1. 校验
        this.validateAssetModelInfo(model);

        // 保存
        this.updateById(model);
    }

    @Override
    public void delete(Integer... ids) {
        // 1. 已被 asset 引用的型号信息不允许删除
        List<Asset> assetList = assetService.list();
        List<Integer> assetUseModelIdList = assetList.stream().map(Asset::getModelId).distinct().collect(Collectors.toList());

        assetUseModelIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(assetUseModelIdList)) {
            throw new NZException("These models have been used by assets and cannot be deleted. Details: " + assetUseModelIdList.toString(), RCode.ASSET_MODELRELATED_ASSET.getCode());
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }
}
