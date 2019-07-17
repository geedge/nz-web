/**

 *

 *

 */

package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysDictEntity;

import com.nis.common.utils.PageUtils;

import java.util.Map;

/**
 * 数据字典
 *

 */
public interface SysDictService extends IService<SysDictEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

