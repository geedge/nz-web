package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.AlertType;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.dao.AssetDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.asset.service.ModelService;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 资产    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("assetService")
public class AssetServiceImpl extends ServiceImpl<AssetDao, Asset> implements AssetService {

    // 数据中心  service
    @Autowired
    private IdcService idcService;

    // 机柜  service
    @Autowired
    private CabinetService cabinetService;

    // 资产型号  service
    @Autowired
    private ModelService modelService;

    // 字典   service
    @Autowired
    private SysDictService sysDictService;

    // 资产  dao
    @Autowired
    private AssetDao assetDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<Asset> page = new Query<Asset>().getPage(params);

        List<Asset> assetEntities = assetDao.queryList(page, params);
        page.setRecords(assetEntities);

        return new PageUtils(page);
    }

    @Override
    public List<Asset> queryListByHosts(Collection<String> hosts) {
        List<Asset> assetList = assetDao.queryListByHosts(hosts);
        return assetList;
    }

    @Override
    public void saveOrUpdateAsset(Asset asset) {
        // 1. sn 唯一校验
        if (!this.checkFieldExist(asset.getId(), "sn", asset.getSn())) {
            throw new NZException(RCode.ASSET_SN_DUPLICATE);
        }

        // 2. host地址唯一校验
        if (!this.checkFieldExist(asset.getId(), "host", asset.getHost())) {
            throw new NZException(RCode.ASSET_HOST_DUPLICATE);
        }

        // 3. 型号有效性校验
        if (asset.getModelId() != null) {
            Model model = modelService.getById(asset.getModelId());
            if (model == null) {
                // 所选资产型号不存在
                throw new NZException(RCode.ASSET_MODEL_NOTFOUND);
            }
        }

        // 4.数据中心有效性校验
        Idc idc = idcService.getById(asset.getIdcId());
        if (idc == null) {
            // 数据中心不存在
            throw new NZException(RCode.ASSET_IDC_NOTFOUND);
        }

        // 5.机柜有效性校验
        if (asset.getCabinetId() != null) {
            Cabinet cabinet = cabinetService.getById(asset.getCabinetId());
            if (cabinet == null) {
                // 机柜未找到
                throw new NZException(RCode.ASSET_CABINET_NOTFOUND);
            }
            if (!idc.getId().equals(cabinet.getIdcId())) {
                // 数据中心与机柜不符  当前数据中心没有含有所选机柜
                throw new NZException(RCode.IDC_NOTHAS_CABINET);
            }
        }

        // 6. 状态有效性校验
        if (asset.getState() != null) {
            Map<String, Object> map = new HashMap<>(1);

            map.put("type", "assetState");
            List<SysDictEntity> assetTypes = sysDictService.queryList(map);
            boolean flag = false;
            for (SysDictEntity dict : assetTypes) {
                if (String.valueOf(asset.getState()).equals(dict.getCode())) {
                    flag = true;
                }
            }
            if (!flag) {
                // 未找到资产状态
                throw new NZException(RCode.ASSET_STATE_NOTFOUND);
            }
        }

        Date now = new Date();
        Long userId = ShiroUtils.getUserId();
        if (asset.getId() == null) {
            asset.setCreateAt(now);
            asset.setCreateBy(userId);
        }
        if (asset.getState() == null) {
            asset.setState(1);
        }
        asset.setUpdateAt(now);
        asset.setUpdateBy(userId);

        this.saveOrUpdate(asset);
    }

    @Override
    public void delete(String ids) {
        // 1.转换类型
        String[] idArr = ids.split(",");
        List<Integer> delIds = new ArrayList<>();

        for (String id : idArr) {
            try {
                delIds.add(Integer.parseInt(id));
            } catch (NumberFormatException e) {
                // ID转换异常
                throw new NZException(RCode.ASSET_ID_FORMAT);
            }
        }

        // 2. 删除
        this.removeByIds(delIds);
    }

    public boolean checkFieldExist(Integer id, String column, String columnValue) {
        List<Asset> list = this.list(new QueryWrapper<Asset>().eq(column, columnValue));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
    }
}