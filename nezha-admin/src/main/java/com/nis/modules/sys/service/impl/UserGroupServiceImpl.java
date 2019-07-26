package com.nis.modules.sys.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;

import com.nis.modules.sys.dao.UserGroupDao;
import com.nis.modules.sys.entity.UserGroupEntity;
import com.nis.modules.sys.service.UserGroupService;


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

	@Override
	public List<UserGroupEntity> queryByUser(Long userId) {
		List<UserGroupEntity> queryByUser = baseMapper.queryByUser(userId);
		return queryByUser;
	}

}