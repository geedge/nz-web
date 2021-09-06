package com.nis.modules.asset.job;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetTalonStatus;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetTalonStatusService;
import com.nis.modules.endpoint.dao.MonitorEndpointStateDao;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import com.nis.modules.endpoint.service.MonitorEndpointConfigService;
import com.nis.modules.endpoint.service.MonitorEndpointStateService;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;

import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class AssetTalonStatusJob extends QuartzJobBean {

    private static Log log = Log.get();

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private AssetTalonStatusService talonStatusService;

    @Autowired
    private MonitorEndpointConfigService endpointConfigService;

    @Autowired
    private MonitorEndpointStateDao endpointStateDao;

    @Autowired
    private MonitorEndpointStateService endpointStateService;


    @Autowired
    private RedisTemplate redisTemplate;

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 检查 haMode
        String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if(!StrUtil.equals(serverId, Constant.SERVER_ID)) return;

        StopWatch sw = new StopWatch();
        sw.start();
        log.info("asset talon state 任务开始执行");
        try {
            this.updateTalonState();
        } catch (RuntimeException e) {
            log.error("asset talon 状态更新异常", e);
        } finally {
            sw.stop();
            log.info("asset talon state Run Time: {}", sw.toString());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTalonState() throws RuntimeException {
        // asset
        List<AssetAsset> assetList = assetService.list();
        List<AssetAsset> talonOpenAssetList = assetList.stream().filter(asset -> 1 == asset.getClientEnable()).collect(Collectors.toList());
        List<AssetAsset> talonCloseAssetList = assetList.stream().filter(asset -> 0 == asset.getClientEnable()).collect(Collectors.toList());

        List<AssetTalonStatus> abnormalTalonList = new ArrayList<>();
        for (AssetAsset talonCloseAsset : talonCloseAssetList) {
            abnormalTalonList.add(new AssetTalonStatus(talonCloseAsset.getId(), 0, "DISABLE", new Date(), 0));
        }

        List<AssetTalonStatus> talonUpdateStatusList = new ArrayList<>();
        List<Callable<Map<String, Object>>> tasks = new ArrayList<>();
        for (AssetAsset talonOpenAsset : talonOpenAssetList) {
            Callable task = new Callable() {
                @Override
                public Map<String, Object> call() throws Exception {
                    UrlBuilder urlBuilder = UrlBuilder.create().setScheme("http").setHost(talonOpenAsset.getManageIp()).setPort(talonOpenAsset.getClientPort()).appendPath("nz-talon").appendPath("healthy");
                    String result = null;
                    try {
                        result = Tool.HttpUtil.createGet(urlBuilder.build()).setConnectionTimeout(5000).auth(talonOpenAsset.getClientToken()).execute().body();
                        if (log.isDebugEnabled()) {
                            log.debug("请求 talon 返回结果" + result);
                        }
                        R parseObject = JSONObject.parseObject(result, R.class);
                        Map<String, String> data = (Map<String, String>) parseObject.get("data");
                        // 认证失败
                        Map<String, Object> map = new HashMap(4);
                        map.put("id", talonOpenAsset.getId());
                        if (StrUtil.equals("FALSE", data.get("auth"))) {
                            map.put("state", 3);
                            map.put("msg", "Authentication failed");
                            return map;
                        }
                        // UP
                        String promtailState = data.get("promtail");
                        if (StrUtil.equals("UP", promtailState)) {
                            map.put("state", 1);
                            map.put("msg", "UP");
                        } else {
                            map.put("state", 0);
                            map.put("msg", "DOWN");
                        }
                        String s = data.get("version");
                        s = s.replaceAll("\\n", "");
                        map.put("version", Integer.valueOf(s));
                        return map;
                    } catch (Exception e) {
                        log.error(String.format("请求 %s 接口失败", urlBuilder.toString()), e);
                        // 连接异常
                        Map<String, Object> map = new HashMap(4);
                        map.put("id", talonOpenAsset.getId());
                        if (e.getCause() instanceof ConnectException) {
                            map.put("state", 2);
                            map.put("msg", e.getMessage());
                            return map;

                        } else {
                            // 未知异常
                            map.put("state", 4);
                            map.put("msg", e.getMessage());
                            return map;
                        }
                    }
                }
            };
            tasks.add(task);
        }

        try {
            List<Future<Map<String, Object>>> futures = executorService.invokeAll(tasks);
            for (Future<Map<String, Object>> future : futures) {
                if (!future.isCancelled()) {
                    Map<String, Object> result = future.get();
                    Integer state = (Integer) result.get("state");
                    Integer version = (Integer) result.get("version");
                    if (version == null) {
                        version = -1;
                    }
                    if (1 == state) {
                        talonUpdateStatusList.add(new AssetTalonStatus((Integer) result.get("id"), (Integer) result.get("state"), result.get("msg").toString(), new Date(), version));
                    } else {
                        abnormalTalonList.add(new AssetTalonStatus((Integer) result.get("id"), (Integer) result.get("state"), result.get("msg").toString(), new Date(), version));
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
        }

        // 更新数据库
        List<AssetTalonStatus> list = talonStatusService.list();
        Map<Integer, AssetTalonStatus> assetIdAndTalonStateMap = list.stream().collect(Collectors.toMap(AssetTalonStatus::getAssetId, Function.identity()));
        // 处理不可达资产的最后可达时间
        for (AssetTalonStatus talonStatus : abnormalTalonList) {
            AssetTalonStatus temp = assetIdAndTalonStateMap.get(talonStatus.getAssetId());
            if (temp != null && temp.getStatus() == talonStatus.getStatus())
                talonStatus.setLastUpdate(temp.getLastUpdate());
        }

        talonUpdateStatusList.addAll(abnormalTalonList);
        talonStatusService.remove(null);
        talonStatusService.saveBatch(talonUpdateStatusList);


        Map<Integer, AssetTalonStatus> map = talonUpdateStatusList.stream().collect(Collectors.toMap(AssetTalonStatus::getAssetId, Function.identity()));
        // 更新 endpoint state 表数据
        List<MonitorEndpointState> logStateList = new ArrayList<>();
        List<MonitorEndpointConfig> logsEndpointConfigList = endpointConfigService.getEndpointConfigInfoListByType(Constant.MonitorConfType.LOGS.getValue());
        for (MonitorEndpointConfig conf : logsEndpointConfigList) {
            Integer assetId = conf.getAsset().getId();
            AssetTalonStatus talonStatus = map.get(assetId);
            Integer state = talonStatus.getStatus() == 1 ? 1 : 0;
            // 0：关闭 1：开启
            Integer clientEnable = conf.getAsset().getClientEnable();
            // 0：停用 1：启用
            Integer enabled = conf.getEnable();
            // 开启：ON, 关闭：OFF
            String dcStateStr = conf.getDc().getState();
            boolean dcState = dcStateStr.equals("ON") ? true : false;
            if (!dcState && clientEnable.equals(0) && enabled.equals(1)) {
                state = 2;
            } else if (!dcState && clientEnable.equals(1) && enabled.equals(0)) {
                state = 4;
            } else if (dcState && clientEnable.equals(0) && enabled.equals(0)) {
                state = 8;
            } else if (!dcState && clientEnable.equals(1) && enabled.equals(1)) {
                state = 6;
            } else if (dcState && clientEnable.equals(0) && enabled.equals(1)) {
                state = 10;
            } else if (dcState && clientEnable.equals(1) && enabled.equals(0)) {
                state = 12;
            }
            logStateList.add(new MonitorEndpointState(conf.getEndpointId(), conf.getId(), conf.getType(), state, talonStatus.getVersion(), new Date()));
        }
        // 更新 endpoint state
        endpointStateDao.deleteConfStateByType(Constant.MonitorConfType.LOGS.getValue());
        endpointStateService.saveBatch(logStateList);
    }
}
