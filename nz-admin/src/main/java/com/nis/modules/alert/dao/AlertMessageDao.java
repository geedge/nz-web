package com.nis.modules.alert.dao;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.alert.entity.AlertMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
@Mapper
public interface AlertMessageDao extends BaseMapper<AlertMessageEntity> {

    void deleteByAssetIds(@Param("ids")List<Integer> idList);

    void deleteByModuleIds(@Param("ids")List ids);

    void deleteByProjectIds(@Param("ids")List ids);

    void deleteByEndpointIds(@Param("ids")List ids);

    void deleteByDCIds(@Param("ids")List ids);

    void deleteByRuleIds(@Param("ids") List ruleIds);

    // 只返回用于统计的列  severity rule_id  project_id module_id asset_id dc_id
    //List<AlertMessageEntity> getAlertsStatColumn();
    /**
     * 根据告警级别统计符合条件的激活状态告警数量
     * @param type 可选值：asset, dc, project, module, endpoint, rule
     * @param id
     * @return List<Map<String,Long>>
     */
    List<Map<String,Object>> getAlertsStatBySeverity(String type,Integer id);
    
    /**
     * 根据 cabinet id统计告警数量
     * @param id
     * @return
     */
    Integer getAlertNumByCabinetId(Integer id);
    
    void removeAllData();

    void cleanExpiredDataByTime(@Param("startCleanAlertMessageTime") DateTime startCleanAlertMessageTime);

}
