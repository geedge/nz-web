/**

 *

 *

 */

package com.nis.modules.sys.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.SysConfigEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统配置信息
 *

 */
@Mapper
public interface SysConfigDao extends BaseMapper<SysConfigEntity> {

	/**
	 * 根据key，查询value
	 */
	SysConfigEntity queryByKey(String paramKey);

	/**
	 * 根据key，更新value
	 */
	int updateValueByKey(@Param("paramKey") String paramKey, @Param("paramValue") String paramValue);

	/**
	 * 系统重置删除用户输入的内容
	 * @param tableName
	 */
	void clearData(@Param("sql") String sql);
}
