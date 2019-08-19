package com.nis.modules.sys.service.impl;

import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;

import com.nis.modules.sys.dao.UserUsergroupDao;
import com.nis.modules.sys.entity.UserUsergroupEntity;
import com.nis.modules.sys.service.UserUsergroupService;


@Service("userUsergroupService")
public class UserUsergroupServiceImpl extends ServiceImpl<UserUsergroupDao, UserUsergroupEntity> implements UserUsergroupService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<UserUsergroupEntity> page = this.page(
                new Query<UserUsergroupEntity>().getPage(params),
                new QueryWrapper<UserUsergroupEntity>()
        );

        return new PageUtils(page);
    }

    public boolean checkUserGroupCorrect(Long userId,Long[] userGroupIds){
        if(userGroupIds==null&&userGroupIds.length<1){
            throw new NZException(RCode.SYS_USERGROUPLIST_ISNULL);
        }
        Integer count=this.getBaseMapper().checkUserGroupCorrect(userId,userGroupIds);
        return userGroupIds.length==count.intValue();
    }

}