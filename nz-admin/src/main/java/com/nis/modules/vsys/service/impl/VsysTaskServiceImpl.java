package com.nis.modules.vsys.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.asset.entity.AssetFieldValue;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetFieldMetaService;
import com.nis.modules.asset.service.AssetFieldValueService;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.vsys.dao.VsysTaskDao;
import com.nis.modules.vsys.entity.VsysResult;
import com.nis.modules.vsys.entity.VsysTask;
import com.nis.modules.vsys.service.VsysResultService;
import com.nis.modules.vsys.service.VsysTaskService;
import com.nis.modules.vsys.utils.VsysConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VsysTaskServiceImpl extends ServiceImpl<VsysTaskDao, VsysTask> implements VsysTaskService {

    private static Log log = Log.get();

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private VsysResultService vsysResultService;

    @Autowired
    private AssetFieldMetaService assetFieldMetaService;

    @Autowired
    private AssetFieldValueService assetFieldValueService;


    /**
     * 校验 task info list
     *
     * @param task
     */
    private void validateTashInfo(VsysTask task) {
        List<VsysTask> vsysInfos = task.getVsysInfos();

        ValidateUtils.is(vsysInfos).notNull(RCode.VSYS_TASK_ISNULL);
        List<Integer> typeEnumValueList = Tool.ListUtil.of(1, 2, 3);

        for (VsysTask vsysInfo : vsysInfos) {
            Integer type = vsysInfo.getType();

            ValidateUtils.is(vsysInfo.getVsysId()).notNull(RCode.VSYS_ID_ISNULL)
                    .and(type).notNull(RCode.VSYS_TASK_TYPE_ISNULL);

            if (!typeEnumValueList.contains(type)) {
                throw new NZException(RCode.VSYS_TASK_TYPE_INVALIDED);
            }

            Object deviceTag = vsysInfo.getDeviceTag();
            if (Tool.ObjectUtil.isNotEmpty(deviceTag)) {
                List<Map> deviceTagList = (List<Map>) deviceTag;
                for (Map dtag : deviceTagList) {
                    if (Tool.ObjectUtil.isEmpty(dtag.get("tagType")))
                        throw new NZException(RCode.VSYS_TASK_DEVICETAG_TYPE_ISNULL);

                    if (Tool.ObjectUtil.isEmpty(dtag.get("tagValue")))
                        throw new NZException(RCode.VSYS_TASK_DEVICETAG_VALUE_ISNULL);
                }
            }

            Object params = vsysInfo.getParams();
            if (Tool.ObjectUtil.isNotEmpty(params)) {
                Map paramsMap = (Map) params;
                ValidateUtils.is(paramsMap.get("osQuotas")).notNull(RCode.VSYS_TASK_PARAM_OSQUOTAS_ISNULL);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTask(VsysTask task) throws Exception {
        // validate
        this.validateTashInfo(task);

        Integer concurrency = task.getConcurrency();
        if (Tool.ObjectUtil.isEmpty(concurrency)) {
            String configConcurrency = sysConfService.getValue("vsys_concurrency");
            concurrency = Tool.StrUtil.isNotEmpty(configConcurrency) ? Integer.valueOf(configConcurrency) : concurrency;
        }
        concurrency = ObjectUtil.defaultIfNull(concurrency, 0);

        Integer retry = task.getRetry();
        if (Tool.ObjectUtil.isEmpty(retry)) {
            String configRetry = sysConfService.getValue("vsys_retry");
            retry = Tool.StrUtil.isNotEmpty(configRetry) ? Integer.valueOf(configRetry) : retry;
        }
        retry = ObjectUtil.defaultIfNull(retry, 0);

        String taskUuid = Tool.StrUtil.uuid();
        for (VsysTask vsysTask : task.getVsysInfos()) {
            vsysTask.setTaskUuid(taskUuid);
            vsysTask.setConcurrency(concurrency);
            vsysTask.setRetry(retry);
            vsysTask.setTs(System.currentTimeMillis());

            List<Integer> vsysAssetIdList = Tool.ListUtil.list(true);
            Object deviceTag = vsysTask.getDeviceTag();
            if (Tool.ObjectUtil.isNotEmpty(deviceTag)) {
                // 通过 tag 获取 vsys asset
                vsysAssetIdList = this.getVsysAssetIdListByTag((List<Map>) deviceTag);
            } else {
                // device tag 为空，total = tsg asset total
                List<AssetAsset> tsgAssetList = assetService.getTsgAssetListbyModel();
                vsysAssetIdList = tsgAssetList.stream().map(AssetAsset::getId).collect(Collectors.toList());
            }

            // total
            vsysTask.setTotal(vsysAssetIdList.size());

            if (deviceTag != null) {
                vsysTask.setDeviceTag(JSONObject.toJSONString(deviceTag));
            }

            if (vsysTask.getParams() != null) {
                vsysTask.setParams(JSONObject.toJSONString(vsysTask.getParams()));
            }

            // save db
            this.save(vsysTask);

            // record 未执行状态日志
            this.recordUnExecStateLog(vsysTask, vsysAssetIdList);

            // save redis
            redisTemplate.opsForList().leftPush(VsysConstant.VSYS_TASK_QUEUE, JSONObject.toJSONString(vsysTask));
            log.info("push vsys task to redis, key: {}, data: {}", VsysConstant.VSYS_TASK_QUEUE, JSONObject.toJSONString(vsysTask));
        }
        return taskUuid;
    }

    /**
     * 添加 task 之后，根据 task & assetIdList 记录 未执行状态日志
     *
     * @param task
     * @param vsysAssetIdList
     */
    private void recordUnExecStateLog(VsysTask task, List<Integer> vsysAssetIdList) {
        if (Tool.CollUtil.isEmpty(vsysAssetIdList)) return;

        List<VsysResult> resultList = Tool.ListUtil.list(true);
        for (Integer assetId : vsysAssetIdList) {
            VsysResult result = new VsysResult();
            result.setTaskId(task.getId());
            result.setVsysId(task.getVsysId());
            result.setAssetId(assetId);
            result.setType(task.getType());
            result.setState(VsysConstant.ResultState.NOT_EXEC.getValue());
            result.setTs(System.currentTimeMillis());
            resultList.add(result);
        }

        vsysResultService.saveBatch(resultList);
    }


    /**
     * 通过 tag 获取 vsys asset
     *
     * @param deviceTagList
     * @return
     */
    private List<Integer> getVsysAssetIdListByTag(List<Map> deviceTagList) {
        // tsg asset
        List<AssetAsset> tsgAssetList = assetService.getTsgAssetListbyModel();
        List<Integer> tsgAssetIdList = tsgAssetList.stream().map(AssetAsset::getId).collect(Collectors.toList());

        // vsys
        List<Integer> vsysAssetIdList = Tool.ListUtil.list(true);
        for (Map deviceTag : deviceTagList) {
            String tagType = deviceTag.get("tagType").toString();
            String tagValue = deviceTag.get("tagValue").toString();

            AssetFieldMeta fieldMeta = assetFieldMetaService.getOne(new LambdaQueryWrapper<AssetFieldMeta>().eq(AssetFieldMeta::getMetaKey, tagType));
            if (ObjectUtil.isNotNull(fieldMeta)) {
                List<AssetFieldValue> list = assetFieldValueService.list(new LambdaUpdateWrapper<AssetFieldValue>()
                        .eq(AssetFieldValue::getMetaId, fieldMeta.getId())
                        .eq(AssetFieldValue::getValue, tagValue));

                // 同时满足 device tag & model tsg_appliance = 1
                Set<Integer> set = list.stream().map(AssetFieldValue::getAssetId).filter(assetId -> tsgAssetIdList.contains(assetId)).collect(Collectors.toSet());
                vsysAssetIdList.addAll(set);
            }
        }
        return vsysAssetIdList;
    }

}
