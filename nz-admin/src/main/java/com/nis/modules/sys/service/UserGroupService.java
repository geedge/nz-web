package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.UserGroupEntity;

import java.util.Map;

/**
 * 
 *
 * @author song
 * @email 
 * @date 2019-11-28 18:07:30
 */
public interface UserGroupService extends IService<UserGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

