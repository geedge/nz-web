package com.nis.modules.vsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.vsys.entity.VsysTask;

public interface VsysTaskService extends IService<VsysTask> {

    /**
     * 创建任务接口
     *
     * @param task
     * @return
     * @throws Exception
     */
    String createTask(VsysTask task) throws Exception;

    /**
     * vsys 配置下发
     * @param vsysTask
     * @param asset
     */
    void vsysConfig(VsysTask vsysTask, AssetAsset asset) throws Exception;
}
