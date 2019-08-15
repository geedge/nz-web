/**

 *

 *

 */

package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.sys.entity.SystemInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

	/**
	 * 查询业务系统分页列表，可筛选用户组
	 * @param entity  systemInfoEntity
	 */
	List<SystemInfoEntity> list(IPage page, @Param("sys")SystemInfoEntity entity);
	
	/**
	 * 查询业务系统详情
	 * @param id
	 * @return
	 */
	SystemInfoEntity detail(Long id);

	/**
	 * 查询节点组使用
	 * @param id  业务系统ID
	 * @return
	 */
	List<SystemInfoEntity> querySystemInfosWithNodeGroup(@Param("id") Long id);
}
