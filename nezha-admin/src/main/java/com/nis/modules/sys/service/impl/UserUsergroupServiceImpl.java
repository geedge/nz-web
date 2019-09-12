package com.nis.modules.sys.service.impl;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.common.utils.RCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;

import com.nis.modules.sys.dao.UserUsergroupDao;
import com.nis.modules.sys.entity.UserUsergroupEntity;
import com.nis.modules.sys.service.UserUsergroupService;
import com.nis.modules.sys.shiro.ShiroUtils;


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
        if(userGroupIds==null||userGroupIds.length<1){
            throw new NZException(RCode.SYS_USERGROUPLIST_ISNULL);
        }
        Integer count=this.getBaseMapper().checkUserGroupCorrect(userId,userGroupIds);
        return userGroupIds.length==count.intValue();
    }

	@Override
	public List<Long> queryByUserId(Long userId) {
		QueryWrapper<UserUsergroupEntity> wrapper = new QueryWrapper<>();
		wrapper.eq("user_id", userId);
		wrapper.eq("del_flag", Constant.DELFLAG_FALSE);
		List<UserUsergroupEntity> selectList = baseMapper.selectList(wrapper);
		List<Long> usergroupIds = new ArrayList<>();
		for (UserUsergroupEntity e : selectList) {
			usergroupIds.add(e.getGroupId());
		}
		return usergroupIds;
	}

	@Override
	public boolean saveOrUpdate(Long userId, List<Long> usergroupIds) {
		Date now = new Date();
		UpdateWrapper<UserUsergroupEntity> w = new UpdateWrapper<>();
		w.eq("del_flag", Constant.DELFLAG_FALSE);
		w.eq("user_id", userId);
		w.set("del_flag", Constant.DELFLAG_TRUE);
		w.set("operator", ShiroUtils.getUserId());
		w.set("op_time", now);
		this.update(w);
		
		if (!CommonUtil.isNull(usergroupIds)) {
			List<UserUsergroupEntity> ls = new ArrayList<>();
			for (Long ugId : usergroupIds) {
				UserUsergroupEntity u = new UserUsergroupEntity();
				u.setDelFlag(Constant.DELFLAG_FALSE);
				u.setGroupId(ugId);
				u.setUserId(userId);
				u.setOperator(ShiroUtils.getUserId());
				u.setOpTime(now);
				ls.add(u);
			}
			this.saveBatch(ls);
		}
		return true;
	}

}