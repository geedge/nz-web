package com.nis.modules.sys.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.detect.entity.DeteSetInfoEntity;
import com.nis.modules.detect.service.DeteSetInfoService;
import com.nis.modules.mission.entity.MissionInfoEntity;
import com.nis.modules.mission.service.MissionInfoService;
import com.nis.modules.sys.dao.UserGroupDao;
import com.nis.modules.sys.entity.*;
import com.nis.modules.sys.service.PermissionInfoService;
import com.nis.modules.sys.service.SystemUsergroupService;
import com.nis.modules.sys.service.UserGroupService;
import com.nis.modules.sys.service.UserUsergroupService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service("userGroupService")
public class UserGroupServiceImpl extends ServiceImpl<UserGroupDao, UserGroupEntity> implements UserGroupService {

	@Autowired
	private UserUsergroupService userUsergroupService;
	@Autowired
	private SystemUsergroupService systemUsergroupService;
	@Autowired
	private DeteSetInfoService deteSetService;
	@Autowired
	private MissionInfoService missionService;
	@Autowired
	private PermissionInfoService permissionService;
	
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
    	IPage<UserGroupEntity2> page = new Query<UserGroupEntity2>().getPage(params);
    	List<UserGroupEntity2> list = baseMapper.list(page);
    	String systemIdStr = (String) params.get("systemId");
    	Integer totalCount = null;
    	if (!CommonUtil.isNull(list) && !StringUtils.isEmpty(systemIdStr)) {
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
    		totalCount = list.size();
    	}
    	page.setRecords(list);
    	if (totalCount != null) {
    		page.setTotal(totalCount);
    	}
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

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(UserGroupEntity entity) {
		/*
		 * insert三个表：user_group、system_usergroup_rel、user_usergroup
		 * */
		Date now = new Date();
		entity.setOperator(ShiroUtils.getUserId());
		entity.setOpTime(now);
		baseMapper.insert(entity);
		
		List<Long> userIds = entity.getUserIds();
		if (!CommonUtil.isNull(userIds)) {
			List<UserUsergroupEntity> uugs = new ArrayList<>();
			for (Long userId : userIds) {
				UserUsergroupEntity uug = new UserUsergroupEntity();
				uug.setGroupId(entity.getId());
				uug.setOperator(ShiroUtils.getUserId());
				uug.setOpTime(now);
				uug.setUserId(userId);
				uugs.add(uug);
			}
			userUsergroupService.saveBatch(uugs);
		}
		
		List<Long> systemIds = entity.getSystemIds();
		if (!CommonUtil.isNull(systemIds)) {
			List<SystemUsergroupEntity> sugs = new ArrayList<>();
			for (Long systemId : systemIds) {
				SystemUsergroupEntity sug = new SystemUsergroupEntity();
				sug.setOperator(ShiroUtils.getUserId());
				sug.setOpTime(now);
				sug.setUsergroupId(entity.getId());
				sug.setSystemId(systemId);
				sugs.add(sug);
			}
			systemUsergroupService.saveBatch(sugs);
		}
		
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean update(UserGroupEntity userGroup) {
		Date now = new Date();
		
		UserGroupEntity2 oldEntity = baseMapper.queryDetail(userGroup.getId());
		
		userGroup.setOperator(ShiroUtils.getUserId());
		userGroup.setOpTime(now);
		baseMapper.updateById(userGroup);
		
		/*和system、user的关联，旧的全删，新的全加*/
		//删旧
		List<SystemInfoEntity> systems = oldEntity.getSystems();
		List<SysUserEntity> users = oldEntity.getUsers();
		List<Long> oldSystemIds = new ArrayList<>();
		List<Long> oldUserIds = new ArrayList<>();
		for (SystemInfoEntity system : systems) {
			oldSystemIds.add(system.getId());
		}
		for (SysUserEntity user : users) {
			oldUserIds.add(user.getUserId());
		}
		if (oldSystemIds.size() > 0) {
			UpdateWrapper<SystemUsergroupEntity> sugWrapper = new UpdateWrapper<>();
			sugWrapper.in(true, "system_id", oldSystemIds);
			sugWrapper.eq(true, "usergroup_id", userGroup.getId());
			sugWrapper.eq(true, "del_flag", Constant.DELFLAG_FALSE);
			sugWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
			systemUsergroupService.update(sugWrapper);
		}
		if (oldUserIds.size() > 0) {
			UpdateWrapper<UserUsergroupEntity> uugWrapper = new UpdateWrapper<>();
			uugWrapper.in(true, "user_id", oldUserIds);
			uugWrapper.eq(true, "group_id", userGroup.getId());
			uugWrapper.eq(true, "del_flag", Constant.DELFLAG_FALSE);
			uugWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
			userUsergroupService.update(uugWrapper);
		}
		//加新
		List<Long> systemIds = userGroup.getSystemIds();
		List<Long> userIds = userGroup.getUserIds();
		if (!CommonUtil.isNull(systemIds)) {
			List<SystemUsergroupEntity> newSugs = new ArrayList<>();
			for (Long systemId : systemIds) {
				SystemUsergroupEntity newSug = new SystemUsergroupEntity();
				newSug.setOperator(ShiroUtils.getUserId());
				newSug.setOpTime(now);
				newSug.setSystemId(systemId);
				newSug.setUsergroupId(userGroup.getId());
				newSugs.add(newSug);
			}
			systemUsergroupService.saveBatch(newSugs);
		}
		if (!CommonUtil.isNull(userIds)) {
			List<UserUsergroupEntity> newUugs = new ArrayList<>();
			for (Long userId : userIds) {
				UserUsergroupEntity newUug = new UserUsergroupEntity();
				newUug.setOperator(ShiroUtils.getUserId());
				newUug.setOpTime(now);
				newUug.setGroupId(userGroup.getId());
				newUug.setUserId(userId);
				newUugs.add(newUug);
			}
			userUsergroupService.saveBatch(newUugs);
		}
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean delete(List<Long> ids) {
		Date now = new Date();
		/*删除user_group、user_usergroup、system_usergroup_rel、permission_info(分为任务和监测设置)*/
		//待删除的permission
		List<PermissionInfoEntity> waitingDeletePermissions = new ArrayList<>();
		
		//删除user_group
		UpdateWrapper<UserGroupEntity> ugWrapper = new UpdateWrapper<>();
		ugWrapper.in(true, "id", ids);
		ugWrapper.eq(true, "del_flag", Constant.DELFLAG_FALSE);
		ugWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
		ugWrapper.set(true, "op_time", now);
		ugWrapper.set(true, "operator", ShiroUtils.getUserId());
		update(ugWrapper);
		
		//删除user_usergroup
		UpdateWrapper<UserUsergroupEntity> uugWrapper = new UpdateWrapper<>();
		uugWrapper.in(true, "group_id", ids);
		uugWrapper.eq(true, "del_flag", Constant.DELFLAG_FALSE);
		uugWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
		uugWrapper.set(true, "op_time", now);
		uugWrapper.set(true, "operator", ShiroUtils.getUserId());
		userUsergroupService.update(uugWrapper);
		
		//删除system_usergroup_rel
		UpdateWrapper<SystemUsergroupEntity> sugWrapper = new UpdateWrapper<>();
		sugWrapper.in(true, "usergroup_id", ids);
		sugWrapper.eq(true, "del_flag", Constant.DELFLAG_FALSE);
		sugWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
		sugWrapper.set(true, "op_time", now);
		sugWrapper.set(true, "operator", ShiroUtils.getUserId());
		systemUsergroupService.update(sugWrapper);
		
		//删除permission_info
		//--监测设置
		PermissionInfoEntity pi = new PermissionInfoEntity();
		pi.setType(Constant.PM_TYPE_DETESET);
		pi.setRelIds(ids);
		pi.setViewLevel(Constant.VIEWLEVEL_UG);
		List<DeteSetInfoEntity> dsByPermission = deteSetService.queryByPermission(pi);
		if (!CommonUtil.isNull(dsByPermission)) {
			for (DeteSetInfoEntity ds : dsByPermission) {
				PermissionInfoEntity waitingDelete = ds.getPermission();
				waitingDelete.setDelFlag(Constant.DELFLAG_TRUE);
				waitingDelete.setOperator(ShiroUtils.getUserId());
				waitingDelete.setOpTime(now);
				waitingDeletePermissions.add(waitingDelete);
			}
		}
		//--任务
		pi.setType(Constant.PM_TYPE_MISSION);
		List<MissionInfoEntity> msByPermission = missionService.queryByPermission(pi);
		if (!CommonUtil.isNull(msByPermission)) {
			for (MissionInfoEntity ds : msByPermission) {
				PermissionInfoEntity waitingDelete = ds.getPermission();
				waitingDelete.setDelFlag(Constant.DELFLAG_TRUE);
				waitingDelete.setOperator(ShiroUtils.getUserId());
				waitingDelete.setOpTime(now);
				waitingDeletePermissions.add(waitingDelete);
			}
		}
		//--删除permission
		if (!CommonUtil.isNull(waitingDeletePermissions)) {
			permissionService.updateBatchById(waitingDeletePermissions);
		}
		
		return true;
	}

	@Override
	public boolean checkName(UserGroupEntity entity) {
		List<UserGroupEntity> list = this.list(new QueryWrapper<UserGroupEntity>().eq(true, "name", entity.getName()).eq(true, "del_flag", Constant.DELFLAG_FALSE));
		boolean dumpFlag = true; //是否名称重复，true为重复
		
		//当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
		if (CommonUtil.isNull(list) ||
				(list != null && list.size() == 1 && list.get(0).getId() == entity.getId())) {
			dumpFlag = false;
		}
		return dumpFlag;
	}
}