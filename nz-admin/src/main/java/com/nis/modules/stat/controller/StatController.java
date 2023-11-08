package com.nis.modules.stat.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.stat.entity.StatAnalysis;
import com.nis.modules.stat.service.StatService;
import com.nis.modules.sys.controller.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stat")
public class StatController extends AbstractController {

	@Autowired
	private StatService statService;

	/**
	 * 统计接口
	 * 
	 * @return
	 */
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R queryAnalyse(@RequestParam Map<String, Object> params) {
		Map<String, Object> result = statService.queryAnalyse(params);
		return R.ok(result);
	}

	/**
	 * 用户统计接口
	 * 
	 * @return
	 */
	@GetMapping("/user/total")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R userTotal() {
		StatAnalysis result = statService.queryUserTotal();
		return R.ok(result);
	}

	/**
	 * asset 总数统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/total")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R assetTotal(@RequestParam Map<String, Object> params) {
		StatAnalysis result = statService.queryAssetTotal(params);
		return R.ok(result);
	}

	/**
	 * asset 按照数据中心统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/dc")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R assetDc(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryAssetDc(params);
		return R.ok().putData("list", result);
	}

	/**
	 * asset类型统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/type")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R assetType(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryAssetType(params);
		return R.ok().putData("list", result);
	}

	/**
	 * asset状态统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/state")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R assetState(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryAssetState(params);
		return R.ok().putData("list", result);
	}

	/**
	 * asset型号统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/model")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R assetModel(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryAssetModel(params);
		return R.ok().putData("list", result);
	}

	/**
	 * asset ping统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/ping")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R assetPing(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryAssetPing(params);
		return R.ok().putData("list", result);
	}

	/**
	 * asset field统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/field")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R assetField(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryAssetField(params);
		return R.ok().putData("list", result);
	}

	/**
	 * asset topN统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/topN")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R assetTopN(@RequestParam Map<String, Object> params) {
		ValidateUtils.is(params.get("dimension")).notNull(RCode.DIMENSION_IS_NULL);
		List<StatAnalysis> result = statService.queryAssetTopN(params);
		return R.ok().putData("list", result);
	}

	/**
	 * Project 总数统计接口
	 * 
	 * @return
	 */
	@GetMapping("/project/total")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R projectTotal() {
		StatAnalysis result = statService.queryProjectTotal();
		return R.ok(result);
	}

	/**
	 * Module 总数统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/module/total")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R moduleTotal(@RequestParam Map<String, Object> params) {
		StatAnalysis result = statService.queryModuleTotal(params);
		return R.ok(result);
	}

	/**
	 * Module topN统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/module/topN")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R moduleTopN(@RequestParam Map<String, Object> params) {
		ValidateUtils.is(params.get("dimension")).notNull(RCode.DIMENSION_IS_NULL);
		List<StatAnalysis> result = statService.queryModuleTopN(params);
		return R.ok().putData("list", result);
	}

	/**
	 * Endpoint 总数统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/endpoint/total")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R endpointTotal(@RequestParam Map<String, Object> params) {
		StatAnalysis result = statService.queryEndpointTotal(params);
		return R.ok(result);
	}

	/**
	 * endpoint类型统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/endpoint/type")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R endpointType(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryEndpointType(params);
		return R.ok().putData("list", result);
	}

	/**
	 * endpoint状态统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/endpoint/state")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R endpointState(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryEndpointState(params);
		return R.ok().putData("list", result);
	}

	/**
	 * 告警规则总数统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/alertRule/total")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R alertRuleTotal(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryAlertRuleTotal(params);
		return R.ok().putData("list", result);
	}

	/**
	 * Alert message 总数统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/alertMessage/total")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R alertMessageTotal(@RequestParam Map<String, Object> params) {
		StatAnalysis result = statService.queryAlertMessageTotal(params);
		return R.ok(result);
	}

	/**
	 * alert message状态统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/alertMessage/state")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R alertMessageState(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryAlertMessageState(params);
		return R.ok().putData("list", result);
	}

	/**
	 * alert message级别统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/alertMessage/severity")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R alertMessageSeverity(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryAlertMessageSeverity(params);
		return R.ok().putData("list", result);
	}

	/**
	 * alert message topN统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/alertMessage/topN")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R alertMessageTopN(@RequestParam Map<String, Object> params) {
		ValidateUtils.is(params.get("dimension")).notNull(RCode.DIMENSION_IS_NULL);
		List<StatAnalysis> result = statService.queryAlertMessageTopN(params);
		return R.ok().putData("list", result);
	}

	/**
	 * datacenter 总数统计接口
	 * 
	 * @return
	 */
	@GetMapping("/dc/total")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R dcTotal() {
		StatAnalysis result = statService.queryDcTotal();
		return R.ok(result);
	}

	/**
	 * datacenter 状态统计接口
	 * 
	 * @return
	 */
	@GetMapping("/dc/state")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R dcState() {
		List<StatAnalysis> result = statService.queryDcState();
		return R.ok().putData("list", result);
	}

	/**
	 * agent 总数统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/agent/total")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R agentTotal(@RequestParam Map<String, Object> params) {
		StatAnalysis result = statService.queryAgentTotal(params);
		return R.ok(result);
	}

	/**
	 * agent状态统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/agent/state")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R agentState(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryAgentState(params);
		return R.ok().putData("list", result);
	}

	/**
	 * agent类型统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/agent/type")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R agentType(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryAgentType(params);
		return R.ok().putData("list", result);
	}

	/**
	 * asset 健康状态统计接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/asset/healthy")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R assetHealthy(@RequestParam Map<String, Object> params) {
		StatAnalysis result = statService.queryAssetHealthy(params);
		return R.ok(result);
	}

	/**
	 * OVERVIEW map数据接口
	 *
	 * @return
	 */
	@GetMapping("/overview/map")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R overviewMap() {
		List<Map> result = statService.queryOverviewMap();
		return R.ok().putData("list", result);
	}

	/**
	 * Module 异常数据统计
	 * 
	 * @return
	 */
	@GetMapping("/module/abnormal")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R moduleAbnormal(@RequestParam Map<String, Object> params) {
		List<StatAnalysis> result = statService.queryModuleAbnormal(params);
		return R.ok().putData("list", result);
	}

	@GetMapping("/alertMessage/trend")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R alertMessageTrend(@RequestParam Map<String, Object> params) {
		Map<String, Object> result = statService.queryAlertMessageTrend(params);
		return R.ok(result);
	}

	/**
	 * 全局检索接口
	 * 
	 * @param params
	 * @return
	 */
	@GetMapping("/search")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R searchAll(@RequestParam Map<String, Object> params) {
		String scopeStr = Tool.MapUtil.get(params, "scope", String.class);
		List<String> scopeList = null;
		if (Tool.StrUtil.isBlank(scopeStr)) {
			scopeList = Constant.SEARCH_SCOPE;
		} else {
			String[] split = scopeStr.split(",");
			scopeList = Tool.ListUtil.list(false, split).stream()
					.filter(s -> Constant.SEARCH_SCOPE.contains(s.toLowerCase())).collect(Collectors.toList());
			scopeList.forEach(s -> s.toLowerCase());
		}
		params.put("scopeList", scopeList);
		// 查询符合条件的内容
		PageUtils page = statService.searchAll(params);
		// 按scope统计符合q 的数据
		List<Map<String, Object>> statList = statService.searchAllStatByType(params);
		Map<String, Object> statMap = Tool.MapUtil.newHashMap();
		statList.forEach(m -> {
			statMap.put(Tool.MapUtil.get(m, "type", String.class), Tool.MapUtil.get(m, "num", Integer.class));
		});
		Constant.SEARCH_SCOPE.forEach(sc -> {
			statMap.putIfAbsent(sc, 0);
		});
		return R.ok(page).put("stat", statMap);
	}

	/**
	 * rownum 查询接口
	 *
	 * @param table
	 * @param id
	 * @param order
	 * @param pageSize
	 * @return
	 */
	@GetMapping("/rownum")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R queryRowNum(@RequestParam(name = "table") String table, @RequestParam(name = "id") Integer id,
			@RequestParam(name = "order", defaultValue = "id") String order,
			@RequestParam(name = "pageSize", required = false) Integer pageSize) {
		pageSize = Tool.ObjectUtil.isNull(pageSize)? (int)Constant.PAGESIZE: pageSize;
		//获取真实 table 名称
		table = Constant.TABLE_MAPPING.get(table);
		ValidateUtils.check(table).notNull(RCode.SYSSTAT_TABLE_INVALIDED);
		//去除空白字符
		order = Tool.StrUtil.replaceIgnoreCase(order, " ", "");
		Map<String, Object> result = statService.queryRowNumById(table, id, order, pageSize);
		return R.ok(result);
	}

	/**
	 * 关系查询接口
	 *
	 * @return
	 */
	@GetMapping("/rel")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
	public R rel(@RequestParam(name = "type") String type,
	             @RequestParam(name = "id") Integer id,
	             @RequestParam(name = "level", defaultValue = "1") Integer level,
	             @RequestParam(name = "limit", required = false) Integer limit) {

		return R.ok().put("data", statService.rel(type, id, level, limit));
	}
}
