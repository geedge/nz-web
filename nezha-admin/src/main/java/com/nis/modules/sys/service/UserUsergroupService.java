package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.UserUsergroupEntity;

import java.util.Map;

/**
 * 
 *
 * @author song
 * @email 
 * @date 2019-08-01 14:56:46
 */
public interface UserUsergroupService extends IService<UserUsergroupEntity> {

    PageUtils queryPage(Map<String, Object> params);
    boolean checkUserGroupCorrect(Long userId,Long[] userGroupIds);
}

