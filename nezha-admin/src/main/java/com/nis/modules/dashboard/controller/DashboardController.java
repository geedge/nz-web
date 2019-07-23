package com.nis.modules.dashboard.controller;

import java.util.List;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nis.common.utils.R;
import com.nis.modules.dashboard.entity.DashboardEntity;
import com.nis.modules.dashboard.service.DashboardService;
import com.nis.modules.sys.controller.AbstractController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Controller
@Api(tags="仪表盘相关")
@RequestMapping("/dashboard")
public class DashboardController extends AbstractController {
	@Autowired
	private DashboardService dashboardService;
	
	@GetMapping("/queryAllDashboards")
	@ResponseBody
	@ApiOperation(httpMethod="GET",value="查询所有仪表盘信息", notes="暂无任何条件")
	public R queryAllDashboards() {
		List<DashboardEntity> dashboards = dashboardService.queryAllDashboards();
		return R.ok().put("data", dashboards);
	}
	
	@DeleteMapping("/removeDashboard")
	@ApiImplicitParam(name = "id", value = "id值", required = false, dataType = "long", defaultValue="")
	@ApiOperation(httpMethod="DELETE",value="删除仪表盘信息", notes="仪表盘ID")
	@RequiresPermissions("dashboard:dashboard:delete")
	@ResponseBody
	public R removeDashboard(@RequestBody Long id) {
		dashboardService.removeDashboardById(id);
		return R.ok();
	}
	
	@PostMapping("/addDashboard")
	@ApiOperation(httpMethod="POST",value="新增仪表盘信息", notes="仪表盘各项属性")
	@RequiresPermissions("dashboard:dashboard:insert")
	@ResponseBody
	public R addDashboard(@RequestBody DashboardEntity dashboard) {
		dashboardService.addDashboard(dashboard);
		return R.ok();
	}
	
	@PutMapping("/modifyDashboard")
	@ApiOperation(httpMethod="PUT",value="修改仪表盘信息", notes="仪表盘各项属性")
	@RequiresPermissions("dashboard:dashboard:update")
	@ResponseBody
	public R modifyDashboard(@RequestBody DashboardEntity dashboard) {
		dashboardService.modifyDashboard(dashboard);
		return R.ok();
	}
	
}
