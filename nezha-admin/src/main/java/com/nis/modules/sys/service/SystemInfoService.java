/**

 *

 *

 */

package com.nis.modules.sys.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SystemInfoEntity;


/**
 * 业务系统
 *
 */
public interface SystemInfoService extends IService<SystemInfoEntity> {

	public List<SystemInfoEntity> querySystemInfosByUserId(Long userId);
}
