package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.sys.entity.SysI18nEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * i18n信息表
 * 
 * @author song
 * @email 
 * @date 2019-08-05 14:35:21
 */
@Mapper
public interface SysI18nDao extends BaseMapper<SysI18nEntity> {
	
	List<SysI18nEntity> selectSysI18nListWithPage(IPage<SysI18nEntity> page, @Param("params")Map<String, Object> params);
	
}
