package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        page.setRecords(modelList);

        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateModel(Model model) {
        // 1.校验资产型号 厂商 类型 有效性
        this.checkAssetVendorAndTypeExist(model);

        // 2.入库
        this.saveOrUpdate(model);
    }

    @Override
    public void delete(String ids) {
        // 1. 校验已存在资产是否关联型号
        String[] idArr = ids.split(",");
        List<Integer> delIds = new ArrayList<>();

        for (String id : idArr) {
            try {
                delIds.add(Integer.parseInt(id));

                int countModel = assetService.count(new QueryWrapper<Asset>().eq("model_id", id));

                if (countModel > 0) {
                    Model model = this.getById(id);
                    // 当前资产类型中关联着资产 不可删除
                    throw new NZException(model.getName() + " related assets , not deletable", 502005);
                }
            } catch (NumberFormatException e) {
                // ID转换异常
                throw new NZException(RCode.ASSET_ID_FORMAT);
            }
        }

        // 2. 删除
        this.removeByIds(delIds);
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
     */
    public void checkAssetVendorAndTypeExist(Model model) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("type", Constant.DICT_ASSET_MODEL_TYPE); // 字典表中资产型号类型type值 assetType

        List<SysDictEntity> assetTypes = sysDictService.queryList(map);
        boolean flag = false;
        for (SysDictEntity entity : assetTypes) {
            if (model.getTypeCode().equals(entity.getCode())) {
                flag = true;
            }
        }
        if (!flag) {
            // 资产类型未找到
            throw new NZException(RCode.ASSET_MODELTYPE_NOTFOUND);
        }

        flag = false;

        map.put("type", Constant.DICT_ASSET_MODEL_VENDOR); // 字典表中资产型号厂商type值 vendor
        List<SysDictEntity> vendors = sysDictService.queryList(map);
        for (SysDictEntity entity : vendors) {
            if (model.getVendorCode().equals(entity.getCode())) {
                flag = true;
            }
        }
        if (!flag) {
            // 厂商未找到
            throw new NZException(RCode.ASSET_VENDOR_NOTFOUND);
        }
    }

}