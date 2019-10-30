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

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.dashboard.entity.ChartEntity;
import com.nis.modules.dashboard.entity.ChartSeriesEntity;
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
	
	@GetMapping("/list")
	@ResponseBody
	@ApiOperation(httpMethod="GET",value="查询所有仪表盘信息", notes="暂无任何条件")
	public R list() {
		List<DashboardEntity> dashboards = dashboardService.queryAllDashboards();
		return R.ok().put("data", dashboards);
	}
	
	@DeleteMapping("/delete")
	@ApiImplicitParam(name = "id", value = "id值", required = false, dataType = "long", defaultValue="")
	@ApiOperation(httpMethod="DELETE",value="删除仪表盘信息", notes="仪表盘ID")
	@RequiresPermissions("dashboard:dashboard:delete")
	@ResponseBody
	public R delete(@RequestParam Long id) {
		ValidateUtils.is(id).notNull(RCode.DASHBOARD_ID_NULL);
		dashboardService.removeDashboardById(id);
		return R.ok();
	}
	
	@PostMapping("/save")
	@ApiOperation(httpMethod="POST",value="新增仪表盘信息", notes="仪表盘各项属性")
	@RequiresPermissions("dashboard:dashboard:insert")
	@ResponseBody
	public R save(@RequestBody DashboardEntity dashboard) {
		// 验证用户传参数 是否能生成有效图形
		dashboardService.validateDashboardData(dashboard);
		dashboardService.addDashboard(dashboard);
		return R.ok();
	}
	
	@PutMapping("/update")
	@ApiOperation(httpMethod="PUT",value="修改仪表盘信息", notes="仪表盘各项属性")
	@RequiresPermissions("dashboard:dashboard:update")
	@ResponseBody
	public R update(@RequestBody DashboardEntity dashboard) {
		ValidateUtils.is(dashboard.getId()).notNull(RCode.DASHBOARD_ID_NULL);
		// 验证用户传参数 是否能生成有效图形
		dashboardService.validateDashboardData(dashboard);
		dashboardService.modifyDashboard(dashboard);
		return R.ok();
	}
	
	@PutMapping("/modifyShape")
	@ApiOperation(httpMethod="PUT",value="修改仪表盘位置大小信息", notes="仪表盘各项属性")
	@RequiresPermissions("dashboard:dashboard:update")
	@ResponseBody
	public R modifyShape(@RequestBody DashboardEntity dashboard) {
		ValidateUtils.is(dashboard.getId()).notNull(RCode.DASHBOARD_ID_NULL);
		ValidateUtils.is(dashboard.getX()).notNull(RCode.DASHBOARD_X_NULL);
		ValidateUtils.is(dashboard.getY()).notNull(RCode.DASHBOARD_Y_NULL);
		ValidateUtils.is(dashboard.getHeight()).notNull(RCode.DASHBOARD_HEIGHT_NULL);
		ValidateUtils.is(dashboard.getWidth()).notNull(RCode.DASHBOARD_WIDTH_NULL);
		dashboardService.modifyDashboard(dashboard);
		return R.ok();
	}
	
	@GetMapping("/queryData")
	@ResponseBody
	@ApiOperation(httpMethod="GET",value="根据用户sql查询信息", notes="仪表盘属性")
	public R queryData(String sql,String timeFormat,Integer chartType) {
		List<ChartSeriesEntity> queryDatas = dashboardService.queryData(sql,timeFormat,chartType);
		return R.ok().put("result", queryDatas);
	}
}
