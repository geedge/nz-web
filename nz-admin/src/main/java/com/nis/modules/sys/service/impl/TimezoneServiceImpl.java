package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.dao.TimezoneDao;
import com.nis.modules.sys.entity.Timezone;
import com.nis.modules.sys.service.TimezoneService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimezoneServiceImpl extends ServiceImpl<TimezoneDao, Timezone> implements TimezoneService{
    @Override
    public List<Timezone> queryTimezone(String name) {
        List<Timezone> list = this.list(new QueryWrapper<Timezone>().lambda().like(Tool.StrUtil.isNotBlank(name), Timezone::getName, Tool.StrUtil.likeEscape(name)));
        return list;
    }
}
