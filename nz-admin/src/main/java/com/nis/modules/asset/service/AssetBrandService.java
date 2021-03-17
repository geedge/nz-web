package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetBrand;

import java.util.Map;

/**
 * 资产品牌 service
 *
 * @author shizhendong
 * @date 2021-03-16
 */
public interface AssetBrandService extends IService<AssetBrand> {

    AssetBrand queryBrandInfo(Integer id);

    PageUtils queryPage(Map<String, Object> params);

    void saveBrand(AssetBrand brand);

    void updateBrand(AssetBrand brand);

    void delete(Integer... ids);
}
