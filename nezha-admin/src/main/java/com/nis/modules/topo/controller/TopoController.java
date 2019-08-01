package com.nis.modules.topo.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.controller.AbstractController;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.topo.entity.TopoInfoEntity;
import com.nis.modules.topo.entity.TopoLineInfoEntity;
import com.nis.modules.topo.entity.TopoNodeInfoEntity;
import com.nis.modules.topo.service.TopoInfoService;
import com.nis.modules.topo.service.TopoLineInfoService;
import com.nis.modules.topo.service.TopoNodeInfoService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;



/**
 * 
 * @ClassName:TopoController
 * @Description:拓扑图模块
 * @author hyx
 * @date 2019年7月24日
 */
@Api(tags="拓扑图模块")
@RestController
@RequestMapping("/topo")
public class TopoController extends AbstractController{
    @Autowired
    private TopoInfoService topoInfoService;
    @Autowired
    private TopoNodeInfoService topoNodeInfoService;
    @Autowired
    private TopoLineInfoService topoLineInfoService;

    /**
     * 
    * @Title: list 
    * @Description: 拓扑图列表查询
    * @param @param params   map封装对应的条件信息  
    * @param @return    设定文件
    * @return R    返回类型
    * @throws
     */
    @ApiOperation(httpMethod="GET", response = TopoInfoEntity.class, value="拓扑图列表查询", notes="拓扑图列表查询")
    @ApiImplicitParams({
        @ApiImplicitParam(paramType="query", name = "id", value = "拓扑图id", required = false, dataType = "long", defaultValue=""),
        @ApiImplicitParam(paramType="query", name = "systemId", value = "业务系统id", required = false, dataType = "long", defaultValue="")
    })
    @RequestMapping("/list")
    public R list(Long id,Long systemId){
    	//global业务系统，systemId的值来自请求参数，普通业务系统systemId的值，则后台获取
    	if(!ShiroUtils.isGlobal()) {
    		systemId = ShiroUtils.getCurrSystemId();
    	}

    	Map<String, Object> params=new HashMap<String, Object>();
    	params.put("id", id);
    	params.put("systemId", systemId);
    	List<TopoInfoEntity> topoInfoList = topoInfoService.queryTopoInfoList(params);
	    Map<String, List<TopoInfoEntity>> map = new HashMap<String, List<TopoInfoEntity>>();
	    map.put("list", topoInfoList);
	    return R.ok().put("data", map);
    }
    
    /**
     * 
     * @Title: detail 
     * @Description: 拓扑图详情查询
     * @param @param params   map封装对应的条件信息  
     * @param @return    设定文件
     * @return R    返回类型
     * @throws
     */
    @ApiOperation(httpMethod="GET", response = TopoInfoEntity.class, value="拓扑图详情查询", notes="拓扑图详情查询，查出所有点、线")
    @ApiImplicitParams({
    	@ApiImplicitParam(paramType="query", name = "id", value = "拓扑图id", required = true, dataType = "long", defaultValue="")
    })
    @RequestMapping("/detail")
    public R detail(@RequestParam("id") Long id){
    	ValidateUtils.is(id).notNull(RCode.NOT_NULL_ERROR);
    	TopoInfoEntity topoInfo = topoInfoService.queryTopoInfoById(id);
    	return R.ok().put("data", topoInfo);
    }
    
	@ApiOperation(httpMethod = "POST", value = "拓扑图节点新增")
	@RequestMapping("/saveNode")
//	@RequiresPermissions("topo:toponodeinfo:save")
	public R saveNode(@RequestBody TopoNodeInfoEntity topoNodeInfoEntity) {
	    topoNodeInfoEntity.setOperator(getUserId());
	    topoNodeInfoEntity.setOpTime(new Date());
	    topoNodeInfoService.save(topoNodeInfoEntity);
	    return R.ok();
	}
    
	@ApiOperation(httpMethod = "PUT", value = " 拓扑图节点修改")
	@RequestMapping("/updateNode")
//	@RequiresPermissions("topo:toponodeinfo:update")
	public R updateNode(@RequestBody TopoNodeInfoEntity topoNodeInfoEntity) {
		ValidateUtils.is(topoNodeInfoEntity.getId()).notNull(RCode.NOT_NULL_ERROR);
		topoNodeInfoService.updateById(topoNodeInfoEntity);
		return R.ok();
	}

	@ApiOperation(httpMethod = "DELETE", value = " 拓扑图节点删除")
    @RequestMapping("/deleteNode")
//    @RequiresPermissions("topo:toponodeinfo:delete")
    public R deleteNode(@RequestParam("id") Long id){
		ValidateUtils.is(id).notNull(RCode.NOT_NULL_ERROR);
    	topoNodeInfoService.delete(id);
        return R.ok();
    }
	
	@ApiOperation(httpMethod = "POST", value = " 拓扑图连线新增")
	@RequestMapping("/saveLine")
//	@RequiresPermissions("topo:topolineinfo:save")
	public R saveLine(@RequestBody TopoLineInfoEntity topoLineInfoEntity) {
		topoLineInfoEntity.setOperator(getUserId());
		topoLineInfoEntity.setOpTime(new Date());
		topoLineInfoService.save(topoLineInfoEntity);
		return R.ok();
	}
	
	@ApiOperation(httpMethod = "PUT", value = " 拓扑图连线修改")
	@RequestMapping("/updateLine")
//	@RequiresPermissions("topo:topolineinfo:update")
	public R updateLine(@RequestBody TopoLineInfoEntity topoLineInfoEntity) {
		ValidateUtils.is(topoLineInfoEntity.getId()).notNull(RCode.NOT_NULL_ERROR);
		topoLineInfoService.updateById(topoLineInfoEntity);
		return R.ok();
	}
	
	@ApiOperation(httpMethod = "DELETE", value = " 拓扑图连线删除")
	@RequestMapping("/deleteLine")
//    @RequiresPermissions("topo:topolineinfo:delete")
	public R deleteLine(@RequestParam("id") Long id){
		ValidateUtils.is(id).notNull(RCode.NOT_NULL_ERROR);
		topoLineInfoService.delete(id);
		return R.ok();
	}
	
	/**
     * 
    * @Title: list 
    * @Description: 拓扑节点静态图片列表查询
    * @return R    返回类型
    * @throws
     */
    @ApiOperation(httpMethod="GET", response = TopoInfoEntity.class, value="拓扑节点静态图片列表查询", notes="拓扑节点静态图片列表查询，查询sys_dict表获取所有节点图片")
    @RequestMapping("/imgList")
    public R imgList(){
    	List<SysDictEntity> topoInfoList = topoInfoService.queryNodeImgList();
	    Map<String, List<SysDictEntity>> map = new HashMap<String, List<SysDictEntity>>();
	    map.put("list", topoInfoList);
	    return R.ok().put("data", map);
    }
	
}
