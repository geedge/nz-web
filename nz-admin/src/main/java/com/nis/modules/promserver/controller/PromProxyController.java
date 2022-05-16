package com.nis.modules.promserver.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.modules.promserver.service.PromserverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@SuppressWarnings("all")
public class PromProxyController {

	@Autowired
	private PromserverService promserverService;

	@RequestMapping(value = "/prom/**")
	public void promProxy(HttpServletRequest request, HttpServletResponse response) {
		String promPath = request.getServletPath().substring(5);

		switch (promPath) {
		case "/api/v1/query":
			// 验证query 参数不能为空
			ValidateUtils.is(request.getParameter("query")).notNull(RCode.PROMSERVER_PARAM_QUERY_ISNULL);
			break;
		case "/api/v1/query_range":
			// 验证query 参数不能为空
			ValidateUtils.is(request.getParameter("query")).notNull(RCode.PROMSERVER_PARAM_QUERY_ISNULL);
			// 验证start 参数不能为空
			ValidateUtils.is(request.getParameter("start")).notNull(RCode.PROMSERVER_PARAM_START_ISNULL);
			// 验证end 参数不能为空
			ValidateUtils.is(request.getParameter("end")).notNull(RCode.PROMSERVER_PARAM_END_ISNULL);
			// 验证step 参数不能为空
			ValidateUtils.is(request.getParameter("step")).notNull(RCode.PROMSERVER_PARAM_STEP_ISNULL);
			break;
		case "/api/v1/series":
			// 验证step 参数不能为空
			ValidateUtils.is(request.getParameter("match[]")).notNull(RCode.PROMSERVER_PARAM_MATCH_ISNULL);
			break;
		default:
			break;
		}
		promserverService.promServerProxy(request, response, promPath, null);
	}

}
