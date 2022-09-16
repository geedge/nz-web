package com.nis.modules.vsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.vsys.entity.VsysAssetRel;

import java.util.List;

public interface VsysAssetRelService extends IService<VsysAssetRel> {

    /**
     * 查询 asset & vsys
     * @param vsysId
     * @param assetId
     * @return
     */
    List<VsysAssetRel> queryAssetVsysRel(Integer vsysId, Integer assetId);

    /**
     * 保存|更新 rel
     *
     * @param vsysId
     * @param assetId
     */
    void saveOrUpdateRelObj(Integer vsysId, Integer assetId);
}
