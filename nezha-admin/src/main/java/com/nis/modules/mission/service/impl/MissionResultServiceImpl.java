package com.nis.modules.mission.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;

import com.nis.modules.mission.dao.MissionResultDao;
import com.nis.modules.mission.entity.MissionResultEntity;
import com.nis.modules.mission.service.MissionResultService;


@Service("missionResultService")
public class MissionResultServiceImpl extends ServiceImpl<MissionResultDao, MissionResultEntity> implements MissionResultService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MissionResultEntity> page = this.page(
                new Query<MissionResultEntity>().getPage(params),
                new QueryWrapper<MissionResultEntity>()
        );

        return new PageUtils(page);
    }

}