package com.nis.modules.vsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.vsys.entity.VsysResult;

import java.util.Map;

public interface VsysResultService extends IService<VsysResult> {

    /**
     * 进度查询接口
     * @param uuid
     * @return
     */
    Map<Object, Object> queryTaskProgress(String uuid);

}
