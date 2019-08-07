package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import com.nis.modules.detect.service.DeteTypeInfoService;
import com.nis.modules.detect.dao.DeteSetInfoDao;
import com.nis.modules.detect.entity.DeteSetInfoEntity;
import com.nis.modules.detect.entity.DeteSetNodeRelEntity;
import com.nis.modules.detect.service.DeteSetInfoService;
import com.nis.modules.detect.service.DeteSetNodeRelService;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.nis.modules.sys.entity.UserGroupEntity;
import com.nis.modules.sys.service.PermissionInfoService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Service("deteSetInfoService")
public class DeteSetInfoServiceImpl extends ServiceImpl<DeteSetInfoDao, DeteSetInfoEntity> implements DeteSetInfoService {
    @Autowired
    private DeteSetNodeRelService deteSetNodeRelService;
    @Autowired
    private PermissionInfoService permissionInfoService;
    @Autowired
    private DeteTypeInfoService deteTypeInfoService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Integer curpage=1;
        Integer pageSize=10;
        try{
            curpage=Integer.parseInt((String)params.get("page"));
            pageSize=Integer.parseInt((String)params.get("limit"));
        }catch (Exception e){
            //do nothing
        }
        String name=(String)params.get("name");
        String typeIdStr=(String)params.get("typeId");
        String sidx=(String)params.get("sidx");
        String order=(String)params.get("order");
        Long typeId=null;
        if(StringUtils.isNotBlank(typeIdStr)){
            typeId=Long.parseLong(typeIdStr);
        }

        int start=(curpage-1)*pageSize;
        Long userId = ShiroUtils.getUserId();
        Long currSystemId = ShiroUtils.getCurrSystemId();
        List<DeteSetInfoEntity> list=this.getBaseMapper().queryList(name,typeId,start,pageSize,userId,currSystemId,sidx,order);
        int count =this.getBaseMapper().queryCount(name,typeId,start,pageSize,userId,currSystemId,sidx,order);
        return new PageUtils(list,count,pageSize,curpage);
    }

	@Override
	public List<DeteSetInfoEntity> queryByPermission(PermissionInfoEntity entity) {
		List<DeteSetInfoEntity> queryByPermission = baseMapper.queryByPermission(entity);
		return queryByPermission;
	}

    @Override
    public boolean checkNameExists(String name, Long setId) {
        List<DeteSetInfoEntity> sets = this.baseMapper.selectList(new QueryWrapper<DeteSetInfoEntity>().eq("name", name).ne(setId != null, "id", setId));

        return sets!=null&&sets.size()>0;
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

        Long setId=deteSetInfo.getId();

        List<PermissionInfoEntity> permissionInfos=new ArrayList<PermissionInfoEntity>();
        //维护set_node_rel
        List<DeteSetNodeRelEntity> setNodeRels=buildRels(deteSetInfo, userId, date);
        deteSetNodeRelService.saveBatch(setNodeRels);

        //维护权限表 无限制则不维护权限表
        switch (deteSetInfo.getViewLevel()){//1:用户组，2：系统内全部人员，3：无限制，默认2
            case Constant.VIEWLEVEL_UG:
                List<UserGroupEntity> usergroups = ShiroUtils.getUserEntity().getUsergroups();
                for(UserGroupEntity userGroup:usergroups){
                    PermissionInfoEntity permissionInfoEntity=new PermissionInfoEntity();
                    permissionInfoEntity.setType(Constant.PM_TYPE_DETESET);//'1、dete_type,2、dete_set,3、dete_warn,4、mission'
                    permissionInfoEntity.setTypeId(setId);
                    permissionInfoEntity.setViewLevel(deteSetInfo.getViewLevel());
                    permissionInfoEntity.setRelId(userGroup.getId());
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



    @Override
    public void updateEntity(DeteSetInfoEntity deteSetInfo) {
        DeteSetInfoEntity oldSet = this.getById(deteSetInfo.getId());
        Long userId = ShiroUtils.getUserId();
        Date date=new Date();
        deteSetInfo.setOperator(userId);
        deteSetInfo.setOpTime(date);
        if(oldSet!=null&&oldSet.getViewLevel()!=null){
            deteSetInfo.setViewLevel(oldSet.getViewLevel());
        }else{
            deteSetInfo.setViewLevel(Constant.VIEWLEVEL_SYS);
        }

        this.updateById(deteSetInfo);

        //删除以前已经建立的关系
        deteSetNodeRelService.deleteBySetId(deteSetInfo.getId());

        //添加新的关系
        List<DeteSetNodeRelEntity> setNodeRels = buildRels(deteSetInfo, userId, date);
        deteSetNodeRelService.saveBatch(setNodeRels);

    }

    @Override
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
        }


    }

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
        DeteSetInfoEntity deteSetInfoEntity = this.getBaseMapper().selectById(id);

        DeteTypeInfoEntity deteTypeInfoEntity = deteTypeInfoService.queryDetail(deteSetInfoEntity.getTypeId());

        deteSetInfoEntity.setType(deteTypeInfoEntity);

        List<NodeGroupInfoEntity> nodeGroups=this.baseMapper.queryNodeGroups(id);

        List<NodeInfoEntity> nodes=this.baseMapper.queryNodes(id);

        deteSetInfoEntity.setNodeGroups(nodeGroups);
        deteSetInfoEntity.setNodes(nodes);

        return deteSetInfoEntity;
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


}