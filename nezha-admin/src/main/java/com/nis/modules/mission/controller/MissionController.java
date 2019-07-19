package com.nis.modules.mission.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.mission.entity.MissionInfoEntity;
import com.nis.modules.mission.service.MissionInfoService;
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
 * @author hkx
 * @date 2019年7月18日
 */
@Api(tags="任务模块")
@RestController
@RequestMapping("mission")
public class MissionController {
    @Autowired
    private MissionInfoService missionInfoService;

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
    	//单独校验的方式：
		//如下面的写法就是校验usergroupId非空、page非空且最大值30
		/*try {
			ValidateUtils.check(params.get("usergroupId")).notNull(RCode.ERROR)
						.and(params.get("page")).notNull(RCode.ERROR)
						.and(Integer.parseInt((String) params.get("page"))).max(30, RCode.ERROR);
		} catch (ParamsException e) {
			return R.error(e.getMessage());
		}*/
        //PageUtils page = missionInfoService.queryPage(params);
    	
    	Map<String, Object> params=new HashMap<String, Object>();
    	/*params.put("page", "1");
    	params.put("limit", "10");*/
    	//ValidateUtils.check(page).notNull()
    	/*if(page!=null) {
    		params.put("page", page);
    	}
    	if(limit!=null) {
    		params.put("limit", limit);
    	}
    	if(StringUtils.isNotBlank(sidx)) {
    		params.put("sidx", sidx);
    	}
    	if(StringUtils.isNotBlank(order)) {
    		params.put("order", order);
    	}
    	if(StringUtils.isNotBlank(isLoop)) {
    		params.put("isLoop", isLoop);
    	}
    	if(StringUtils.isNotBlank(type)) {
    		params.put("type", type);
    	}
    	if(StringUtils.isNotBlank(name)) {
    		params.put("name", name);
    	}*/
    	/*params.put("page", page);
    	params.put("limit", limit);*/
    	params.put("sidx", sidx);
    	params.put("order", order);
    	params.put("isLoop", isLoop);
    	params.put("type", type);
    	params.put("name", name);
    	
    	
    	PageUtils Ipage = missionInfoService.queryPage(params);
    	//PageUtils Ipage = missionInfoService.queryPageList(params);
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
    @ApiOperation(value="获取详情查询", notes="根据url的id来获取用户详细信息")
    @RequestMapping(value="/detail",method=RequestMethod.GET)
    //@RequiresPermissions("mission:missioninfo:list")
    public R detail(@RequestParam Integer id){
    	//单独校验的方式：
		//如下面的写法就是校验usergroupId非空、page非空且最大值30
		ValidateUtils.check(id).notNull(RCode.ERROR);
		
        //PageUtils page = missionInfoService.queryPage(params);
    	MissionInfoEntity missionInfoEntity = missionInfoService.getMissionById(id);
        return R.ok().put("data", missionInfoEntity);
    }
    
    
    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("mission:missioninfo:save")
    public R save(@RequestBody MissionInfoEntity missionInfo){
    	
    	
		missionInfoService.save(missionInfo);

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
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("mission:missioninfo:update")
    public R update(@RequestBody MissionInfoEntity missionInfo){
		missionInfoService.updateById(missionInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("mission:missioninfo:delete")
    public R delete(@RequestBody Long[] ids){
		missionInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
