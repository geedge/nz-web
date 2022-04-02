package com.nis.modules.tool.service.impl;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.util.ReUtil;
import cn.hutool.log.Log;
import com.google.common.base.Stopwatch;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.tool.entity.PingEntity;
import com.nis.modules.tool.service.CmdService;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Service
public class CmdServiceImpl implements CmdService {

    private Log log = Log.get();

    // windows ipv4/6 匹配 最短 = 3ms，最长 = 6ms，平均 = 4ms
    static Pattern WinPingStatRegex = Pattern.compile("(.{2} = \\d+ms)");
    // windows ipv4/6 匹配 25% 丢失
    static Pattern WinPingRateRegex = Pattern.compile("\\d+(\\.\\d+)?% 丢失");
    // linux ipv4/6 匹配 rtt min/avg/max/mdev = 0.788/1.296/1.627/0.319 ms
    static Pattern LinuxPingStatRegex = Pattern.compile("(min/avg/max.*=.*ms)");
    // linux ipv4/6 匹配 25% packet loss
    static Pattern LinuxPingRateRegex = Pattern.compile("\\d+(\\.\\d+)?% packet loss");

    static ThreadPoolExecutor execPingThreadPoolExecutor;
    static ThreadPoolExecutor execTracerouteThreadPoolExecutor;

    static Integer defaultCorePoolSize = 10;

    static {
        execPingThreadPoolExecutor = new ThreadPoolExecutor(defaultCorePoolSize, Integer.MAX_VALUE,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setNamePrefix("ping-pool-").build());

        execTracerouteThreadPoolExecutor = new ThreadPoolExecutor(defaultCorePoolSize, Integer.MAX_VALUE,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setNamePrefix("traceroute-pool-").build());
    }

    @Override
    public List ping(PingEntity pingEntity) throws UnknownHostException {
        Integer packetsize = Tool.ObjectUtil.defaultIfNull(pingEntity.getPacketsize(), 64);
        Integer count = Tool.ObjectUtil.defaultIfNull(pingEntity.getCount(), 4);
        Integer timeout = Tool.ObjectUtil.defaultIfNull(pingEntity.getTimeout(), 100);
        Integer concurrent = Tool.ObjectUtil.defaultIfNull(pingEntity.getConcurrent(), 10);

        List<Callable<Map<String, Object>>> taskList = Tool.ListUtil.list(false);

        // 获取本地 IP 当作 source ip
        String sourceIp = Tool.InetAddressUtil.getLocalHostLANAddress();
        List<String> ips = pingEntity.getIps();
        for (String ip : ips) {
            if (Tool.StrUtil.equals(Constant.OSType.Linux.toString(), Tool.OSUtil.getOSname())) {
                String pingCmd = null;
                if (CommonUtils.checkIpv4(ip)) {
                    pingCmd = String.format("ping -c %s -s %s -W %s %s", count, packetsize, (double) timeout / 1000, ip);
                } else if (CommonUtils.checkIpv6(ip)) {
                    pingCmd = String.format("ping -6 -c %s -s %s -W %s %s", count, packetsize, (double) timeout / 1000, ip);
                } else {
                    // 都没有匹配就执行IPV4
                    pingCmd = String.format("ping -c %s -s %s -W %s %s", count, packetsize, (double) timeout / 1000, ip);
                }
                log.info("ping cmd : {}", pingCmd);
                Callable<Map<String, Object>> callable = this.getLinuxPingCallable(ip, pingCmd, sourceIp);
                taskList.add(callable);
            } else if (Tool.StrUtil.equals(Constant.OSType.Windows.toString(), Tool.OSUtil.getOSname())) {
                String pingCmd = null;
                if (CommonUtils.checkIpv4(ip)) {
                    pingCmd = String.format("ping -n %s -l %s -w %s %s", count, packetsize, timeout, ip);
                } else if (CommonUtils.checkIpv6(ip)) {
                    pingCmd = String.format("ping -6 -n %s -l %s -w %s %s", count, packetsize, timeout, ip);
                } else {
                    // 都没有匹配就执行IPV4
                    pingCmd = String.format("ping -n %s -l %s -w %s %s", count, packetsize, timeout, ip);
                }
                log.info("ping cmd : {}", pingCmd);
                Callable<Map<String, Object>> callable = this.getWinPingCallable(ip, pingCmd, sourceIp);
                taskList.add(callable);
            }
        }

        List<Map<String, Object>> result = Tool.ListUtil.list(false);
        boolean b = false;
        try {
            if (concurrent > execPingThreadPoolExecutor.getCorePoolSize()) {
                // 修改线程池核心线程数
                execPingThreadPoolExecutor.setCorePoolSize(concurrent);
                b = true;
            }
            List<Future<Map<String, Object>>> futures = ((ExecutorService) execPingThreadPoolExecutor).invokeAll(taskList);
            for (Future<Map<String, Object>> future : futures) {
                if (!future.isCancelled()) {
                    Map<String, Object> map = future.get();
                    result.add(map);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error(e);
        } finally {
            // 如果修改了核心线程数 ，本次执行后恢复初始数量
            if (b) execPingThreadPoolExecutor.setCorePoolSize(defaultCorePoolSize);
        }

        return result;
    }

    private Callable<Map<String, Object>> getWinPingCallable(String ip, String pingCmd, String sourceIp) {
        Callable<Map<String, Object>> callable = () -> {
            Map<String, Object> stat = Tool.MapUtil.newHashMap();
            String forStr = Tool.RuntimeUtil.execForStr(pingCmd);
            List<String> stringList = ReUtil.findAllGroup0(WinPingStatRegex, forStr);
            if (Tool.CollectionUtil.isEmpty(stringList)) {
                stat.put("min", 0);
                stat.put("max", 0);
                stat.put("avg", 0);
                // 没有匹配到 min max avg 数据，证明百分百丢包 windows 如果ping 全部失败返回为 0 ，这里不取他返回的数据，全部失败丢包率为 100%
                stat.put("rate", 100);
                stat.put("state", 0);
            } else {
                stat.put("min", ReUtil.getFirstNumber(stringList.get(0)));
                stat.put("max", ReUtil.getFirstNumber(stringList.get(1)));
                stat.put("avg", ReUtil.getFirstNumber(stringList.get(2)));
                String rateStr = ReUtil.getGroup0(WinPingRateRegex, forStr);
                Double rateDoubleValue = Double.valueOf(rateStr.split("%")[0]);
                stat.put("rate", rateDoubleValue);
                // 丢包率为0，state = up 否则为 error
                stat.put("state", rateDoubleValue == 0 ? 1 : 2);
            }
            stat.put("raw", forStr);
            stat.put("ip", ip);
            stat.put("source", sourceIp);
            return stat;
        };
        return callable;
    }

    private Callable<Map<String, Object>> getLinuxPingCallable(String ip, String pingCmd, String sourceIp) {
        Callable<Map<String, Object>> callable = () -> {
            Map<String, Object> stat = Tool.MapUtil.newHashMap();
            String forStr = Tool.RuntimeUtil.execForStr(pingCmd);
            String group0 = ReUtil.getGroup0(LinuxPingStatRegex, forStr);
            if (Tool.StrUtil.isEmpty(group0)) {
                stat.put("min", 0);
                stat.put("max", 0);
                stat.put("avg", 0);
                stat.put("rate", 100);
                stat.put("state", 0);
            } else {
                List<String> strings = Arrays.asList(group0.split("="));
                List<String> minAvgMaxList = Arrays.asList(strings.get(1).trim().split("/"));
                stat.put("min", Double.valueOf(minAvgMaxList.get(0)));
                stat.put("max", Double.valueOf(minAvgMaxList.get(2)));
                stat.put("avg", Double.valueOf(minAvgMaxList.get(1)));
                String rateStr = ReUtil.getGroup0(LinuxPingRateRegex, forStr);
                Double rateDoubleValue = Double.valueOf(rateStr.split("%")[0]);
                // 丢包率为0，state = up 否则为 error
                stat.put("state", rateDoubleValue == 0 ? 1 : 2);
            }

            stat.put("raw", forStr);
            stat.put("ip", ip);
            stat.put("source", sourceIp);
            return stat;
        };
        return callable;
    }


    @Override
    public List traceroute(PingEntity pingEntity) throws UnknownHostException {
        // Linux 可以指定 package size ,win 不行
        Integer packetsize = Tool.ObjectUtil.defaultIfNull(pingEntity.getPacketsize(), 64);
        Integer maxHops = Tool.ObjectUtil.defaultIfNull(pingEntity.getMaxHops(), 10);
        // win 单位 ms,,linux 单位 s,(支持浮点数)
        Integer timeout = Tool.ObjectUtil.defaultIfNull(pingEntity.getTimeout(), 100);
        Integer concurrent = Tool.ObjectUtil.defaultIfNull(pingEntity.getConcurrent(), 10);

        List<Callable<Map<String, Object>>> taskList = Tool.ListUtil.list(false);

        // 获取本地 IP 当作 source ip
        String sourceIp = Tool.InetAddressUtil.getLocalHostLANAddress();

        List<String> ips = pingEntity.getIps();
        for (String ip : ips) {
            if (Tool.StrUtil.equals(Constant.OSType.Linux.toString(), Tool.OSUtil.getOSname())) {
                String tracerouteCmd = null;
                if (CommonUtils.checkIpv4(ip)) {
                    tracerouteCmd = String.format("traceroute -4 -m %s -w %s %s %s", maxHops, (double) timeout / 1000, ip, packetsize);
                } else if (CommonUtils.checkIpv6(ip)) {
                    tracerouteCmd = String.format("traceroute -6 -m %s -w %s %s %s", maxHops, (double) timeout / 1000, ip, packetsize);
                } else {
                    // 都没有匹配就执行IPV4
                    tracerouteCmd = String.format("traceroute -4 -m %s -w %s %s %s", maxHops, (double) timeout / 1000, ip, packetsize);
                }
                log.info("traceroute cmd : {}", tracerouteCmd);
                Callable<Map<String, Object>> callable = this.getTracerouteCallable(ip, tracerouteCmd, sourceIp);
                taskList.add(callable);
            } else if (Tool.StrUtil.equals(Constant.OSType.Windows.toString(), Tool.OSUtil.getOSname())) {
                String tracerouteCmd = null;
                if (CommonUtils.checkIpv4(ip)) {
                    tracerouteCmd = String.format("tracert -4 -h %s -w %s %s", maxHops, timeout, ip);
                } else if (CommonUtils.checkIpv6(ip)) {
                    tracerouteCmd = String.format("tracert -6 -h %s -w %s %s", maxHops, timeout, ip);
                } else {
                    // 都没有匹配就执行IPV4
                    tracerouteCmd = String.format("tracert -4 -h %s -w %s %s", maxHops, timeout, ip);
                }
                log.info("traceroute cmd : {}", tracerouteCmd);
                Callable<Map<String, Object>> callable = this.getTracerouteCallable(ip, tracerouteCmd, sourceIp);
                taskList.add(callable);
            }
        }

        List<Map<String, Object>> result = Tool.ListUtil.list(false);
        boolean b = false;
        try {
            if (concurrent > execTracerouteThreadPoolExecutor.getCorePoolSize()) {
                // 修改线程池核心线程数
                execTracerouteThreadPoolExecutor.setCorePoolSize(concurrent);
                b = true;
            }
            List<Future<Map<String, Object>>> futures = ((ExecutorService) execTracerouteThreadPoolExecutor).invokeAll(taskList);
            for (Future<Map<String, Object>> future : futures) {
                if (!future.isCancelled()) {
                    Map<String, Object> map = future.get();
                    result.add(map);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error(e);
        } finally {
            // 如果修改了核心线程数 ，本次执行后恢复初始数量
            if (b) execTracerouteThreadPoolExecutor.setCorePoolSize(defaultCorePoolSize);
        }

        return result;
    }

    private Callable<Map<String, Object>> getTracerouteCallable(String ip, String tracerouteCmd, String sourceIp) {
        Callable<Map<String, Object>> callable = () -> {
            Map<String, Object> stat = Tool.MapUtil.newHashMap();
            Stopwatch watch = Stopwatch.createStarted();
            Process process = Tool.RuntimeUtil.exec(tracerouteCmd);
            String result = Tool.RuntimeUtil.getResult(process);
            stat.put("time", watch.elapsed(TimeUnit.MILLISECONDS));
            // {0} indicates normal termination.
            int i = process.waitFor();
            stat.put("state", i == 0 ? 1 : 0);
            stat.put("raw", result);
            stat.put("ip", ip);
            stat.put("source", sourceIp);
            return stat;
        };
        return callable;
    }
}
