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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tool")
public class PingController {

    private final static Log log = Log.get();

    @Autowired
    private PromserverService agentService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DcService dcService;

    static final List<Thread> ACTIVE_TASKS = Collections.synchronizedList(new ArrayList<>());

    @GetMapping("/ping")
    public R ping(@RequestParam Map<String, Object> params) {
        String ip = (String) params.get("ip");
        String dcIds = (String) params.get("dcIds");
        String timeout = Tool.ObjectUtil.defaultIfEmpty((String) params.get("timeout"), "30");

        // 根据 dcids 获取当前 state = up 的所有 agent
        List<Promserver> promserverList = agentService.list(new LambdaQueryWrapper<Promserver>()
                .in(Tool.StrUtil.isNotEmpty(dcIds), Promserver::getDcId, Tool.StrUtil.split(dcIds, ','))
                .eq(Promserver::getStatus, 1));

        String tid = UUID.randomUUID().toString();

        List<Dc> dcList = dcService.list();
        Map<Integer, String> dcIdAndNameMap = dcList.stream().collect(Collectors.toMap(Dc::getId, Dc::getName));

        for (Promserver promserver : promserverList) {
            PingRunnable pingRunnable = new PingRunnable(promserver, ip, tid, Long.valueOf(timeout), TimeUnit.SECONDS, dcIdAndNameMap);
            pingRunnable.setName(Tool.StrUtil.concat(true, tid, "-", promserver.getName()));
            ThreadUtil.execAsync(pingRunnable);
        }

        Map m = Tool.MapUtil.newHashMap();
        m.put("tid", tid);
        Map task = Tool.MapUtil.newHashMap();
        task.put("total", promserverList.size());
        task.put("done", 0);
        task.put("process", 0);
        m.put("task", task);

        // 设置 30分钟超时，避免 redis 中存在垃圾数据
        redisTemplate.opsForValue().set(Tool.StrUtil.concat(true, "test_ping_stat_", tid), JSONObject.toJSONString(task), 30, TimeUnit.MINUTES);
        return R.ok(m);
    }

    @GetMapping("/ping/result/{tid}")
    public R pingResult(@PathVariable(value = "tid", required = true) String tid) {
        String pingResultKey = Tool.StrUtil.concat(true, "test_ping_result_", tid);
//        List<String> pingResultList = redisTemplate.opsForList().range(pingResultKey, 0, -1);
        List<String> pingResultList = Tool.ListUtil.list(false);
        while (true) {
            String obj = (String) redisTemplate.opsForList().rightPop(pingResultKey);
            if (obj == null) break;
            pingResultList.add(obj);
        }

        String pingStatStr = (String) redisTemplate.opsForValue().get(Tool.StrUtil.concat(true, "test_ping_stat_", tid));
        // 任务已被取消或 key 超时
        if (Tool.StrUtil.isEmpty(pingStatStr)) {
            Map result = Tool.MapUtil.newHashMap();
            Map<String, Integer> task = Tool.MapUtil.of(Pair.of("total", 0), Pair.of("done", 0), Pair.of("process", 0));
            result.put("task", task);
            result.put("list", Tool.ListUtil.empty());
            return R.ok(result);
        }

        Map pingStatMap = JSONObject.parseObject(pingStatStr, Map.class);
        Integer total = (Integer) pingStatMap.get("total");
        Integer done = (Integer) pingStatMap.get("done");

        done = done + pingResultList.size();
        pingStatMap.put("done", done);
        String percent = NumberUtil.formatPercent((double) done / total, 2);
        pingStatMap.put("process", percent.replace("%", ""));

        // 更新 redis test_ping_stat  设置 30 分钟超时，避免 redis 中存在垃圾数据
        redisTemplate.opsForValue().set(Tool.StrUtil.concat(true, "test_ping_stat_", tid), JSONObject.toJSONString(pingStatMap), 30, TimeUnit.MINUTES);

        Map result = Tool.MapUtil.newHashMap();
        result.put("task", pingStatMap);
        List list = Tool.ListUtil.list(false);
        pingResultList.stream().forEach(jsonArrayStr -> {
            JSONArray jsonArray = JSONArray.parseArray(jsonArrayStr);
            list.addAll(jsonArray);
        });
        result.put("list", list);
        return R.ok(result);
    }

    @GetMapping("/ping/cancel/{tid}")
    public R pingCancel(@PathVariable(value = "tid", required = false) String tid) {
        if (Tool.CollectionUtil.isNotEmpty(ACTIVE_TASKS)) {
            ACTIVE_TASKS.stream().filter(thread -> Tool.StrUtil.startWith(thread.getName(), tid)).forEach(thread -> {
                log.info(String.format("Ping thread: %s has been canceled", thread.getName()));
                thread.interrupt();
            });
        }

        List<String> keys = Tool.ListUtil.of(
                Tool.StrUtil.concat(true, "test_ping_stat_", tid),
                Tool.StrUtil.concat(true, "test_ping_result_", tid));
        redisTemplate.delete(keys);
        return R.ok();
    }

    public class PingRunnable extends Thread {
        private Promserver promserver;
        private String ip;
        private String tid;
        private long timeout;
        private TimeUnit timeUnit;
        private Map<Integer, String> dcIdAndNameMap;

        private volatile boolean isWrite = true;
        private volatile boolean timeoutToWrite = true;

        public PingRunnable(Promserver promserver, String ip, String tid, long timeout, TimeUnit timeUnit, Map<Integer, String> dcIdAndNameMap) {
            this.promserver = promserver;
            this.ip = ip;
            this.tid = tid;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            this.dcIdAndNameMap = dcIdAndNameMap;
        }

        @Override
        public void run() {
            ACTIVE_TASKS.add(this);
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                String path = "/cmd/ping";
                Map<String, List<String>> body = Tool.MapUtil.of(Pair.of("ips", Tool.ListUtil.of(ip)));
                String resultJsonStr = AgentUtil.request(promserver, HttpMethod.POST, path, null, body, String.class);
                Object list = JSONPath.read(resultJsonStr, "data.list");
                log.info("host: {}, path: {}, result: {}", promserver.getHost(), path, resultJsonStr);
                // 由于 future.cancel 在任务已经运行状态下，不一定能取消，所以在写 redis 前增加判断
                if (isWrite) {
                    List<Map> mapList = JSONArray.parseArray(JSONArray.toJSONString(list), Map.class);
                    for (Map map : mapList) {
                        Map dc = Tool.MapUtil.newHashMap(4);
                        dc.put("id", promserver.getDcId());
                        dc.put("name", dcIdAndNameMap.get(promserver.getDcId()));

                        Map agent = Tool.MapUtil.newHashMap(4);
                        agent.put("id", promserver.getId());
                        agent.put("name", promserver.getName());

                        map.put("dc", dc);
                        map.put("agent", agent);
                    }
                    redisTemplate.opsForList().leftPush(Tool.StrUtil.concat(true, "test_ping_result_", tid), JSONArray.toJSONString(mapList));
                } else {
                    log.info(String.format("Ping thread: %s has been canceled, Do not write data to Redis", this.getName()));
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
                log.error(String.format("%s agent ping %s timeout, Task cancel status: %s", promserver.getName(), ip, cancel));

                if (timeoutToWrite) {
                    log.info(String.format("%s agent ping %s timeout, Timeout data write to redis", promserver.getName(), ip));
                    // 超时也需要向 redis 中写入 timeout 消息
                    Map agent = Tool.MapUtil.newHashMap(4);
                    agent.put("id", promserver.getId());
                    agent.put("name", promserver.getName());

                    Map dc = Tool.MapUtil.newHashMap(4);
                    dc.put("id", promserver.getDcId());
                    dc.put("name", dcIdAndNameMap.get(promserver.getDcId()));
                    Map timeout = Tool.MapUtil.of(
                            Pair.of("ip", ip),
                            Pair.of("state", 2),
                            Pair.of("rate", 0),
                            Pair.of("min", 0),
                            Pair.of("avg", 0),
                            Pair.of("max", 0),
                            Pair.of("source", promserver.getHost()),
                            Pair.of("raw", "Timeout"),
                            Pair.of("agent", agent),
                            Pair.of("dc", dc)
                    );
                    List<Map> list = Tool.ListUtil.of(timeout);
                    redisTemplate.opsForList().leftPush(Tool.StrUtil.concat(true, "test_ping_result_", tid), JSONObject.toJSONString(list));
                } else {
                    log.info(String.format("%s agent ping %s timeout, But the thread is interrupted, and the timeout data is not write to redis.", promserver.getName(), ip));
                }
            }
            ACTIVE_TASKS.remove(this);
        }

        @Override
        public void interrupt() {
            super.interrupt();
            isWrite = false;
            timeoutToWrite = false;
        }
    }

}
