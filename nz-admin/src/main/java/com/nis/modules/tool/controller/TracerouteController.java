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
import com.nis.common.utils.R;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private PromserverService agentService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DcService dcService;

    static final List<Thread> ACTIVE_TASKS = Collections.synchronizedList(new ArrayList<>());

    @GetMapping("/traceroute")
    public R traceroute(@RequestParam Map<String, Object> params) {
        String ip = (String) params.get("ip");
        String dcIds = (String) params.get("dcIds");
        String timeout = Tool.ObjectUtil.defaultIfEmpty((String) params.get("timeout"), "30");
        String maxHops = Tool.ObjectUtil.defaultIfEmpty((String) params.get("maxHops"), "30");

        List<Dc> dcList = dcService.list(new LambdaQueryWrapper<Dc>().in(Tool.StrUtil.isNotEmpty(dcIds), Dc::getId, Tool.StrUtil.split(dcIds, ',')));

        // 根据 dcids 获取当前 state = up 的所有 agent
        List<Promserver> promserverList = agentService.list(new LambdaQueryWrapper<Promserver>()
                .in(Tool.StrUtil.isNotEmpty(dcIds), Promserver::getDcId, Tool.StrUtil.split(dcIds, ','))
                .eq(Promserver::getStatus, 1));

        List<Dc> futureDcList = Tool.ListUtil.list(false);
        for (Dc dc : dcList) {
            Promserver agentByDc = promserverList.stream().filter(p -> dc.getId().equals(p.getDcId())).findFirst().orElse(null);
            dc.setAgent(agentByDc);
            futureDcList.add(dc);
        }

        String tid = UUID.randomUUID().toString();

        for (Dc dc : futureDcList) {
            TracerouteRunnable tracerouteRunnable = new TracerouteRunnable(dc, ip, maxHops, tid, Long.valueOf(timeout), TimeUnit.SECONDS);
            tracerouteRunnable.setName(Tool.StrUtil.concat(true, tid, "-", dc.getName()));
            ThreadUtil.execAsync(tracerouteRunnable);
        }

        Map m = Tool.MapUtil.newHashMap();
        m.put("tid", tid);
        Map task = Tool.MapUtil.newHashMap();
        task.put("total", futureDcList.size());
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

        done = done + tracerouteResultList.size();
        tracerouteStatMap.put("done", done);
        String percent = NumberUtil.formatPercent((double) done / total, 2);
        tracerouteStatMap.put("process", percent.replace("%", ""));

        // 更新 redis test_traceroute_stat  设置 30分钟超时，避免 redis 中存在垃圾数据
        redisTemplate.opsForValue().set(Tool.StrUtil.concat(true, "test_traceroute_stat_", tid), JSONObject.toJSONString(tracerouteStatMap), 30, TimeUnit.MINUTES);

        Map result = Tool.MapUtil.newHashMap();
        result.put("task", tracerouteStatMap);
        List list = Tool.ListUtil.list(false);
        tracerouteResultList.stream().forEach(jsonArrayStr -> {
            JSONArray jsonArray = JSONArray.parseArray(jsonArrayStr);
            list.addAll(jsonArray);
        });
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
        private String ip;
        private String maxHops;
        private String tid;
        private long timeout;
        private TimeUnit timeUnit;

        private volatile boolean isWrite = true;
        // 任务运行状态，调用 interrupt() 后，该值为 false
        private volatile boolean isRun = true;

        public TracerouteRunnable(Dc dc, String ip, String maxHops, String tid, long timeout, TimeUnit timeUnit) {
            this.dc = dc;
            this.ip = ip;
            this.maxHops = maxHops;
            this.tid = tid;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
        }

        @Override
        public void run() {
            ACTIVE_TASKS.add(this);
            // agent 存在，向 agent 发起请求获取数据
            if (Optional.ofNullable(dc).map(Dc::getAgent).isPresent()) {
                Promserver promserver = dc.getAgent();
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    String path = "/cmd/traceroute";
                    Map<String, Object> body = new HashMap<>();
                    body.put("ips", Tool.ListUtil.of(ip));
                    body.put("maxHops", Integer.valueOf(maxHops));
                    String resultJsonStr = AgentUtil.request(promserver, HttpMethod.POST, path, null, body, String.class);
                    Object list = JSONPath.read(resultJsonStr, "data.list");
                    log.info("host: {}, path: {}, result: {}", promserver.getHost(), path, resultJsonStr);
                    // 由于 future.cancel 在任务已经运行状态下，不一定能取消，所以在写 redis 前增加判断
                    if (isWrite) {
                        List<Map> mapList = JSONArray.parseArray(JSONArray.toJSONString(list), Map.class);
                        for (Map map : mapList) {
                            Map dcMap = Tool.MapUtil.newHashMap(4);
                            dcMap.put("id", dc.getId());
                            dcMap.put("name", dc.getName());

                            Map agent = Tool.MapUtil.newHashMap(4);
                            agent.put("id", promserver.getId());
                            agent.put("name", promserver.getName());

                            map.put("dc", dcMap);
                            map.put("agent", agent);

                            // source 赋值为 agent ip
                            map.put("source", promserver.getHost());
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
                    log.error(String.format("%s agent traceroute %s timeout, Task cancel status: %s", promserver.getName(), ip, cancel));

                    if (isRun) {
                        log.info(String.format("%s agent traceroute %s timeout, Timeout data write to redis", promserver.getName(), ip));
                        // 超时也需要向 redis 中写入 timeout 消息
                        Map agent = Tool.MapUtil.newHashMap(4);
                        agent.put("id", promserver.getId());
                        agent.put("name", promserver.getName());

                        Map dcMap = Tool.MapUtil.newHashMap(4);
                        dcMap.put("id", dc.getId());
                        dcMap.put("name", dc.getName());
                        Map timeout = Tool.MapUtil.of(
                                Pair.of("ip", ip),
                                Pair.of("state", 2),
                                Pair.of("time", null),
                                Pair.of("source", promserver.getHost()),
                                Pair.of("raw", "Timeout"),
                                Pair.of("agent", agent),
                                Pair.of("dc", dcMap)
                        );
                        List<Map> list = Tool.ListUtil.of(timeout);
                        redisTemplate.opsForList().leftPush(Tool.StrUtil.concat(true, "test_traceroute_result_", tid), JSONObject.toJSONString(list));
                    } else {
                        log.info(String.format("%s agent traceroute %s timeout, But the thread is interrupted, and the timeout data is not write to redis.", promserver.getName(), ip));
                    }
                }
            } else {
                // agent 不存在（DC 下没有可用 agent,记录错误数据至 REDIS 中）
                if (isRun) {
                    log.info(String.format("Dc info : %s , No PromServer available", JSONObject.toJSONString(dc)));
                    Map dcMap = Tool.MapUtil.newHashMap(4);
                    dcMap.put("id", dc.getId());
                    dcMap.put("name", dc.getName());
                    Map noAgentInfo = Tool.MapUtil.of(
                            Pair.of("ip", ip),
                            Pair.of("state", 2),
                            Pair.of("time", null),
                            Pair.of("source", ""),
                            Pair.of("raw", "No agent available"),
                            Pair.of("agent", Tool.MapUtil.empty()),
                            Pair.of("dc", dcMap)
                    );
                    List<Map> list = Tool.ListUtil.of(noAgentInfo);
                    redisTemplate.opsForList().leftPush(Tool.StrUtil.concat(true, "test_traceroute_result_", tid), JSONObject.toJSONString(list));
                } else {
                    log.info(String.format("Dc info : %s , No PromServer available, But the thread is interrupted, not write to redis.", JSONObject.toJSONString(dc)));
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
