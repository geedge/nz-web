package com.nis.modules.mission.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.mission.entity.MissionResultEntity;

import java.util.Map;

/**
 * 任务结果表
 *
 * @author hkx
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
public interface MissionResultService extends IService<MissionResultEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

