package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.IdcTraffic;

import java.util.List;
import java.util.Map;

public interface IdcTrafficService extends IService<IdcTraffic> {

    PageUtils queryTrafficSetting(Map<String, Object> params);

    void validateTrafficInfo(Integer idcId, List<IdcTraffic> setting);

    void updateTraffic(Integer idcId, List<IdcTraffic> setting);
}

