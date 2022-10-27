package com.nis.modules.panel.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.panel.service.VisualService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/visual")
public class VisualController {

	@Autowired
	private VisualService visualService;

	/**
	 * explore 快照
	 */
	@GetMapping("/explore/snapshot")
	public void exploreSnapshot(HttpServletResponse response, HttpServletRequest request, String format, Integer type, Long start,
								Long end, Integer unit, String limit, @RequestParam(value="expressions[]")String[] expressions) throws IOException {
		ValidateUtils.is(type).notNull(RCode.AGENT_TYPE_ISNULL)
				.and(start).notNull(RCode.AGENT_PARAM_START_ISNULL)
				.and(end).notNull(RCode.AGENT_PARAM_END_ISNULL)
				.and(expressions).notNull(RCode.AGENT_PARAM_QUERY_ISNULL);
		String language = request.getHeader(Constant.LANGUAGE);
		visualService.exploreSnapshot(response, format, type, start, end, language, unit, limit, expressions);
	}


	/**
	 * create snapshot task
	 *
	 * @param language
	 * @param requestody
	 * @return
	 */
	@PostMapping("/panel/snapshot/task")
	public R snapshotTask(@RequestHeader(required = false, defaultValue = "en") String language,
	                      @RequestBody Map<String, Object> requestody) {
		String format = (String) requestody.get("format");
		format = Tool.StrUtil.emptyToDefault(format, "html");

		Object panelId = requestody.get("panelId");
		Object start = requestody.get("start");
		Object end = requestody.get("end");

		ValidateUtils.is(panelId).notNull(RCode.PANEL_ID_ISNULL)
				.and(start).notNull(RCode.AGENT_PARAM_START_ISNULL)
				.and(end).notNull(RCode.AGENT_PARAM_END_ISNULL);

		Object varsObj = requestody.get("vars");
		Object tvarListObj = Tool.ObjectUtil.defaultIfNull(varsObj, Tool.ListUtil.empty());
		String varsJsonStr = Tool.JSONUtil.toJsonStr(tvarListObj);

		String taskId = visualService.createSnapshotTask(format, Integer.valueOf(panelId.toString()), Long.valueOf(start.toString()), Long.valueOf(end.toString()), varsJsonStr);

		return R.ok().putData("tid", taskId);
	}

	/**
	 * query snapshot task process
	 *
	 * @param tid
	 * @return
	 */
	@GetMapping("/panel/snapshot/result/{tid}")
	public R snapshotResultProcess(@PathVariable(required = true) String tid) {
		return R.ok().putData("taskId", visualService.snapshotResultProcess(tid));
	}

	/**
	 * cancel snapshot task
	 *
	 * @param tid
	 * @return
	 */
	@GetMapping("/panel/snapshot/cancel/{tid}")
	public R cancelSnapshotTask(@PathVariable(required = true) String tid) {
		visualService.cancelSnapshotTask(tid);
		return R.ok();
	}

	/**
	 * download snmapshot file
	 *
	 * @param response
	 * @param language
	 * @param tid
	 */
	@GetMapping("/panel/snapshot/download/{tid}")
	public void downloadSnapshotFile(HttpServletResponse response,
	                                 @RequestHeader(required = false, defaultValue = "en") String language,
	                                 @PathVariable(required = true) String tid) {
		visualService.downloadSnapshotFile(response, language, tid);
	}
}
