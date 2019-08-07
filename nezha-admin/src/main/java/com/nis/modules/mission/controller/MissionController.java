package com.nis.modules.mission.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.mission.entity.MissionInfoEntity;
import com.nis.modules.mission.service.MissionInfoService;
import com.nis.modules.mission.service.MissionResultService;
import com.nis.modules.mission.utils.ApiJsonObject;
import com.nis.modules.mission.utils.ApiJsonProperty;
import com.nis.modules.sys.entity.SystemInfoEntity;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;



/**
 * 
 * @ClassName:MissionController
 * @Description:任务模块
 * @author th
 * @date 2019年7月18日
 */
@Api(tags="任务模块")
@RestController
@RequestMapping("mission")
public class MissionController {
    @Autowired
    private MissionInfoService missionInfoService;
    @Autowired
    private MissionResultService missionResultService;
    
    /**
     * 
    * @Title: list 
    * @Description: 任务列表查询
    * @param @param params   map封装对应的条件信息  
    * @param @return    设定文件
    * @return R    返回类型
    * @throws
     */
    @ApiOperation(httpMethod="GET", response = MissionInfoEntity.class, value="获取任务列表", notes="根据url的id来获取用户详细信息")
    @ApiImplicitParams({
        @ApiImplicitParam(paramType="query", name = "page", value = "页码，为空时默认为1", required = false, dataType = "int", defaultValue=""),
        @ApiImplicitParam(paramType="query", name = "limit", value = "每页条数,为空时默认为10", required = false, dataType = "int", defaultValue=""),
        @ApiImplicitParam(paramType="query", name = "sidx", value = "排序字段", required = false, dataType = "string", defaultValue=""),
        @ApiImplicitParam(paramType="query", name = "order", value = "排序方式，如：asc、desc", required = false, dataType = "string", defaultValue=""),
        @ApiImplicitParam(paramType="query", name = "isLoop", value = "是否周期任务 0：不是，1：是", required = false, dataType = "string", defaultValue=""),
        @ApiImplicitParam(paramType="query", name = "type", value = "任务类型 1：文件推送，2：命令执行，3：升级部署", required = false, dataType = "string", defaultValue=""),
        @ApiImplicitParam(paramType="query", name = "name", value = "任务名称", required = false, dataType = "string", defaultValue="")
    })
    @RequestMapping("/list")
   /* @RequiresPermissions("mission:missioninfo:list")*/
    public R list(Integer page,Integer limit,String sidx,String order,String isLoop,String type,String name){
        Page queryPage=new Page();
    	if(limit!=null) {
    		queryPage.setSize(limit);
    	}
        if(page!=null) {
        	queryPage.setCurrent(page);
        }
        MissionInfoEntity missionInfoEntity = new MissionInfoEntity();
        missionInfoEntity.setSidx(sidx);
        missionInfoEntity.setOrder(order);
        missionInfoEntity.setIsLoop(isLoop);
        missionInfoEntity.setType(type);
        missionInfoEntity.setName(name);
        
    	PageUtils Ipage = missionInfoService.queryPage(queryPage,missionInfoEntity);
        return R.ok().put("page", Ipage);
    }
    
    
    /**
     * 
    * @Title: detail   
    * @Description: 任务详情查询
    * @param @param id   任务id
    * @return R    返回类型
    * @throws
     */
    @ApiOperation(value="获取详情查询", notes="根据url的id来获取任务详细信息")
    @RequestMapping(value="/detail",method=RequestMethod.GET)
    //@RequiresPermissions("mission:missioninfo:list")
    public R detail(@RequestParam Integer id){
		ValidateUtils.check(id).notNull(RCode.MISSION_ID_ISNULL);
		
    	MissionInfoEntity missionInfoEntity = missionInfoService.getMissionById(id);
        return R.ok().put("data", missionInfoEntity);
    }
    
    /**
     * 任务结果查询
     * @param id
     * @return
     */
    @ApiOperation(value="获取任务结果", notes="根据url的id来获取任务结果")
    @RequestMapping(value="/result",method=RequestMethod.GET)
    public R result(Integer page,Integer limit,Integer id) {
    	ValidateUtils.check(id).notNull(RCode.MISSION_ID_ISNULL);
    	 Page queryPage=new Page();
     	if(limit!=null) {
     		queryPage.setSize(limit);
     	}
         if(page!=null) {
         	queryPage.setCurrent(page);
         }
        PageUtils missionResults = missionResultService.queryPage(queryPage,id);
        return R.ok().put("page", missionResults);
    }
    
    
    /**
     * 保存
     */
    @PostMapping("/save")
    /*@RequiresPermissions("mission:missioninfo:save")*/
    @ApiOperation(httpMethod="POST",value="新增任务", notes="任务各项属性")
    public R save(MissionInfoEntity missionInfo,MultipartFile[] files){
    	// 验证处理参数
    	missionInfoService.validateMissionInfoData(missionInfo,files);
		missionInfoService.saveMission(missionInfo);

        return R.ok();
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("mission:missioninfo:info")
    public R info(@PathVariable("id") Long id){
		MissionInfoEntity missionInfo = missionInfoService.getById(id);

        return R.ok().put("missionInfo", missionInfo);
    }

   

    /**
     * 任务仅能修改名称和备注  周期任务可以修改任务运行和暂停
     */
    @PutMapping("/update")
    /*@RequiresPermissions("mission:missioninfo:update")*/
    @ApiOperation(httpMethod="PUT",value="修改任务名称备注", notes="任务名称备注")
    public R update(@RequestBody MissionInfoEntity missionInfo){
    	ValidateUtils.is(missionInfo.getId()).notNull(RCode.MISSION_ID_ISNULL);
    	ValidateUtils.is(missionInfo.getName()).notNull(RCode.MISSION_NAME_ISNULL);
		missionInfoService.updateMission(missionInfo);
        return R.ok();
    }

    /**
     * 周期任务停用启用
     */
    @PutMapping("/loopStateChange")
    /*@RequiresPermissions("mission:missioninfo:update")*/
    @ApiOperation(httpMethod="PUT",value="修改仪表盘位置大小信息", notes="仪表盘各项属性")
    public R loopStateChange(@RequestBody MissionInfoEntity missionInfo){
    	ValidateUtils.is(missionInfo.getIds()).notNull(RCode.MISSION_ID_ISNULL);
    	ValidateUtils.is(missionInfo.getState()).notNull(RCode.MISSION_STATE_ISNULL);
    	//验证周期任务启用停用参数是否正确
    	List<MissionInfoEntity> updateMissions=missionInfoService.validateLoopMissionInfoData(missionInfo.getIds(),missionInfo.getState());
		missionInfoService.updateBatchById(updateMissions);
        return R.ok();
    }
    
}
