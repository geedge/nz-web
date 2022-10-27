package com.nis.modules.vsys.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.vsys.dao.VsysAssetRelDao;
import com.nis.modules.vsys.entity.VsysAssetRel;
import com.nis.modules.vsys.entity.VsysInfo;
import com.nis.modules.vsys.entity.VsysResult;
import com.nis.modules.vsys.entity.VsysTask;
import com.nis.modules.vsys.service.VsysAssetRelService;
import com.nis.modules.vsys.service.VsysInfoService;
import com.nis.modules.vsys.service.VsysResultService;
import com.nis.modules.vsys.service.VsysTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VsysAssetRelServiceImpl extends ServiceImpl<VsysAssetRelDao, VsysAssetRel> implements VsysAssetRelService {

    private static final Log log = Log.get();

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private VsysTaskService vsysTaskService;

    @Autowired
    private VsysInfoService vsysInfoService;

    @Autowired
    private VsysResultService vsysResultService;

    @Override
    public List<VsysAssetRel> queryAssetVsysRel(Integer vsysId, Integer assetId) {
        List<VsysAssetRel> relList = this.list(new LambdaUpdateWrapper<VsysAssetRel>()
                .eq(vsysId != null, VsysAssetRel::getVsysId, vsysId)
                .eq(assetId != null, VsysAssetRel::getAssetId, assetId)
        );

        if (Tool.CollUtil.isNotEmpty(relList)) {
            List<AssetAsset> list = assetService.list();
            Map<Integer, Map<Object, Object>> relAssetMapInfo = list.stream().collect(Collectors.toMap(AssetAsset::getId, asset -> {
                Map<Object, Object> m = Tool.MapUtil.builder()
                        .put("id", asset.getId())
                        .put("name", asset.getName())
                        .build();
                return m;
            }));

            List<VsysTask> taskList = vsysTaskService.list();
            Map<Integer, VsysTask> taskIdAndObjMap = taskList.stream().collect(Collectors.toMap(VsysTask::getId, Function.identity()));

            List<VsysResult> resultList = vsysResultService.list();
            Map<Integer, VsysResult> resultIdAndObjMap = resultList.stream().collect(Collectors.toMap(VsysResult::getId, Function.identity()));

            List<VsysInfo> infoList = vsysInfoService.list(new LambdaQueryWrapper<VsysInfo>().eq(VsysInfo::getState, 1));
            Map<Integer, VsysInfo> vsysIdAndInfoMap = infoList.stream().collect(Collectors.toMap(VsysInfo::getVsysId, Function.identity()));

            for (VsysAssetRel rel : relList) {
                Map<Object, Object> relAsset = relAssetMapInfo.get(rel.getAssetId());
                rel.setAsset(relAsset);

                VsysTask task = taskIdAndObjMap.get(rel.getTaskId());
                rel.setVsysTask(task);

                VsysResult result = resultIdAndObjMap.get(rel.getResultId());
                rel.setVsysResult(result);

                VsysInfo vsysInfo = vsysIdAndInfoMap.get(rel.getVsysId());
                rel.setVsysInfo(vsysInfo);
            }
        }

        return relList;
    }

    @Override
    public void saveOrUpdateRelObj(Integer taskId, Integer vsysId, Integer assetId, Integer state, Integer delFlag) {
        try {
            // 先查询是否存在对应关系
            VsysAssetRel rel = this.getOne(new LambdaQueryWrapper<VsysAssetRel>()
                    .eq(VsysAssetRel::getVsysId, vsysId)
                    .eq(VsysAssetRel::getAssetId, assetId));

            // save
            if (rel == null) {
                rel = new VsysAssetRel(null, assetId, taskId, vsysId, state, delFlag, System.currentTimeMillis());
            } else {
                // update state delFlag uts takId
                rel.setState(state);
                rel.setDelFlag(delFlag);
                rel.setUts(System.currentTimeMillis());
                rel.setTaskId(taskId);
            }
            // 根据本次 task id 获得 result
            VsysResult result = vsysResultService.getOne(new LambdaQueryWrapper<VsysResult>()
                    .eq(VsysResult::getTaskId, taskId)
                    .eq(VsysResult::getAssetId, assetId)
                    .eq(VsysResult::getVsysId, vsysId));
            if (result != null) {
                rel.setResultId(result.getId());
            }

            this.saveOrUpdate(rel);
            log.info("save vsys_asset_rel info: {}", JSONUtil.toJsonStr(rel));
        } catch (Exception e) {
            log.error(e, "save vsys_asset_rel error,taskId: %s, vsysId: %s, assetId: %s, state: %s, delFlag: %s", taskId, vsysId, assetId, state, delFlag);
        }
    }
}
