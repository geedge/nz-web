package com.nis.modules.asset.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.asset.dao.CabinetDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.overview.entity.AssetStat;
import com.nis.modules.overview.service.OverviewService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 机柜    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("cabinetService")
public class CabinetServiceImpl extends ServiceImpl<CabinetDao, Cabinet> implements CabinetService {

    private static Log log = Log.get();

    @Autowired
    private AssetService assetService;

    @Autowired
    private CabinetDao cabinetDao;

    @Autowired
    private OverviewService overviewService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private DcService dcService;

    @Autowired
    private AssetAssetService assetAssetService;

    @Autowired
    private AlertMessageDao alertMessageDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<Cabinet> page = new Query<Cabinet>().getPage(params);
        // 机柜列表 并且含有asset统计信息
        List<Cabinet> cabinets = cabinetDao.queryPage(page, params);

        Map<Integer, AssetStat> cabinetIdAndStatMap = new HashMap<>();
        List<AssetStat> statByCabinet = overviewService.getAssetStatByCabinet();
        for (AssetStat stat : statByCabinet) {
            cabinetIdAndStatMap.put(stat.getId(), stat);
        }

        AssetStat stat;
        Map assetStat;
        Map alertStat;
        for (Cabinet cabinet : cabinets) {
            stat = cabinetIdAndStatMap.get(cabinet.getId());
            stat = stat == null ? new AssetStat() : stat;
            assetStat = new HashMap();
            alertStat = new HashMap();

            assetStat.put("total", stat.getTotal());
            assetStat.put("inStock", stat.getInStock());
            assetStat.put("outStock", stat.getOutStock());
            assetStat.put("suspended", stat.getSuspended());

            alertStat.put("total", stat.getAlertTotal());
            alertStat.put(Constant.ALERT_SEVERITY_HIGH, stat.getAlertP1());
            alertStat.put(Constant.ALERT_SEVERITY_MEDIUM, stat.getAlertP2());
            alertStat.put(Constant.ALERT_SEVERITY_LOW, stat.getAlertP3());

            cabinet.setAssetStat(assetStat);
            cabinet.setAlertStat(alertStat);
        }

        page.setRecords(new ArrayList<>(cabinets));
        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateCabinet(Cabinet cabinet) {
        // 1. 校验 cab 相关信息
        this.validateCabinetInfo(cabinet);

        // 2. 添加时如果 Usize 为空 则从配置表中取出 cabinet u位 默认值
        if (cabinet.getId() == null && cabinet.getuSize() == null) {
            SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_DEFAULT_CABINETU).eq(SysConfigEntity::getStatus, 1));
            if (configEntity != null) {
                Integer defaultSize = null;
                try {
                    defaultSize = Integer.parseInt(configEntity.getParamValue());
                } catch (NumberFormatException e) {
                    log.error("系统配置表中机柜默认值有误 param_key 为 {},error:{}", Constant.SYSCONFIG_KEY_DEFAULT_CABINETU, e);
                }
                if (defaultSize == null) {
                    throw new NZException(RCode.SYS_CONFIG_DEFALUTCABINETU_INTERVAL);
                }
                cabinet.setuSize(defaultSize);
            } else {
                log.error(String.format("系统配置表中没有机柜默认U位值，key为:%s", Constant.SYSCONFIG_KEY_DEFAULT_CABINETU));
            }
        }

        // 3. 入库
        this.saveOrUpdate(cabinet);
    }

    /**
     * 校验 cabinet 相关信息
     * 1. 校验机房是否存在
     * 2. 校验机柜名称唯一
     * 3. 校验 usize 最小最大值
     * 4. 校验行列值
     * @param cabinet
     */
    private void validateCabinetInfo(Cabinet cabinet) {
        // 1. 校验机房是否存在
        Dc dc = dcService.getById(cabinet.getDcId());
        if (dc == null) {
            throw new NZException(RCode.DC_NOT_EXISTS);
        }

        // 2. 校验机柜名称唯一
        List<Cabinet> list = this.list(new QueryWrapper<Cabinet>().lambda().eq(Cabinet::getName, cabinet.getName()).eq(Cabinet::getDcId, dc.getId()));

        boolean flag = false;

        if ((list == null || list.size() == 0) ||  (list != null && list.size() == 1 && list.get(0).getId().equals(cabinet.getId()))) {
            flag = true;
        }

        if(!flag){
            throw new NZException(RCode.DC_CABINETNAME_DUPLICATE);
        }

        // 3.校验 usize 最小最大值
        if(cabinet.getuSize() != null){
            ValidateUtils.is(cabinet.getuSize()).min(1, RCode.DC_CABINETUSIZE_MINIMUM).max(Constant.CURRENT_CABINET_MAXU, RCode.DC_CABINET_MAXU);
        }

        // 4. 校验行列值
        this.validateXY(cabinet);
    }

    /**
     * 校验所在机房行列值是否正确
     * @param cabinet
     */
    private void validateXY(Cabinet cabinet) {
        Dc dc = dcService.getById(cabinet.getDcId());

        Integer y = cabinet.getY();
        Integer x = cabinet.getX();


        if (y != null) {
            Integer dcY = dc.getY();
            if (dcY != null) {
                ValidateUtils.is(cabinet.getY()).max(dcY, RCode.CABINET_Y_INCORRECT);
            }
        }

        if (x != null) {
            Integer dcX = dc.getX();
            if (dcX != null) {
                ValidateUtils.is(cabinet.getX()).max(dcX, RCode.CABINET_X_INCORRECT);
            }
        }

        // 当行列都不为空时 校验位置是否被占用 如果只单独填写了行或列的话 暂时不做具体位置校验
        if (y != null && x != null) {
            Cabinet entity = this.getOne(new QueryWrapper<Cabinet>().lambda().eq(Cabinet::getDcId, cabinet.getDcId()).eq(Cabinet::getY, y).eq(Cabinet::getX, x));
            if (entity != null) {
                Integer id = cabinet.getId();
                if (id == null) {
                    throw new NZException(RCode.CABINET_POSITION_OCCUPIED);
                } else {
                    if (!id.equals(entity.getId())) {
                        throw new NZException(RCode.CABINET_POSITION_OCCUPIED);
                    }
                }
            }
        }
    }

    @Override
    public void delete(Integer... ids) {
        List<Asset> assets = assetService.list();

        // asset 使用的 cabinet
        Set<Integer> assetUsedCabinetIds = assets.stream().filter(asset -> {
            if (ToolUtil.isNotEmpty(asset.getCabinetId())) {
                return true;
            }
            return false;
        }).map(Asset::getCabinetId).collect(Collectors.toSet());

        List<Integer> idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(assetUsedCabinetIds);

        // 1. 如果删除的机柜包含资产 不可删除
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> cabNames = this.listByIds(idList).stream().map(Cabinet::getName).collect(Collectors.toList());
            throw new NZException("These cabinets contain assets and cannot be deleted, cabinets info: " + cabNames.toString(), RCode.DC_CABINET_ASSETS_EXISTS.getCode());
        }

        // 2.删除
        this.removeByIds(Arrays.asList(ids));
    }

    @Override
    public Map getUInfo(Integer id) {
        // 1. 获取当前机柜
        Cabinet cabinet = this.getById(id);
        if (cabinet == null) {
            throw new NZException(RCode.ASSET_CABINET_NOTFOUND);
        }

        // 最终返回结果
        Map resuleData = new HashMap();

        Integer size = cabinet.getuSize();
        // 2. 如果机柜没有大小 返回信息
        if (size == null) {
            resuleData.put("total", 0);
            resuleData.put("available", 0);
            resuleData.put("occupy", new ArrayList<>());
            return resuleData;
        }

        resuleData.put("total", size);

        // 3. 查询当前机柜下资产
        List<Asset> assets = assetService.list(new QueryWrapper<Asset>().lambda().eq(Asset::getCabinetId, cabinet.getId()));
        assets = assets.stream().filter(asset -> asset.getCabinetStart() != null && asset.getCabinetEnd() != null).collect(Collectors.toList());

        // 已经被占用位置
        List<Integer> occupyList = new ArrayList<>();
        for (Asset asset : assets) {
            for (int i = asset.getCabinetStart(); i < asset.getCabinetEnd(); i++) {
                occupyList.add(i);
            }
        }

        // 4. 对占用位置结果集排序
        Collections.sort(occupyList);
        resuleData.put("occupy", occupyList);
        resuleData.put("available", size - occupyList.size());
        return resuleData;
    }

    @Override
    public Cabinet queryCabinetDetails(Integer id) {
        Cabinet cabinet = this.getById(id);
        if (ToolUtil.isEmpty(cabinet)) {
            throw new NZException(RCode.CABINET_NOT_EXISTS);
        }

        Dc dc = dcService.getById(cabinet.getDcId());
        cabinet.setDc(dc);

        List<AssetAsset> assetList = assetAssetService.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getCabinetId, cabinet.getId()));
        cabinet.setAssetNum(assetList.size());
        if(CollectionUtils.isNotEmpty(assetList)){
            // 包含 asset 则统计 alert
            List<Integer> cabinetAssetIdList = assetList.stream().map(AssetAsset::getId).collect(Collectors.toList());
            List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
            long alertNum = alertList.stream().filter(alert -> cabinetAssetIdList.contains(alert.getAssetId())).count();
            cabinet.setAlertNum((int) alertNum);
        }else{
            cabinet.setAlertNum(0);
        }
        return cabinet;
    }

    @Override
    public PageUtils queryCabinetPage(Map<String, Object> params) {
        IPage<Cabinet> page = this.page(new Query<Cabinet>().getPage(params), new LambdaQueryWrapper<Cabinet>()
                .in(ToolUtil.isNotEmpty(params.get("ids")), Cabinet::getId, ToolUtil.isNotEmpty(params.get("ids")) ? Arrays.asList(params.get("ids").toString().split(",")) : null)
                .like(ToolUtil.isNotEmpty(params.get("name")), Cabinet::getName, params.get("name"))
                .in(ToolUtil.isNotEmpty(params.get("dcIds")), Cabinet::getDcId, ToolUtil.isNotEmpty(params.get("dcIds")) ? Arrays.asList(params.get("dcIds").toString().split(",")) : null));

        List<Cabinet> cabinetList = page.getRecords();
        if(CollectionUtils.isNotEmpty(cabinetList)){
            // 处理返回结果
            this.handleResuleList(cabinetList);
        }
        page.setRecords(cabinetList);
        return new PageUtils(page);
    }

    private void handleResuleList(List<Cabinet> cabinetList) {
        List<Dc> dcList = dcService.list();
        Map<Integer, Dc> dcIdAndEntityMap = dcList.stream().collect(Collectors.toMap(Dc::getId, Function.identity()));

        List<AssetAsset> assetList = assetAssetService.list();
        assetList = assetList.stream().filter(asset -> asset.getCabinetId() != null).collect(Collectors.toList());

        List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
        alertList = alertList.stream().filter(alertMessageEntity -> alertMessageEntity.getAssetId() != null).collect(Collectors.toList());

        for (Cabinet cabinet : cabinetList) {
            cabinet.setDc(dcIdAndEntityMap.get(cabinet.getDcId()));

            List<Integer> cabinetAssetIdList = assetList.stream().filter(asset -> cabinet.getId().equals(asset.getCabinetId())).map(AssetAsset::getId).collect(Collectors.toList());
            cabinet.setAssetNum(cabinetAssetIdList.size());

            long alertNum = alertList.stream().filter(alert -> cabinetAssetIdList.contains(alert.getAssetId())).count();
            cabinet.setAlertNum((int) alertNum);
        }
    }

    @Override
    public Integer saveCabinet(Cabinet cabinet) {
        // 校验
        this.validateCabinetInfo(cabinet);

        // Usize 为空 则从配置表中取出 cabinet u位默认值
        if (cabinet.getuSize() == null) {
            SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_DEFAULT_CABINETU).eq(SysConfigEntity::getStatus, 1));
            cabinet.setuSize(Integer.valueOf(configEntity.getParamKey()));
        }

        // 保存
        this.save(cabinet);
        return cabinet.getId();
    }

    @Override
    public Integer updateCabinet(Cabinet cabinet) {
        // 校验
        this.validateCabinetInfo(cabinet);

        // 保存
        this.updateById(cabinet);
        return cabinet.getId();
    }

    @Override
    public void deleteByIds(Integer... ids) {
        List<AssetAsset> assets = assetAssetService.list();

        // asset 使用的 cabinet
        Set<Integer> assetUsedCabinetIds = assets.stream().filter(asset -> {
            if (ToolUtil.isNotEmpty(asset.getCabinetId())) {
                return true;
            }
            return false;
        }).map(AssetAsset::getCabinetId).collect(Collectors.toSet());

        List<Integer> idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(assetUsedCabinetIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> cabNames = this.listByIds(idList).stream().map(Cabinet::getName).collect(Collectors.toList());
            throw new NZException("These cabinets contain assets and cannot be deleted, Details: " + cabNames.toString(), RCode.DC_CABINET_ASSETS_EXISTS.getCode());
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }

    @Override
    public Map getCabinetUsizeDetails(Integer id) {
        // 获取当前机柜
        Cabinet cabinet = this.getById(id);
        if (cabinet == null) {
            throw new NZException(RCode.ASSET_CABINET_NOTFOUND);
        }

        Map resuleData = new HashMap();
        Integer size = cabinet.getuSize();
        // 如果机柜没有大小 返回信息
        if (size == null) {
            resuleData.put("total", 0);
            resuleData.put("available", 0);
            resuleData.put("occupy", new ArrayList<>());
            return resuleData;
        }

        resuleData.put("total", size);

        //  查询当前机柜下资产
        List<AssetAsset> assets = assetAssetService.list(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getCabinetId, cabinet.getId()));
        assets = assets.stream().filter(asset -> asset.getCabinetStart() != null && asset.getCabinetEnd() != null).collect(Collectors.toList());

        // 已经被占用位置
        Set<Integer> occupyList = new TreeSet<>();
        for (AssetAsset asset : assets) {
            for (int i = asset.getCabinetStart(); i < asset.getCabinetEnd(); i++) {
                occupyList.add(i);
            }
        }

        // 对占用位置结果集排序
        List<Integer> tempList = new ArrayList<>(occupyList);
        Collections.sort(tempList);
        resuleData.put("occupy", tempList);
        resuleData.put("available", size - tempList.size());
        return resuleData;
    }
}
