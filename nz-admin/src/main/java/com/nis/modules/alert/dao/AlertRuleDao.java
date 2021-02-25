package com.nis.modules.alert.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleStat;
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

    List<AlertRuleEntity> queryList(IPage<AlertRuleEntity> page, @Param("params") Map<String, Object> params, @Param("receivers")List<String> receivers);

    /**
     * 根据 project 分组统计 此 rule 下告警消息数量
     * 返回列： id(project_id) 、 nums(告警消息计数)
     *
     * @param id
     * @return
     */
    List<AlertRuleStat> getRuleStatGooupByProject(@Param("ruleId") Integer id);

    /**
     * 根据 module 分组统计 此 rule 下告警消息数量
     * 返回列： id(module_id) 、 nums(告警消息计数)
     *
     * @param id
     * @return
     */
    List<AlertRuleStat> getRuleStatGooupByModule(@Param("ruleId") Integer id);

    /**
     * 根据 asset 分组统计 此 rule 下告警消息数量
     * 返回列： id(asset_id) 、 nums(告警消息计数)
     *
     * @param id
     * @return
     */
    List<AlertRuleStat> getRuleStatGooupByAsset(@Param("ruleId") Integer id);

    /**
     * 根据 endpoint 分组统计 此 rule 下告警消息数量
     * 返回列： id(endpoint_id) 、name(endpoint_id)、 nums(告警消息计数)
     *
     * @param id
     * @return
     */
    List<AlertRuleStat> getRuleStatGooupByEndpoint(@Param("ruleId") Integer id);

    // 获取用于统计的列 id alert_name severity
    List<AlertRuleEntity> getAlertRulesStatColumn();
}
