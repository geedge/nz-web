package com.nis.modules.tool.controller;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/tool")
public class TracerouteController {

    private final static Log log = Log.get();

    /**
     * trace job timeout (seconds) 默认五分钟
     */
    @Value("${tool.trace.job.timeout:300}")
    private Long timeout;

    @Autowired
    private AgentService agentService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DcService dcService;

    static final List<Thread> ACTIVE_TASKS = Collections.synchronizedList(new ArrayList<>());

    @GetMapping("/traceroute")
    public R traceroute(@RequestParam Map<String, Object> params) {
        String ips = (String) params.get("ips");
        if (Tool.StrUtil.isEmpty(ips)) throw new NZException(RCode.TOOL_TRACEROUTE_IP_ISNULL);

        String dcIds = (String) params.get("dcIds");

        List<Dc> dcList = dcService.list(new LambdaQueryWrapper<Dc>().in(Tool.StrUtil.isNotEmpty(dcIds), Dc::getId, Tool.StrUtil.split(dcIds, ',')));

        // 根据 dcids 获取当前 state = up 的所有 agent
        List<Agent> agentList = agentService.list(new LambdaQueryWrapper<Agent>()
                .in(Tool.StrUtil.isNotEmpty(dcIds), Agent::getDcId, Tool.StrUtil.split(dcIds, ','))
                .eq(Agent::getStatus, 1));

        List<Dc> futureDcList = Tool.ListUtil.list(false);
        for (Dc dc : dcList) {
            Agent agentByDc = agentList.stream().filter(p -> dc.getId().equals(p.getDcId())).findFirst().orElse(null);
            dc.setAgent(agentByDc);
            futureDcList.add(dc);
        }

        Map<String, Object> requestParam = Tool.MapUtil.newHashMap();

        String maxHops = Tool.ObjectUtil.defaultIfEmpty((String) params.get("maxHops"), "30");
        String packetsize = Tool.ObjectUtil.defaultIfEmpty((String) params.get("packetsize"), "64");
        String traceCmdTimeout = Tool.ObjectUtil.defaultIfEmpty((String) params.get("timeout"), "100");
        String concurrent = Tool.ObjectUtil.defaultIfEmpty((String) params.get("concurrent"), "10");

        requestParam.put("maxHops", maxHops);
        requestParam.put("packetsize", packetsize);
        requestParam.put("timeout", traceCmdTimeout);
        requestParam.put("concurrent", concurrent);

        String tid = UUID.randomUUID().toString();
        List<String> ipList = Tool.StrUtil.split(ips, ',');
        for (Dc dc : futureDcList) {
            for (String ip : ipList) {
                Map<String, Object> map = Tool.MapUtil.newHashMap();
                map.putAll(requestParam);
                map.put("ips", Tool.StrUtil.split(ip, ','));

                TracerouteRunnable tracerouteRunnable = new TracerouteRunnable(dc, tid, Long.valueOf(timeout), TimeUnit.SECONDS, map);
                tracerouteRunnable.setName(Tool.StrUtil.concat(true, tid, "-", dc.getName()));
                ThreadUtil.execAsync(tracerouteRunnable);
            }
        }

        Map m = Tool.MapUtil.newHashMap();
        m.put("tid", tid);
        Map task = Tool.MapUtil.newHashMap();
        task.put("total", (int) Tool.NumberUtil.mul(futureDcList.size(), ipList.size()));
        task.put("done", 0);
        task.put("process", 0);
        m.put("task", task);

        // 设置 30 分钟超时，避免 redis 中存在垃圾数据
        redisTemplate.opsForValue().set(Tool.StrUtil.concat(true, "test_traceroute_stat_", tid), JSONObject.toJSONString(task), 30, TimeUnit.MINUTES);
        return R.ok(m);
    }

    @GetMapping("/traceroute/result/{tid}")
    public R tracerouteResult(@PathVariable(value = "tid", required = true) String tid) {
        String tracerouteResultKey = Tool.StrUtil.concat(true, "test_traceroute_result_", tid);
//        List<String> tracerouteResultList = redisTemplate.opsForList().range(tracerouteResultKey, 0, -1);
        List<String> tracerouteResultList = Tool.ListUtil.list(false);
        while (true) {
            String obj = (String) redisTemplate.opsForList().rightPop(tracerouteResultKey);
            if (obj == null) break;
            tracerouteResultList.add(obj);
        }

        String tracerouteStatStr = (String) redisTemplate.opsForValue().get(Tool.StrUtil.concat(true, "test_traceroute_stat_", tid));
        // 任务已被取消或 key 超时
        if (Tool.StrUtil.isEmpty(tracerouteStatStr)) {
            Map result = Tool.MapUtil.newHashMap();
            Map<String, Integer> task = Tool.MapUtil.of(Pair.of("total", 0), Pair.of("done", 0), Pair.of("process", 0));
            result.put("task", task);
            result.put("list", Tool.ListUtil.empty());
            return R.ok(result);
        }

        Map tracerouteStatMap = JSONObject.parseObject(tracerouteStatStr, Map.class);
        Integer total = (Integer) tracerouteStatMap.get("total");
        Integer done = (Integer) tracerouteStatMap.get("done");

        List list = Tool.ListUtil.list(false);
        for (String jsonStr : tracerouteResultList) {
            JSONArray jsonArray = JSONArray.parseArray(jsonStr);
            list.addAll(jsonArray);
        }
        done = done + list.size();
        tracerouteStatMap.put("done", done);
        String percent = NumberUtil.formatPercent((double) done / total, 2);
        tracerouteStatMap.put("process", percent.replace("%", ""));

        // 更新 redis test_traceroute_stat  设置 30分钟超时，避免 redis 中存在垃圾数据
        redisTemplate.opsForValue().set(Tool.StrUtil.concat(true, "test_traceroute_stat_", tid), JSONObject.toJSONString(tracerouteStatMap), 30, TimeUnit.MINUTES);

        Map result = Tool.MapUtil.newHashMap();
        result.put("task", tracerouteStatMap);
        result.put("list", list);
        return R.ok(result);
    }

    @GetMapping("/traceroute/cancel/{tid}")
    public R tracerouteCancel(@PathVariable(value = "tid", required = false) String tid) {
        if (Tool.CollectionUtil.isNotEmpty(ACTIVE_TASKS)) {
            ACTIVE_TASKS.stream().filter(thread -> Tool.StrUtil.startWith(thread.getName(), tid)).forEach(thread -> {
                log.info(String.format("Traceroute thread: %s has been canceled", thread.getName()));
                thread.interrupt();
            });
        }

        List<String> keys = Tool.ListUtil.of(
                Tool.StrUtil.concat(true, "test_traceroute_stat_", tid),
                Tool.StrUtil.concat(true, "test_traceroute_result_", tid));
        redisTemplate.delete(keys);
        return R.ok();
    }

    public class TracerouteRunnable extends Thread {
        private Dc dc;
        private String tid;
        private long timeout;
        private TimeUnit timeUnit;
        private Map<String, Object> requestBody;

        private volatile boolean isWrite = true;
        // 任务运行状态，调用 interrupt() 后，该值为 false
        private volatile boolean isRun = true;

        public TracerouteRunnable(Dc dc, String tid, Long timeout, TimeUnit timeUnit, Map<String, Object> requestBody) {
            this.dc = dc;
            this.tid = tid;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            this.requestBody = requestBody;
        }

        @Override
        public void run() {
            ACTIVE_TASKS.add(this);
            // agent 存在，向 agent 发起请求获取数据
            if (Optional.ofNullable(dc).map(Dc::getAgent).isPresent()) {
                Agent agent = dc.getAgent();
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    String path = "/cmd/traceroute";
                    String resultJsonStr = AgentUtil.request(agent, HttpMethod.POST, path, null, requestBody, String.class);
                    Object list = JSONPath.read(resultJsonStr, "data.list");
                    log.info("host: {}, path: {}, result: {}", agent.getHost(), path, resultJsonStr);
                    // 由于 future.cancel 在任务已经运行状态下，不一定能取消，所以在写 redis 前增加判断
                    if (isWrite) {
                        List<Map> mapList = JSONArray.parseArray(JSONArray.toJSONString(list), Map.class);
                        for (Map map : mapList) {
                            Map dcMap = Tool.MapUtil.newHashMap(4);
                            dcMap.put("id", dc.getId());
                            dcMap.put("name", dc.getName());

                            Map agentMap = Tool.MapUtil.newHashMap(4);
                            agentMap.put("id", agent.getId());
                            agentMap.put("name", agent.getName());

                            map.put("dc", dcMap);
                            map.put("agent", agentMap);

                            // source 赋值为 agent ip
                            map.put("source", agent.getHost());
                        }
                        redisTemplate.opsForList().leftPush(Tool.StrUtil.concat(true, "test_traceroute_result_", tid), JSONArray.toJSONString(mapList));
                    } else {
                        log.info(String.format("Traceroute thread: %s has been canceled, Do not write data to Redis", this.getName()));
                    }
                    return Tool.StrUtil.EMPTY;
                });

                try {
                    future.get(timeout, timeUnit);
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e);
                } catch (TimeoutException e) {
                    boolean cancel = future.cancel(true);
                    isWrite = false;
                    log.error(String.format("%s agent traceroute %s timeout, Task cancel status: %s", agent.getName(), requestBody.get("ips").toString(), cancel));

                    if (isRun) {
                        log.info(String.format("%s agent traceroute %s timeout, Timeout data write to redis", agent.getName(), requestBody.get("ips").toString()));
                        // 超时也需要向 redis 中写入 timeout 消息
                        Map agentMap = Tool.MapUtil.newHashMap(4);
                        agentMap.put("id", agent.getId());
                        agentMap.put("name", agent.getName());

                        Map dcMap = Tool.MapUtil.newHashMap(4);
                        dcMap.put("id", dc.getId());
                        dcMap.put("name", dc.getName());
                        List<String> timeOutIpList =  (List<String>) requestBody.get("ips");
                        List<Map> list = Tool.ListUtil.list(false);
                        for (String timeOutIp : timeOutIpList) {
                            Map timeoutInfoMap = Tool.MapUtil.of(
                                    Pair.of("ip", timeOutIp),
                                    Pair.of("state", 2),
                                    Pair.of("time", null),
                                    Pair.of("source", agent.getHost()),
                                    Pair.of("raw", "Timeout"),
                                    Pair.of("agent", agentMap),
                                    Pair.of("dc", dcMap)
                            );
                            list.add(timeoutInfoMap);
                        }
                        redisTemplate.opsForList().leftPush(Tool.StrUtil.concat(true, "test_traceroute_result_", tid), JSONObject.toJSONString(list));
                    } else {
                        log.info(String.format("%s agent traceroute %s timeout, But the thread is interrupted, and the timeout data is not write to redis.", agent.getName(), requestBody.get("ips").toString()));
                    }
                }
            } else {
                // agent 不存在（DC 下没有可用 agent,记录错误数据至 REDIS 中）
                if (isRun) {
                    log.info(String.format("Dc info : %s , No Agent available", JSONObject.toJSONString(dc)));
                    Map dcMap = Tool.MapUtil.newHashMap(4);
                    dcMap.put("id", dc.getId());
                    dcMap.put("name", dc.getName());
                    List<String> ipList =  (List<String>) requestBody.get("ips");
                    List<Map> list = Tool.ListUtil.list(false);
                    for (String ip : ipList) {
                        Map noAgentInfo = Tool.MapUtil.of(
                                Pair.of("ip", ip),
                                Pair.of("state", 2),
                                Pair.of("time", null),
                                Pair.of("source", ""),
                                Pair.of("raw", "No agent available"),
                                Pair.of("agent", Tool.MapUtil.empty()),
                                Pair.of("dc", dcMap)
                        );
                        list.add(noAgentInfo);
                    }
                    redisTemplate.opsForList().leftPush(Tool.StrUtil.concat(true, "test_traceroute_result_", tid), JSONObject.toJSONString(list));
                } else {
                    log.info(String.format("Dc info : %s , No Agent available, But the thread is interrupted, not write to redis.", JSONObject.toJSONString(dc)));
                }
            }
            ACTIVE_TASKS.remove(this);
        }

        @Override
        public void interrupt() {
            super.interrupt();
            isWrite = false;
            isRun = false;
        }
    }
}
