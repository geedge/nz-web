package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.asset.dao.AssetDao;
import com.nis.modules.asset.dao.CabinetDao;
import com.nis.modules.asset.dao.IdcDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.overview.entity.AssetStat;
import com.nis.modules.overview.service.OverviewService;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserService;
import org.apache.commons.collections.CollectionUtils;
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

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private IdcDao idcDao;

    @Autowired
    private AlertMessageDao alertMessageDao;

    @Autowired
    private CabinetDao cabinetDao;

    @Autowired
    private PromserverDao promserverDao;

    @Autowired
    private OverviewService overviewService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<Idc> page = new Query<Idc>().getPage(params);
        // dc列表数据
        List<Idc> datas = idcDao.queryPage(page, params);

        List<Asset> assetList = assetDao.getAssetsStatColumn();
        List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
        Map<Integer, AssetStat> idcIdAndStatInfoMap = new HashMap<>();
        List<AssetStat> assetStatByDc = overviewService.getAssetStatByDc(assetList, Collections.emptyList(), alertList);
        for (AssetStat stat : assetStatByDc) {
            idcIdAndStatInfoMap.put(stat.getId(), stat);
        }

        AssetStat stat;
        Map assetStat;
        Map alertStat;
        for (Idc data : datas) {
            stat = idcIdAndStatInfoMap.get(data.getId());
            stat = stat == null ? new AssetStat() : stat;

            assetStat = new HashMap();
            assetStat.put("total", stat.getTotal());
            assetStat.put("inStock", stat.getInStock());
            assetStat.put("outStock", stat.getOutStock());
            assetStat.put("suspended", stat.getSuspended());

            alertStat = new HashMap();
            alertStat.put("total", stat.getAlertTotal());
            alertStat.put(Constant.ALERT_SEVERITY_HIGH, stat.getAlertP1());
            alertStat.put(Constant.ALERT_SEVERITY_MEDIUM, stat.getAlertP2());
            alertStat.put(Constant.ALERT_SEVERITY_LOW, stat.getAlertP3());

            data.setAssetStat(assetStat);
            data.setAlertStat(alertStat);
        }

        page.setRecords(datas);
        return new PageUtils(page);
    }

    /**
     * 校验 idc 相关的信息
     * 1. 机房负责人有效性校验
     * 2. 名称重复校验
     * 3. 修改时行列值校验
     *
     * @param idc
     */
    private void validateIdcInfo(Idc idc) {
        // 1. 校验机房负责人是否存在
        if (idc.getPrincipal() != null) {
            SysUserEntity principal = sysUserService.getById(idc.getPrincipal());
            if (principal == null) {
                throw new NZException(RCode.IDC_PRINCIPAL_NOT_EXISTS);
            }
        }

        // 2. 校验名称重复
        List<Idc> list = this.list(new QueryWrapper<Idc>().lambda().eq(Idc::getName, idc.getName()));

        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(idc.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.IDC_NAME_DUPLICATE);
        }

        // 3.修改行列时校验行列值是否正确
        if (idc.getId() != null) {
            Integer y = idc.getY();
            Integer x = idc.getX();

            // 查询数据库中 机柜已占用机房最大行列作为修改的最小值
            Map<String, Integer> cabinetMaxXY = cabinetDao.getCabinetMaxXY(idc.getId());
            if (ToolUtil.isNotEmpty(cabinetMaxXY)) {
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

        // 4. 校验 State 是否合法
        String state = idc.getState();
        if (StringUtils.isEmpty(state)) {
            throw new NZException(RCode.IDC_STATE_ISNULL);
        }

        if (!Constant.SWITCH_OPEN.toUpperCase().equals(state) && !Constant.SWITCH_CLOSE.toUpperCase().equals(state)) {
            throw new NZException(RCode.IDC_STATE_ERROR);
        }

        // 5. 经纬度校验
        Double longitude = idc.getLongitude();
        if (longitude != null) {
            if (longitude > 180 || longitude < -180) {
                throw new NZException(RCode.IDC_LONGITUDE_ERROR);
            }
        }
        Double latitude = idc.getLatitude();
        if (latitude != null) {
            if (latitude > 90 || latitude < -90) {
                throw new NZException(RCode.IDC_LATITUDE_ERROR);
            }
        }
    }


    @Override
    public void saveOrUpdateIdc(Idc idc) {
        // 1. 校验
        this.validateIdcInfo(idc);

        // 2. 新增时设置行列默认值
        if (idc.getId() == null) {
            if (idc.getY() == null) {
                idc.setY(1);
            }
            if (idc.getX() == null) {
                idc.setX(1);
            }
            if (idc.getState() == null) {
                idc.setState(Constant.SWITCH_OPEN.toUpperCase());
            }
        }

        // 3. 入库
        this.saveOrUpdate(idc);
    }

    @Override
    public void delete(Integer... ids) {
        // 1. 校验是否包含 cabinet
        List<Cabinet> cabinets = cabinetDao.selectList(null);
        Set<Integer> cabinetUsedIdcIds = cabinets.stream().map(Cabinet::getIdcId).collect(Collectors.toSet());

        List<Integer> idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(cabinetUsedIdcIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> idcNames = this.listByIds(idList).stream().map(Idc::getName).collect(Collectors.toList());
            throw new NZException("These idc contain cabinets and cannot be deleted, idcs info: " + idcNames.toString(), RCode.IDC_CABINETS_EXISTS.getCode());
        }

        // 2. 校验是否包含 asset
        List<Asset> assets = assetDao.selectList(null);
        Set<Integer> assetUsedIdcIds = assets.stream().map(Asset::getIdcId).collect(Collectors.toSet());

        idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(assetUsedIdcIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> idcNames = this.listByIds(idList).stream().map(Idc::getName).collect(Collectors.toList());
            throw new NZException("These idc contain assets and cannot be deleted, idcs info: " + idcNames.toString(), RCode.IDC_ASSETS_EXISTS.getCode());
        }

        // 3. 校验是否包含 promeserver
        List<Promserver> promservers = promserverDao.selectList(null);
        Set<Integer> promserverUsedIdcIds = promservers.stream().map(Promserver::getIdcId).collect(Collectors.toSet());

        idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(promserverUsedIdcIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> idcNames = this.listByIds(idList).stream().map(Idc::getName).collect(Collectors.toList());
            throw new NZException("These idc contain prometheus servers and cannot be deleted, idcs info: " + idcNames.toString(), RCode.IDC_CABINETS_EXISTS.getCode());
        }

        // 4. 删除
        this.removeByIds(Arrays.asList(ids));
    }
}