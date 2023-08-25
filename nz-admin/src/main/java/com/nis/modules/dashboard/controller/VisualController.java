package com.nis.modules.dashboard.controller;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.dashboard.service.VisualService;
import com.nis.modules.dashboard.utils.DashboardConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/visual")
public class VisualController {

	private final static Log log = Log.get();

	@Autowired
	private VisualService visualService;

	@Autowired
	private RedisLock redisLock;

	/**
	 * explore 快照
	 */
	@GetMapping("/explore/snapshot")
	public void exploreSnapshot(HttpServletResponse response, HttpServletRequest request, String format, Integer type, Long start,
								Long end, Integer unit, String limit, String direction, @RequestParam(value="expressions[]")String[] expressions) throws IOException {
		ValidateUtils.is(type).notNull(RCode.AGENT_TYPE_ISNULL)
				.and(start).notNull(RCode.AGENT_PARAM_START_ISNULL)
				.and(end).notNull(RCode.AGENT_PARAM_END_ISNULL)
				.and(expressions).notNull(RCode.AGENT_PARAM_QUERY_ISNULL);
		String language = request.getHeader(Constant.LANGUAGE);
		visualService.exploreSnapshot(response, format, type, start, end, language, unit, limit, direction, expressions);
	}


	/**
	 * create snapshot task
	 *
	 * @param language
	 * @param requestody
	 * @return
	 */
	@PostMapping("/dashboard/snapshot/task")
	@SysLog(operation = OperationEnum.ADD, type = TypeEnum.DASHBOARD)
	public R snapshotTask(@RequestHeader(required = false, defaultValue = "en") String language,
	                      @RequestBody Map<String, Object> requestody) {
		String format = (String) requestody.get("format");
		format = Tool.StrUtil.emptyToDefault(format, "html");

		Object dashboardId = requestody.get("dashboardId");
		Object start = requestody.get("start");
		Object end = requestody.get("end");

		ValidateUtils.is(dashboardId).notNull(RCode.DASHBOARD_ID_ISNULL)
				.and(start).notNull(RCode.AGENT_PARAM_START_ISNULL)
				.and(end).notNull(RCode.AGENT_PARAM_END_ISNULL);

		Object varsObj = requestody.get("vars");
		Object tvarListObj = Tool.ObjectUtil.defaultIfNull(varsObj, Tool.ListUtil.empty());
		String varsJsonStr = Tool.JSONUtil.toJsonStr(tvarListObj);

		String taskId = visualService.createDashboardSnapshotTask(format, Integer.valueOf(dashboardId.toString()), Long.valueOf(start.toString()), Long.valueOf(end.toString()), varsJsonStr);

		return R.ok().putData("tid", taskId);
	}

	/**
	 * query snapshot task process
	 *
	 * @param tid
	 * @return
	 */
	@GetMapping("/dashboard/snapshot/result/{tid}")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.DASHBOARD)
	public R snapshotResultProcess(@PathVariable(required = true) String tid) {
		return R.ok().putData("taskId", visualService.querySnapshotResultProcess(DashboardConstant.DASHBOARD_SNAPSHOT_TYPE, tid));
	}

	/**
	 * cancel snapshot task
	 *
	 * @param tid
	 * @return
	 */
	@GetMapping("/dashboard/snapshot/cancel/{tid}")
	@SysLog(operation = OperationEnum.DELETE, type = TypeEnum.DASHBOARD)
	public R cancelSnapshotTask(@PathVariable(required = true) String tid) {
		visualService.cancelSnapshotTask(DashboardConstant.DASHBOARD_SNAPSHOT_TYPE, tid);
		return R.ok();
	}

	/**
	 * download snmapshot file
	 *
	 * @param response
	 * @param language
	 * @param tid
	 */
	@GetMapping("/dashboard/snapshot/download/{tid}")
	public void downloadSnapshotFile(HttpServletResponse response,
	                                 @RequestHeader(required = false, defaultValue = "en") String language,
	                                 @PathVariable(required = true) String tid) {
		visualService.downloadSnapshotFile(response, DashboardConstant.DASHBOARD_SNAPSHOT_TYPE, language, tid);
	}


	/**
	 * dashboard sync Tmpl
	 *
	 * @param requestBody
	 * @return
	 */
	@PostMapping("/dashboard/syncTmpl")
	@SysLog(operation = OperationEnum.ADD, type = TypeEnum.DASHBOARD)
	public R syncTemplateDashboard(@RequestBody(required = false) Map<String, Integer> requestBody) {
		// validate param
		requestBody = Tool.MapUtil.defaultIfEmpty(requestBody, Tool.MapUtil.empty());

		Integer id = requestBody.get("id");
		Integer tmplId = requestBody.get("tmplId");
		Integer modelId = requestBody.get("model");
		Integer moduleId = requestBody.get("module");

		if (Tool.ObjectUtil.isAllEmpty(id, tmplId, modelId, moduleId))
			return R.error(RCode.DASHBOARD_TEMPLATE_SYNC_PARAM_ISNULL);

		// get lock
		String uuid = Tool.StrUtil.uuid();
		try {
			if (redisLock.lock(DashboardConstant.TEMPLATE_SYNC_LOCK_KEY, uuid, 5L)) {
				log.info("[syncTemplateDashboard] [Get lock and start synchronization template]");

				// sync Template On dashboard
				visualService.syncTemplateDashboard(id, tmplId, modelId, moduleId);
				return R.ok();
			} else {
				log.warn("[syncTemplateDashboard] [Failed to get lock and do not perform template synchronization]");
				return R.error(RCode.DASHBOARD_TEMPLATE_SYNC_FAILED_GET_LOCK);
			}
		} finally {
			redisLock.unlock(DashboardConstant.TEMPLATE_SYNC_LOCK_KEY, uuid);
		}
	}

	/**
	 * create Dashboard Using Template
	 *
	 * @param requestBody
	 * @return
	 */
	@PostMapping("/dashboard/fromTmpl")
	@SysLog(operation = OperationEnum.ADD, type = TypeEnum.DASHBOARD)
	public R createDashboardUsingTemplate(@RequestBody(required = false) Map<String, Object> requestBody) {
		// validate param
		requestBody = Tool.MapUtil.defaultIfEmpty(requestBody, Tool.MapUtil.empty());

		String name = StrUtil.str(requestBody.get("name"), CharsetUtil.CHARSET_UTF_8);
		ValidateUtils.is(name).notNull(RCode.DASHBOARD_NAME_ISNULL);

		String type = StrUtil.str(requestBody.get("type"), CharsetUtil.CHARSET_UTF_8);
		if (!StrUtil.equals(DashboardConstant.Type.DASHBOARD.getValue(), type)) {
			throw new NZException(RCode.DASHBOARD_TYPE_INVALIDE);
		}

		String description = StrUtil.str(requestBody.get("description"), CharsetUtil.CHARSET_UTF_8);
		String tmplId = StrUtil.str(requestBody.get("tmplId"), CharsetUtil.CHARSET_UTF_8);
		String link = StrUtil.str(requestBody.get("link"), CharsetUtil.CHARSET_UTF_8);

		Integer tmplIdIntVal = ObjectUtil.isNull(tmplId) ? null : Integer.valueOf(tmplId);
		Integer linkIntVal = ObjectUtil.isNull(link) ? null : Integer.valueOf(link);

		// create Dashboard Using Template
		Integer id = visualService.createDashboardUsingTemplate(StrUtil.trim(name), type, description, tmplIdIntVal, linkIntVal);
		return R.ok().putData("id", id);
	}

}
