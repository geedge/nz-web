package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetTypeConf;

import java.util.List;
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

    Integer saveConf(AssetTypeConf conf);

    Integer updateConf(AssetTypeConf conf);

    void delete(Integer... ids);

    List<AssetTypeConf> queryTypeConfTree(Integer pid);
}
