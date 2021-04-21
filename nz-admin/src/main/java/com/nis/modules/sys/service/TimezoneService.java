package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.Timezone;

import java.util.List;

public interface TimezoneService extends IService<Timezone> {
    List<Timezone> queryTimezone(String name);
}
