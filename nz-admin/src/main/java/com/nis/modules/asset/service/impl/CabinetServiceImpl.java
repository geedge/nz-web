package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.StrUtil;
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
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
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
    private CabinetDao cabinetDao;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private DcService dcService;

    @Autowired
    private AssetAssetService assetAssetService;

    @Autowired
    private AlertMessageDao alertMessageDao;
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
        String ids = (String) params.get("ids");
        if (StrUtil.isNotBlank(ids)) {
            params.put("ids", Arrays.asList(ids.split(",")));
        }

        String dcIds = (String) params.get("dcIds");
        if (StrUtil.isNotBlank(dcIds)) {
            params.put("dcIds", Arrays.asList(dcIds.split(",")));
        }

        IPage<Cabinet> page = new Query<Cabinet>().getPage(params);
        List<Cabinet> cabinetList = cabinetDao.queryPage(page, params);

        page.setRecords(cabinetList);
        return new PageUtils(page);
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
