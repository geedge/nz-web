package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.AssetBrandDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetBrand;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetBrandService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产品牌 service 实现类
 *
 * @author shizhendong
 * @date 2021-03-16
 */
@Service
public class AssetBrandServiceImpl extends ServiceImpl<AssetBrandDao, AssetBrand> implements AssetBrandService {

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private AssetModelService modelService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Override
    public AssetBrand queryBrandInfo(Integer id) {
        AssetBrand brand = this.getById(id);
        if (ToolUtil.isEmpty(brand)) {
            throw new NZException(RCode.ASSET_BRAND_NOT_FOUND);
        }
        return brand;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<AssetBrand> queryWrapper = new LambdaQueryWrapper<AssetBrand>()
                .like(ToolUtil.isNotEmpty(params.get("name")), AssetBrand::getName, Tool.StrUtil.likeEscape(Tool.MapUtil.getStr(params,"name")));
        IPage page = this.page(new Query(AssetBrand.class).getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    private void validateAssetBrandInfo(AssetBrand brand) {
        // 1. 名称重复
        List<AssetBrand> list = this.list(new LambdaQueryWrapper<AssetBrand>().eq(AssetBrand::getName, brand.getName()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(brand.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.ASSET_BRAND_NAME_DUPLICATE);
        }
    }

    @Override
    public void saveBrand(AssetBrand brand) {
        // 1. 校验
        this.validateAssetBrandInfo(brand);

        // 保存
        this.save(brand);
    }

    @Override
    public void updateBrand(AssetBrand brand) {
        // 1. 校验
        this.validateAssetBrandInfo(brand);

        // 保存
        this.updateById(brand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map> delete(Integer force, Integer... ids) {
        List<Map> delRelObjectMsgList = Tool.ListUtil.list(true);

        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        List<AssetBrand> delAssetBrands = this.list(new LambdaQueryWrapper<AssetBrand>().in(AssetBrand::getId, idList));
        if (Tool.CollUtil.isEmpty(delAssetBrands)) {
            return delRelObjectMsgList;
        }

        delAssetBrands.forEach(assetBrand -> {
            Map<Object, Object> relObjectInfoMap = this.getAssetBrandDelRelObjectInfoMap(assetBrand);
            if (Tool.CollUtil.isNotEmpty(relObjectInfoMap)) {
                delRelObjectMsgList.add(relObjectInfoMap);
            }
        });

        force = Tool.ObjectUtil.defaultIfNull(force, 0);
        switch (force) {
            case 0: {// 0：存在关联数据时，删除失败
                if (Tool.CollUtil.isNotEmpty(delRelObjectMsgList)) {
                    return delRelObjectMsgList;
                }
            }
            case 1:  // 1：存在关联数据时，同时删除关联数据
                break;
            default:
                throw new NZException(RCode.ASSET_FORCE_INVALIDE);
        }

        // 删除自身和关联内容
        // brand
        this.removeByIds(idList);

        // model
        List<AssetModel> modelList = modelService.list(new LambdaQueryWrapper<AssetModel>().in(AssetModel::getBrandId, idList));
        if (Tool.CollUtil.isNotEmpty(modelList)) {
            Integer[] removeModelIdList = modelList.stream().map(AssetModel::getId).toArray(n -> new Integer[n]);
            modelService.delete(1, removeModelIdList);
        }

        // asset & endpoint
        List<AssetAsset> assetList = assetService.list(new LambdaQueryWrapper<AssetAsset>().in(AssetAsset::getBrandId, idList));
        if (Tool.CollUtil.isNotEmpty(assetList)) {
            Integer[] removeAssetIdList = assetList.stream().map(AssetAsset::getId).toArray(n -> new Integer[n]);
            assetService.deleteAsset(1, removeAssetIdList);
        }
        return delRelObjectMsgList;
    }

    /**
     * 获取 brand 删除提示关联信息：
     * model
     * asset
     * endpoint
     *
     * @return
     */
    private Map<Object, Object> getAssetBrandDelRelObjectInfoMap(AssetBrand brand) {
        List<Map> relatesList = Tool.ListUtil.list(true);

        // model
        List<AssetModel> modelList = modelService.list(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getBrandId, brand.getId()));
        if (Tool.CollUtil.isNotEmpty(modelList)) {
            for (AssetModel assetModel : modelList) {
                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", assetModel.getId())
                        .put("name", assetModel.getName())
                        .put("type", "model")
                        .build();
                relatesList.add(map);
            }
        }

        // asset & endpoint
        List<AssetAsset> assetList = assetService.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getBrandId, brand.getId()));
        if (Tool.CollUtil.isNotEmpty(assetList)) {
            List<Integer> assetIdList = assetList.stream().map(AssetAsset::getId).collect(Collectors.toList());
            List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().in(MonitorEndpoint::getAssetId, assetIdList));

            for (AssetAsset asset : assetList) {
                List<MonitorEndpoint> endpointListByAssetId = endpointList.stream().filter(endpoint -> asset.getId().equals(endpoint.getAssetId())).collect(Collectors.toList());
                List<Map> relatesEndpointList = Tool.ListUtil.list(true);
                for (MonitorEndpoint endpoint : endpointListByAssetId) {
                    Map<Object, Object> map = Tool.MapUtil.builder()
                            .put("id", endpoint.getId())
                            .put("name", endpoint.getName())
                            .put("type", "endpoint")
                            .build();
                    relatesEndpointList.add(map);
                }

                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", asset.getId())
                        .put("name", asset.getName())
                        .put("type", "asset")
                        .build();
                if (Tool.CollUtil.isNotEmpty(relatesEndpointList))
                    map.put("relates", relatesEndpointList);
                relatesList.add(map);
            }
        }

        if (Tool.CollUtil.isNotEmpty(relatesList)) {
            Map<Object, Object> delRelObjectMap = Tool.MapUtil.builder()
                    .put("id", brand.getId())
                    .put("name", brand.getName())
                    .put("type", "brand")
                    .put("relates", relatesList)
                    .build();

            return delRelObjectMap;
        }
        return Tool.MapUtil.empty();
    }
}
