package com.nis.modules.alert.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertMessageWithExport;
import com.nis.modules.metric.dto.MetricsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 
 * 
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
@Mapper
public interface AlertMessageDao extends BaseMapper<AlertMessageEntity> {

    void updateBatch(@Param("hashKeys") List<String> updateCacheKeys, @Param("nowDate") Date utcTimeByConfigTimeZone);

    List<AlertMessageEntity> list(IPage<AlertMessageEntity> page,@Param("params") Map<String, Object> params);

    List<AlertMessageWithExport> selectWithExport(@Param("params") Map<String, Object> params);

    void deleteByAssetIds(@Param("ids")List<Integer> idList);

    void deleteByModuleIds(@Param("ids")List ids);

    void deleteByProjectIds(@Param("ids")List ids);

    void updateAlertStateByIds(@Param("ids")List<String> idList, @Param("params") Map<String, Object> params);

    List<MetricsDto> queryAlertMessageStatisticsInfoByRule();
}
