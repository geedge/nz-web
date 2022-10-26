package com.nis.modules.vsys.service.impl;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
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
import com.nis.modules.vsys.entity.VsysAssetRel;
import com.nis.modules.vsys.entity.VsysInfo;
import com.nis.modules.vsys.entity.VsysResult;
import com.nis.modules.vsys.entity.VsysTask;
import com.nis.modules.vsys.service.VsysAssetRelService;
import com.nis.modules.vsys.service.VsysInfoService;
import com.nis.modules.vsys.service.VsysResultService;
import com.nis.modules.vsys.service.VsysTaskService;
import com.nis.modules.vsys.utils.VsysConfigUtil;
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
    private VsysInfoService vsysInfoService;

    @Autowired
    private VsysResultService vsysResultService;

    @Autowired
    private VsysAssetRelService vsysAssetRelService;

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

            ValidateUtils.is(vsysInfo.getId()).notNull(RCode.VSYS_ID_ISNULL)
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

            // 添加|修改 时校验必填项
            if (type == 1 || type == 2) {
                // osQuotas not null
                Map osQuotas = vsysInfo.getOsQuotas();
                ValidateUtils.is(osQuotas).notNull(RCode.VSYS_TASK_PARAM_OSQUOTAS_ISNULL);

                // classification Rule
                Map classificationRule = vsysInfo.getClassificationRule();
                if (Tool.MapUtil.isNotEmpty(classificationRule)) {
                    // 当前默认取 andRules[0].orRules 配置
                    List<Map> orRules = (List<Map>) JSONPath.read(JSONObject.toJSONString(classificationRule), "andRules[0].orRules");
                    if (Tool.CollUtil.isNotEmpty(orRules)) {
                        // 当前只处理 IP 类型，存在其他类型配置则报错
                        List<Map> otherRuleConfigList = orRules.stream().filter(map -> {
                            String str = ObjectUtil.defaultIfNull(map.get("type"), StrUtil.EMPTY).toString();
                            return !StrUtil.equalsIgnoreCase("ip", str);
                        }).collect(Collectors.toList());
                        // 存在其他类型的配置，提示错误
                        if (Tool.CollUtil.isNotEmpty(otherRuleConfigList))
                            throw new NZException(RCode.VSYS_ONLY_SUPPORTS_IP_RULE);

                        // ip rule 配置不能重复
                        List<Map> ipRuleConfigList = orRules.stream().filter(map -> {
                            String str = ObjectUtil.defaultIfNull(map.get("type"), StrUtil.EMPTY).toString();
                            return StrUtil.equalsIgnoreCase("ip", str);
                        }).collect(Collectors.toList());

                        List<String> distinctConfigList = ipRuleConfigList.stream().map(map -> {
                            // sip & dip 不能重复
                            Map ruleMap = (Map) ObjectUtil.defaultIfNull(map.get("rule"), Tool.MapUtil.empty());
                            String sip = (String) ruleMap.get("sip");
                            String dip = (String) ruleMap.get("dip");
                            sip = StrUtil.emptyToDefault(sip, "sip");
                            dip = StrUtil.emptyToDefault(dip, "dip");
                            return StrUtil.concat(true, sip, dip);
                        }).distinct().collect(Collectors.toList());

                        if (ipRuleConfigList.size() != distinctConfigList.size())
                            throw new NZException(RCode.VSYS_IP_RULE_CONFIGDUPLICATE);
                    }
                }
            }
        }
    }

    @Override
    public String createTask(VsysTask task) throws Exception {
        // validate
        task.setName(StrUtil.trim(task.getName()));
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
            Integer vsysId = vsysTask.getId();
            vsysTask.setVsysId(vsysId);
            vsysTask.setId(null);
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

            // asset ids
            String assetIds = StrUtil.join(StrUtil.COMMA, vsysAssetIdList);
            vsysTask.setAssetIds(assetIds);

            if (deviceTag != null) {
                vsysTask.setDeviceTag(JSONObject.toJSONString(deviceTag));
            }

            // osQuotas & classificationRule
            Map<Object, Object> paramMap = Tool.MapUtil.builder()
                    .put("name", StrUtil.emptyToDefault(vsysTask.getName(), StrUtil.EMPTY))
                    .put("osQuotas", vsysTask.getOsQuotas())
                    .put("classificationRule", Tool.MapUtil.defaultIfEmpty(vsysTask.getClassificationRule(), Tool.MapUtil.empty()))
                    .build();
            vsysTask.setParams(JSONObject.toJSONString(paramMap));

            // save task
            this.saveVsysTask(vsysTask, vsysAssetIdList);
        }
        return taskUuid;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveVsysTask(VsysTask vsysTask, List<Integer> vsysAssetIdList) {
        // default value
        if (vsysTask.getTotal() == 0) {
            vsysTask.setProgress(100d);
        } else {
            vsysTask.setProgress(0d);
        }

        // save db
        this.save(vsysTask);

        // record 未执行状态日志
        this.recordUnExecStateLog(vsysTask, vsysAssetIdList);

        // vsys info
        // update state
        LambdaUpdateWrapper updateWrapper = new LambdaUpdateWrapper<VsysInfo>()
                .set(VsysInfo::getState, 0)
                .eq(VsysInfo::getVsysId, vsysTask.getVsysId());
        vsysInfoService.update(new VsysInfo(), updateWrapper);

        // insert
        VsysInfo info = new VsysInfo();
        info.setTaskId(vsysTask.getId());
        info.setVsysId(vsysTask.getVsysId());

        Object deviceTag = vsysTask.getDeviceTag();
        if (deviceTag != null) {
            info.setDeviceTag(deviceTag.toString());
        }
        info.setParams(vsysTask.getParams());
        info.setState(1);
        info.setTs(System.currentTimeMillis());
        vsysInfoService.save(info);

        // save redis
        redisTemplate.opsForList().leftPush(VsysConstant.VSYS_TASK_QUEUE, JSONObject.toJSONString(vsysTask));
        log.info("push vsys task to redis, key: {}, data: {}", VsysConstant.VSYS_TASK_QUEUE, JSONObject.toJSONString(vsysTask));
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
            result.setMsg(StrUtil.EMPTY);
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
        // 去重
        vsysAssetIdList = vsysAssetIdList.stream().distinct().collect(Collectors.toList());
        return vsysAssetIdList;
    }

    @Override
    public void vsysConfig(VsysTask vsysTask, AssetAsset asset) throws Exception {
        // target ip
        String manageIp = asset.getManageIp();
        Integer type = vsysTask.getType();
        // retry
        int execNum = vsysTask.getRetry() == 0 ? 1 : vsysTask.getRetry();
        for (int i = 0; i < execNum; i++) {
            int currentExecNum = i + 1;
            VsysResult result = vsysResultService.getOne(new LambdaQueryWrapper<VsysResult>()
                    .eq(VsysResult::getAssetId, asset.getId())
                    .eq(VsysResult::getTaskId, vsysTask.getId())
                    .and(qw -> qw.eq(VsysResult::getState, VsysConstant.ResultState.SUCCESS.getValue())
                            .or().eq(VsysResult::getState, VsysConstant.ResultState.FAILURE.getValue())));
            if (result != null) {
                log.info("vsys task finished, ExecNum:{}, taskId:{}, type:{}, assetId:{}, ManageIp:{}", i, vsysTask.getId(), type, asset.getId(), manageIp);
                break;
            }
            // 没有产生 SUCCESS|FAILURE 结果， 就重复执行
            log.info("vsys task no successful or failure records, ExecNum:{}, taskId:{}, type:{}, assetId:{}, ManageIp:{}", currentExecNum, vsysTask.getId(), type, asset.getId(), manageIp);

            VsysResult res = new VsysResult();
            res.setTs(System.currentTimeMillis());
            res.setNum(currentExecNum);
            switch (type) {
                // add & update
                case 1:
                case 2: {
                    this.deleteChain(vsysTask, manageIp);
                    this.deleteRuleSet(vsysTask, manageIp);
                    this.deleteServiceFunction(vsysTask, manageIp);

                    Map<Object, Object> sfResult = this.addServiceFunction(vsysTask, manageIp);
                    Map<Object, Object> ruleSetResult = this.addRuleSet(vsysTask, manageIp);
                    Map<Object, Object> chainResult = this.addChain(vsysTask, manageIp);

                    Integer sfResultCode = (Integer) sfResult.get("code");
                    Integer ruleSetResultCode = (Integer) ruleSetResult.get("code");
                    Integer chainResultCode = (Integer) chainResult.get("code");
                    if (HttpStatus.HTTP_CREATED == sfResultCode
                            && HttpStatus.HTTP_CREATED == ruleSetResultCode
                            && HttpStatus.HTTP_CREATED == chainResultCode) {
                        // success
                        res.setState(VsysConstant.ResultState.SUCCESS.getValue());
                        // save|update vsys asset rel
                        vsysAssetRelService.saveOrUpdateRelObj(vsysTask.getId(), vsysTask.getVsysId(), asset.getId(), VsysConstant.REL_SUCCESS, VsysConstant.REL_NOT_DEL);
                        // task progress
                        this.updateTaskProgress(vsysTask.getId(), true, false);
                    } else {
                        // failure
                        // 直至重试最后一次，更新状态为 FAILURE，并设置错误消息
                        if (currentExecNum == execNum) {
                            res.setState(VsysConstant.ResultState.FAILURE.getValue());
                            String sfErrMsg = (String) sfResult.get("msg");
                            String ruleSetErrMsg = (String) ruleSetResult.get("msg");
                            String chainErrMsg = (String) chainResult.get("msg");

                            String msg = this.handleTaskErrorMsg(sfErrMsg, ruleSetErrMsg, chainErrMsg);
                            res.setMsg(msg);
                            // save|update vsys asset rel
                            vsysAssetRelService.saveOrUpdateRelObj(vsysTask.getId(), vsysTask.getVsysId(), asset.getId(), VsysConstant.REL_FAIL, VsysConstant.REL_NOT_DEL);
                            // task progress
                            this.updateTaskProgress(vsysTask.getId(), false, true);
                        }
                    }
                    break;
                }
                // delete
                case 3: {
                    Map<Object, Object> chainResult = this.deleteChain(vsysTask, manageIp);
                    Map<Object, Object> ruleSetResult = this.deleteRuleSet(vsysTask, manageIp);
                    Map<Object, Object> sfResult = this.deleteServiceFunction(vsysTask, manageIp);

                    Integer sfResultCode = (Integer) sfResult.get("code");
                    Integer ruleSetResultCode = (Integer) ruleSetResult.get("code");
                    Integer chainResultCode = (Integer) chainResult.get("code");
                    if (HttpStatus.HTTP_NO_CONTENT == sfResultCode
                            && HttpStatus.HTTP_NO_CONTENT == ruleSetResultCode
                            && HttpStatus.HTTP_NO_CONTENT == chainResultCode) {
                        // success
                        res.setState(VsysConstant.ResultState.SUCCESS.getValue());
                        // del vsys asset rel
                        vsysAssetRelService.remove(new LambdaQueryWrapper<VsysAssetRel>()
                                .eq(VsysAssetRel::getVsysId, vsysTask.getVsysId())
                                .eq(VsysAssetRel::getAssetId, asset.getId()));
                        // task progress
                        this.updateTaskProgress(vsysTask.getId(), true, false);
                    } else {
                        // failure
                        if (currentExecNum == execNum) {
                            res.setState(VsysConstant.ResultState.FAILURE.getValue());
                            String sfErrMsg = (String) sfResult.get("msg");
                            String ruleSetErrMsg = (String) ruleSetResult.get("msg");
                            String chainErrMsg = (String) chainResult.get("msg");

                            String msg = this.handleTaskErrorMsg(sfErrMsg, ruleSetErrMsg, chainErrMsg);
                            res.setMsg(msg);
                            // save|update vsys asset rel
                            vsysAssetRelService.saveOrUpdateRelObj(vsysTask.getId(), vsysTask.getVsysId(), asset.getId(), VsysConstant.REL_FAIL, VsysConstant.REL_DEL);
                            // task progress
                            this.updateTaskProgress(vsysTask.getId(), false, true);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
            // 本次执行结束 更新 result log
            vsysResultService.update(res, new LambdaUpdateWrapper<VsysResult>()
                    .eq(VsysResult::getTaskId, vsysTask.getId())
                    .eq(VsysResult::getAssetId, asset.getId()));
        }
    }

    /**
     * 对错误信息进行处理
     *
     * @param sfErrMsg
     * @param ruleSetErrMsg
     * @param chainErrMsg
     * @return
     */
    private String handleTaskErrorMsg(String sfErrMsg, String ruleSetErrMsg, String chainErrMsg) {
        sfErrMsg = StrUtil.isNotEmpty(sfErrMsg) ? StrUtil.concat(true, " SF: ", sfErrMsg) : sfErrMsg;
        ruleSetErrMsg = StrUtil.isNotEmpty(ruleSetErrMsg) ? StrUtil.concat(true, " RuleSet: ", ruleSetErrMsg) : ruleSetErrMsg;
        chainErrMsg = StrUtil.isNotEmpty(chainErrMsg) ? StrUtil.concat(true, " Chain: ", chainErrMsg) : chainErrMsg;

        String msg = Tool.StrUtil.concat(true, sfErrMsg, ruleSetErrMsg, chainErrMsg);

        // 超长截断
        String str = StrUtil.subWithLength(msg, 0, Constant.MYSQL_TEXT_MAXLENGTH);
        return str;
    }

    /**
     * 更新 task success & failure & progress
     *
     * @param taskId
     * @param success
     * @param failure
     */
    private synchronized void updateTaskProgress(Integer taskId, boolean success, boolean failure) {
        VsysTask updateTask = this.getById(taskId);
        if (success) {
            updateTask.setSuccess(updateTask.getSuccess() + 1);
        }
        if (failure) {
            updateTask.setFailed(updateTask.getFailed() + 1);
        }
        String percent = NumberUtil.formatPercent((double) (updateTask.getSuccess() + updateTask.getFailed()) / updateTask.getTotal(), 2);
        updateTask.setProgress(Double.valueOf(percent.replace("%", "")));
        this.updateById(updateTask);
    }


    /**
     * 添加 sf
     *
     * @param vsysTask
     * @param manageIp
     * @return
     */
    private Map<Object, Object> addServiceFunction(VsysTask vsysTask, String manageIp) {
        // service function name
        String sfName = String.format("tsg-traffic-engine-vsys-%s", vsysTask.getVsysId());

        // sf default settings
        Map<String, String> settingsMap = Tool.MapUtil.builder("template", VsysConstant.VSYS_DEFAULT_TEMPLATE_NAME).build();

        String osQuotas = JSONPath.read(vsysTask.getParams(), "osQuotas").toString();
        JSONObject osQuotasMap = JSONObject.parseObject(osQuotas);

        Tool.MapUtil.renameKey(osQuotasMap, "totalPercent", "total");
        Tool.MapUtil.renameKey(osQuotasMap, "firewallPercent", "firewall");
        Tool.MapUtil.renameKey(osQuotasMap, "proxyPercent", "proxy");

        // sf body
        Map<Object, Object> sfMap = Tool.MapUtil.builder()
                .put("name", sfName)
                .put("vsys_id", vsysTask.getVsysId())
                .put("quotas", osQuotasMap)
                .put("settings", settingsMap)
                .put("enable", "yes")
                .build();

        Map<Object, Object> sfBody = Tool.MapUtil.builder()
                .put("tsg-os-mgnt-srv:service_function", sfMap)
                .build();

        // request
        String path = "/restconf/data";
        Map<Object, Object> resultMp = VsysConfigUtil.requestPost(manageIp, path, sfBody);
        return resultMp;
    }

    /**
     * 添加 ruleset
     *
     * @param vsysTask
     * @param manageIp
     * @return
     */
    private Map<Object, Object> addRuleSet(VsysTask vsysTask, String manageIp) {
        // ruleset name
        String ruleSetName = String.format("ruleset_vsys_%s", vsysTask.getVsysId());
        // 当前默认取 andRules[0].orRules 配置
        List<Map> orRules = (List<Map>) JSONPath.read(vsysTask.getParams(), "classificationRule.andRules[0].orRules");

        List<Map> ruleConfigListMap = Tool.ListUtil.list(true);
        if (Tool.CollUtil.isNotEmpty(orRules)) {
            /**
             * 只处理 IP 类型，其余类型暂不下发
             */
            orRules = orRules.stream().filter(map -> {
                String type = ObjectUtil.defaultIfNull(map.get("type"), StrUtil.EMPTY).toString();
                return StrUtil.equalsIgnoreCase("ip", type);
            }).collect(Collectors.toList());

            for (int i = 0; i < orRules.size(); i++) {
                Map<Object, Object> tm = Tool.MapUtil.builder()
                        // id 以序号编排
                        .put("id", i + 1)
                        .build();

                Map ruleMap = (Map) ObjectUtil.defaultIfNull(orRules.get(i).get("rule"), Tool.MapUtil.empty());

                Object sip = ruleMap.get("sip");
                if (ObjectUtil.isNotEmpty(sip)) {
                    String sipStr = sip.toString();
                    String validateSipStr = sipStr;
                    if (sipStr.lastIndexOf("/") != StrUtil.INDEX_NOT_FOUND) {
                        validateSipStr = sipStr.substring(0, sipStr.lastIndexOf("/"));
                    }
                    boolean ipv4 = Validator.isIpv4(validateSipStr);
                    boolean ipv6 = Validator.isIpv6(validateSipStr);

                    if (ipv4) {
                        tm.put("ipv4-sip", sipStr);
                    } else if (ipv6) {
                        tm.put("ipv6-sip", sipStr);
                    }

                    if (ObjectUtil.isAllEmpty(tm.get("ipv4-sip"), tm.get("ipv6-sip"))) {
                        log.info("vsys ruleset sip format error,sip: {}, task: {}", sipStr, JSONObject.toJSONString(vsysTask));
                        tm.put("ipv4-sip", sipStr);
                    }
                }
                Object dip = ruleMap.get("dip");
                if (ObjectUtil.isNotEmpty(dip)) {
                    String dipStr = dip.toString();
                    String validateDipStr = dipStr;
                    if (dipStr.lastIndexOf("/") != StrUtil.INDEX_NOT_FOUND) {
                        validateDipStr = dipStr.substring(0, dipStr.lastIndexOf("/"));
                    }
                    boolean ipv4 = Validator.isIpv4(validateDipStr);
                    boolean ipv6 = Validator.isIpv6(validateDipStr);

                    if (ipv4) {
                        tm.put("ipv4-dip", dipStr);
                    } else if (ipv6) {
                        tm.put("ipv6-dip", dipStr);
                    }

                    if (ObjectUtil.isAllEmpty(tm.get("ipv4-dip"), tm.get("ipv6-dip"))) {
                        log.info("vsys ruleset dip format error,dip: {}, task: {}", dipStr, JSONObject.toJSONString(vsysTask));
                        tm.put("ipv4-dip", dipStr);
                    }
                }
                ruleConfigListMap.add(tm);
            }
        }

        // ruleset body
        Map<Object, Object> ruleSetMap = Tool.MapUtil.builder()
                .put("name", ruleSetName)
                .build();

        if (Tool.CollUtil.isNotEmpty(ruleConfigListMap)) {
            ruleSetMap.put("rule", ruleConfigListMap);
        }

        Map<Object, Object> ruleSetBody = Tool.MapUtil.builder()
                .put("tsg-os-mgnt-srv:ruleset", ruleSetMap)
                .build();

        // request
        String path = "/restconf/data";
        Map<Object, Object> resultMp = VsysConfigUtil.requestPost(manageIp, path, ruleSetBody);
        return resultMp;
    }

    /**
     * 添加 sf chain
     *
     * @param vsysTask
     * @param manageIp
     * @return
     */
    private Map<Object, Object> addChain(VsysTask vsysTask, String manageIp) {
        // service function name
        String sfName = String.format("tsg-traffic-engine-vsys-%s", vsysTask.getVsysId());

        // ruleset name
        String ruleSetName = String.format("ruleset_vsys_%s", vsysTask.getVsysId());

        // chain body
        Map<Object, Object> chainMap = Tool.MapUtil.builder()
                .put("id", vsysTask.getVsysId())
                .put("service_function", sfName)
                .put("ruleset", ruleSetName)
                .build();

        Map<Object, Object> chainBody = Tool.MapUtil.builder()
                .put("tsg-os-mgnt-srv:index", chainMap)
                .build();

        // request
        String path = String.format("%s%s", "/restconf/data", "/tsg-os-mgnt-srv:service_function_chain=Default");
        Map<Object, Object> resultMp = VsysConfigUtil.requestPostWithoutEncodeUrl(manageIp, path, chainBody);
        return resultMp;
    }

    /**
     * 删除 sf
     *
     * @param vsysTask
     * @param manageIp
     * @return
     */
    private Map<Object, Object> deleteServiceFunction(VsysTask vsysTask, String manageIp) {
        // service function name
        String sfName = String.format("tsg-traffic-engine-vsys-%s", vsysTask.getVsysId());

        // request
        String path = String.format("%s%s=%s", "/restconf/data", "/tsg-os-mgnt-srv:service_function", sfName);
        Map<Object, Object> resultMp = VsysConfigUtil.requestDeleteWithoutEncodeUrl(manageIp, path, null);
        return resultMp;
    }

    /**
     * 删除 ruleset
     *
     * @param vsysTask
     * @param manageIp
     * @return
     */
    private Map<Object, Object> deleteRuleSet(VsysTask vsysTask, String manageIp) {
        // ruleset name
        String ruleSetName = String.format("ruleset_vsys_%s", vsysTask.getVsysId());

        // request
        String path = String.format("%s%s=%s", "/restconf/data", "/tsg-os-mgnt-srv:ruleset", ruleSetName);
        Map<Object, Object> resultMp = VsysConfigUtil.requestDeleteWithoutEncodeUrl(manageIp, path, null);
        return resultMp;
    }

    /**
     * 删除 chain
     *
     * @param vsysTask
     * @param manageIp
     * @return
     */
    private Map<Object, Object> deleteChain(VsysTask vsysTask, String manageIp) {
        Integer vsysId = vsysTask.getVsysId();

        // request
        String indexq = "/index=" + vsysId;
        String path = String.format("%s%s%s", "/restconf/data", "/tsg-os-mgnt-srv:service_function_chain=Default", indexq);
        Map<Object, Object> resultMp = VsysConfigUtil.requestDeleteWithoutEncodeUrl(manageIp, path, null);
        return resultMp;
    }

}
