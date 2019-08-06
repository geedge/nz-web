package com.nis.modules.mission.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;

import com.nis.modules.mission.dao.MissionResultDao;
import com.nis.modules.mission.entity.MissionInfoEntity;
import com.nis.modules.mission.entity.MissionResultEntity;
import com.nis.modules.mission.service.MissionResultService;


@Service("missionResultService")
public class MissionResultServiceImpl extends ServiceImpl<MissionResultDao, MissionResultEntity> implements MissionResultService {

	@Override
	public PageUtils queryPage(Page page, Integer missionId) {
		Page<MissionResultEntity> missionResults = baseMapper.selectMissionResultInfoList(page, missionId);
		return new PageUtils(missionResults);
	}
}