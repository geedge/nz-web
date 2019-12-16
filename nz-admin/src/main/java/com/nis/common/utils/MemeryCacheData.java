package com.nis.common.utils;


import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class MemeryCacheData {
    public static final String CACHE_KEY_PREFIX="memery_cache_";
    public static final String PROM_SERVER_CACHE=CACHE_KEY_PREFIX+"prom_server";

    public static Map<String, List<Object>> memeryCache=new ConcurrentHashMap<String, List<Object>>();

}
