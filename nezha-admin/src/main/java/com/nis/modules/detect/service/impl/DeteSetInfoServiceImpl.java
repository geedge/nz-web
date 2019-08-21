package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.detect.dao.DeteSetInfoDao;
import com.nis.modules.detect.entity.DeteSetInfoEntity;
import com.nis.modules.detect.entity.DeteSetNodeRelEntity;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import com.nis.modules.detect.entity.DeteWarnRelEntity;
import com.nis.modules.detect.service.*;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.entity.UserGroupEntity;
import com.nis.modules.sys.service.PermissionInfoService;
import com.nis.modules.sys.service.UserUsergroupService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Service("deteSetInfoService")
public class DeteSetInfoServiceImpl extends DeteCommonServiceImpl<DeteSetInfoDao, DeteSetInfoEntity> implements DeteSetInfoService {
    private Logger log= LoggerFactory.getLogger(DeteSetInfoServiceImpl.class);
    @Autowired
    private DeteSetNodeRelService deteSetNodeRelService;
    @Autowired
    private PermissionInfoService permissionInfoService;
    @Autowired
    private DeteTypeInfoService deteTypeInfoService;
    @Autowired
    private UserUsergroupService userUsergroupService;
    @Autowired
    private DeteWarnRelService warnRelService;
    @Autowired
    private DeteWarnInfoService deteWarnInfoService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<DeteSetInfoEntity> page = new Query<DeteSetInfoEntity>().getPage(params);
        String name=(String)params.get("name");
        String typeIdStr=(String)params.get("typeId");
        Long typeId=null;
        if(StringUtils.isNotBlank(typeIdStr)){
            typeId=Long.parseLong(typeIdStr);
        }
        Integer isGlobal=0; //是否是global业务系统 0：不是 1 是
        if(ShiroUtils.isGlobal()){
            isGlobal=1;
            page=this.getBaseMapper().queryList(page,name,typeId,isGlobal,null,null);
            log.info("global system query... size[{}]",page.getRecords()==null?null:page.getRecords().size());
        }else{
            Long userId = ShiroUtils.getUserId();
            Long currSystemId = ShiroUtils.getCurrSystemId();

            //view_level=2 系统内可见
            QueryWrapper<PermissionInfoEntity> permissionInfoVL2QueryWrapper = getPermissionInfoVL2QueryWrapper(Constant.PM_TYPE_DETESET,currSystemId);
            List<PermissionInfoEntity> permissionInfoVL2 = permissionInfoService.list(permissionInfoVL2QueryWrapper);
            PermissionInfoEntity sysPermissionInfo=permissionInfoVL2!=null&&permissionInfoVL2.size()>0?permissionInfoVL2.get(0):null;
            //view_level=1 用户组内可见
            QueryWrapper<PermissionInfoEntity> permissionInfoVL1QueryWrapper = getPermissionInfoVL1QueryWrapper(Constant.PM_TYPE_DETESET,userId);
            List<PermissionInfoEntity> permissionInfoVL1 = permissionInfoService.list(permissionInfoVL1QueryWrapper);

            page=this.getBaseMapper().queryList(page,name,typeId,isGlobal,sysPermissionInfo,permissionInfoVL1);
            log.info("#{} system query... size[{}]",currSystemId, page.getRecords()==null?null:page.getRecords().size());
        }

        List<DeteSetInfoEntity> list = page.getRecords();
        for(DeteSetInfoEntity setInfo:list){
            setViewer(setInfo);
        }
        return new PageUtils(page);
    }

    /**
    * @Description 查询当前用户可见的所有监测设置的id
    * @Author  rui
    * @Date   2019/8/19
    * @Param
    * @Return
    * @Exception
    */
    public List<Long> queryAllowedSetIds(){
        List<Long> allowedSetIds=null;
        Integer isGlobal=0; //是否是global业务系统 0：不是 1 是
        if(ShiroUtils.isGlobal()){
            isGlobal=1;
            allowedSetIds = this.getBaseMapper().queryAllowedIds(isGlobal, null, null);
        }else{
            Long userId = ShiroUtils.getUserId();
            Long currSystemId = ShiroUtils.getCurrSystemId();

            //view_level=2 系统内可见
            QueryWrapper<PermissionInfoEntity> permissionInfoVL2QueryWrapper = getPermissionInfoVL2QueryWrapper(Constant.PM_TYPE_DETESET,currSystemId);
            List<PermissionInfoEntity> permissionInfoVL2 = permissionInfoService.list(permissionInfoVL2QueryWrapper);
            PermissionInfoEntity sysPermissionInfo=permissionInfoVL2!=null&&permissionInfoVL2.size()>0?permissionInfoVL2.get(0):null;
            //view_level=1 用户组内可见
            QueryWrapper<PermissionInfoEntity> permissionInfoVL1QueryWrapper = getPermissionInfoVL1QueryWrapper(Constant.PM_TYPE_DETESET,userId);
            List<PermissionInfoEntity> permissionInfoVL1 = permissionInfoService.list(permissionInfoVL1QueryWrapper);
            allowedSetIds=this.getBaseMapper().queryAllowedIds(isGlobal,sysPermissionInfo,permissionInfoVL1);
        }
        return allowedSetIds;
    }

	@Override
	public List<DeteSetInfoEntity> queryByPermission(PermissionInfoEntity entity) {
		List<DeteSetInfoEntity> queryByPermission = baseMapper.queryByPermission(entity);
		return queryByPermission;
	}



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEntity(DeteSetInfoEntity deteSetInfo) {
        if(StringUtils.isBlank(deteSetInfo.getViewLevel())){
            deteSetInfo.setViewLevel(Constant.VIEWLEVEL_SYS);
        }
        Long userId = ShiroUtils.getUserId();
        Date date=new Date();
        deteSetInfo.setOperator(userId);
        deteSetInfo.setOpTime(date);
        //保存setInfo
        this.save(deteSetInfo);


        List<PermissionInfoEntity> permissionInfos=new ArrayList<PermissionInfoEntity>();
        //维护set_node_rel
        List<DeteSetNodeRelEntity> setNodeRels=buildRels(deteSetInfo, userId, date);
        deteSetNodeRelService.saveBatch(setNodeRels);

        //维护权限表 无限制则不维护权限表
        buildPermissionInfoRel(deteSetInfo, userId, date);
        log.info("save deteSetInfo success! setId[{}],viewLevel[{}],viewer[{}]",deteSetInfo.getId(),deteSetInfo.getViewLevel(),deteSetInfo.getViewerId());
    }




    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEntity(DeteSetInfoEntity deteSetInfo) {
        Long userId = ShiroUtils.getUserId();
        Date date=new Date();
        deteSetInfo.setOperator(userId);
        deteSetInfo.setOpTime(date);

        this.updateById(deteSetInfo);

        //删除以前已经建立的关系
        deteSetNodeRelService.deleteBySetId(deteSetInfo.getId());

        //添加新的关系
        List<DeteSetNodeRelEntity> setNodeRels = buildRels(deteSetInfo, userId, date);
        deteSetNodeRelService.saveBatch(setNodeRels);

        //viewLevel 不允许修改，viewer可以修改，用于用户组变化时
        DeteSetInfoEntity oldSet = this.getById(deteSetInfo.getId());
        deteSetInfo.setViewLevel(oldSet.getViewLevel());
        if(Constant.VIEWLEVEL_UG.equals(oldSet.getViewLevel())){
            //删除之前建立的权限关系
            UpdateWrapper<PermissionInfoEntity> permissionInfoUpdateWrapper = new UpdateWrapper<PermissionInfoEntity>();
            permissionInfoUpdateWrapper.set("operator",userId);
            permissionInfoUpdateWrapper.set("op_time",date);
            permissionInfoUpdateWrapper.set("del_flag",Constant.DELFLAG_TRUE);
            permissionInfoUpdateWrapper.eq("type",Constant.PM_TYPE_DETESET);
            permissionInfoUpdateWrapper.eq("type_id",deteSetInfo.getId());
            permissionInfoService.update(permissionInfoUpdateWrapper);
            //添加新的权限信息
            buildPermissionInfoRel(deteSetInfo,userId,date);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStateByIds(Long[] ids, String state) {
        Long userId = ShiroUtils.getUserId();
        Date date=new Date();
        //更新监测设置
        UpdateWrapper<DeteSetInfoEntity> setInfoUpdateWrapper = new UpdateWrapper<DeteSetInfoEntity>();
        setInfoUpdateWrapper.in("id",ids);
        setInfoUpdateWrapper.set("state",state);//状态，1：删除，2：停止，3：监控中
        setInfoUpdateWrapper.set("operator",userId);
        setInfoUpdateWrapper.set("op_time",date);
        this.update(setInfoUpdateWrapper);

        if("1".equals(state)){ //删除监测设置
            //删除setRel
            UpdateWrapper<DeteSetNodeRelEntity> setRelUpdateWrapper = new UpdateWrapper<DeteSetNodeRelEntity>();
            setRelUpdateWrapper.set("operator",userId);
            setRelUpdateWrapper.set("op_time",date);
            setRelUpdateWrapper.set("del_flag", Constant.DELFLAG_TRUE);
            setRelUpdateWrapper.in("set_id",ids);
            deteSetNodeRelService.update(setRelUpdateWrapper);
            //删除权限信息
            UpdateWrapper<PermissionInfoEntity> permissionUpdateWrapper = new UpdateWrapper<>();
            permissionUpdateWrapper.set("operator",userId);
            permissionUpdateWrapper.set("op_time",date);
            permissionUpdateWrapper.set("del_flag",Constant.DELFLAG_TRUE);
            permissionUpdateWrapper.eq("type",Constant.PM_TYPE_DETESET);//1、dete_type,2、dete_set,3、dete_warn,4、mission
            permissionUpdateWrapper.in("type_id",ids);
            permissionInfoService.update(permissionUpdateWrapper);

            //删除告警信息
            QueryWrapper<DeteWarnRelEntity> deteWarnRelQueryWrapper = new QueryWrapper<>();
            deteWarnRelQueryWrapper.eq("rel_type",Constant.WARN_REL_SET);
            deteWarnRelQueryWrapper.in("rel_id",ids);

            List<DeteWarnRelEntity> deteWarnRels = warnRelService.list(deteWarnRelQueryWrapper);
            if(deteWarnRels!=null&&deteWarnRels.size()>0){
                Long[] warnIds=new Long[deteWarnRels.size()];
                for(int i=0;i<deteWarnRels.size();i++){
                    DeteWarnRelEntity warnRelEntity=deteWarnRels.get(i);
                    warnIds[i]=warnRelEntity.getWarnId();
                }
                deteWarnInfoService.deleteEntity(warnIds);
            }

        }

        log.info("change deteSetInfo state to [{}],is delete [{}]",state,"1".equals(state));
    }
    @Transactional(rollbackFor = Exception.class)
    public void deleteByTypeId(Long typeId){
        List<DeteSetInfoEntity> setInfos = this.list(new QueryWrapper<DeteSetInfoEntity>().eq("type_id", typeId));
        Long[] setIds=new Long[setInfos.size()];
        for(int i=0;i<setInfos.size();i++){
            DeteSetInfoEntity deteSetInfoEntity = setInfos.get(i);
            setIds[i]=deteSetInfoEntity.getId();
        }
        this.changeStateByIds(setIds,Constant.DELFLAG_TRUE );

    }

    @Override
    public DeteSetInfoEntity queryDetail(Long id) {
        QueryWrapper<DeteSetInfoEntity> setInfoQueryWrapper = new QueryWrapper<>();
        setInfoQueryWrapper.eq("id",id);
        setInfoQueryWrapper.ne("state",Constant.DELFLAG_TRUE);
        DeteSetInfoEntity deteSetInfoEntity = this.getBaseMapper().selectOne(setInfoQueryWrapper);
        if(deteSetInfoEntity!=null){
            DeteTypeInfoEntity deteTypeInfoEntity = deteTypeInfoService.queryDetail(deteSetInfoEntity.getTypeId());

            deteSetInfoEntity.setType(deteTypeInfoEntity);

            List<NodeGroupInfoEntity> nodeGroups=this.baseMapper.queryNodeGroups(id);

            List<NodeInfoEntity> nodes=this.baseMapper.queryNodes(id);

            deteSetInfoEntity.setNodeGroups(nodeGroups);
            deteSetInfoEntity.setNodes(nodes);

            setViewer(deteSetInfoEntity);
        }


        return deteSetInfoEntity;
    }

    @Override
    public boolean checkTypeExists(Long typeId) {
        DeteTypeInfoEntity typeInfo = deteTypeInfoService.getById(typeId);
        return typeInfo!=null;
    }

    private void setViewer(DeteSetInfoEntity deteSetInfoEntity) {
        if(Constant.VIEWLEVEL_UG.equals(deteSetInfoEntity.getViewLevel())){
            List<UserGroupEntity> userGroups=this.getBaseMapper().queryUserGroup(deteSetInfoEntity.getId());
            deteSetInfoEntity.setViewer(userGroups);
        }else if(Constant.VIEWLEVEL_SYS.equals(deteSetInfoEntity.getViewLevel())){
            List<SystemInfoEntity> system=this.getBaseMapper().querySystem(deteSetInfoEntity.getId());
            deteSetInfoEntity.setViewer(system);
        }
    }

    private  List<DeteSetNodeRelEntity> buildRels(DeteSetInfoEntity deteSetInfo, Long userId, Date date) {
        List<DeteSetNodeRelEntity> setNodeRels=new ArrayList<DeteSetNodeRelEntity>();
        for (Long ngId:deteSetInfo.getNodeGroupIds()){
            DeteSetNodeRelEntity deteSetNodeRelEntity = new DeteSetNodeRelEntity();
            deteSetNodeRelEntity.setOperator(userId);
            deteSetNodeRelEntity.setOpTime(date);
            deteSetNodeRelEntity.setType(Constant.DETESET_NODE_RELTYPE_NODEGROUP);//'1、node,2、nodeGroup',
            deteSetNodeRelEntity.setSetId(deteSetInfo.getId());
            deteSetNodeRelEntity.setRelId(ngId);
            setNodeRels.add(deteSetNodeRelEntity);


        }
        for (Long nodeId:deteSetInfo.getNodeIds()){
            DeteSetNodeRelEntity deteSetNodeRelEntity = new DeteSetNodeRelEntity();
            deteSetNodeRelEntity.setOperator(userId);
            deteSetNodeRelEntity.setOpTime(date);
            deteSetNodeRelEntity.setType(Constant.DETESET_NODE_RELTYPE_NODE);//'1、node,2、nodeGroup',
            deteSetNodeRelEntity.setSetId(deteSetInfo.getId());
            deteSetNodeRelEntity.setRelId(nodeId);

            setNodeRels.add(deteSetNodeRelEntity);
        }

        return setNodeRels;
    }
    private void buildPermissionInfoRel(DeteSetInfoEntity deteSetInfo, Long userId, Date date) {
        Long setId =deteSetInfo.getId() ;
        List<PermissionInfoEntity> permissionInfos =new ArrayList<PermissionInfoEntity>();
        switch (deteSetInfo.getViewLevel()){//1:用户组，2：系统内全部人员，3：无限制，默认2
            case Constant.VIEWLEVEL_UG:
                Long[] usergroupIds = deteSetInfo.getViewerId();
                for(Long userGroupId:usergroupIds){
                    PermissionInfoEntity permissionInfoEntity=new PermissionInfoEntity();
                    permissionInfoEntity.setType(Constant.PM_TYPE_DETESET);//'1、dete_type,2、dete_set,3、dete_warn,4、mission'
                    permissionInfoEntity.setTypeId(setId);
                    permissionInfoEntity.setViewLevel(deteSetInfo.getViewLevel());
                    permissionInfoEntity.setRelId(userGroupId);
                    permissionInfoEntity.setOperator(userId);
                    permissionInfoEntity.setOpTime(date);

                    permissionInfos.add(permissionInfoEntity);
                }
                break;
            case Constant.VIEWLEVEL_SYS:
                Long currSystemId = ShiroUtils.getCurrSystemId();
                PermissionInfoEntity permissionInfoEntity=new PermissionInfoEntity();
                permissionInfoEntity.setType(Constant.PM_TYPE_DETESET);//'1、dete_type,2、dete_set,3、dete_warn,4、mission'
                permissionInfoEntity.setTypeId(setId);
                permissionInfoEntity.setViewLevel(deteSetInfo.getViewLevel());
                permissionInfoEntity.setRelId(currSystemId);
                permissionInfoEntity.setOperator(userId);
                permissionInfoEntity.setOpTime(date);

                permissionInfos.add(permissionInfoEntity);
                break;
            case Constant.VIEWLEVEL_ALL:

                break;
        }
        if(permissionInfos.size()>0){
            permissionInfoService.saveBatch(permissionInfos);
        }
    }

}