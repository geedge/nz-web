package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
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

    /**
     * AssetBrand
     * @param force 0：存在关联数据时，删除失败 1：存在关联数据时，同时删除关联数据 默认：0
     * @param ids
     * @return
     */
    R delete(Integer force, Integer... ids);
}
