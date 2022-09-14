package com.nis.modules.vsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.vsys.dao.VsysAssetRelDao;
import com.nis.modules.vsys.entity.VsysAssetRel;
import com.nis.modules.vsys.service.VsysAssetRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VsysAssetRelServiceImpl extends ServiceImpl<VsysAssetRelDao, VsysAssetRel> implements VsysAssetRelService {

    @Autowired
    private AssetAssetService assetService;

    @Override
    public List<VsysAssetRel> queryAssetVsysRel(Integer vsysId, Integer assetId) {
        List<VsysAssetRel> relList = this.list(new LambdaUpdateWrapper<VsysAssetRel>()
                .eq(vsysId != null, VsysAssetRel::getVsysId, vsysId)
                .eq(assetId != null, VsysAssetRel::getAssetId, assetId)
        );

        if (Tool.CollUtil.isNotEmpty(relList)) {
            List<AssetAsset> list = assetService.list();
            Map<Integer, Map<Object, Object>> relAssetMapInfo = list.stream().collect(Collectors.toMap(AssetAsset::getId, asset -> {
                Map<Object, Object> m = Tool.MapUtil.builder()
                        .put("id", asset.getId())
                        .put("name", asset.getName())
                        .build();
                return m;
            }));

            for (VsysAssetRel rel : relList) {
                Map<Object, Object> relAsset = relAssetMapInfo.get(rel.getAssetId());
                rel.setAsset(relAsset);
            }
        }

        return relList;
    }
}
