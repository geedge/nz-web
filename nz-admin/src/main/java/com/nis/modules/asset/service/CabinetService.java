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

    Map getUInfo(Integer id);

    /**
     * 详情
     * @param id
     * @return
     */
    Cabinet queryCabinetDetails(Integer id);

    /**
     * 分页列表
     * @param params
     * @return
     */
    PageUtils queryCabinetPage(Map<String, Object> params);

    /**
     * 保存
     * @param cabinet
     * @return
     */
    Integer saveCabinet(Cabinet cabinet);

    /**
     * 修改
     * @param cabinet
     * @return
     */
    Integer updateCabinet(Cabinet cabinet);

    /**
     * 删除
     * @param ids
     */
    void deleteByIds(Integer... ids);

    /**
     * 机柜u位查询
     * @param id
     * @return
     */
    Map getCabinetUsizeDetails(Integer id);
}

