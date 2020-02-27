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
import com.nis.modules.overview.dao.OverviewDao;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<Idc> page = new Query<Idc>().getPage(params);
        // dc列表数据
        List<Idc> datas = idcDao.queryPage(page, params);
        Map<Integer, Idc> idcMap = new HashMap<>();
        for (Idc idc : datas) {
            idcMap.put(idc.getId(), idc);
        }

        // 按照DC维度统计alert数量 在库的资产
        List<Map> mapList = overviewDao.assetDcAndAlertSeveritysNums(1);
        Integer idcId, highNum, mediumNum, lowNum;
        Idc idc;
        Map alertStat;
        for (Map map : mapList) {
            idcId = Integer.parseInt(map.get("idcId").toString());
            idc = idcMap.get(idcId);
            if (idc != null) {
                alertStat = new HashMap();
                highNum = Integer.parseInt(map.get("alertHigh").toString());
                mediumNum = Integer.parseInt(map.get("alertMedium").toString());
                lowNum = Integer.parseInt(map.get("alertLow").toString());
                alertStat.put("high", highNum);
                alertStat.put("medium", mediumNum);
                alertStat.put("low", lowNum);
                alertStat.put("total", highNum + mediumNum + lowNum);
                idc.setAlertStat(alertStat);
            }
        }

        alertStat = new HashMap();
        alertStat.put("high", 0);
        alertStat.put("medium", 0);
        alertStat.put("low", 0);
        alertStat.put("total", 0);
        for (Idc entity : idcMap.values()) {
            if(entity.getAlertStat() == null){
                entity.setAlertStat(alertStat);
            }
        }
        page.setRecords(new ArrayList<>(idcMap.values()));
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
        for (Integer id : ids) {
            if(id == null){
                continue;
            }

            int countCabinet = cabinetService.count(new QueryWrapper<Cabinet>().eq("idc_id", id));

            if (countCabinet > 0) {
                // 数据中心下存在机柜
                Idc idc = this.getById(id);
                throw new NZException(idc.getName() + " related cabinets , not deletable", RCode.IDC_CABINETS_EXISTS.getCode());
            }

            int countAsset = assetService.count(new QueryWrapper<Asset>().eq("idc_id", id));
            if (countAsset > 0) {
                // 数据中心下存在资产
                Idc idc = this.getById(id);
                throw new NZException(idc.getName() + " related assets , not deletable", RCode.IDC_ASSETS_EXISTS.getCode());
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