package com.nis.modules.mission.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.mission.entity.MissionNodeRelEntity;

import java.util.Map;

/**
 * 任务节点关系表
 *
 * @author th
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
public interface MissionNodeRelService extends IService<MissionNodeRelEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

