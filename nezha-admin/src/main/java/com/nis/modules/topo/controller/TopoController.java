package com.nis.modules.topo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.topo.entity.TopoInfoEntity;
import com.nis.modules.topo.service.TopoInfoService;

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
public class TopoController {
    @Autowired
    private TopoInfoService topoInfoService;

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
    public R detail(Long id){
    	ValidateUtils.is(id).notNull(RCode.NOT_NULL_ERROR);
    	TopoInfoEntity topoInfo = topoInfoService.queryTopoInfoById(id);
    	return R.ok().put("data", topoInfo);
    }
    
}
