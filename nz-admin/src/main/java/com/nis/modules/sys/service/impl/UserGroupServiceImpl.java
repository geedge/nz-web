package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.sys.dao.UserGroupDao;
import com.nis.modules.sys.entity.UserGroupEntity;
import com.nis.modules.sys.service.UserGroupService;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("userGroupService")
public class UserGroupServiceImpl extends ServiceImpl<UserGroupDao, UserGroupEntity> implements UserGroupService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<UserGroupEntity> page = this.page(
                new Query<UserGroupEntity>().getPage(params),
                new QueryWrapper<UserGroupEntity>()
        );

        return new PageUtils(page);
    }

}