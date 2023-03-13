package com.nis.modules.alert.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.alert.entity.AlertSilenceConf;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AlertSilenceDao extends BaseMapper<AlertSilenceConf>{

	List<AlertSilenceConf> selectAlertSilenceConfs(IPage page,@Param("params")Map<String,Object> params);

	List<AlertSilenceConf> selectAlertSilenceMessageRel(IPage page, @Param("params") Map<String, Object> params, @Param("silenceId") Integer silenceId);
}
