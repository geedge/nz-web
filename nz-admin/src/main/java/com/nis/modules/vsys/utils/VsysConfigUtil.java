package com.nis.modules.vsys.utils;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.service.SysConfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;

@Configuration
@SuppressWarnings("all")
public class VsysConfigUtil {

    private static Log log = Log.get();

    private static RestTemplate restTemplate;
    private static SysConfService sysConfService;

    /**
     * build HttpEntity
     * basic_auth 从 sys_config.vsys_restconf_auth 获取
     *
     * @param body
     * @return
     */
    private static HttpEntity buildHttpEntity(Map<Object, Object> body) {
        String vsysRestconfAuthJsonStr = sysConfService.getValue(VsysConstant.VSYS_RESTCONF_AUTH);
        String type = JSONPath.read(vsysRestconfAuthJsonStr, "type").toString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/yang-data+json"));

        headers.add("Accept", MediaType.ALL_VALUE);
        if (StrUtil.equals("basicAuth", type)) {
            String username = JSONPath.read(vsysRestconfAuthJsonStr, "username").toString();
            String password = JSONPath.read(vsysRestconfAuthJsonStr, "password").toString();
            headers.add("username", username);
            headers.add("password", password);
        }

        HttpEntity httpEntity = body == null ? new HttpEntity(headers) : new HttpEntity(body, headers);
        return httpEntity;
    }

    /**
     * build url
     *
     * @param host
     * @param path
     * @return
     */
    private static String buildUrl(String host, String path) {
        String vsysRestconfAuthJsonStr = sysConfService.getValue(VsysConstant.VSYS_RESTCONF_AUTH);
        String protocol = JSONPath.read(vsysRestconfAuthJsonStr, "protocol").toString();
        protocol = StrUtil.emptyToDefault(protocol, "http");

        // 默认 8447
        Object portObj = JSONPath.read(vsysRestconfAuthJsonStr, "port");
        Integer port = Integer.valueOf(ObjectUtil.defaultIfNull(portObj, VsysConstant.VSYS_TSG_OS_DEFAULT_PORT).toString());

        String urlStr = UrlBuilder.create()
                .setScheme(protocol)
                .setHost(host)
                .setPort(port)
                .setPath(UrlPath.of(path, Charset.forName("UTF-8")))
                .build();
        return urlStr;
    }

    /**
     * build url Without Encode
     *
     * @param host
     * @param path
     * @return
     */
    private static String buildUrlWithoutEncode(String host, String path) {
        String vsysRestconfAuthJsonStr = sysConfService.getValue(VsysConstant.VSYS_RESTCONF_AUTH);
        String protocol = JSONPath.read(vsysRestconfAuthJsonStr, "protocol").toString();
        protocol = StrUtil.emptyToDefault(protocol, "http");

        // 默认 8447
        Object portObj = JSONPath.read(vsysRestconfAuthJsonStr, "port");
        Integer port = Integer.valueOf(ObjectUtil.defaultIfNull(portObj, VsysConstant.VSYS_TSG_OS_DEFAULT_PORT).toString());

        // 使用 URI，默认不进行 encode path
        URI uri = null;
        try {
            uri = new URI(protocol, null, host, port, path, null, null);
        } catch (URISyntaxException e) {
            log.error(e);
        }
        String urlStr = uri.toString();
        return urlStr;
    }

    /**
     * POST 请求
     *
     * @param host
     * @param path
     * @param body
     * @return
     */
    public static Map<Object, Object> requestPost(String host, String path, Map<Object, Object> body) {
        // build HttpEntity
        HttpEntity httpEntity = VsysConfigUtil.buildHttpEntity(body);

        // build url
        String url = VsysConfigUtil.buildUrl(host, path);

        Map<Object, Object> resultMp = Tool.MapUtil.builder().build();
        try {
            ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);
            int statusCodeValue = exchange.getStatusCodeValue();
            log.info("request url:{}, responseStatus:{}", url, statusCodeValue);

            resultMp.put("code", statusCodeValue);
            if (statusCodeValue != HttpStatus.HTTP_CREATED) {
                // failure
                log.warn("request url:{}, responseStatus:{}, body:{}", url, statusCodeValue, exchange.getBody());
                resultMp.put("msg", exchange.getBody());
            }
        } catch (Exception e) {
            log.error(e, "request error,path: {}", url);
            resultMp.put("code", HttpStatus.HTTP_INTERNAL_ERROR);
            resultMp.put("msg", e.getMessage());
        }
        return resultMp;
    }

    /**
     * POST 请求，不对URL进行编码
     *
     * @param host
     * @param path
     * @param body
     * @return
     */
    public static Map<Object, Object> requestPostWithoutEncodeUrl(String host, String path, Map<Object, Object> body) {
        // build HttpEntity
        HttpEntity httpEntity = VsysConfigUtil.buildHttpEntity(body);

        // build url
        String url = VsysConfigUtil.buildUrlWithoutEncode(host, path);

        Map<Object, Object> resultMp = Tool.MapUtil.builder().build();
        try {
            ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);
            int statusCodeValue = exchange.getStatusCodeValue();
            log.info("request url:{}, responseStatus:{}", url, statusCodeValue);

            resultMp.put("code", statusCodeValue);
            if (statusCodeValue != HttpStatus.HTTP_CREATED) {
                // failure
                log.warn("request url:{}, responseStatus:{}, body:{}", url, statusCodeValue, exchange.getBody());
                resultMp.put("msg", exchange.getBody());
            }
        } catch (Exception e) {
            log.error(e, "request error,path: {}", url);
            resultMp.put("code", HttpStatus.HTTP_INTERNAL_ERROR);
            resultMp.put("msg", e.getMessage());
        }
        return resultMp;
    }


    /**
     * DEL 请求
     *
     * @param host
     * @param path
     * @param body
     * @return
     */
    public static Map<Object, Object> requestDelete(String host, String path, Map<Object, Object> body) {
        // build HttpEntity
        HttpEntity httpEntity = VsysConfigUtil.buildHttpEntity(body);

        // build url
        String url = VsysConfigUtil.buildUrl(host, path);

        Map<Object, Object> resultMp = Tool.MapUtil.builder().build();
        try {
            ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.DELETE, httpEntity, String.class);
            int statusCodeValue = exchange.getStatusCodeValue();
            log.info("request url:{}, responseStatus:{}", url, statusCodeValue);

            resultMp.put("code", statusCodeValue);
            if (statusCodeValue != HttpStatus.HTTP_NO_CONTENT) {
                // failure
                log.warn("request url:{}, responseStatus:{}, body:{}", url, statusCodeValue, exchange.getBody());
                resultMp.put("msg", exchange.getBody());
            }
        } catch (Exception e) {
            log.error(e, "request error,path: {}", url);
            resultMp.put("code", HttpStatus.HTTP_INTERNAL_ERROR);
            resultMp.put("msg", e.getMessage());
        }
        return resultMp;
    }

    /**
     * DEL 请求 不对URL进行编码
     *
     * @param host
     * @param path
     * @param body
     * @return
     */
    public static Map<Object, Object> requestDeleteWithoutEncodeUrl(String host, String path, Map<Object, Object> body) {
        // build HttpEntity
        HttpEntity httpEntity = VsysConfigUtil.buildHttpEntity(body);

        // build url
        String url = VsysConfigUtil.buildUrlWithoutEncode(host, path);

        Map<Object, Object> resultMp = Tool.MapUtil.builder().build();
        try {
            ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.DELETE, httpEntity, String.class);
            int statusCodeValue = exchange.getStatusCodeValue();
            log.info("request url:{}, responseStatus:{}", url, statusCodeValue);

            resultMp.put("code", statusCodeValue);
            if (statusCodeValue != HttpStatus.HTTP_NO_CONTENT) {
                // failure
                log.warn("request url:{}, responseStatus:{}, body:{}", url, statusCodeValue, exchange.getBody());
                resultMp.put("msg", exchange.getBody());
            }
        } catch (Exception e) {
            log.error(e, "request error,path: {}", url);
            resultMp.put("code", HttpStatus.HTTP_INTERNAL_ERROR);
            resultMp.put("msg", e.getMessage());
        }
        return resultMp;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        VsysConfigUtil.restTemplate = restTemplate;
    }

    @Autowired
    public void setSysConfService(SysConfService sysConfService) {
        VsysConfigUtil.sysConfService = sysConfService;
    }
}
