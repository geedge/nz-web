package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.ModelDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.ModelService;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 资产型号    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("modelService")
public class ModelServiceImpl extends ServiceImpl<ModelDao, Model> implements ModelService {

    // 资产  service
    @Autowired
    private AssetService assetService;

    // 字典  service
    @Autowired
    private SysDictService sysDictService;

    // 资产型号 dao
    @Autowired
    private ModelDao modelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        IPage<Model> page = new Query<Model>().getPage(params);

        List<Model> modelList = modelDao.queryList(page, params);

        String stat = null;
        if (ToolUtil.isNotEmpty(params)) {
            stat = (String) params.get("stat");
        }
        // 返回不含统计信息
        if (!"1".equals(stat)) {
            for (Model model : modelList) {
                model.setAssetStat(null);
            }
        }

        page.setRecords(modelList);

        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateModel(Model model) {
        // 1.校验资产型号 厂商 类型 有效性
        this.checkAssetVendorAndTypeExist(model);

        // 2. 校验是否存在该型号
        Model entity = this.getOne(new QueryWrapper<Model>().lambda().eq(Model::getName, model.getName()).eq(Model::getTypeCode, model.getTypeCode()).eq(Model::getVendorCode, model.getVendorCode()));
        if (entity != null && !entity.getId().equals(model.getId())) {
            throw new NZException(RCode.ASSET_MODEL_EXISTS);
        }

        // 3.入库
        this.saveOrUpdate(model);
    }

    @Override
    public void delete(Integer... ids) {
        // 1. 校验已存在资产是否关联型号
        List<Asset> assets = assetService.list();
        Set<Integer> set = assets.stream().map(Asset::getModelId).collect(Collectors.toSet());

        Map<Integer, String> modelIdAndNameMap = this.list().stream().collect(Collectors.toMap(Model::getId, Model::getName));

        for (Integer id : ids) {
            if (id == null) {
                continue;
            }

            if (set.contains(id)) {
                // 当前资产类型中关联着资产 不可删除
                throw new NZException(modelIdAndNameMap.get(id) + " related assets , not deletable", RCode.ASSET_MODELRELATED_ASSET.getCode());
            }
        }

        // 2. 删除
        this.removeByIds(Arrays.asList(ids));
    }

    /**
     * 名称唯一校验
     *
     * @param id
     * @param name
     * @return
     */
    @Override
    public boolean checkName(Integer id, String name) {
        List<Model> list = this.list(new QueryWrapper<Model>().lambda().eq(Model::getName, name));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
    }

    @Override
    public void checkUsize(Model model) {
        List<Asset> assets = assetService.list(new QueryWrapper<Asset>().lambda().eq(Asset::getModelId, model.getId()));

        // 只校验关联资产的型号
        if (CollectionUtils.isNotEmpty(assets)) {
            Model modelEntity = this.getById(model.getId());
            Integer usize = modelEntity.getUsize();

            int minUsize = 1;
            // 有关联的情况下 修改usize值不能小于当前设备的最小U
            if (usize != null) {
                minUsize = usize;
            }

            Integer start, end;
            boolean flag = false;
            for (Asset asset : assets) {
                start = asset.getCabinetStart();
                end = asset.getCabinetEnd();
                if (start != null && end != null) {
                    flag = true;
                    if (end - start < minUsize) {
                        minUsize = end - start;
                    }
                }
            }

            if (!flag) {
                minUsize = 1;
            }

            if (model.getUsize() < minUsize) {
                throw new NZException(RCode.ASSET_MODEL_USIZEERROR);
            }
        }
    }

    /**
     * 资产类型有效性校验
     */
    public void checkAssetVendorAndTypeExist(Model model) {
        List<SysDictEntity> dictEntities = sysDictService.list(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getDelFlag, 0));

        List<SysDictEntity> assetTypes = new ArrayList<>();
        List<SysDictEntity> vendors = new ArrayList<>();

        dictEntities.stream().forEach(dict -> {
            switch (dict.getType()) {
                case Constant.DICT_ASSET_MODEL_TYPE: {
                    assetTypes.add(dict);
                    break;
                }
                case Constant.DICT_ASSET_MODEL_VENDOR: {
                    vendors.add(dict);
                    break;
                }
                default:
            }
        });

        List<Integer> typeCodeList = assetTypes.stream().map(SysDictEntity::getCode).collect(Collectors.toList());
        if (!typeCodeList.contains(model.getTypeCode())) {
            // 资产类型未找到
            throw new NZException(RCode.ASSET_MODELTYPE_NOTFOUND);
        }

        List<Integer> vendorCodeList = vendors.stream().map(SysDictEntity::getCode).collect(Collectors.toList());
        if (!vendorCodeList.contains(model.getVendorCode())) {
            // 厂商未找到
            throw new NZException(RCode.ASSET_VENDOR_NOTFOUND);
        }
    }

}