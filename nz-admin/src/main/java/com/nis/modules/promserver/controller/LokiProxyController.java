package com.nis.modules.promserver.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.modules.promserver.service.LokiserverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@SuppressWarnings("all")
public class LokiProxyController {

    @Autowired
    private LokiserverService lokiserverService;

    @RequestMapping(value = "/logs/loki/**")
    public void logLokiProxy(HttpServletRequest request, HttpServletResponse response) {
        String proxyPath = request.getServletPath().substring(5);

        switch (proxyPath) {
            case "/loki/api/v1/query":
                // 验证query 参数不能为空
                ValidateUtils.is(request.getParameter("query")).notNull(RCode.PROMSERVER_PARAM_QUERY_ISNULL);
                break;
            case "/loki/api/v1/query_range":
                // 验证query 参数不能为空
                ValidateUtils.is(request.getParameter("query")).notNull(RCode.PROMSERVER_PARAM_QUERY_ISNULL);
                // 验证start 参数不能为空
                ValidateUtils.is(request.getParameter("start")).notNull(RCode.PROMSERVER_PARAM_START_ISNULL);
                // 验证end 参数不能为空
                ValidateUtils.is(request.getParameter("end")).notNull(RCode.PROMSERVER_PARAM_END_ISNULL);
                break;
            default:
                break;
        }
        lokiserverService.lokiServerProxy(request, response, proxyPath, null);
    }
}
