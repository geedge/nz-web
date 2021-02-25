package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.sys.dao.UserGroupRelDao;
import com.nis.modules.sys.entity.UserGroupRelEntity;
import com.nis.modules.sys.service.UserGroupRelService;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("userGroupRelService")
public class UserGroupRelServiceImpl extends ServiceImpl<UserGroupRelDao, UserGroupRelEntity> implements UserGroupRelService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<UserGroupRelEntity> page = this.page(
                new Query<UserGroupRelEntity>().getPage(params),
                new QueryWrapper<UserGroupRelEntity>()
        );

        return new PageUtils(page);
    }

}