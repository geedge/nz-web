package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.dao.CabinetDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.IdcService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 机柜    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("cabinetService")
public class CabinetServiceImpl extends ServiceImpl<CabinetDao, Cabinet> implements CabinetService {

    // 数据中心  service
    @Autowired
    private IdcService idcService;

    // 资产   service
    @Autowired
    private AssetService assetService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String id = null, name = null, idcId = null, uSize = null, remark = null;

        if (params != null && params.size() > 0) {
            id = (String) params.get("id");
            name = (String) params.get("name");
            idcId = (String) params.get("idcId");
            uSize = (String) params.get("uSize");
            remark = (String) params.get("remark");
        }

        IPage<Cabinet> page = this.page(
                new Query<Cabinet>().getPage(params),
                new QueryWrapper<Cabinet>()
                        .eq(StringUtils.isNotEmpty(id), "id", id)
                        .eq(StringUtils.isNotEmpty(idcId), "idc_id", idcId)
                        .eq(StringUtils.isNotEmpty(uSize), "u_size", uSize)
                        .like(StringUtils.isNotEmpty(name), "name", name)
                        .like(StringUtils.isNotEmpty(remark), "remark", remark)
        );
        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateCabinet(Cabinet cabinet) {
        // 1. 机柜名称校验 同一个数据中心名称唯一
        if (this.cabinetNameCheck(cabinet.getId(), cabinet.getName(), cabinet.getIdcId())) {
            // 2. 入库
            this.saveOrUpdate(cabinet);
        } else {
            // 名称重复
            throw new NZException(RCode.IDC_CABINETNAME_DUPLICATE);
        }
    }

    @Override
    public void delete(Long[] ids) {
        // 1.校验是否关联资产  to do
        for (Long id : ids) {
            int count = assetService.count(new QueryWrapper<Asset>().eq("cabinet_id", id));
            if (count > 0) {
                // 机柜中有资产
                throw new NZException(RCode.IDC_CABINET_ASSETS_EXISTS);
            }
        }
        // 2.删除
        this.removeByIds(Arrays.asList(ids));
    }


    public boolean cabinetNameCheck(Integer id, String name, Integer idcId) {
        // 1. 校验机房是否存在
        Idc idc = idcService.getById(idcId);
        if (idc == null) {
            throw new NZException(RCode.IDC_NOT_EXISTS);
        }

        // 2. 校验机柜名称唯一
        List<Cabinet> list = this.list(new QueryWrapper<Cabinet>().eq("name", name).eq("idc_id", idcId));
        boolean flag = false;

        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
    }


}