package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.dao.IdcDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 数据中心    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("idcService")
public class IdcServiceImpl extends ServiceImpl<IdcDao, Idc> implements IdcService {

    // 用户  service
    @Autowired
    private SysUserService sysUserService;

    // 机柜  service
    @Autowired
    private CabinetService cabinetService;

    // 资产  service
    @Autowired
    private AssetService assetService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        //      id         负责人           姓名          地址             电话
        String id = null, principal = null, name = null, location = null, tel = null;

        if (params != null && params.size() > 0) {
            id = (String) params.get("id");
            principal = (String) params.get("principal");
            name = (String) params.get("name");
            location = (String) params.get("location");
            tel = (String) params.get("tel");
        }

        IPage<Idc> page = this.page(
                new Query<Idc>().getPage(params),
                new QueryWrapper<Idc>()
                        .eq(StringUtils.isNotEmpty(id), "id", id)
                        .eq(StringUtils.isNotEmpty(principal), "principal", principal)
                        .like(StringUtils.isNotEmpty(name), "name", name)
                        .like(StringUtils.isNotEmpty(location), "location", location)
                        .like(StringUtils.isNotEmpty(tel), "tel", tel)
        );

        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateIdc(Idc idc) {
        // 1. 名称校验
        if (this.checkName(idc.getId(), idc.getName())) {
            // 2. 校验机房负责人是否存在
            if (idc.getPrincipal() != null) {
                SysUserEntity principal = sysUserService.getById(idc.getPrincipal());
                if (principal == null) {
                    // 负责人不存在
                    throw new NZException(RCode.IDC_PRINCIPAL_NOT_EXISTS);
                }
            }

            // 3. 入库
            this.saveOrUpdate(idc);
        } else {
            // 名称重复
            throw new NZException(RCode.IDC_NAME_DUPLICATE);
        }
    }

    @Override
    public void delete(Long[] ids) {
        // 1. 校验是否存在关联的机柜信息 资产信息
        for (Long id : ids) {
            int countCabinet = cabinetService.count(new QueryWrapper<Cabinet>().eq("idc_id", id));

            if (countCabinet > 0) {
                // 数据中心下存在机柜
                Idc idc = this.getById(id);
                throw new NZException(idc.getName() + " related cabinets , not deletable", 401006);
            }

            int countAsset = assetService.count(new QueryWrapper<Asset>().eq("idc_id", id));
            if (countAsset > 0) {
                // 数据中心下存在资产
                Idc idc = this.getById(id);
                throw new NZException(idc.getName() + " related assets , not deletable", 401007);
            }
        }
        // 2. 进行逻辑删除
        this.removeByIds(Arrays.asList(ids));
    }

    /**
     * 名称唯一校验
     *
     * @param id
     * @param name
     * @return
     */
    public boolean checkName(Integer id, String name) {
        List<Idc> list = this.list(new QueryWrapper<Idc>().eq("name", name));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
    }
}