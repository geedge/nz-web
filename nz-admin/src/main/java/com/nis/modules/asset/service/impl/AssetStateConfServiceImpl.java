package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.dao.AssetStateConfDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.AssetStateConf;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.AssetStateConfService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AssetStateConf service
 *
 * @author shizhendong
 * @date 2021-03-08
 */
@Service
public class AssetStateConfServiceImpl extends ServiceImpl<AssetStateConfDao, AssetStateConf> implements AssetStateConfService {

    @Autowired
    private AssetService assetService;

    @Override
    public AssetStateConf queryConfInfo(Integer id) {
        AssetStateConf conf = this.getById(id);
        if (ToolUtil.isNotEmpty(conf)) {
            int assetCount = assetService.count(new LambdaQueryWrapper<Asset>().eq(Asset::getStateId, conf.getId()));
            conf.setAssetStat(assetCount);
        } else {
            throw new NZException(RCode.STATECONF_NOT_FOUND);
        }
        return conf;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<AssetStateConf> queryWrapper = new LambdaQueryWrapper<AssetStateConf>().
                eq(ToolUtil.isNotEmpty(params.get("id")), AssetStateConf::getId, params.get("id"))
                .like(ToolUtil.isNotEmpty(params.get("name")), AssetStateConf::getName, params.get("name"));
        IPage<AssetStateConf> page = this.page(new Query<AssetStateConf>().getPage(params), queryWrapper);

        List<AssetStateConf> data = page.getRecords();
        if (CollectionUtils.isNotEmpty(data)) {
            List<Asset> list = assetService.list();
            for (AssetStateConf conf : data) {
                long count = list.stream().filter(asset -> conf.getId().equals(asset.getStateId())).count();
                conf.setAssetStat((int) count);
            }
            page.setRecords(data);
        }

        return new PageUtils(page);
    }


    private void validateAssetStateConfInfo(AssetStateConf conf) {
        // 1. 名称重复
        List<AssetStateConf> list = this.list(new QueryWrapper<AssetStateConf>().lambda().eq(AssetStateConf::getName, conf.getName()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(conf.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.STATECONF_NAME_DUPLICATE);
        }

        // 2. 校验 ping monitor alert build 值
        List<Integer> switchList = Arrays.asList(new Integer[]{1, 0});
        if (!switchList.contains(conf.getPing())) {
            throw new NZException(RCode.STATECONF_PING_INCORRECT);
        }
        if (!switchList.contains(conf.getAlert())) {
            throw new NZException(RCode.STATECONF_ALERT_INCORRECT);
        }
        if (!switchList.contains(conf.getMonitor())) {
            throw new NZException(RCode.STATECONF_MONITOR_INCORRECT);
        }
        if (ToolUtil.isNotEmpty(conf.getBuildIn()) && !switchList.contains(conf.getBuildIn())) {
            throw new NZException(RCode.STATECONF_BUILDIN_INCORRECT);
        }
    }

    @Override
    public void saveConf(AssetStateConf conf) {
        // 1. 校验
        this.validateAssetStateConfInfo(conf);

        // 2. 保存
        this.save(conf);
    }

    @Override
    public void updateConf(AssetStateConf conf) {
        // 1. 校验
        this.validateAssetStateConfInfo(conf);

        // 2. 保存
        this.updateById(conf);
    }

    @Override
    public void delete(Integer... ids) {
        // 1. 内置的 配置不允许删除
        List<AssetStateConf> list = this.list(new LambdaQueryWrapper<AssetStateConf>().in(AssetStateConf::getId, Arrays.asList(ids)));
        for (AssetStateConf pojo : list) {
            if (pojo.getBuildIn().equals(1)) {
                throw new NZException(RCode.STATECONF_BUILDIN_CAN_NOT_REMOVE);
            }
        }

        // 2. 已被 asset 表引用的 状态不允许删除
        List<Asset> assetList = assetService.list();
        List<Integer> stateIdList = assetList.stream().map(Asset::getStateId).distinct().collect(Collectors.toList());
        for (AssetStateConf pojo : list) {
            if (stateIdList.contains(pojo.getId())) {
                throw new NZException(RCode.STATECONF_CAN_NOT_REMOVE);
            }
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }
}
