package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.dao.AssetTagDao;
import com.nis.modules.asset.entity.AssetTag;
import com.nis.modules.asset.service.AssetTagService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产标签    serviceImpl
 *
 * @author shizhendong
 * @date 2020-06-11
 */
@Service
public class AssetTagServiceImpl extends ServiceImpl<AssetTagDao, AssetTag> implements AssetTagService {

    @Override
    public void saveTagByAssetId(Integer assetId, List<AssetTag> tags) {
        List<AssetTag> list = this.list(new QueryWrapper<AssetTag>().lambda().eq(AssetTag::getAssetId, assetId));
        List<Integer> tagIds = list.stream().map(AssetTag::getId).collect(Collectors.toList());

        List<AssetTag> saveList = new ArrayList<>();
        List<AssetTag> updateList = new ArrayList<>();

        if (ToolUtil.isEmpty(tags)) {
            tags = new ArrayList<>();
        }

        for (AssetTag pojo : tags) {
            if (StringUtils.isEmpty(pojo.getTag())) {
                throw new NZException(RCode.ASSET_TAGKEY_ISNULL);
            }

            if (!tagIds.contains(pojo.getId())) {
                saveList.add(pojo);
            } else {
                updateList.add(pojo);
                tagIds.remove(pojo.getId());
            }

            pojo.setAssetId(assetId);
        }
        if (CollectionUtils.isNotEmpty(saveList)) {
            this.saveBatch(saveList);
        }
        if (CollectionUtils.isNotEmpty(updateList)) {
            this.updateBatchById(updateList);
        }
        if (CollectionUtils.isNotEmpty(tagIds)) {
            this.removeByIds(tagIds);
        }
    }
}