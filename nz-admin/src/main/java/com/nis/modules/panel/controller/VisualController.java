package com.nis.modules.panel.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.modules.panel.service.VisualService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
								Long end, Integer unit, String limit, String direction, @RequestParam(value="expressions[]")String[] expressions) throws IOException {
		ValidateUtils.is(type).notNull(RCode.AGENT_TYPE_ISNULL)
				.and(start).notNull(RCode.AGENT_PARAM_START_ISNULL)
				.and(end).notNull(RCode.AGENT_PARAM_END_ISNULL)
				.and(expressions).notNull(RCode.AGENT_PARAM_QUERY_ISNULL);
		String language = request.getHeader(Constant.LANGUAGE);
		visualService.exploreSnapshot(response, format, type, start, end, language, unit, limit, direction, expressions);
	}
}
