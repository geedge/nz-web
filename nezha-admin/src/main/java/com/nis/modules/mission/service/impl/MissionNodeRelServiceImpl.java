package com.nis.modules.mission.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;

import com.nis.modules.mission.dao.MissionNodeRelDao;
import com.nis.modules.mission.entity.MissionNodeRelEntity;
import com.nis.modules.mission.service.MissionNodeRelService;


@Service("missionNodeRelService")
public class MissionNodeRelServiceImpl extends ServiceImpl<MissionNodeRelDao, MissionNodeRelEntity> implements MissionNodeRelService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MissionNodeRelEntity> page = this.page(
                new Query<MissionNodeRelEntity>().getPage(params),
                new QueryWrapper<MissionNodeRelEntity>()
        );

        return new PageUtils(page);
    }

}