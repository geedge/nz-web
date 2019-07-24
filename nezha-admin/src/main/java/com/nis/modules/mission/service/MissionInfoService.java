package com.nis.modules.mission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.mission.entity.MissionInfoEntity;

import java.util.Map;

/**
 * 任务信息表
 *
 * @author hkx
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
public interface MissionInfoService extends IService<MissionInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
    
    //PageUtils queryPageList(Map<String, Object> params);
    
    //根据任务id获取任务详情
    MissionInfoEntity getMissionById(Integer id);
}

