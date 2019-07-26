package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.UserGroupEntity;

import java.util.Map;

/**
 * 用户组信息表
 *
 * @author song
 * @email 
 * @date 2019-07-26 11:22:14
 */
public interface UserGroupService extends IService<UserGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

