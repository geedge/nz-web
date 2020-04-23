package com.nis.modules.asset.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.IdcTrafficDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.entity.IdcTraffic;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.asset.service.IdcTrafficService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据中心    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("idcTrafficService")
public class IdcTrafficServiceImpl extends ServiceImpl<IdcTrafficDao, IdcTraffic> implements IdcTrafficService {

    @Autowired
    private AssetService assetService;

    @Autowired
    private IdcService idcService;

    @Autowired
    private IdcTrafficDao idcTrafficDao;

    @Override
    public PageUtils queryTrafficSetting(Map<String, Object> params) {
        IPage<IdcTraffic> page = new Query<IdcTraffic>().getPage(params);

        // 校验 direction 是否在可选值内
        String direction = (String) params.get("direction");
        if (StringUtils.isNotEmpty(direction) && !Constant.TRAFFIC_DIRECTIONS.contains(direction)) {
            throw new NZException(RCode.IDC_TRAFFIC_DIRECTIONS_INVALIDE);
        }
        List<IdcTraffic> traffics = idcTrafficDao.queryTrafficSetting(page, params);

        traffics.stream().forEach(idcTraffic -> {
            if (ToolUtil.isNotEmpty(idcTraffic.getTags())) {
                idcTraffic.setTags(JSONObject.parseObject(idcTraffic.getTags().toString(), Map.class));
            }
        });
        page.setRecords(traffics);
        return new PageUtils(page);
    }

    @Override
    public void validateTrafficInfo(Integer idcId, List<IdcTraffic> setting) {
        ValidateUtils.is(idcId).notNull(RCode.IDC_IDCID_ISNULL)
                .and(setting).notNull(RCode.IDC_TRAFFIC_SETTING_ISNULL);

        Idc idc = idcService.getById(idcId);
        if (ToolUtil.isEmpty(idc)) {
            throw new NZException(RCode.IDC_NOT_EXISTS);
        }

        List<Asset> assetList = assetService.list();
        Map<Integer, Integer> assetIdAndDcIdMap = assetList.stream().collect(Collectors.toMap(Asset::getId, Asset::getIdcId));

        setting.stream().forEach(idcTraffic -> {
            ValidateUtils.is(idcTraffic.getAssetId()).notNull(RCode.ASSET_ID_ISNULL)
                    .and(idcTraffic.getDirection()).notNull(RCode.IDC_TRAFFIC_DIRECTIONS_ISNULL)
                    .and(idcTraffic.getIfindex()).notNull(RCode.IDC_TRAFFIC_IFINDEX_ISNULL)
                    .and(idcTraffic.getIfdescr()).notNull(RCode.IDC_TRAFFIC_IFDESCR_ISNULL);

            Integer dcId = assetIdAndDcIdMap.get(idcTraffic.getAssetId());
            if (ToolUtil.isEmpty(dcId)) {
                throw new NZException(RCode.ASSET_NOT_EXIST);
            }

            if (!idcId.equals(dcId)) {
                throw new NZException(RCode.IDC_NOT_INCLUDED_ASSET);
            }

            // 流量方向可多选
            String[] split = idcTraffic.getDirection().split(",");
            Arrays.asList(split).stream().forEach(s -> {
                if (!Constant.TRAFFIC_DIRECTIONS.contains(s)) {
                    throw new NZException(RCode.IDC_TRAFFIC_DIRECTIONS_INVALIDE);
                }
            });
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTraffic(Integer idcId, List<IdcTraffic> setting) {
        // 先删再增
        this.remove(new QueryWrapper<IdcTraffic>().lambda().eq(IdcTraffic::getIdcId, idcId));

        setting.stream().forEach(idcTraffic -> {
            idcTraffic.setIdcId(idcId);
            if (ToolUtil.isNotEmpty(idcTraffic.getTags())) {
                idcTraffic.setTags(JSONObject.toJSONString(idcTraffic.getTags()));
            }
        });

        this.saveBatch(setting);
    }
}