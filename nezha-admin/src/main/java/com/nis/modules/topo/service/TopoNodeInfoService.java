/**

 *

 *

 */

package com.nis.modules.topo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.topo.entity.TopoNodeInfoEntity;

/**
 * 系统用户
 *

 */
public interface TopoNodeInfoService extends IService<TopoNodeInfoEntity> {
	void delete(Long id);

}
