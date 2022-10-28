package com.nis.modules.alert.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.metric.dto.MetricsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
public interface AlertRuleDao extends BaseMapper<AlertRuleEntity> {

    List<AlertRuleEntity> queryList(IPage page, @Param("params") Map<String, Object> params, @Param("receivers")List<String> receivers);

    // nz_alert_nums 指标 / 根据告警规则 id 统计激活状态的告警消息数量
    List<MetricsDto> nzAlertMumsMetric();

    void delNotify(List<String> idList);
}
