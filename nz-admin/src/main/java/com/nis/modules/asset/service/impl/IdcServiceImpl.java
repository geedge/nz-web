package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.dao.CabinetDao;
import com.nis.modules.asset.dao.IdcDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.overview.dao.OverviewDao;
import com.nis.modules.overview.entity.AssetStat;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private IdcDao idcDao;

    @Autowired
    private OverviewDao overviewDao;

    @Autowired
    private CabinetDao cabinetDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<Idc> page = new Query<Idc>().getPage(params);
        // dc列表数据
        List<Idc> datas = idcDao.queryPage(page, params);

        // 获取idc 中 asset alert 统计信息
        List<AssetStat> assetStatInfoByIdc = overviewDao.getAssetStatInfoByIdc();
        Map<String, AssetStat> idcNameAndStatMap = new HashMap<>();
        for (AssetStat stat : assetStatInfoByIdc) {
            idcNameAndStatMap.put(stat.getName(), stat);
        }

        AssetStat assetStat;
        Map alertStat;
        for (Idc idc : datas) {
            assetStat = idcNameAndStatMap.get(idc.getName());

            alertStat = new HashMap();
            alertStat.put("total", assetStat.getAlertTotal());
            alertStat.put("high", assetStat.getAlertHigh());
            alertStat.put("medium", assetStat.getAlertMedium());
            alertStat.put("low", assetStat.getAlertLow());

            idc.setAssetStat(assetStat);
            idc.setAlertStat(alertStat);
        }

        page.setRecords(datas);
        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateIdc(Idc idc) {
        // 1. 校验机房负责人是否存在
        if (idc.getPrincipal() != null) {
            SysUserEntity principal = sysUserService.getById(idc.getPrincipal());
            if (principal == null) {
                // 负责人不存在
                throw new NZException(RCode.IDC_PRINCIPAL_NOT_EXISTS);
            }
        }

        // 2. 入库
        this.saveOrUpdate(idc);
    }

    @Override
    public void delete(Integer... ids) {
        // 1. 校验是否存在关联的机柜信息 资产信息
        List<Cabinet> cabinets = cabinetService.list();
        Set<Integer> set = cabinets.stream().map(Cabinet::getIdcId).collect(Collectors.toSet());

        List<Asset> assets = assetService.list();
        Set<Integer> set1 = assets.stream().map(Asset::getIdcId).collect(Collectors.toSet());


        Map<Integer, String> dcIdAndNameMap = this.list().stream().collect(Collectors.toMap(Idc::getId, Idc::getName));

        for (Integer id : ids) {
            if (id == null) {
                continue;
            }

            if (set.contains(id)) {
                // 数据中心下存在机柜
                throw new NZException(dcIdAndNameMap.get(id) + " related cabinets , not deletable", RCode.IDC_CABINETS_EXISTS.getCode());
            }

            if (set1.contains(id)) {
                // 数据中心下存在资产
                throw new NZException(dcIdAndNameMap.get(id) + " related assets , not deletable", RCode.IDC_ASSETS_EXISTS.getCode());
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
        List<Idc> list = this.list(new QueryWrapper<Idc>().lambda().eq(Idc::getName, name));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
    }

    @Override
    public void validateIdcSize(Idc idc) {
        Integer y = idc.getY();
        Integer x = idc.getX();

        // 查询数据库中 机柜已占用机房最大行列
        Map<String, Integer> cabinetMaxXY = cabinetDao.getCabinetMaxXY(idc.getId());
        if (cabinetMaxXY != null && cabinetMaxXY.size() > 0) {
            // 行 不为 null ，校验行
            Integer maxY = cabinetMaxXY.get("y");
            if (y != null && maxY != null) {
                if (y < maxY) {
                    throw new NZException(RCode.IDC_Y_ALREADYINUSE);
                }
            }

            // 列 不为 null ，校验列
            Integer maxX = cabinetMaxXY.get("x");
            if (x != null && maxX != null) {
                if (x < maxX) {
                    throw new NZException(RCode.IDC_X_ALREADYINUSE);
                }
            }
        }
    }
}