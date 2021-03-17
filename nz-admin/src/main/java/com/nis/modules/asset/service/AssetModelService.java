package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetModel;

import java.util.Map;

/**
 * 资产型号 service
 *
 * @author shizhendong
 * @date 2021-03-16
 */
public interface AssetModelService extends IService<AssetModel> {

    AssetModel queryModelInfo(Integer id);

    PageUtils queryPage(Map<String, Object> params);

    void saveModel(AssetModel model);

    void updateModel(AssetModel model);

    void delete(Integer... ids);
}
