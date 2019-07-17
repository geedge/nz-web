/**

 *

 *

 */

package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.SystemInfoEntity;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统用户
 *

 */
@Mapper
public interface SystemInfoDao extends BaseMapper<SystemInfoEntity> {
	
	/**
	 * 查询用户所有的业务系统
	 * @param userId  用户ID
	 */
	List<SystemInfoEntity> querySystemInfosByUserId(Long userId);


}
