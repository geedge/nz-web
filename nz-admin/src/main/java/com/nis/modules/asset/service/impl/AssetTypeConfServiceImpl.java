package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.AssetTypeConfDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetTypeConf;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetTypeConfService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AssetTypeConf service
 *
 * @author shizhendong
 * @date 2021-03-08
 */
@Service
public class AssetTypeConfServiceImpl extends ServiceImpl<AssetTypeConfDao, AssetTypeConf> implements AssetTypeConfService {

    @Autowired
    private AssetAssetService assetService;

    @Override
    public AssetTypeConf queryConfInfo(Integer id) {
        AssetTypeConf conf = this.getById(id);
        if (ToolUtil.isEmpty(conf)) {
            throw new NZException(RCode.TYPECONF_NOT_FOUND);
        }
        AssetTypeConf pconf = this.getById(conf.getPid());
        conf.setPname(ToolUtil.isNotEmpty(pconf) ? pconf.getName() : "default");
        return conf;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<AssetTypeConf> queryWrapper = new LambdaQueryWrapper<AssetTypeConf>()
                .eq(ToolUtil.isNotEmpty(params.get("id")), AssetTypeConf::getId, params.get("id"))
                .like(ToolUtil.isNotEmpty(params.get("name")), AssetTypeConf::getName, Tool.StrUtil.likeEscape(Tool.MapUtil.getStr(params,"name")))
                .eq(ToolUtil.isNotEmpty(params.get("vm")), AssetTypeConf::getVm, params.get("vm"))
                .eq(ToolUtil.isNotEmpty(params.get("vmh")), AssetTypeConf::getVmh, params.get("vmh"))
                .eq(ToolUtil.isNotEmpty(params.get("authProtocol")), AssetTypeConf::getAuthProtocol, params.get("authProtocol"))
                .eq(ToolUtil.isNotEmpty(params.get("snmpEnable")), AssetTypeConf::getSnmpEnable, params.get("snmpEnable"))
                .eq(ToolUtil.isNotEmpty(params.get("clientEnable")), AssetTypeConf::getClientEnable, params.get("clientEnable"));

        IPage page = this.page(new Query(AssetTypeConf.class).getPage(params), queryWrapper);
        List<AssetTypeConf> assetTypeConfs = page.getRecords();
        if (CollectionUtils.isNotEmpty(assetTypeConfs)) {
            List<AssetTypeConf> allConfig = this.list();
            Map<Integer, String> confIdAndNameMap = allConfig.stream().collect(Collectors.toMap(AssetTypeConf::getId, AssetTypeConf::getName));
            for (AssetTypeConf conf : assetTypeConfs) {
                String pname = confIdAndNameMap.get(conf.getPid());
                conf.setPname(ToolUtil.isNotEmpty(pname) ? pname : "default");
            }
            page.setRecords(assetTypeConfs);
        }
        return new PageUtils(page);
    }

    private void validateAssetTypeConfInfo(AssetTypeConf conf) {
        // 1. 名称重复
        List<AssetTypeConf> list = this.list(new LambdaQueryWrapper<AssetTypeConf>().eq(AssetTypeConf::getPid, ToolUtil.isEmpty(conf.getPid()) ? 0 : conf.getPid()).eq(AssetTypeConf::getName, conf.getName()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(conf.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.TYPECONF_NAME_DUPLICATE);
        }

        // 2. 校验 vm vmh ssh te;net build clientEnable 值
        List<Integer> switchList = Arrays.asList(new Integer[]{1, 0});
        if (!switchList.contains(conf.getVm())) {
            throw new NZException(RCode.TYPECONF_VM_INCORRECT);
        }
        if (!switchList.contains(conf.getVmh())) {
            throw new NZException(RCode.TYPECONF_VMH_INCORRECT);
        }
        if (ToolUtil.isNotEmpty(conf.getBuildIn()) && !switchList.contains(conf.getBuildIn())) {
            throw new NZException(RCode.TYPECONF_BUILDIN_INCORRECT);
        }
        if (!switchList.contains(conf.getClientEnable())) {
            throw new NZException(RCode.TYPECONF_CLIENTENABLE_INCORRECT);
        }

        // 3. pid 是否存在
        if (ToolUtil.isNotEmpty(conf.getPid()) && !conf.getPid().equals(0)) {
            AssetTypeConf parentPojo = this.getById(conf.getPid());
            if (ToolUtil.isEmpty(parentPojo)) {
                throw new NZException(RCode.TYPECONF_PARENT_NOT_FOUND);
            }
        }
    }

    @Override
    public Integer saveConf(AssetTypeConf conf) {
        // 1. 校验
        this.validateAssetTypeConfInfo(conf);

        // 2. 保存
        this.save(conf);
        return conf.getId();
    }

    @Override
    public Integer updateConf(AssetTypeConf conf) {
        // 1. 校验
        this.validateAssetTypeConfInfo(conf);

        // 2. 保存
        this.updateById(conf);
        return conf.getId();
    }

    @Override
    public void delete(Integer... ids) {
        List<AssetTypeConf> list = this.list();

        // 1. 内置的 配置不允许删除
        List<Integer> buildInIdList = list.stream().filter(assetTypeConf -> assetTypeConf.getBuildIn().equals(1)).map(AssetTypeConf::getId).collect(Collectors.toList());
        buildInIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(buildInIdList)) {
            throw new NZException("These configurations are built-in and cannot be deleted. Details: " + buildInIdList.toString(), RCode.TYPECONF_BUILDIN_CAN_NOT_REMOVE.getCode());
        }

        // 2. 含有 子级 配置不允许删除
        List<Integer> containBabyConfIds = new ArrayList<>();
        for (Integer delId : Arrays.asList(ids)) {
            List<AssetTypeConf> babyConfs = list.stream().filter(conf -> conf.getPid().equals(delId)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(babyConfs)) {
                containBabyConfIds.add(delId);
            }
        }
        if (CollectionUtils.isNotEmpty(containBabyConfIds)) {
            throw new NZException("These configurations contain sub-configuration and cannot be deleted. Details: " + containBabyConfIds.toString(), RCode.TYPECONF_CONTAIN_BABY_CAN_NOT_REMOVE.getCode());
        }

        // 3. 已被 asset 表引用的 类型不允许删除
        List<AssetAsset> assetList = assetService.list();
        List<Integer> typeIdList = assetList.stream().map(AssetAsset::getTypeId).distinct().collect(Collectors.toList());
        typeIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(typeIdList)) {
            throw new NZException("These configurations have been used by assets and cannot be deleted. Details: " + typeIdList.toString(), RCode.TYPECONF_BEUSED_CAN_NOT_REMOVE.getCode());
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }

    @Override
    public List<AssetTypeConf> queryTypeConfTree(Integer pid) {
        List<AssetTypeConf> assetTypeConfList = this.list();
        Map<Integer, String> confIdAndNameMap = assetTypeConfList.stream().collect(Collectors.toMap(AssetTypeConf::getId, AssetTypeConf::getName));
        assetTypeConfList = this.getConfTreeList(assetTypeConfList, confIdAndNameMap, ToolUtil.isEmpty(pid) ? 0 : pid);
        return assetTypeConfList;
    }

    private List<AssetTypeConf> getConfTreeList(List<AssetTypeConf> assetTypeConfList, Map<Integer, String> confIdAndNameMap, Integer pid) {
        List<AssetTypeConf> resultList = new ArrayList<>();
        for (AssetTypeConf conf : assetTypeConfList) {
            if (conf.getPid().equals(pid)) {
                conf.setChildren(this.getConfTreeList(assetTypeConfList, confIdAndNameMap, conf.getId()));
                resultList.add(conf);
            }
            String pname = confIdAndNameMap.get(conf.getPid());
            conf.setPname(ToolUtil.isNotEmpty(pname) ? pname : "default");
        }
        return resultList;
    }

}
