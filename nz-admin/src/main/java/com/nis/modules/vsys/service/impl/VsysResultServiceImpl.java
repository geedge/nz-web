package com.nis.modules.vsys.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.vsys.dao.VsysResultDao;
import com.nis.modules.vsys.entity.VsysResult;
import com.nis.modules.vsys.entity.VsysTask;
import com.nis.modules.vsys.service.VsysResultService;
import com.nis.modules.vsys.service.VsysTaskService;
import com.nis.modules.vsys.utils.VsysConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VsysResultServiceImpl extends ServiceImpl<VsysResultDao, VsysResult> implements VsysResultService {

    private static final Log log = Log.get();

    @Autowired
    private VsysTaskService vsysTaskService;

    @Override
    public Map<Object, Object> queryTaskProgress(String uuid) {
        List<VsysTask> taskList = vsysTaskService.list(new LambdaUpdateWrapper<VsysTask>().eq(VsysTask::getTaskUuid, uuid));
        ValidateUtils.is(taskList).notNull(RCode.VSYS_TASK_NOT_FOUND);

        // 通过 task id 获取 result list
        List<Integer> taskIdList = taskList.stream().map(VsysTask::getId).collect(Collectors.toList());
        List<VsysResult> totalresultList = this.list(new LambdaUpdateWrapper<VsysResult>().in(VsysResult::getTaskId, taskIdList));

        // 总数量
        int totalNum = taskList.stream().mapToInt(VsysTask::getTotal).sum();
        long totalSuccess = totalresultList.stream().filter(result -> VsysConstant.ResultState.SUCCESS.getValue() == result.getState()).count();
        long totalFailed = totalresultList.stream().filter(result -> VsysConstant.ResultState.FAILURE.getValue() == result.getState()).count();

        // 总进度
        String totalProgress = NumberUtil.formatPercent((double) (totalSuccess + totalFailed) / totalNum, 2);

        // 根据 taskId 得到 vsysDetail, vsysId & type 并不能确保唯一
        List<Map> vsysDetail = Tool.ListUtil.list(true);
        Map<Integer, List<VsysResult>> resultListGroupByTaskId = totalresultList.stream().collect(Collectors.groupingBy(VsysResult::getTaskId));
        for (Map.Entry<Integer, List<VsysResult>> entry : resultListGroupByTaskId.entrySet()) {
            Integer taskId = entry.getKey();
            List<VsysResult> list = entry.getValue();

            VsysTask t = taskList.stream().filter(task -> task.getId().equals(taskId)).findFirst().get();

            Integer ttotal = t.getTotal();
            long successNum = list.stream().filter(result -> VsysConstant.ResultState.SUCCESS.getValue() == result.getState()).count();
            long failedNum = list.stream().filter(result -> VsysConstant.ResultState.FAILURE.getValue() == result.getState()).count();

            // 进度
            String percent = NumberUtil.formatPercent((double) (successNum + failedNum) / ttotal, 2);

            Map<Object, Object> m = Tool.MapUtil.builder()
                    .put("vsysId", t.getVsysId())
                    .put("type", t.getType())
                    .put("progress", percent.replace("%", ""))
                    .put("success", successNum)
                    .put("failed", failedNum)
                    .build();
            vsysDetail.add(m);
        }
        // total = 0
        taskList.stream().filter(task -> task.getTotal() == 0).forEach(task -> {
            Map<Object, Object> m = Tool.MapUtil.builder()
                    .put("vsysId", task.getVsysId())
                    .put("type", task.getType())
                    .put("progress", 100)
                    .put("success", 0)
                    .put("failed", 0)
                    .build();
            vsysDetail.add(m);
        });

        Map<Object, Object> proMap = Tool.MapUtil.builder()
                .put("totalProgress", totalProgress.replace("%", ""))
                .put("totalSuccess", totalSuccess)
                .put("totalFailed", totalFailed)
                .put("vsysDetail", vsysDetail)
                .build();
        return proMap;
    }

}
