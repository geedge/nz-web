package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.UserGroupRelEntity;

import java.util.Map;

/**
 * 
 *
 * @author song
 * @email 
 * @date 2019-11-28 18:07:30
 */
public interface UserGroupRelService extends IService<UserGroupRelEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

