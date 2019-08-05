/**

 *

 *

 */

package com.nis.modules.sys.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.SystemInfoEntity;


/**
 * 业务系统
 *
 */
public interface SystemInfoService extends IService<SystemInfoEntity> {

	List<SystemInfoEntity> querySystemInfosByUserId(Long userId);
	
	PageUtils queryPage(Map<String, Object> params);

	SystemInfoEntity queryDetail(Long id);

	boolean checkName(SystemInfoEntity systemInfo);

	boolean delete(List<Long> ids);

	boolean saveNodeRel(SystemInfoEntity entity);

	boolean deleteNodeRel(SystemInfoEntity entity);
}
