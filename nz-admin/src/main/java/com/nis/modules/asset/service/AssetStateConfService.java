package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetStateConf;

import java.util.Map;

/**
 * AssetStateConf service
 *
 * @author shizhendong
 * @date 2021-03-08
 */
public interface AssetStateConfService extends IService<AssetStateConf> {

    PageUtils queryPage(Map<String, Object> params);

    Integer saveConf(AssetStateConf conf);

    Integer updateConf(AssetStateConf conf);

    void delete(Integer... ids);

    AssetStateConf queryConfInfo(Integer id);
}

