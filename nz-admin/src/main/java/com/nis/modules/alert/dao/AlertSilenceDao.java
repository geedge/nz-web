package com.nis.modules.alert.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertSilenceConf;

@Mapper
public interface AlertSilenceDao extends BaseMapper<AlertSilenceConf>{

	List<AlertSilenceConf> selectAlertSilenceConfs(IPage<AlertSilenceConf> page,@Param("params")Map<String,Object> params);

	void updateSilenceState( @Param("nowDate") Date date);
}
