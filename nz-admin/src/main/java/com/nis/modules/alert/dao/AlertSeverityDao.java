package com.nis.modules.alert.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.alert.entity.AlertSeverityConf;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AlertSeverityDao extends BaseMapper<AlertSeverityConf> {
    void saveWeightBatch(@Param("confs")List<AlertSeverityConf> alertSeverityConfs);

    List<AlertSeverityConf> queryList(IPage page, @Param("params") Map<String, Object> params);
}
