package com.nis.modules.sys.service.impl;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.utils.HttpClientUtils;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysInfoService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service("sysInfoService")
public class SysInfoServiceImpl implements SysInfoService {

    private static Log log = Log.get();

    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private PromserverDao promserverDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Map getAboutInfo() {
        Map resultMap = new HashMap(4);
        // 1. nehza 系统版本
        SysConfigEntity sysVersion = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_SYSTEM_VERSION));
        Map sysVersionMap = new HashMap(4);
        sysVersionMap.put("name", "Nezha Monitoring System");
        sysVersionMap.put("version", sysVersion.getParamValue());

        resultMap.put("nezha", sysVersionMap);

        // 2. 组件版本信息  mariaDB redis prometheus
        List<Map> componentVersionInfoList = new ArrayList<>();

        // ① mariaDB version
        Map componentVersion = new HashMap(4);
        String dataBasesVersion = sysConfigDao.getDataBasesVersion();
        componentVersion.put("name", "Mariadb");
        componentVersion.put("version", dataBasesVersion);
        componentVersionInfoList.add(componentVersion);

        // ② redis version
        Object redisVersion = redisTemplate.execute(new RedisCallback<String>() {
            @Override
            public String doInRedis(RedisConnection connection) throws DataAccessException {
                RedisServerCommands redisServerCommands = connection.serverCommands();
                Properties info = redisServerCommands.info();
                return info.get("redis_version").toString();
            }
        });
        componentVersion = new HashMap(4);
        componentVersion.put("name", "Redis");
        componentVersion.put("version", redisVersion.toString());
        componentVersionInfoList.add(componentVersion);

        // ③ prometheus version 随机获取可用promserver
        List<Promserver> healthyPromserverList = promserverDao.selectList(new QueryWrapper<Promserver>().lambda().eq(true, Promserver::getStatus, 1));
        if (CollectionUtils.isNotEmpty(healthyPromserverList)) {
            Promserver server = healthyPromserverList.get(RandomUtils.nextInt(0, healthyPromserverList.size()));
            URL url = null;
            try {
                String buildinfoUrl = "/api/v1/status/buildinfo";
                url = new URL("http", server.getHost(), server.getPort(), buildinfoUrl);
                JSONObject jsonObject = HttpClientUtils.httpGet(url.toString());
                Object data = jsonObject.get("data");
                if (ToolUtil.isNotEmpty(data)) {
                    Map map = JSONObject.parseObject(data.toString(), Map.class);
                    Object version = map.get("version");
                    componentVersion = new HashMap(4);
                    componentVersion.put("name", "Prometheus");
                    componentVersion.put("version", version.toString());
                    componentVersionInfoList.add(componentVersion);
                }
            } catch (IOException e) {
                log.error(String.format("获取prometheus版本信息接口失败，promserver 信息是: %s, URL: %s, 错误信息: %s", JSONObject.toJSONString(server), url, e));
            }
        }

        resultMap.put("components", componentVersionInfoList);
        return resultMap;
    }

    @Override
    public Map getGitVersionInfo() {
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("git.properties");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));) {
            StringBuilder versionJson = null;
            String line;
            versionJson = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                versionJson.append(line);
            }

            JSONObject jsonObject = JSON.parseObject(versionJson.toString());
            Set<Map.Entry<String, Object>> entrySet = jsonObject.entrySet();
            if (CollectionUtils.isNotEmpty(entrySet)) {
                // 默认排序 a-z
                Map<String, Object> sortMap = new TreeMap<>(new Comparator<String>() {
                    public int compare(String obj1, String obj2) {
                        return obj1.compareTo(obj2);
                    }
                });
                Map<String, Object> collect = entrySet.stream()
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (o, n) -> n));
                sortMap.putAll(collect);
                return sortMap;
            }
        } catch (IOException e) {
            log.error("读取 git.properties 文件信息失败，错误信息是：" + e.getMessage(), e);
        }
        return Collections.emptyMap();
    }
}