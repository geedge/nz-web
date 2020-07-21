package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.Idc;

import java.util.Map;

/**
 * 数据中心 service
 *
 * @author shizhendong
 * @date 2019-11-21
 */
public interface IdcService extends IService<Idc> {

    PageUtils queryPage(Map<String, Object> params);

    void saveOrUpdateIdc(Idc idc);

    void delete(Integer ... ids);
}

