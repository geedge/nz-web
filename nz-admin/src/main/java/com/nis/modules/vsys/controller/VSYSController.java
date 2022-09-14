package com.nis.modules.vsys.controller;

import cn.hutool.log.Log;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.vsys.entity.VsysAssetRel;
import com.nis.modules.vsys.entity.VsysTask;
import com.nis.modules.vsys.service.VsysAssetRelService;
import com.nis.modules.vsys.service.VsysResultService;
import com.nis.modules.vsys.service.VsysTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/vsys")
public class VSYSController {

    private static Log log = Log.get();

    @Autowired
    private VsysTaskService vsysTaskService;

    @Autowired
    private VsysResultService vsysResultService;

    @Autowired
    private VsysAssetRelService vsysAssetRelService;

    /**
     * vsys任务创建接口
     * CM 通过调用此接口下发vsys 任务
     *
     * @param task
     * @return
     */
    @PostMapping("/task")
    public R createTask(@RequestBody VsysTask task) {
        try {
            String taskId = vsysTaskService.createTask(task);
            return R.ok().putData("uuid", taskId);
        } catch (NZException e) {
            log.error("vsys create task error", e);
            throw e;
        } catch (Exception e) {
            log.error("vsys create task error", e);
            return R.error(RCode.VSYS_TASK_CREATE_ERROR);
        }
    }


    /**
     * vsys任务进度查询接口
     * CM 通过调用此接口查询vsys下发进度
     *
     * @param uuid
     * @return
     */
    @GetMapping("/result/{uuid}")
    public R queryTaskProgress(@PathVariable("uuid") String uuid) {
        ValidateUtils.is(uuid).notNull(RCode.VSYS_TASK_UUID_ISNULL);

        Map<Object, Object> progressMap = vsysResultService.queryTaskProgress(uuid);
        return R.ok(progressMap);
    }


    /**
     * asset vsys 查询接口
     * 查询asset当前生效vsys
     *
     * @param vsysId
     * @param assetId
     * @return
     */
    @GetMapping("/asset")
    public R queryAssetVsys(@RequestParam(required = false) Integer vsysId,
                            @RequestParam(required = false) Integer assetId) {

        List<VsysAssetRel> relList = vsysAssetRelService.queryAssetVsysRel(vsysId, assetId);
        return R.ok().putData("list", relList);
    }

}
