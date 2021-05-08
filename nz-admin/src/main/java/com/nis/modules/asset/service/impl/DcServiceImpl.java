package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.dao.CabinetDao;
import com.nis.modules.asset.dao.DcDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据中心    serviceImpl
 * @author shizhendong
 * @date 2021-4-6
 */
@Service
public class DcServiceImpl extends ServiceImpl<DcDao, Dc> implements DcService {

    @Autowired
    private AssetAssetDao assetDao;

    @Autowired
    private CabinetDao cabinetDao;

    @Autowired
    private PromserverDao promserverDao;

    @Autowired
    private AlertMessageDao alertMessageDao;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DcDao dcDao;


    @Override
    public Dc queryDcDetails(Integer id) {
        Dc dc = this.getById(id);
        if (ToolUtil.isEmpty(dc)) {
            throw new NZException(RCode.DC_NOT_EXISTS);
        }

        if(ToolUtil.isNotEmpty(dc.getPrincipal())){
            SysUserEntity principal = sysUserService.getById(dc.getPrincipal());
            dc.setAdminUser(principal);
        }

        Integer cabinetNum = cabinetDao.selectCount(new LambdaQueryWrapper<Cabinet>().eq(Cabinet::getDcId, dc.getId()));
        dc.setCabinetNum(cabinetNum);

        Integer alertNum = alertMessageDao.selectCount(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getDcId, dc.getId()).eq(AlertMessageEntity::getState, 1));
        dc.setAlertNum(alertNum);

        Integer assetNum = assetDao.selectCount(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getDcId, dc.getId()));
        dc.setAssetNum(assetNum);

        return dc;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String ids = (String) params.get("ids");
        if (StrUtil.isNotBlank(ids)) {
            List<String> listIds = Arrays.asList(ids.split(","));
            params.put("ids", listIds);
        }

        IPage page = new Query(Dc.class).getPage(params);
        List<Dc> dcList = dcDao.queryDcList(page, params);

        if(CollectionUtils.isNotEmpty(dcList)){
            // 处理返回结果
            this.handleResuleList(dcList);
        }
        page.setRecords(dcList);
        return new PageUtils(page);
    }

    private void handleResuleList(List<Dc> dcList) {
        List<SysUserEntity> userEntityList = sysUserService.list();
        Map<Long, SysUserEntity> userIdAndEntityMap = userEntityList.stream().collect(Collectors.toMap(SysUserEntity::getId, Function.identity()));

        for (Dc dc : dcList) {
            if (ToolUtil.isNotEmpty(dc.getPrincipal())) {
                dc.setAdminUser(userIdAndEntityMap.get(dc.getPrincipal().longValue()));
            }
        }
    }


    /**
     * 校验 dc 相关的信息
     * @param dc
     */
    private void validateDcInfo(Dc dc) {
        // 1. 校验机房负责人是否存在
        if (dc.getPrincipal() != null) {
            SysUserEntity principal = sysUserService.getById(dc.getPrincipal());
            if (principal == null) {
                throw new NZException(RCode.DC_PRINCIPAL_NOT_EXISTS);
            }
        }

        // 2. 校验名称重复
        List<Dc> list = this.list(new QueryWrapper<Dc>().lambda().eq(Dc::getName, dc.getName()));

        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(dc.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.DC_NAME_DUPLICATE);
        }

        // 3.修改行列时校验行列值是否正确
        if (dc.getId() != null) {
            Integer y = dc.getY();
            Integer x = dc.getX();

            // 查询数据库中 机柜已占用机房最大行列作为修改的最小值
            Map<String, Integer> cabinetMaxXY = cabinetDao.getCabinetMaxXY(dc.getId());
            if (ToolUtil.isNotEmpty(cabinetMaxXY)) {
                // 行 不为 null ，校验行
                Integer maxY = cabinetMaxXY.get("y");
                if (y != null && maxY != null) {
                    if (y < maxY) {
                        throw new NZException(RCode.DC_Y_ALREADYINUSE);
                    }
                }
                // 列 不为 null ，校验列
                Integer maxX = cabinetMaxXY.get("x");
                if (x != null && maxX != null) {
                    if (x < maxX) {
                        throw new NZException(RCode.DC_X_ALREADYINUSE);
                    }
                }
            }
        }

        // 4. 校验 State 是否合法
        String state = dc.getState();
        if (!StrUtil.equals(Constant.SWITCH_OPEN.toUpperCase(), state) && !StrUtil.equals(Constant.SWITCH_CLOSE.toUpperCase(), state)) {
            throw new NZException(RCode.DC_STATE_ERROR);
        }

        // 5. 经纬度校验
        Double longitude = dc.getLongitude();
        if (longitude != null) {
            if (longitude > 180 || longitude < -180) {
                throw new NZException(RCode.DC_LONGITUDE_ERROR);
            }
        }

        Double latitude = dc.getLatitude();
        if (latitude != null) {
            if (latitude > 90 || latitude < -90) {
                throw new NZException(RCode.DC_LATITUDE_ERROR);
            }
        }
    }

    @Override
    public Integer saveDc(Dc dc) {
        // 校验 dc 相关信息
        this.validateDcInfo(dc);

        // 设置属性默认值
        if (dc.getY() == null) {
            dc.setY(1);
        }

        if (dc.getX() == null) {
            dc.setX(1);
        }

        // 保存
        this.save(dc);
        return dc.getId();
    }

    @Override
    public Integer updateDc(Dc dc) {
        // 校验 dc 相关信息
        this.validateDcInfo(dc);

        // 保存
        this.updateById(dc);
        return dc.getId();
    }

    @Override
    public void delete(Integer... ids) {
        // 1. 校验是否包含 cabinet
        List<Cabinet> cabinets = cabinetDao.selectList(null);
        Set<Integer> cabinetUsedIdcIds = cabinets.stream().map(Cabinet::getDcId).collect(Collectors.toSet());

        List<Integer> idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(cabinetUsedIdcIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> dcNames = this.listByIds(idList).stream().map(Dc::getName).collect(Collectors.toList());
            throw new NZException("These dc contain cabinets and cannot be deleted, Details: " + dcNames.toString(), RCode.DC_CABINETS_EXISTS.getCode());
        }

        // 2. 校验是否包含 asset
        List<AssetAsset> assets = assetDao.selectList(null);
        Set<Integer> assetUsedIdcIds = assets.stream().map(AssetAsset::getDcId).collect(Collectors.toSet());

        idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(assetUsedIdcIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> dcNames = this.listByIds(idList).stream().map(Dc::getName).collect(Collectors.toList());
            throw new NZException("These dc contain assets and cannot be deleted, Details: " + dcNames.toString(), RCode.DC_ASSETS_EXISTS.getCode());
        }

        // 3. 校验是否包含 promeserver
        List<Promserver> promservers = promserverDao.selectList(null);
        Set<Integer> promserverUsedIdcIds = promservers.stream().map(Promserver::getDcId).collect(Collectors.toSet());

        idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(promserverUsedIdcIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> dcNames = this.listByIds(idList).stream().map(Dc::getName).collect(Collectors.toList());
            throw new NZException("These dc contain prometheus servers and cannot be deleted, Details: " + dcNames.toString(), RCode.DC_CABINETS_EXISTS.getCode());
        }

        // 4. 删除
        this.removeByIds(Arrays.asList(ids));

        // 5. 删除 alert
        alertMessageDao.deleteByDCIds(Arrays.asList(ids));
    }
}
