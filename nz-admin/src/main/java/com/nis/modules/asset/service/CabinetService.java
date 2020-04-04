package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.Cabinet;

import java.util.Map;

/**
 * 机柜 service
 *
 * @author shizhendong
 * @date 2019-11-21
 */
public interface CabinetService extends IService<Cabinet> {

    PageUtils queryPage(Map<String, Object> params);

    void saveOrUpdateCabinet(Cabinet cabinet);

    void delete(Integer ... ids);

    boolean cabinetNameCheck(Integer id, String name, Integer idcId);

    Map getUInfo(Integer id);
}

