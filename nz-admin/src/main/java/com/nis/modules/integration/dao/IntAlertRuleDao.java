package com.nis.modules.integration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.integration.entity.IntAlertRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface IntAlertRuleDao extends BaseMapper<IntAlertRuleEntity> {

    List<IntAlertRuleEntity> queryList(IPage page, @Param("params") Map params);
}
