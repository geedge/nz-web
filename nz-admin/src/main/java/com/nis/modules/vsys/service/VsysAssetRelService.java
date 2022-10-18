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
     * @param taskId
     * @param vsysId
     * @param assetId
     * @param state
     * @param delFlag
     */
    void saveOrUpdateRelObj(Integer taskId, Integer vsysId, Integer assetId, Integer state, Integer delFlag);
}
