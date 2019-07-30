package com.nis.modules.sys.service.impl;

import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;

import com.nis.modules.sys.dao.UserGroupDao;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.entity.UserGroupEntity;
import com.nis.modules.sys.entity.UserGroupEntity2;
import com.nis.modules.sys.service.UserGroupService;


@Service("userGroupService")
public class UserGroupServiceImpl extends ServiceImpl<UserGroupDao, UserGroupEntity> implements UserGroupService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
    	IPage<UserGroupEntity2> page = new Query<UserGroupEntity2>().getPage(params);
    	List<UserGroupEntity2> list = baseMapper.list(page);
    	String systemIdStr = (String) params.get("systemId");
    	if (list != null && list.size() > 0 && !StringUtils.isEmpty(systemIdStr)) {
    		Long systemId = Long.valueOf(systemIdStr);
    		Iterator<UserGroupEntity2> it = list.iterator();
    		while (it.hasNext()) {
    			UserGroupEntity2 ug = it.next();
    			List<SystemInfoEntity> systems = ug.getSystems();
    			if (systems != null && systems.size() > 0) {
    				boolean removeFlag = true;
    				for (SystemInfoEntity si : systems) {
    					if (si.getId() == systemId) {
    						removeFlag = false;
    						break;
    					}
    				}
    				if (removeFlag) {
    					it.remove();
    				}
    			} else {
    				it.remove();
    			}
    		}
    	}
    	page.setRecords(list);
        return new PageUtils(page);
    }

	@Override
	public List<UserGroupEntity> queryByUser(Long userId) {
		List<UserGroupEntity> queryByUser = baseMapper.queryByUser(userId);
		return queryByUser;
	}

	@Override
	public UserGroupEntity2 queryDetail(Long id) {
		UserGroupEntity2 entity = baseMapper.queryDetail(id);
		return entity;
	}

}