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
import com.nis.modules.detectSet.entity.DeteSetNodeRelEntity;
import com.nis.modules.detectSet.service.DeteSetInfoService;
import com.nis.modules.detectSet.service.DeteSetNodeRelService;
import com.nis.modules.mission.entity.MissionInfoEntity;
import com.nis.modules.mission.service.MissionInfoService;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeGroupRelEntity;
import com.nis.modules.node.entity.SystemNodeRelEntity;
import com.nis.modules.node.service.NodeGroupInfoService;
import com.nis.modules.node.service.NodeGroupRelService;
import com.nis.modules.node.service.SystemNodeRelService;
import com.nis.modules.sys.dao.SystemInfoDao;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.entity.SystemUsergroupEntity;
import com.nis.modules.sys.service.PermissionInfoService;
import com.nis.modules.sys.service.SystemInfoService;
import com.nis.modules.sys.service.SystemUsergroupService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.topo.entity.TopoInfoEntity;
import com.nis.modules.topo.service.TopoInfoService;

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
	private NodeGroupRelService nodeGroupRelService;
	@Autowired
	private PermissionInfoService permissionService;
	@Autowired
	private DeteSetInfoService deteSetService;
	@Autowired
	private DeteSetNodeRelService deteSetNodeService;
	@Autowired
	private MissionInfoService missionService;
	@Autowired
	private TopoInfoService topoService;
	
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
		//新增拓扑图
		TopoInfoEntity topo = new TopoInfoEntity();
		topo.setName(systemInfo.getName());
		topo.setHeight(Constant.TOPO_DEFAULT_HEIGHT);
		topo.setWidth(Constant.TOPO_DEFAULT_WIDTH);
		topo.setOperator(ShiroUtils.getUserId());
		topo.setOpTime(systemInfo.getUpdateDate());
		topo.setSystemId(systemInfo.getId());
		topo.setStyle("");
		topoService.save(topo);
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
	public boolean delete(List<Long> ids) {
		/**解除与节点、用户组、节点组、监测设置、任务、拓扑的关联*/
		Date now = new Date();
		//等待删除的permission记录
		List<PermissionInfoEntity> waitingDeletePermissions = new ArrayList<>();
		//解除节点关联
		UpdateWrapper<SystemNodeRelEntity> sysNodeWrapper = new UpdateWrapper<>();
		sysNodeWrapper.in(true, "system_id", ids);
		sysNodeWrapper.eq(true, "del_flag", Constant.DELFLAG_FALSE);
		sysNodeWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
		sysNodeWrapper.set(true, "operator", ShiroUtils.getUserId());
		sysNodeWrapper.set(true, "op_time", now);
		sysNodeService.update(sysNodeWrapper);
		
		//解除用户组关联
		UpdateWrapper<SystemUsergroupEntity> sugWrapper = new UpdateWrapper<>();
		sugWrapper.in(true, "system_id", ids);
		sugWrapper.eq(true, "del_flag", Constant.DELFLAG_FALSE);
		sugWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
		sugWrapper.set(true, "operator", ShiroUtils.getUserId());
		sugWrapper.set(true, "op_time", now);
		sysUsergroupService.update(sugWrapper);
		
		//删除节点组
		UpdateWrapper<NodeGroupInfoEntity> nodegroupWrapper = new UpdateWrapper<>();
		nodegroupWrapper.in(true, "system_id", ids);
		nodegroupWrapper.eq(true, "del_flag", Constant.DELFLAG_FALSE);
		nodegroupWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
		nodegroupWrapper.set(true, "operator", ShiroUtils.getUserId());
		nodegroupWrapper.set(true, "op_time", now);
		nodegroupService.update(nodegroupWrapper);
		
		//删除拓扑
		UpdateWrapper<TopoInfoEntity> tpWrapper = new UpdateWrapper<>();
		tpWrapper.eq(true, "del_flag", Constant.DELFLAG_FALSE);
		tpWrapper.in(true, "system_id", ids);
		tpWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
		tpWrapper.set(true, "operator", ShiroUtils.getUserId());
		tpWrapper.set(true, "op_time", now);
		
		//删除监测设置，系统内可见的设置解除关联并删除，全体可见和组内可见的设置不处理
		PermissionInfoEntity pi = new PermissionInfoEntity();
		pi.setType(Constant.PM_TYPE_DETESET);
		pi.setRelIds(ids);
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
			
		//删除systemInfo
		UpdateWrapper<SystemInfoEntity> siWrapper = new UpdateWrapper<>();
		siWrapper.eq(true, "del_flag", Constant.DELFLAG_FALSE);
		siWrapper.in(true, "id", ids);
		siWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
		siWrapper.set(true, "update_by", ShiroUtils.getUserId());
		siWrapper.set(true, "update_date", now);
		update(siWrapper);
		
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveNodeRel(SystemInfoEntity entity) {
		Date now = new Date();
		List<Long> uuids = entity.getUuids();
		if (uuids.size() > 0) {
			//先查出已有的关联，用于去重
			List<SystemNodeRelEntity> oldSysNodeRel = sysNodeService.list(
					new QueryWrapper<SystemNodeRelEntity>()
					.eq(true, "del_flag", Constant.DELFLAG_FALSE)
					.eq(true, "system_id", entity.getId()));
			//保存sys-node
			List<SystemNodeRelEntity> toSaveSysNodeRel = new ArrayList<>();
			for (Long uuid : uuids) {
				if (!checkDuplicate(oldSysNodeRel, entity.getId(), uuid)) {
					continue;
				}
				
				SystemNodeRelEntity snr = new SystemNodeRelEntity();
				snr.setUuid(uuid);
				snr.setSystemId(entity.getId());
				snr.setOperator(ShiroUtils.getUserId());
				snr.setOpTime(now);
				toSaveSysNodeRel.add(snr);
			}
			
			if (toSaveSysNodeRel.size() > 0) {
				sysNodeService.saveBatch(toSaveSysNodeRel);
			}
		}
		return true;
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteNodeRel(SystemInfoEntity entity) {
		Date now = new Date();
		/*
		 * 一共更新三个表：system_node_rel、node_group_rel、dete_set_node_rel
		 * 即删除系统-节点、系统专属监测设置-节点、节点组-节点三个关联
		 */
		
		//system_node_rel
		UpdateWrapper<SystemNodeRelEntity> sysNodeWrapper = new UpdateWrapper<>();
		sysNodeWrapper.eq(true, "del_flag", Constant.DELFLAG_FALSE);
		sysNodeWrapper.eq(true, "system_id", entity.getId());
		sysNodeWrapper.in(true, "uuid", entity.getUuids());
		sysNodeWrapper.set(true, "del_flag", Constant.DELFLAG_TRUE);
		sysNodeService.update(sysNodeWrapper);
		
		//node_group_rel
		List<NodeGroupRelEntity> queryBySystemId = nodeGroupRelService.queryBySystemId(entity.getId());
		if (queryBySystemId != null && queryBySystemId.size() > 0) {
			for (NodeGroupRelEntity en : queryBySystemId) {
				if (!entity.getUuids().contains(en.getUuid())) {
					continue;
				}
				en.setDelFlag(Constant.DELFLAG_TRUE);
				en.setOperator(ShiroUtils.getUserId());
				en.setOpTime(now);
			}
			nodeGroupRelService.updateBatchById(queryBySystemId);
		}
		
		//dete_set_node_rel
		PermissionInfoEntity queryParam = new PermissionInfoEntity();
		queryParam.setRelId(entity.getId());
		queryParam.setType(Constant.PM_TYPE_DETESET);
		queryParam.setViewLevel(Constant.VIEWLEVEL_SYS);
		queryParam.setQueryColumn(Constant.DETESET_NODE_RELTYPE_NODE + "");
		queryParam.setUuids(entity.getUuids());
		List<DeteSetNodeRelEntity> queryBySystemId2 = deteSetNodeService.queryBySystemId(queryParam);
		if (queryBySystemId2 != null && queryBySystemId2.size() > 0) {
			for (DeteSetNodeRelEntity en : queryBySystemId2) {
				en.setDelFlag(Constant.DELFLAG_TRUE);
				en.setOperator(ShiroUtils.getUserId());
				en.setOpTime(now);
			}
			deteSetNodeService.updateBatchById(queryBySystemId2);
		}
		
		return true;
	}
	
	/**
	 * oldSysNodeRel-uuid查重，重复返回false，不重复返回true
	 * @param oldSysNodeRel
	 * @param uuid
	 * @return
	 */
	private boolean checkDuplicate(List<SystemNodeRelEntity> oldSysNodeRel, Long systemId, Long uuid) {
		boolean flag = true;
		for (SystemNodeRelEntity o : oldSysNodeRel) {
			if (o.getUuid() == uuid && o.getSystemId() == systemId) {
				flag = false;
				break;
			}
		}
		return flag;
	}
}
