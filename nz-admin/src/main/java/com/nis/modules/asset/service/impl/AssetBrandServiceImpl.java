package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.dao.AssetBrandDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetBrand;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetBrandService;
import com.nis.modules.asset.service.AssetModelService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产品牌 service 实现类
 *
 * @author shizhendong
 * @date 2021-03-16
 */
@Service
public class AssetBrandServiceImpl extends ServiceImpl<AssetBrandDao, AssetBrand> implements AssetBrandService {

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private AssetModelService modelService;

    @Override
    public AssetBrand queryBrandInfo(Integer id) {
        AssetBrand brand = this.getById(id);
        if (ToolUtil.isEmpty(brand)) {
            throw new NZException(RCode.ASSET_BRAND_NOT_FOUND);
        }
        return brand;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<AssetBrand> queryWrapper = new LambdaQueryWrapper<AssetBrand>()
                .like(ToolUtil.isNotEmpty(params.get("name")), AssetBrand::getName, params.get("name"));
        IPage page = this.page(new Query(AssetBrand.class).getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    private void validateAssetBrandInfo(AssetBrand brand) {
        // 1. 名称重复
        List<AssetBrand> list = this.list(new LambdaQueryWrapper<AssetBrand>().eq(AssetBrand::getName, brand.getName()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(brand.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.ASSET_BRAND_NAME_DUPLICATE);
        }
    }

    @Override
    public void saveBrand(AssetBrand brand) {
        // 1. 校验
        this.validateAssetBrandInfo(brand);

        // 保存
        this.save(brand);
    }

    @Override
    public void updateBrand(AssetBrand brand) {
        // 1. 校验
        this.validateAssetBrandInfo(brand);

        // 保存
        this.updateById(brand);
    }

    @Override
    public void delete(Integer... ids) {
        // 1. 已被 asset_model 引用的品牌信息不允许删除
        List<AssetModel> modelList = modelService.list();
        List<Integer> modelUseBrandIdList = modelList.stream().map(AssetModel::getBrandId).distinct().collect(Collectors.toList());

        modelUseBrandIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(modelUseBrandIdList)) {
            throw new NZException("These brands have been used by models and cannot be deleted. Details: " + modelUseBrandIdList.toString(), RCode.ASSET_BRAND_BEUSED_CAN_NOT_REMOVE.getCode());
        }

        // 2. 已被 asset 引用的品牌信息不允许删除
        List<AssetAsset> assetList = assetService.list();
        List<Integer> assetUseBrandIdList = assetList.stream().map(AssetAsset::getBrandId).distinct().collect(Collectors.toList());
        assetUseBrandIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(assetUseBrandIdList)) {
            throw new NZException("These brands have been used by assets and cannot be deleted. Details: " + assetUseBrandIdList.toString(), RCode.ASSET_BRAND_BEUSED_CAN_NOT_REMOVE.getCode());
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }
}
