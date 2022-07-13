package com.nis.modules.promserver.utils;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.promserver.entity.Agent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;

@Component
public class AgentProxyApiUtil {

    private static Log log = Log.get();

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 构建 HttpEntity
     *
     * @param request
     * @return
     */
    public static HttpEntity buildHttpEntity(HttpServletRequest request) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String hn = headerNames.nextElement();
            if (Constant.AUTH_TOKEN_CODE.equalsIgnoreCase(hn)) {
                continue;
            }
            headers.add(hn, request.getHeader(hn));
        }
        byte[] body = Tool.IoUtil.readBytes(request.getInputStream());
        return new HttpEntity(body, headers);
    }

    /**
     * GET 请求
     *
     * @param server
     * @param url
     * @param httpEntity
     * @param requestParam 支持 key - value & key - values
     * @return
     */
    public JSONObject getForObject(Agent server, String url, HttpEntity httpEntity, Map requestParam) {
        httpEntity = Tool.ObjectUtil.defaultIfNull(httpEntity, HttpEntity.EMPTY);

        UrlBuilder urlBuilder = UrlBuilder.create()
                .setScheme(server.getProtocol())
                .setHost(server.getHost())
                .setPort(server.getPort())
                .setPath(UrlPath.of(url, Charset.forName("UTF-8")));

        UrlQuery urlQuery = new UrlQuery();
        Iterator<Map.Entry<String, Object>> iterator = ((Map<String, Object>) requestParam).entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            Object value = entry.getValue();
            if (value == null) continue;
            if (value instanceof Collection) {
                List list = new ArrayList((Collection) value);
                list.stream().forEach(obj -> urlQuery.add(entry.getKey(), obj.toString()));
                iterator.remove();
            }
        }

        urlQuery.addAll(requestParam);
        String urlStr = urlBuilder.setQuery(urlQuery).toString();

        // 添加 agent token
        HttpHeaders headers = new HttpHeaders();
        Tool.BeanUtil.copyProperties(httpEntity.getHeaders(), headers);
        // agent token
        headers.add(Constant.AUTH_TOKEN_CODE, server.getToken());

        HttpEntity entity = new HttpEntity(httpEntity.getBody(), headers);
        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(new URI(urlStr), HttpMethod.GET, entity, String.class);
        } catch (URISyntaxException e) {
            log.error(e);
        }
        log.info("agent request url:{}, responseStatus:{}", urlStr, responseEntity.getStatusCodeValue());

        if (log.isDebugEnabled()) {
            log.debug("agent request body:{}", urlStr, responseEntity.getStatusCodeValue(), responseEntity.getBody());
        }
        return JSONObject.parseObject(responseEntity.getBody());
    }


    /**
     * agent api 代理接口
     *
     * @param server
     * @param url
     * @param request
     * @param response
     */
    public void agentApiProxy(Agent server, String url, HttpServletRequest request, HttpServletResponse response) {
        try {
            // 重新构造请求参数内容，header body 添加 agent token
            HttpEntity httpEntity = AgentProxyApiUtil.buildHttpEntity(request);
            HttpHeaders headers = new HttpHeaders();
            Tool.BeanUtil.copyProperties(httpEntity.getHeaders(), headers);
            // agent token
            headers.add(Constant.AUTH_TOKEN_CODE, server.getToken());
            httpEntity = new HttpEntity(httpEntity.getBody(), headers);

            String queryString = request.getQueryString();
            queryString = StrUtil.isNotBlank(queryString) ? queryString : "";
            queryString = URLUtil.decode(queryString);

            String urlStr = UrlBuilder.create()
                    .setScheme(server.getProtocol())
                    .setHost(server.getHost())
                    .setPort(server.getPort())
                    .setPath(UrlPath.of(url, Charset.forName("UTF-8")))
                    .setQuery(UrlQuery.of(queryString, Charset.forName("UTF-8"))).toString();

            String method = request.getMethod();
            ResponseEntity<byte[]> exchange = restTemplate.exchange(new URI(urlStr), HttpMethod.resolve(method), httpEntity, byte[].class);
            
            int statusCode = exchange.getStatusCodeValue();
            log.info("agent request url:{}, responseStatus:{}", urlStr, statusCode);

            HttpHeaders httpHeaders = exchange.getHeaders();

            byte[] responseBody = exchange.getBody();
            response.reset();
            response.setStatus(statusCode);
            Set<Map.Entry<String, List<String>>> entrySet = httpHeaders.entrySet();
            // 设置 cors 响应头
            Constant.CORS_HEADER.forEach((k, v) -> {
                response.setHeader(k, v);
            });
            for (Map.Entry<String, List<String>> en : entrySet) {
                String name = en.getKey();
                List<String> value = en.getValue();
                if (en.getKey().equalsIgnoreCase(Header.CONTENT_LENGTH.getValue())) {
                    continue;
                }
                if (en.getKey().equalsIgnoreCase(Header.TRANSFER_ENCODING.getValue())) {
                    continue;
                }
                response.setHeader(name, Tool.StrUtil.join(",", value.toArray()));
            }

            response.setContentLength(Tool.ArrayUtil.length(responseBody));
            response.getOutputStream().write(responseBody);
            response.flushBuffer();
        } catch (Exception e) {
            log.error(e, "agent proxy api error, url: {}", url);
        }
    }

}
