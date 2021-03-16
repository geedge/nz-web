package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.SysUserNotification;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserNotificationDao extends BaseMapper<SysUserNotification> {
	
	List<SysUserNotification> selectSysUserNotificationInfo();
	
}
