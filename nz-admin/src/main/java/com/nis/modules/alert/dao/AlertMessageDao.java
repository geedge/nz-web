package com.nis.modules.alert.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.metric.dto.MetricsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 
 * 
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
@Mapper
public interface AlertMessageDao extends BaseMapper<AlertMessageEntity> {

    // void updateBatch(@Param("hashKeys") List<String> updateCacheKeys, @Param("nowDate") Date utcTimeByConfigTimeZone);

    // List<AlertMessageEntity> list(IPage<AlertMessageEntity> page,@Param("params") Map<String, Object> params);

    void deleteByAssetIds(@Param("ids")List<Integer> idList);

    void deleteByModuleIds(@Param("ids")List ids);

    void deleteByProjectIds(@Param("ids")List ids);

    void deleteByEndpointIds(@Param("ids")List ids);

    void deleteByDCIds(@Param("ids")List ids);

    // void updateAlertStateByIds(@Param("ids")List<String> idList, @Param("params") Map<String, Object> params);

    List<MetricsDto> queryAlertMessageStatisticsInfoByRule();

    // 只返回用于统计的列  severity rule_id  project_id module_id asset_id dc_id
    List<AlertMessageEntity> getAlertsStatColumn();

    // void updateStateBySilence(AlertSilenceConf alertSilenceConf);

    @Select("SELECT COUNT(1) FROM ALERT_MESSAGE_ACTIVE WHERE module_id = #{moduleId};")
    Integer getAactiveAlertCountByModuleId(@Param("moduleId")Integer moduleId);

    @Select("SELECT COUNT(1) FROM ALERT_MESSAGE_ACTIVE WHERE project_id = #{projectId};")
    Integer getAactiveAlertCountByProjectId(@Param("projectId")Integer projectId);
}
