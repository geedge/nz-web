package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
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
import com.nis.modules.overview.dao.OverviewDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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

    @Autowired
    private CabinetDao cabinetDao;

    @Autowired
    private OverviewDao overviewDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<Cabinet> page = new Query<Cabinet>().getPage(params);
        // 机柜列表 并且含有asset统计信息
        List<Cabinet> cabinets = cabinetDao.queryPage(page, params);
        Map<Integer, Cabinet> cabinetMap = new HashMap<>();
        for (Cabinet cabinet : cabinets) {
            cabinetMap.put(cabinet.getId(), cabinet);
        }

        // 按照DC维度统计alert数量 在库的资产
        List<Map> mapList = overviewDao.assetCabAndAlertSeveritysNums(1);
        Integer cabinetId, highNum, mediumNum, lowNum;
        Cabinet cabinet;
        Map alertStat;
        for (Map map : mapList) {
            if (map.get("cabinetId") == null) {
                continue;
            }
            cabinetId = Integer.parseInt(map.get("cabinetId").toString());
            cabinet = cabinetMap.get(cabinetId);
            if (cabinet != null) {
                alertStat = new HashMap();
                highNum = Integer.parseInt(map.get("alertHigh").toString());
                mediumNum = Integer.parseInt(map.get("alertMedium").toString());
                lowNum = Integer.parseInt(map.get("alertLow").toString());
                alertStat.put("high", highNum);
                alertStat.put("medium", mediumNum);
                alertStat.put("low", lowNum);
                alertStat.put("total", highNum + mediumNum + lowNum);
                cabinet.setAlertStat(alertStat);
            }
        }

        alertStat = new HashMap();
        alertStat.put("high", 0);
        alertStat.put("medium", 0);
        alertStat.put("low", 0);
        alertStat.put("total", 0);
        for (Cabinet entity : cabinetMap.values()) {
            if (entity.getAlertStat() == null) {
                entity.setAlertStat(alertStat);
            }
        }
        page.setRecords(new ArrayList<>(cabinetMap.values()));
        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateCabinet(Cabinet cabinet) {
        // 校验行列参数是否正确
        this.validatePosition(cabinet);

        // 入库
        this.saveOrUpdate(cabinet);
    }

    // 校验所在机房行列值是否正确
    private void validatePosition(Cabinet cabinet) {
        Idc idc = idcService.getById(cabinet.getIdcId());

        Integer y = cabinet.getY();
        Integer x = cabinet.getX();


        if (y != null) {
            ValidateUtils.is(cabinet.getY()).max(idc.getY(), RCode.CABINET_Y_INCORRECT);
        }

        if (x != null) {
            ValidateUtils.is(cabinet.getX()).max(idc.getX(), RCode.CABINET_X_INCORRECT);
        }

        // 当行列都不会空时 校验位置是否被占用 如果只单独填写了行或列的话 暂时不做具体位置校验
        if (y != null && x != null) {
            Cabinet entity = this.getOne(new QueryWrapper<Cabinet>().eq("idc_id", cabinet.getIdcId()).eq("y", y).eq("x", x));
            if(entity != null && cabinet.getId() != entity.getId()){
                // 此位置已被占用
                throw new NZException(RCode.CABINET_POSITION_OCCUPIED);
            }
        }
    }

    @Override
    public void delete(Integer... ids) {
        // 1.校验是否关联资产  to do
        for (Integer id : ids) {
            if(id == null){
                continue;
            }

            int count = assetService.count(new QueryWrapper<Asset>().eq("cabinet_id", id));
            if (count > 0) {
                // 机柜中有资产
                Cabinet cab = this.getById(id);
                //throw new NZException(RCode.IDC_CABINET_ASSETS_EXISTS);
                throw new NZException("There are assets in the " + cab.getName(), RCode.IDC_CABINET_ASSETS_EXISTS.getCode());
            }
        }

        // 2.删除
        this.removeByIds(Arrays.asList(ids));
    }


    /**
     * 名称校验
     *
     * @param id
     * @param name
     * @param idcId
     * @return
     */
    @Override
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

    @Override
    public Map getUInfo(Integer id) {
        Cabinet cabinet = this.getById(id);
        if (cabinet == null) {
            throw new NZException(RCode.ASSET_CABINET_NOTFOUND);
        }

        // 返回结果
        Map resuleData = new HashMap();

        Integer size = cabinet.getuSize();
        if (size == null) {
            resuleData.put("total", 0);
            resuleData.put("available", 0);
            resuleData.put("occupy", new ArrayList<>());
            return resuleData;
        }

        resuleData.put("total", size);

        List<Asset> assets = assetService.list(new QueryWrapper<Asset>().eq("cabinet_id", cabinet.getId()));
        // 已经被占用位置
        List<Integer> occupyList = new ArrayList<>();
        for (Asset asset : assets) {
            if (asset.getCabinetStart() != null && asset.getCabinetEnd() != null) {
                for (int i = asset.getCabinetStart(); i < asset.getCabinetEnd(); i++) {
                    occupyList.add(i);
                }
            }
        }
        resuleData.put("occupy", occupyList);
        resuleData.put("available", size - occupyList.size());
        return resuleData;
    }


}