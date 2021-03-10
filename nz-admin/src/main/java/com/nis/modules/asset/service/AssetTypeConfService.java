package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetTypeConf;

import java.util.Map;

/**
 * AssetTypeConf service
 *
 * @author shizhendong
 * @date 2021-03-08
 */
public interface AssetTypeConfService extends IService<AssetTypeConf> {

    AssetTypeConf queryConfInfo(Integer id);

    PageUtils queryPage(Map<String, Object> params);

    void saveConf(AssetTypeConf conf);

    void updateConf(AssetTypeConf conf);

    void delete(Integer... ids);
}
