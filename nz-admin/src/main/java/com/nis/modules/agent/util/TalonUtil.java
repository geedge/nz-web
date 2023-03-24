package com.nis.modules.agent.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.sys.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

@Configuration
@SuppressWarnings("all")
public class TalonUtil {

    private static Log log = Log.get();

    private static RestTemplate restTemplate;
    private static PromApiService promApiService;
    private static SysConfigService sysConfigService;

    /**
     * 构建  UrlQuery
     */
    private static UrlQuery buildUrlQuery(Map requestParam) {
        UrlQuery urlQuery = new UrlQuery(true);
        Iterator<Map.Entry<String, Object>> iterator = ((Map<String, Object>) requestParam).entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            Object value = entry.getValue();
            if (value == null)
                continue;
            if (value instanceof Collection) {
                List list = new ArrayList((Collection) value);
                list.stream().forEach(obj -> urlQuery.add(entry.getKey(), obj.toString()));
                iterator.remove();
            }
        }
        urlQuery.addAll(requestParam);
        return urlQuery;
    }

    /**
     * GET 请求
     *
     * @param targetUrl
     * @param targetToken
     * @param path
     * @param requestParam 支持 key - value & key - values
     * @return
     */
    public static <T> T getForObject(String targetUrl, String targetToken, String path, Map requestParam, Class<T> responseType) {
        String talonOshiMode = sysConfigService.getValueOrDefault("nz_talon_oshi_mode", "1");
        switch (talonOshiMode) {
            case "1": {
                // 通过 agent 代理访问 talon 接口
                return TalonUtil.requestTalonThroughAgent(targetUrl, targetToken, HttpMethod.GET, path, requestParam, null, responseType);
            }
            case "2": {
                // 直接请求 talon 接口
                return TalonUtil.requestTalon(targetUrl, targetToken, HttpMethod.GET, path, requestParam, null, responseType);
            }
            default: {
                log.error("sys_config key: nz_talon_oshi_mode value is not within the enumeration range");
                throw new NZException(RCode.ERROR);
            }
        }
    }

    /**
     * POST 请求
     *
     * @param targetUrl
     * @param targetToken
     * @param path
     * @param requestParam 支持 key - value & key - values
     * @param body         自定义 body，Multipartfile 使用 MultipartBodyBuilder build 即可
     */
    public static <T> T postForObject(String targetUrl, String targetToken, String path, Map requestParam, Object body,
                                      Class<T> responseType) {
        String talonOshiMode = sysConfigService.getValueOrDefault("nz_talon_oshi_mode", "1");
        switch (talonOshiMode) {
            case "1": {
                // 通过 agent 代理访问 talon 接口
                return TalonUtil.requestTalonThroughAgent(targetUrl, targetToken, HttpMethod.POST, path, requestParam, body, responseType);
            }
            case "2": {
                // 直接请求 talon 接口
                return TalonUtil.requestTalon(targetUrl, targetToken, HttpMethod.POST, path, requestParam, body, responseType);
            }
            default: {
                log.error("sys_config key: nz_talon_oshi_mode value is not within the enumeration range");
                throw new NZException(RCode.ERROR);
            }
        }
    }

    /**
     * 直接请求 talon 接口
     *
     * @return
     */
    public static <T> T requestTalon(String targetUrl, String targetToken, HttpMethod method, String path, Map requestParam, Object body,
                                     Class<T> responseType) {
        // build urlQuery
        UrlQuery urlQuery = TalonUtil.buildUrlQuery(requestParam);

        UrlBuilder urlBuilder = UrlBuilder.create()
                .setScheme("http")
                .setHost(targetUrl.split(":")[0])
                .setPort(Integer.valueOf(targetUrl.split(":")[1]))
                .setQuery(urlQuery)
                .setPath(UrlPath.of(String.format("%s%s", "/nz-talon", path), Charset.forName("UTF-8")));
        String urlStr = urlBuilder.build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, targetToken);

        HttpEntity httpEntity = body == null ? new HttpEntity(headers) : new HttpEntity(body, headers);

        // 发送 请求
        ResponseEntity<T> exchange = null;
        try {
            exchange = restTemplate.exchange(urlStr, method, httpEntity, responseType);
            log.info("request url:{}, responseStatus:{}", urlStr, exchange.getStatusCodeValue());

            if (log.isDebugEnabled()) {
                log.debug("request responseStatus:{}, body:{}", urlStr, exchange.getStatusCodeValue(), exchange.getBody());
            }
        } catch (RestClientException e) {
            log.error(e, "request error,path: {}", urlStr);
        }
        return exchange.getBody();
    }

    /**
     * 通过 agent 代理访问 talon 接口
     */
    public static <T> T requestTalonThroughAgent(String targetUrl, String targetToken, HttpMethod method, String path, Map requestParam,
                                                 Object body, Class<T> responseType) {
        // build urlQuery
        UrlQuery urlQuery = TalonUtil.buildUrlQuery(requestParam);

        Agent server = promApiService.getOneAvaliableGlobalAgent();
        String agentPath = sysConfigService.getValue(Constant.CONFAGENT_PATH);

        UrlBuilder urlBuilder = UrlBuilder.create()
                .setScheme(server.getProtocol())
                .setHost(server.getHost())
                .setPort(server.getPort())
                .setQuery(urlQuery)
                .setPath(UrlPath.of(String.format("%s%s%s", agentPath, "/talon/proxy", path), Charset.forName("UTF-8")));
        String urlStr = urlBuilder.build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, server.getToken());
        headers.add("targetUrl", targetUrl);
        headers.add("targetToken", targetToken);
        HttpEntity httpEntity = body == null ? new HttpEntity(headers) : new HttpEntity(body, headers);

        // 发送 请求
        ResponseEntity<T> exchange = null;
        try {
            exchange = restTemplate.exchange(urlStr, method, httpEntity, responseType);
            log.info("request url:{}, responseStatus:{}", urlStr, exchange.getStatusCodeValue());

            if (log.isDebugEnabled()) {
                log.debug("request responseStatus:{}, body:{}", urlStr, exchange.getStatusCodeValue(), exchange.getBody());
            }
        } catch (RestClientException e) {
            log.error(e, "request error,path: {}", urlStr);
        }
        return exchange.getBody();
    }

    /**
     * get Talon Rpm Package
     *
     * @param os                      default: centos
     * @param talonInstallPackagePath
     * @return
     */
    public static File getTalonRpmPackage(String os, String talonInstallPackagePath) {
        try {
            os = Tool.StrUtil.emptyToDefault(os, "centos");
            log.info("[getTalonRpmPackage] [begin] [os: {}]", os);

            File installPackageDir = CommonUtils.getResourceFile(talonInstallPackagePath);
            log.info("[getTalonRpmPackage] [Package Path: {}] [IsExist: {}] [IsDir: {}]",
                    talonInstallPackagePath, FileUtil.exist(installPackageDir), FileUtil.isDirectory(installPackageDir));

            if (Tool.BooleanUtil.or(
                    Tool.BooleanUtil.negate(FileUtil.exist(installPackageDir)),
                    Tool.BooleanUtil.negate(FileUtil.isDirectory(installPackageDir))
            )) {
                log.warn("[getTalonRpmPackage] [Package Path Error] [Path: {}]", talonInstallPackagePath);
                throw new NZException("file download error", 500);
            }

            String talonRpmNameRegexJsonStr = sysConfigService.getValue("talon_name");
            JSONObject jsonObject = JSONUtil.parseObj(talonRpmNameRegexJsonStr);

            String talonNameRegex = (String) jsonObject.get(os);
            Pattern talonNamePattern = Pattern.compile(talonNameRegex);
            log.info("[getTalonRpmPackage] [Talon Name Regex: {}]", talonNameRegex);

            File[] listFiles = installPackageDir.listFiles((file, name) -> {
                return talonNamePattern.matcher(name).find();
            });
            log.info("[getTalonRpmPackage] [Files filtered by talon name regex] [size: {}]", listFiles.length);

            if (Tool.ArrayUtil.isEmpty(listFiles)) {
                log.warn("[getTalonRpmPackage] [talon installation package not found]");
                throw new NZException("file download error", 500);
            }

            File file = CommonUtils.getResourceFile(listFiles[0].getAbsolutePath());
            log.info("[getTalonRpmPackage] [finshed]");

            return file;
        } catch (Exception e) {
            log.error(e, "[getTalonRpmPackage] [error]");
            throw new NZException("file download error", 500);
        }
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        TalonUtil.restTemplate = restTemplate;
    }

    @Autowired
    public void setSysconfigService(SysConfigService sysConfigService) {
        TalonUtil.sysConfigService = sysConfigService;
    }

    @Autowired
    public void setPromApiService(PromApiService promApiService) {
        TalonUtil.promApiService = promApiService;
    }
}
