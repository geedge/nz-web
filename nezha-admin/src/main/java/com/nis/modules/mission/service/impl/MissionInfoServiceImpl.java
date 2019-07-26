package com.nis.modules.mission.service.impl;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;

import com.nis.modules.mission.dao.MissionInfoDao;
import com.nis.modules.mission.dao.MissionResultDao;
import com.nis.modules.mission.entity.MissionInfoEntity;
import com.nis.modules.mission.entity.MissionResultEntity;
import com.nis.modules.mission.service.MissionInfoService;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.shiro.ShiroUtils;

import net.bytebuddy.matcher.EqualityMatcher;


@Service("missionInfoService")
public class MissionInfoServiceImpl extends ServiceImpl<MissionInfoDao, MissionInfoEntity> implements MissionInfoService {
	@Autowired
	private MissionInfoDao missionInfoDao;
	@Autowired
	private MissionResultDao missionResultDao;
	@Autowired
	private SysUserDao sysUserDao;
	
	
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageUtils queryPage(Map<String, Object> params) {
    	String isLoop=(String) params.get("isLoop");//是否是周期任务
    	String type=(String) params.get("type");
    	String name=(String) params.get("name");
    	String disc=(String) params.get("disc");
    	String order=(String) params.get("order");
        IPage<MissionInfoEntity> page = this.page(
                new Query<MissionInfoEntity>().getPage(params),
                new QueryWrapper<MissionInfoEntity>()
                .eq(true, "del_Flag", "0")
                .eq(StringUtils.isNotBlank(isLoop), "is_Loop", isLoop)
                .eq(StringUtils.isNotBlank(type), "type", type)
                .eq(StringUtils.isNotBlank(name), "name", name)
                .orderBy(StringUtils.isNotBlank(disc), "asc".equals(order), disc)
                
        );

        //获取MissionInfoEntity 列表
        List<MissionInfoEntity> list=page.getRecords();
        //遍历查询任务执行情况
        for(MissionInfoEntity missionInfoEntity:list) {
        	//任务id下对应的各个节点任务的执行情况
        	List<MissionResultEntity> missionResultList=missionResultDao.selectList(
        		new QueryWrapper<MissionResultEntity>()
        		.eq("mission_id", missionInfoEntity.getId())
        	);
        	
        	//遍历 missionResultList   进行任务执行情况统计
        	int success=0;
        	int fail=0;
        	int timeOut=0;
        	for(MissionResultEntity missionResult:missionResultList) {
        		if("1".equals(missionResult.getState())) {
        			//成功
        			success++;
        		}else if("2".equals(missionResult.getState())) {
        			fail++;
        		}else if("3".equals(missionResult.getState())) {
        			timeOut++;
        		}
        		
        	}
        	
        	//String res=success+","+fail+","+timeOut;
        	Integer[] res = new Integer[3];
        	res[0]=success;
        	res[1]=fail;
        	res[2]=timeOut;
        	missionInfoEntity.setStateDesc(res);
        	
        	
        	//获取操作人所在组
        	if("1".equals(missionInfoEntity.getViewLevel())) {
        		//String iperateId=ShiroUtils.getUserId();//操作人id
        		
        	}
        	
        	
        	//根据用户id获取用户名
        	SysUserEntity sysUser=sysUserDao.selectById(missionInfoEntity.getOperator());
        	missionInfoEntity.setOperator(ShiroUtils.getUserId());
        	
        	
        }
        page.setRecords(list);
        return new PageUtils(page);
    }
	
   /* public PageUtils queryPageList(Map<String, Object> params) {
    	String isLoop=(String) params.get("isLoop");//是否是周期任务
    	String type=(String) params.get("type");
    	
    	QueryWrapper<MissionInfoEntity> wrapper = new QueryWrapper();
    	wrapper.eq(StringUtils.isNotBlank(isLoop), "isLoop", isLoop);
        wrapper.eq(StringUtils.isNotBlank(type), "type", type);
        IPage<MissionInfoEntity> page = this.page(
                new Query<MissionInfoEntity>().getPage(params),
                new QueryWrapper<MissionInfoEntity>()
                .eq(StringUtils.isNotBlank("isLoop"), "isLoop", isLoop)
                .eq(StringUtils.isNotBlank(type), "type", type)
        );
        IPage<MissionInfoEntity> page = this.page(
                new Query<MissionInfoEntity>().getPage(params));
         
        //return new PageUtils(missionInfoDao.queryPageMission(page, params));
        return new PageUtils(missionInfoDao.queryAllMission(wrapper,page));
    }*/
    
    
    /*public PageUtils queryPageList(Map<String, Object> params) {
      
        IPage<MissionInfoEntity> page = this.page(
                new Query<MissionInfoEntity>().getPage(params));
        
        return new PageUtils(missionInfoDao.queryPageMission(page,params));    
        Page page1=new Page<MissionInfoEntity>();
         
        return Pag(missionInfoDao.queryPageMission(page1,params));
        
    }*/
    
    public MissionInfoEntity getMissionById(Integer id) {
    	
    	MissionInfoEntity missionInfoEntity=this.getById(id);
    	
    	//任务id下对应的各个节点任务的执行情况
    	List<MissionResultEntity> missionResultList=missionResultDao.selectList(
    		new QueryWrapper<MissionResultEntity>()
    		.eq(true, "mission_id", missionInfoEntity.getId())
    	);
    	
    	//进行任务执行情况统计
    	int success=0;
    	int fail=0;
    	int timeOut=0;
    	for(MissionResultEntity missionResult:missionResultList) {
    		if("1".equals(missionResult.getState())) {
    			//成功
    			success++;
    		}else if("2".equals(missionResult.getState())) {
    			fail++;
    		}else if("3".equals(missionResult.getState())) {
    			timeOut++;
    		}
    		
    	}
    	
    	//String res=success+","+fail+","+timeOut;
    	Integer[] res = new Integer[3];
    	res[0]=success;
    	res[1]=fail;
    	res[2]=timeOut;
    	missionInfoEntity.setStateDesc(res);
    	
    	//查询节点组
    	
    	
    	
    	//查询节点
    	
    	
    	
    	//根据用户id获取用户名
    	SysUserEntity sysUser=sysUserDao.selectById(missionInfoEntity.getOperator());
    	missionInfoEntity.setOperator(ShiroUtils.getUserId());
    	
    	return missionInfoEntity;
    }

	@Override
	public List<MissionInfoEntity> queryByPermission(PermissionInfoEntity entity) {
		List<MissionInfoEntity> queryByPermission = baseMapper.queryByPermission(entity);
		return queryByPermission;
	}

}