package com.nis.modules.sys.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.detectSet.entity.DeteSetInfoEntity;
import com.nis.modules.detectSet.service.DeteSetInfoService;
import com.nis.modules.mission.entity.MissionInfoEntity;
import com.nis.modules.mission.service.MissionInfoService;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.SystemNodeRelEntity;
import com.nis.modules.node.service.NodeGroupInfoService;
import com.nis.modules.node.service.SystemNodeRelService;
import com.nis.modules.sys.dao.SystemInfoDao;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.entity.SystemUsergroupEntity;
import com.nis.modules.sys.service.PermissionInfoService;
import com.nis.modules.sys.service.SystemInfoService;
import com.nis.modules.sys.service.SystemUsergroupService;
import com.nis.modules.sys.shiro.ShiroUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 业务系统
 *
 */
@Service("systemInfoService")
public class SystemInfoServiceImpl extends ServiceImpl<SystemInfoDao, SystemInfoEntity> implements SystemInfoService {

	@Autowired
	private SystemUsergroupService sysUsergroupService;
	@Autowired
	private SystemNodeRelService sysNodeService;
	@Autowired
	private NodeGroupInfoService nodegroupService;
	@Autowired
	private PermissionInfoService permissionService;
	@Autowired
	private DeteSetInfoService deteSetService;
	@Autowired
	private MissionInfoService missionService;
	
	@Override
	public List<SystemInfoEntity> querySystemInfosByUserId(Long userId) {
		List<SystemInfoEntity> systemInfos = baseMapper.querySystemInfosByUserId(userId);
		return systemInfos;
	}

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String usergroupIdStr = (String)params.get("usergroupId");
		Long id = (Long)params.get("id");
		SystemInfoEntity se = new SystemInfoEntity();
		IPage<SystemInfoEntity> page = new Query<SystemInfoEntity>().getPage(params);
		if (!StringUtils.isEmpty(usergroupIdStr)) {
			se.setUsergroupId(Long.valueOf(usergroupIdStr));
		}
		if (id != null) {
			se.setId(id);
		}
		
		List<SystemInfoEntity> list = baseMapper.list(page, se);
		page.setRecords(list);
		return new PageUtils(page);
	}

	@Override
	public SystemInfoEntity queryDetail(Long id) {
		SystemInfoEntity detail = baseMapper.detail(id);
		return detail;
	}

	@Override
	public boolean checkName(SystemInfoEntity systemInfo) {
		List<SystemInfoEntity> list = this.list(new QueryWrapper<SystemInfoEntity>().eq(true, "name", systemInfo.getName()));
		boolean dumpFlag = true; //是否名称重复，true为重复
		
		//当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
		if ((list == null || list.size() == 0) ||
				(list != null && list.size() == 1 && list.get(0).getId() == systemInfo.getId())) {
			dumpFlag = false;
		}
		return dumpFlag;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public boolean save(SystemInfoEntity systemInfo) {
		//新增system
		baseMapper.insert(systemInfo);
		//新增system-usergroup记录
		List<Integer> usergroupIds = systemInfo.getUsergroupIds();
		if (usergroupIds != null && usergroupIds.size() > 0) {
			for (Integer uid : usergroupIds) {
				SystemUsergroupEntity su = new SystemUsergroupEntity();
				su.setSystemId(systemInfo.getId());
				su.setUsergroupId(Long.valueOf(uid.toString()));
				su.setOpTime(systemInfo.getUpdateDate());
				su.setOperator(systemInfo.getUpdateBy());
				sysUsergroupService.save(su);
			}
		}
		//TODO 新增拓扑图，暂时不写，等玉霞提交代码
		return true;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public boolean updateById(SystemInfoEntity systemInfo) {
		//修改system
		baseMapper.updateById(systemInfo);
		
		/**修改system-usergroup记录。
		 * 
		 * 1.旧记录为空、新纪录不为空时，新增新纪录；
		 * 2.旧记录新纪录都不为空时，删掉旧记录，新增新纪录
		 * 3.新旧记录都为空时，不处理
		 * 4.旧记录不为空，新纪录为空时，删掉旧记录
		 * 
		 * 即有旧记录就删掉，有新纪录就新增
		*/
		//删掉旧记录
		QueryWrapper<SystemUsergroupEntity> sugQuery = new QueryWrapper<SystemUsergroupEntity>();
		sugQuery.eq(true, "system_id", systemInfo.getId());
		sugQuery.eq(true, "del_flag", Constant.DELFLAG_FALSE);
		List<SystemUsergroupEntity> oldSystemUsergroups = sysUsergroupService.list(sugQuery);
		if (oldSystemUsergroups != null && oldSystemUsergroups.size() > 0) {
			for (SystemUsergroupEntity suEntity : oldSystemUsergroups) {
				suEntity.setDelFlag(Constant.DELFLAG_TRUE);
				suEntity.setOperator(systemInfo.getUpdateBy());
				suEntity.setOpTime(systemInfo.getUpdateDate());
			}
			sysUsergroupService.updateBatchById(oldSystemUsergroups);
		}
		//新增新纪录
		List<Integer> usergroupIds = systemInfo.getUsergroupIds();
		if (usergroupIds != null && usergroupIds.size() > 0) {
			for (Integer ugId : usergroupIds) {
				SystemUsergroupEntity su = new SystemUsergroupEntity();
				su.setUsergroupId(Long.valueOf(ugId.toString()));
				su.setOperator(systemInfo.getUpdateBy());
				su.setOpTime(systemInfo.getUpdateDate());
				su.setSystemId(systemInfo.getId());
				sysUsergroupService.save(su);
			}
		}
		return true;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public boolean delete(Long id) {
		/**解除与节点、用户组、节点组、监测设置、任务、拓扑的关联*/
		Date now = new Date();
		//等待删除的permission记录
		List<PermissionInfoEntity> waitingDeletePermissions = new ArrayList<>();
		//解除节点关联
		UpdateWrapper<SystemNodeRelEntity> sysNodeWrapper = new UpdateWrapper<>();
		sysNodeWrapper.eq(true, "system_id", id);
		sysNodeWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
		sysNodeWrapper.set(true, "operator", ShiroUtils.getUserId());
		sysNodeWrapper.set(true, "op_time", now);
		sysNodeService.update(sysNodeWrapper);
		
		//解除用户组
		//TODO
		
		//删除节点组
		UpdateWrapper<NodeGroupInfoEntity> nodegroupWrapper = new UpdateWrapper<>();
		nodegroupWrapper.eq(true, "system_id", id);
		nodegroupWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
		nodegroupWrapper.set(true, "operator", ShiroUtils.getUserId());
		nodegroupWrapper.set(true, "op_time", now);
		nodegroupService.update(nodegroupWrapper);
		
		//删除监测设置，系统内可见的设置解除关联并删除，全体可见和组内可见的设置不处理
		PermissionInfoEntity pi = new PermissionInfoEntity();
		pi.setType(Constant.PM_TYPE_DETESET);
		pi.setRelId(id);
		pi.setViewLevel(Constant.VIEWLEVEL_SYS);
		List<DeteSetInfoEntity> dsByPermission = deteSetService.queryByPermission(pi);
		if (dsByPermission != null && dsByPermission.size() > 0) {
			for (DeteSetInfoEntity ds : dsByPermission) {
				PermissionInfoEntity waitingDelete = ds.getPermission();
				waitingDelete.setDelFlag(Constant.DELFLAG_TRUE);
				waitingDelete.setOperator(ShiroUtils.getUserId());
				waitingDelete.setOpTime(now);
				waitingDeletePermissions.add(waitingDelete);
				
				ds.setState(Constant.DELFLAG_TRUE);
				ds.setOperator(ShiroUtils.getUserId());
				ds.setOpTime(now);
			}
			deteSetService.updateBatchById(dsByPermission);
		}
		
		//删除任务
		pi.setType(Constant.PM_TYPE_MISSION);
		List<MissionInfoEntity> msByPermission = missionService.queryByPermission(pi);
		if (msByPermission != null && msByPermission.size() > 0) {
			for (MissionInfoEntity ds : msByPermission) {
				PermissionInfoEntity waitingDelete = ds.getPermission();
				waitingDelete.setDelFlag(Constant.DELFLAG_TRUE);
				waitingDelete.setOperator(ShiroUtils.getUserId());
				waitingDelete.setOpTime(now);
				waitingDeletePermissions.add(waitingDelete);
				
				ds.setDelFlag(Constant.DELFLAG_TRUE);
				ds.setOperator(ShiroUtils.getUserId());
				ds.setOpTime(now);
			}
			missionService.updateBatchById(msByPermission);
		}
		
		//删除permission_info记录
		permissionService.updateBatchById(waitingDeletePermissions);
		
		return true;
	}
}
