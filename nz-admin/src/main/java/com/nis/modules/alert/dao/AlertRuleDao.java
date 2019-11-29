package com.nis.modules.alert.dao;

import com.nis.modules.alert.entity.AlertNameMapUserDTO;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;

/**
 * 
 * 
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
@Mapper
public interface AlertRuleDao extends BaseMapper<AlertRuleEntity> {

	List<AlertNameMapUserDTO> queryUserByAlertName(Set<String> alertNames);
	
}
