package com.nis.modules.vsys.backend;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.vsys.entity.VsysResult;
import com.nis.modules.vsys.entity.VsysTask;
import com.nis.modules.vsys.service.VsysResultService;
import com.nis.modules.vsys.service.VsysTaskService;
import com.nis.modules.vsys.utils.VsysConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class VsysTaskConfigThread implements Runnable {

    private static final Log log = Log.get();

    @Value("${nezha.vsysConcurrentThreads:50}")
    private Integer nzVsysConcurrentThreads;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private VsysResultService vsysResultService;

    @Autowired
    private VsysTaskService vsysTaskService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    static ThreadPoolExecutor threadPoolExecutor;

    static {
        threadPoolExecutor = new ThreadPoolExecutor(
                10,
                Integer.MAX_VALUE,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactoryBuilder().setNamePrefix("vsys-task-pool-").build());
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Vsys Task Config Thread");
        log.info("VsysTaskConfigThread start...");

        while (true) {
            try {
                // 检查 HA_LOCK
                LeaderElectionDao dao = LeaderElectionDao.getInstance();
                if (Tool.BooleanUtil.negate(dao.isLeader())) {
                    boolean sleep = Tool.ThreadUtil.sleep(10000);
                    log.info("vsys task config thread sleep 10000ms :{}", sleep);
                    continue;
                }

                ListOperations<String, String> opsForList = redisTemplate.opsForList();
                if (redisTemplate.hasKey(VsysConstant.VSYS_TASK_QUEUE)) {
                    Long size = opsForList.size(VsysConstant.VSYS_TASK_QUEUE);
                    log.info("vsys_queue_key size : {}", size);
                }

                String vsysTaskStr = opsForList.rightPop(VsysConstant.VSYS_TASK_QUEUE, 30, TimeUnit.SECONDS);
                VsysTask vsysTask = JSONObject.parseObject(vsysTaskStr, VsysTask.class);
                if (vsysTask == null) {
                    log.warn("vsys task config error, VsysTask is null, data in redis: {}", vsysTaskStr);
                    continue;
                }

                Integer total = vsysTask.getTotal();
                if (total == 0) {
                    log.warn("vsys task config warn, total = 0, No associated assets, data in redis: {}", vsysTaskStr);
                    continue;
                }

                StopWatch sw = StopWatch.create("vsys task");
                sw.start();
                log.info("vsys task config begin...");
                log.info("vsys task config info in the redis queue: {}", vsysTaskStr);

                // NOT_EXEC 未执行状态日志
                List<VsysResult> resultList = vsysResultService.list(new LambdaQueryWrapper<VsysResult>().eq(VsysResult::getTaskId, vsysTask.getId()));
                List<Integer> vsysAssetIdList = resultList.stream().map(VsysResult::getAssetId).collect(Collectors.toList());

                Collection<AssetAsset> listByIds = assetService.listByIds(vsysAssetIdList);
                List<AssetAsset> vsysConfigAssetList = listByIds.stream().collect(Collectors.toList());
                if (Tool.CollUtil.isEmpty(vsysConfigAssetList)) {
                    log.info("vsys task config not executed, not found rel assets");
                    continue;
                }

                // 先更新 result 状态为 执行中
                resultList.stream().forEach(result -> {
                    result.setState(VsysConstant.ResultState.IN_EXEC.getValue());
                    result.setTs(System.currentTimeMillis());
                });
                vsysResultService.updateBatchById(resultList);

                Integer concurrency = vsysTask.getConcurrency();
                concurrency = concurrency == 0 ? nzVsysConcurrentThreads : concurrency;
                threadPoolExecutor.setCorePoolSize(concurrency);

                for (AssetAsset asset : vsysConfigAssetList) {
                    threadPoolExecutor.submit(() -> {
                        // vsys 配置下发
                        try {
                            vsysTaskService.vsysConfig(vsysTask, asset);
                        } catch (Exception e) {
                            log.error(e, "vsys task config error");
                        }
                    });
                }

                log.info("vsys task config finished.");
                sw.stop();
                log.info("vsys task config Run Time:{}", sw.prettyPrint());
            } catch (Exception e) {
                log.error(e, "VsysTaskConfigThread error");
            }
        }
    }

}
