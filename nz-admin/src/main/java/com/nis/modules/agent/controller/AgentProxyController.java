package com.nis.modules.agent.controller;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.sys.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;

@RestController
@RequestMapping("/agent")
public class AgentProxyController {


    @Autowired
    private AgentService agentService;

    @Autowired
    private SysConfigService configService;

    /**
     * prometheus ready 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/prometheus/ready")
    public R ready(@PathVariable("id") Integer id) {

        Agent agent = agentService.queryAgentDetails(id);
        String url = this.buildUrl(agent, Constant.PROMETHEUS_READY_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        // 响应结果
        if (Tool.StrUtil.isNotEmpty(result)) {
            return R.ok().putData("status", result);
        } else {
            return R.error(500, "proxy request ready error");
        }
    }

    /**
     * prometheus  config 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/prometheus/api/v1/status/config")
    public R config(@PathVariable("id") Integer id) throws FileNotFoundException {
        Agent agent = agentService.queryAgentDetails(id);

        String url = this.buildUrl(agent, Constant.PROMETHEUS_CONFIG_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        // 响应结果
        if (Tool.StrUtil.isNotEmpty(result)) {
            R resultObj = JSONObject.parseObject(result, R.class);
            String config = JSONPath.read(resultObj.get("data").toString(),"yaml").toString();
//            Yaml yaml = new Yaml();
//            Map<String, Object> configMap  = (Map<String, Object>)yaml.load(config);
            return R.ok().putData("conent",config);
        } else {
            return R.error(500, "proxy request config error");
        }

    }

    /**
     * prometheus  flags 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/prometheus/api/v1/status/flags")
    public R flags(@PathVariable("id") Integer id) {

        Agent agent = agentService.queryAgentDetails(id);
        String url = this.buildUrl(agent, Constant.PROMETHEUS_FLAGS_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        if (Tool.StrUtil.isNotEmpty(result)) {
            return Tool.JSONUtil.toBean(result, R.class);
        } else {
            return R.error(500, "proxy request flags error");
        }
    }

    /**
     * prometheus  buildinfo 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/prometheus/api/v1/status/buildinfo")
    public R buildInfo(@PathVariable("id") Integer id) {

        Agent agent = agentService.queryAgentDetails(id);
        String url = this.buildUrl(agent, Constant.PROMETHEUS_BUILD_INFO_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        if (Tool.StrUtil.isNotEmpty(result)) {
            return Tool.JSONUtil.toBean(result, R.class);
        } else {
            return R.error(500, "proxy request buildinfo error");
        }
    }

    /**
     * prometheus  runtimeinfo 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/prometheus/api/v1/status/runtimeinfo")
    public R runTimeInfo(@PathVariable("id") Integer id) {

        Agent agent = agentService.queryAgentDetails(id);
        String url = this.buildUrl(agent, Constant.PROMETHEUS_RUN_TIME_INFO_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        if (Tool.StrUtil.isNotEmpty(result)) {
            return Tool.JSONUtil.toBean(result, R.class);
        } else {
            return R.error(500, "proxy request runtimeinfo error");
        }
    }

    /**
     * prometheus  tsdb 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/prometheus/api/v1/status/tsdb")
    public R tsdb(@PathVariable("id") Integer id) {

        Agent agent = agentService.queryAgentDetails(id);
        String url = this.buildUrl(agent, Constant.PROMETHEUS_TSDB_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        if (Tool.StrUtil.isNotEmpty(result)) {
            return Tool.JSONUtil.toBean(result, R.class);
        } else {
            return R.error(500, "proxy request tsdb error");
        }
    }

    /**
     * 构造请求路径
     * @param server
     * @param path
     * @return
     */
    public String buildUrl(Agent server, String path){
        String agentPath = configService.getValueOrDefault(Constant.CONFAGENT_PATH, "");
        path = path.startsWith("/") ? (String.format("%s%s", agentPath, path))
                : (String.format("%s/%s", agentPath, path));
        UrlBuilder urlString = UrlBuilder.create().setScheme(server.getProtocol()).setHost(server.getHost())
                .setPort(server.getPort()).setPath(UrlPath.of(path, Charset.forName("UTF-8")));

        return urlString.build();
    }
}
