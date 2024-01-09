package com.nis.modules.vsys.backend;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.sys.dao.SysLockDao;
import com.nis.modules.sys.entity.SysLock;
import com.nis.modules.vsys.entity.VsysResult;
import com.nis.modules.vsys.entity.VsysTask;
import com.nis.modules.vsys.service.VsysResultService;
import com.nis.modules.vsys.service.VsysTaskService;
import com.nis.modules.vsys.utils.VsysConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private SysLockDao sysLockDao;

    static ThreadPoolExecutor threadPoolExecutor;

    static final String SYS_LOCK_LAST_VSYS_TASK_ID = "last_vsys_task_id";

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
            // 检查 HA_LOCK
            LeaderElectionDao dao = LeaderElectionDao.getInstance();
            if (Tool.BooleanUtil.negate(dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID))) {
                boolean sleep = Tool.ThreadUtil.sleep(10000);
                log.info("vsys task config thread sleep 10000ms :{}", sleep);
                continue;
            }

            SysLock lastVsysTaskIdRecord = sysLockDao.selectOne(new LambdaQueryWrapper<SysLock>().eq(SysLock::getLockKey, SYS_LOCK_LAST_VSYS_TASK_ID));
            Integer lastVsysTaskId = Integer.valueOf(lastVsysTaskIdRecord.getLockValue());
            log.info("get vsys task, last processed vsys task id: {}", lastVsysTaskId);

            VsysTask vsysTask = vsysTaskService.getOne(new LambdaQueryWrapper<VsysTask>().gt(VsysTask::getId, lastVsysTaskId).orderByAsc(VsysTask::getId).last("limit 1"));
            if (vsysTask == null) {
                boolean sleep = Tool.ThreadUtil.sleep(10000);
                log.warn("get vsys task is null, thread sleep 10000ms :{}", sleep);
                continue;
            }

            // apply config for task
            this.applyConfigForTask(vsysTask);

            // update last_vsys_task_id value
            sysLockDao.update(new SysLock(), new LambdaUpdateWrapper<SysLock>()
                    .set(SysLock::getLockValue, vsysTask.getId())
                    .eq(SysLock::getLockKey, SYS_LOCK_LAST_VSYS_TASK_ID));
            log.info("update sys_lock.last_vsys_task_id finshed. value: {}", vsysTask.getId());
        }
    }

    /**
     * apply config for task
     *
     * @param vsysTask
     */
    private void applyConfigForTask(VsysTask vsysTask) {
        try {
            Integer total = vsysTask.getTotal();
            if (total == 0) {
                log.warn("vsys task config warn, total = 0, No associated assets, task: {}", JSONUtil.toJsonStr(vsysTask));
                return;
            }

            StopWatch sw = StopWatch.create("vsys task");
            sw.start();
            log.info("vsys task config begin...");
            log.info("vsys task config info: {}", JSONUtil.toJsonStr(vsysTask));

            // NOT_EXEC 未执行状态日志
            List<VsysResult> resultList = vsysResultService.list(new LambdaQueryWrapper<VsysResult>().eq(VsysResult::getTaskId, vsysTask.getId()));
            List<Integer> vsysAssetIdList = resultList.stream().map(VsysResult::getAssetId).collect(Collectors.toList());

            Collection<AssetAsset> listByIds = assetService.listByIds(vsysAssetIdList);
            List<AssetAsset> vsysConfigAssetList = listByIds.stream().collect(Collectors.toList());
            if (Tool.CollUtil.isEmpty(vsysConfigAssetList)) {
                log.info("vsys task config not executed, not found rel assets");
                return;
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
