package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.dao.ModelDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.ModelService;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据中心    serviceImpl
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

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String id = null, name = null, vendor = null, type = null, remark = null;

        if (params != null && params.size() > 0) {
            id = (String) params.get("id");
            name = (String) params.get("name");
            vendor = (String) params.get("vendor");
            type = (String) params.get("type");
            remark = (String) params.get("remark");
        }

        IPage<Model> page = this.page(
                new Query<Model>().getPage(params),
                new QueryWrapper<Model>()
                        .eq(StringUtils.isNotEmpty(id), "id", id)
                        .eq(StringUtils.isNotEmpty(type), "type", type)
                        .like(StringUtils.isNotEmpty(name), "name", name)
                        .like(StringUtils.isNotEmpty(vendor), "vendor", vendor)
                        .like(StringUtils.isNotEmpty(remark), "remark", remark)
        );

        Map<String, Object> map = new HashMap<>(1);

        map.put("type", "assetType");
        List<SysDictEntity> assetTypes = sysDictService.queryList(map);

        Map<String, String> assetTypesMap = new HashMap<>();
        for (SysDictEntity dictEntity : assetTypes) {
            assetTypesMap.put(dictEntity.getCode(), dictEntity.getValue());
        }

        for (Model model : page.getRecords()) {
            if (model.getType() != null) {
                model.setTypeName(assetTypesMap.get(String.valueOf(model.getType())));
            }
        }
        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateModel(Model model) {
        // 1. 校验名称
        if (checkName(model.getId(), model.getName())) {

            // 2.校验资产型号类型有效性
            if (model.getType() != null) {
                this.checkAssetTypeExist(model.getType());
            }

            // 3.入库
            this.saveOrUpdate(model);
        } else {
            // 资产型号名称重复
            throw new NZException(RCode.ASSET_MODELNAME_DUPLICATE);
        }
    }

    @Override
    public void delete(Long[] ids) {
        // 1. 校验已存在资产是否关联型号
        for (Long id : ids) {
            int countModel = assetService.count(new QueryWrapper<Asset>().eq("model_id", id));

            if (countModel > 0) {
                Model model = this.getById(id);
                // 当前资产类型中关联着资产 不可删除
                throw new NZException(model.getName() + " related assets , not deletable", 502005);
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
    public boolean checkName(Long id, String name) {
        List<Model> list = this.list(new QueryWrapper<Model>().eq("name", name));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
    }

    /**
     * 资产类型有效性校验
     *
     * @param assetType
     */
    public void checkAssetTypeExist(Integer assetType) {
        Map<String, Object> map = new HashMap<>(1);
        map.put("type", "assetType");

        List<SysDictEntity> assetTypes = sysDictService.queryList(map);
        boolean flag = false;
        for (SysDictEntity entity : assetTypes) {
            if (Integer.valueOf(entity.getCode()).equals(assetType)) {
                flag = true;
            }
        }
        if (!flag) {
            // 资产类型未找到
            throw new NZException(RCode.ASSET_MODELTYPE_NOTFOUND);
        }
    }

}