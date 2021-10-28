package com.nis.modules.stat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.stat.entity.StatAnalysis;
import com.nis.modules.stat.service.StatService;
import com.nis.modules.sys.controller.AbstractController;

@RestController
@RequestMapping("/stat")
public class StatController extends AbstractController {
	
	@Autowired
	private StatService statService;
	
	/**
	 * 用户统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/user/total")
	public R userTotal() {
		StatAnalysis result = statService.queryUserTotal();
		return R.ok(result);
	}
	
	/**
	 * asset 总数统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/total")
	public R assetTotal(@RequestParam Map<String,Object> params) {
		StatAnalysis result = statService.queryAssetTotal(params);
		return R.ok(result);
	}
	
	/**
	 * asset 按照数据中心统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/dc")
	public R assetDc(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryAssetDc(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * asset类型统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/type")
	public R assetType(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryAssetType(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * asset状态统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/state")
	public R assetState(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryAssetState(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * asset型号统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/model")
	public R assetModel(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryAssetModel(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * asset ping统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/ping")
	public R assetPing(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryAssetPing(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * asset field统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/field")
	public R assetField(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryAssetField(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * asset topN统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/topN")
	public R assetTopN(@RequestParam Map<String,Object> params) {
		ValidateUtils.is(params.get("dimension")).notNull(RCode.DIMENSION_IS_NULL);
		List<StatAnalysis> result = statService.queryAssetTopN(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * Project 总数统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/project/total")
	public R projectTotal() {
		StatAnalysis result = statService.queryProjectTotal();
		return R.ok(result);
	}
	
	/**
	 * Module 总数统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/module/total")
	public R moduleTotal(@RequestParam Map<String,Object> params) {
		StatAnalysis result = statService.queryModuleTotal(params);
		return R.ok(result);
	}
	
	/**
	 * Module topN统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/module/topN")
	public R moduleTopN(@RequestParam Map<String,Object> params) {
		ValidateUtils.is(params.get("dimension")).notNull(RCode.DIMENSION_IS_NULL);
		List<StatAnalysis> result = statService.queryModuleTopN(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * Endpoint 总数统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/endpoint/total")
	public R endpointTotal(@RequestParam Map<String,Object> params) {
		StatAnalysis result = statService.queryEndpointTotal(params);
		return R.ok(result);
	}
	
	/**
	 * endpoint类型统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/endpoint/type")
	public R endpointType(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryEndpointType(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * endpoint状态统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/endpoint/state")
	public R endpointState(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryEndpointState(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * 告警规则总数统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/alertRule/total")
	public R alertRuleTotal(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryAlertRuleTotal(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * Alert message 总数统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/alertMessage/total")
	public R alertMessageTotal(@RequestParam Map<String,Object> params) {
		StatAnalysis result = statService.queryAlertMessageTotal(params);
		return R.ok(result);
	}
	
	/**
	 * alert message状态统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/alertMessage/state")
	public R alertMessageState(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryAlertMessageState(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * alert message级别统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/alertMessage/severity")
	public R alertMessageSeverity(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryAlertMessageSeverity(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * alert message topN统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/alertMessage/topN")
	public R alertMessageTopN(@RequestParam Map<String,Object> params) {
		ValidateUtils.is(params.get("dimension")).notNull(RCode.DIMENSION_IS_NULL);
		List<StatAnalysis> result = statService.queryAlertMessageTopN(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * datacenter 总数统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/dc/total")
	public R dcTotal() {
		StatAnalysis result = statService.queryDcTotal();
		return R.ok(result);
	}
	
	/**
	 * datacenter 状态统计接口
	 * @return
	 */
	@GetMapping("/dc/state")
	public R dcState() {
		List<StatAnalysis> result = statService.queryDcState();
		return R.ok().putData("list", result);
	}
	
	/**
	 * agent 总数统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/agent/total")
	public R agentTotal(@RequestParam Map<String,Object> params) {
		StatAnalysis result = statService.queryAgentTotal(params);
		return R.ok(result);
	}
	
	/**
	 * agent状态统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/agent/state")
	public R agentState(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryAgentState(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * agent类型统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/agent/type")
	public R agentType(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryAgentType(params);
		return R.ok().putData("list", result);
	}
	
	/**
	 * asset 健康状态统计接口
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/healthy")
	public R assetHealthy(@RequestParam Map<String,Object> params) {
		StatAnalysis result = statService.queryAssetHealthy(params);
		return R.ok(result);
	}
	
	/**
	 * OVERVIEW map数据接口
	 * @param params
	 * @return
	 */
	@GetMapping("/overview/map")
	public R overviewMap() {
		List<Map> result = statService.queryOverviewMap();
		return R.ok().putData("list", result);
	}
	
	/**
	 * Module 异常数据统计
	 * @return
	 */
	@GetMapping("/module/abnormal")
	public R moduleAbnormal(@RequestParam Map<String,Object> params) {
		List<StatAnalysis> result = statService.queryModuleAbnormal(params);
		return R.ok().putData("list", result);
	}
	
	@GetMapping("/alertMessage/trend")
	public R alertMessageTrend(@RequestParam Map<String,Object> params) {
		Map<String,Object> result = statService.queryAlertMessageTrend(params);
		return R.ok(result);
	}
}
