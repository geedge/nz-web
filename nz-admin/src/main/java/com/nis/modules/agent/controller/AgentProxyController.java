package com.nis.modules.agent.controller;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.sys.service.SysConfigService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/agent")
public class AgentProxyController {

    private Log log = Log.get();


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
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.PROMETHEUS_READY_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        // 响应结果
        if (Tool.StrUtil.isNotEmpty(result)) {
            return R.ok().putData("status", result);
        } else {
            return R.error(500, "prometheus proxy request ready error");
        }
    }

    /**
     * prometheus  config 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/prometheus/api/v1/status/config")
    public R config(@PathVariable("id") Integer id) throws FileNotFoundException {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.PROMETHEUS_CONFIG_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        // 响应结果
        if (Tool.StrUtil.isNotEmpty(result)) {
            R resultObj = JSONObject.parseObject(result, R.class);
            String config = JSONPath.read(resultObj.get("data").toString(),"yaml").toString();
            return R.ok().putData("content",config);
        } else {
            return R.error(500, "prometheus proxy request config error");
        }

    }

    /**
     * prometheus  flags 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/prometheus/api/v1/status/flags")
    public R flags(@PathVariable("id") Integer id) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.PROMETHEUS_FLAGS_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        if (Tool.StrUtil.isNotEmpty(result)) {
            return Tool.JSONUtil.toBean(result, R.class);
        } else {
            return R.error(500, "prometheus proxy request flags error");
        }
    }

    /**
     * prometheus  buildinfo 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/prometheus/api/v1/status/buildinfo")
    public R buildInfo(@PathVariable("id") Integer id) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.PROMETHEUS_BUILD_INFO_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        if (Tool.StrUtil.isNotEmpty(result)) {
            return Tool.JSONUtil.toBean(result, R.class);
        } else {
            return R.error(500, "prometheus proxy request buildinfo error");
        }
    }

    /**
     * prometheus  runtimeinfo 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/prometheus/api/v1/status/runtimeinfo")
    public R runTimeInfo(@PathVariable("id") Integer id) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.PROMETHEUS_RUN_TIME_INFO_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        if (Tool.StrUtil.isNotEmpty(result)) {
            return Tool.JSONUtil.toBean(result, R.class);
        } else {
            return R.error(500, "prometheus proxy request runtimeinfo error");
        }
    }

    /**
     * prometheus  tsdb 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/prometheus/api/v1/status/tsdb")
    public R tsdb(@PathVariable("id") Integer id) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.PROMETHEUS_TSDB_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        if (Tool.StrUtil.isNotEmpty(result)) {
            return Tool.JSONUtil.toBean(result, R.class);
        } else {
            return R.error(500, "prometheus proxy request tsdb error");
        }
    }



    /**
     * cortex ready 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/cortex/ready")
    public R cortexReady(@PathVariable("id") Integer id) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.CORTEX_READY_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        // 响应结果
        if (Tool.StrUtil.isNotEmpty(result)) {
            return R.ok().putData("status", result);
        } else {
            return R.error(500, "cortex proxy request ready error");
        }
    }

    /**
     * cortex config 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/cortex/config")
    public R cortexConfig(@PathVariable("id") Integer id, @RequestParam(required = false, defaultValue = "") String mode) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String queryString = String.format("mode=%s" ,mode);
        String result = AgentUtil.requestGet(agent, Constant.CORTEX_CONFIG_PATH, queryString, String.class);
        // 响应结果
        if (Tool.ObjectUtil.isNotEmpty(result)) {
            return R.ok().putData("content", result);
        } else {
            return R.error(500, "cortex proxy request config error");
        }
    }

    /**
     * cortex service 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/cortex/services")
    public R cortexServices(@PathVariable("id") Integer id) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.CORTEX_SERVICES_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        // 响应结果
        if (Tool.StrUtil.isNotEmpty(result)) {
            List<Map> services = (List<Map>) JSONPath.read(result, "services");
            return R.ok().putData("list", services);
        } else {
            return R.error(500, "cortex proxy request services error");
        }
    }

    /**
     * cortex user_stats 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/cortex/api/v1/user_stats")
    public R cortexUserStats(@PathVariable("id") Integer id, @RequestParam(required = false, defaultValue = "") String mode) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String queryString = String.format("mode=%s" ,mode);
        String result = AgentUtil.requestGet(agent, Constant.CORTEX_USER_STATS_PATH, queryString, String.class);
        // 响应结果
        if (Tool.StrUtil.isNotEmpty(result)) {
            Map map = JSONObject.parseObject(result, Map.class);
            return R.ok(map);
        } else {
            return R.error(500, "cortex proxy request user stats error");
        }
    }


    /**
     * cortex ingester ring 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/cortex/ingester/ring")
    public R cortexIngesterRing(@PathVariable("id") Integer id) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.CORTEX_INGESTER_RING_PATH);
        String result = AgentUtil.requestAgentProxy(url, agent.getToken(), String.class);
        // 响应结果
        if (Tool.StrUtil.isNotEmpty(result)) {
            List<Map> map = Tool.ListUtil.list(false);
            //解析 html
            Document parse = Jsoup.parse(result);
            Elements tbody = parse.getElementsByTag("tbody");
            for (Element element : tbody) {
                Elements trs = element.getElementsByTag("tr");
                for (Element tr : trs) {
                    Elements childrens = tr.children();
                    Map<Object, Object> results = Tool.MapUtil.builder()
                            .put("instanceId", childrens.get(0).getElementsByTag("td").text())
                            .put("zone", childrens.get(1).getElementsByTag("td").text())
                            .put("state", childrens.get(2).getElementsByTag("td").text())
                            .put("address", childrens.get(3).getElementsByTag("td").text())
                            .put("registeredAt", childrens.get(4).getElementsByTag("td").text())
                            .put("heartbeat", childrens.get(5).getElementsByTag("td").text())
                            .put("tokens", childrens.get(6).getElementsByTag("td").text())
                            .put("ownership", childrens.get(7).getElementsByTag("td").text())
                            .build();
                    map.add(results);
                }
            }
            return R.ok().putData("list", map);
        } else {
            return R.error(500, "cortex proxy request ingester ring error");
        }
    }

    /**
     * cortex store-gateway ring 接口
     * @param id
     * @return
     */
    @GetMapping("/{id}/cortex/store-gateway/ring")
    public R cortexStoreGatewayRing(@PathVariable("id") Integer id) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.CORTEX_STORE_GATEWAY_RING_PATH);
        String result = AgentUtil.requestAgentProxy(url, agent.getToken(), String.class);
        // 响应结果
        if (Tool.StrUtil.isNotEmpty(result)) {
            List<Map> map = Tool.ListUtil.list(false);
            //解析 html
            Document parse = Jsoup.parse(result);
            Elements tbody = parse.getElementsByTag("tbody");
            for (Element element : tbody) {
                Elements trs = element.getElementsByTag("tr");
                for (Element tr : trs) {
                    //获取 td 标签集合
                    Elements childrens = tr.children();
                    Map<Object, Object> results = Tool.MapUtil.builder()
                            .put("instanceId", childrens.get(0).getElementsByTag("td").text())
                            .put("zone", childrens.get(1).getElementsByTag("td").text())
                            .put("state", childrens.get(2).getElementsByTag("td").text())
                            .put("address", childrens.get(3).getElementsByTag("td").text())
                            .put("registeredAt", childrens.get(4).getElementsByTag("td").text())
                            .put("heartbeat", childrens.get(5).getElementsByTag("td").text())
                            .put("tokens", childrens.get(6).getElementsByTag("td").text())
                            .put("ownership", childrens.get(7).getElementsByTag("td").text())
                            .build();
                    map.add(results);
                }
            }
            return R.ok().putData("list", map);
        } else {
            return R.error(500, "cortex proxy request store-gateway ring error");
        }
    }

    

    /**
     * loki ready
     * @param id
     * @return
     */
    @GetMapping("/{id}/loki/ready")
    public R lokiReady(@PathVariable("id") Integer id){
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.LOKI_READY_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        // 响应结果
        if (Tool.StrUtil.isNotEmpty(result)) {
            return R.ok().putData("status", result);
        } else {
            return R.error(500, "loki proxy request ready error");
        }
    }


    /**
     * loki config
     * @param id
     * @return
     */
    @GetMapping("/{id}/loki/config")
    public R lokiConfig(@PathVariable("id") Integer id, @RequestParam(required = false, defaultValue = "") String mode) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String result = AgentUtil.requestGet(agent, Constant.LOKI_CONFIG_PATH, String.format("mode=%s" ,mode), String.class);
        // 响应结果
        if (Tool.ObjectUtil.isNotEmpty(result)) {
            return R.ok().putData("content", result);
        } else {
            return R.error(500, "loki proxy request config error");
        }
    }

    /**
     * loki service
     * @param id
     * @return
     */
    @GetMapping("/{id}/loki/services")
    public R lokiServices(@PathVariable("id") Integer id) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.LOKI_SERVICES_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        List<String> lokiTargets = null;
        // 响应结果
        if (Tool.StrUtil.isNotEmpty(result)) {
            lokiTargets = Arrays.asList(result.split("\n"));
            List<Map> services = lokiTargets.stream()
                    .map(target -> Tool.MapUtil.builder()
                            .put("service", Arrays.asList(target.split("=>")).get(0).trim())
                            .put("status",Arrays.asList(target.split("=>")).get(1).trim())
                            .build())
                    .collect(Collectors.toList());
            return R.ok().putData("list", services);
        } else {
            return R.error(500, "loki proxy request services error");
        }
    }


    /**
     * loki  buildinfo
     * @param id
     * @return
     */
    @GetMapping("/{id}/loki/api/v1/status/buildinfo")
    public R lokiBuildInfo(@PathVariable("id") Integer id) {
        ValidateUtils.is(id).notNull(RCode.AGENT_ID_ISNULL);
        //获取可用的 agent
        Agent agent = agentService.getOne(new LambdaUpdateWrapper<Agent>().eq(Agent::getId, id).eq(Agent::getStatus, "1"));
        ValidateUtils.is(agent).notNull(RCode.AGENT_UNAVAILABLE);

        String url = this.buildUrl(agent, Constant.LOKI_BUILD_INFO_PATH);
        String result = AgentUtil.requestGet(url, agent.getToken(), String.class);
        if (Tool.StrUtil.isNotEmpty(result)) {
            return R.ok(JSONObject.parseObject(result));
        } else {
            return R.error(500, "loki proxy request buildinfo error");
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
