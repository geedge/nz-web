package com.nis.modules.endpoint.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.endpoint.dao.MetadataMetricDao;
import com.nis.modules.endpoint.entity.MetadataMetric;
import com.nis.modules.endpoint.service.MetadataLabelService;
import com.nis.modules.endpoint.service.MetadataMetricService;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.service.impl.PromserverProxyServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.util.*;

@Service
public class MetadataMetricServiceImpl extends ServiceImpl<MetadataMetricDao, MetadataMetric> implements MetadataMetricService {

    private static Logger logger = LoggerFactory.getLogger(MetadataMetricServiceImpl.class);

    @Autowired
    private ModuleService moduleService;
    @Autowired
    private MetadataLabelService labelService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private PromserverProxyServiceImpl promserverProxyService;

    @Value("${nezha.promApis.metadata}")
    private String apiMetadata;

    @Value("${nezha.promApis.series}")
    private String apiSeries;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Integer moduleId=null;
        try{

            moduleId = params.get("moduleId")==null?null:Integer.parseInt((String)params.get("moduleId"));
        }catch (NumberFormatException e){}

        String metric=params.get("metric")==null?null:(String)params.get("metric");


        IPage<MetadataMetric> page = this.page(new Query<MetadataMetric>().getPage(params));

        List<MetadataMetric> list=this.baseMapper.queryMetricList(page,moduleId,metric);

        page.setRecords(list);

        return new PageUtils(page);
    }

    @Override
    public List<Integer> queryIdsByMetric(String metric) {
        return this.baseMapper.queryIdsByMetric(metric);

    }

    @Override
    public Integer queryIdByMetric(String metric) {
        return this.baseMapper.queryIdByMetric(metric);
    }

    @Override
    public List<MetadataMetric> metadata(Integer endpointId) {
        // 返回结果
        List<MetadataMetric> metrics = new ArrayList<>();

        // 从redis中根据endpointId获取promserver id
        Object promeId = redisTemplate.opsForHash().get(Constant.REDIS_KEY_ENDPOINT_PROM, endpointId.toString());

        if (promeId != null) {
            Promserver promserver = promserverService.getById(promeId.toString());

            if (promserver == null) {
                // 没有找到指定的 promserver
                throw new NZException(RCode.PROMSERVER_NOTEXSITS);
            }

            Map queryParam = new HashMap();
            String endpoint = "{" + Constant.PROMSERVERUP_ENDPOINT_LABEL + "=\"" + endpointId + "\"}";
            queryParam.put("match_target", DigestUtils.urlEncode(endpoint));

            // 处理请求url
            String url = this.buildUrl(promserver.getHost(), promserver.getPort(), apiMetadata, queryParam);

            logger.debug("query api/v1/targets/metadata url {}", url);

            Map result = promserverProxyService.requestPromAPI(url);
            // 查询成功的
            if (result != null && result.get("status").equals("success")) {
                JSONArray array = (JSONArray) result.get("data");
                JSONObject jsonObject;
                MetadataMetric metadataMetric;
                for (int i = 0; i < array.size(); i++) {
                    jsonObject = (JSONObject) array.get(i);
                    metadataMetric = JSONObject.parseObject(jsonObject.toJSONString(), MetadataMetric.class);
                    if (metadataMetric != null) {
                        metrics.add(metadataMetric);
                    }
                }
                // 去除重复数据
                metrics = new ArrayList<>(new HashSet<>(metrics));
                return metrics;
            } else {
                logger.error("query api/v1/targets/metadata error {}", promserver);
            }
        }
        return metrics;
    }

    @Override
    public Map series(Map<String, Object> params) {
        if (!params.containsKey("match[]") || StringUtils.isBlank((String) params.get("match[]"))) {
            throw new NZException(RCode.PROMSERVER_PARAM_MATCH_ISNULL);
        } else {
            String matchsParam = (String) params.get("match[]");
            String[] matchs = matchsParam.split(",");

            params.put("match[]", Arrays.asList(matchs));
        }

        // 处理时间
        if (params.containsKey("start") && StringUtils.isNotEmpty((String) params.get("start"))) {
            params.put("start", promserverProxyService.formatTimeToUnixTimestemp((String) params.get("start")));
        }

        if (params.containsKey("end") && StringUtils.isNotEmpty((String) params.get("end"))) {
            params.put("end", promserverProxyService.formatTimeToUnixTimestemp((String) params.get("end")));
        }

        // global promserver
        List<Promserver> promservers = promserverService.list(new QueryWrapper<Promserver>().eq("type", 1).eq("status", 1));
        // 随机取一个global promserver
        int randomPos = new Random().nextInt(promservers.size());
        Promserver promserver = promservers.get(randomPos);

        // 结果集去重 标签名称切标签值相同则去重
        Set<Map.Entry<String, Object>> set = new HashSet();
        // 处理请求url
        String url = this.buildUrl(promserver.getHost(), promserver.getPort(), apiSeries, params);
        Map result = promserverProxyService.requestPromAPI(url);
        // 查询成功的
        if (result != null && result.get("status").equals("success")) {
            // 所有标签
            JSONArray array = (JSONArray) result.get("data");

            // 所有标签去重
            for (int i = 0; i < array.size(); i++) {
                JSONObject jsonObject = (JSONObject) array.get(i);
                Set<Map.Entry<String, Object>> entries = jsonObject.entrySet();
                for (Map.Entry<String, Object> entry : entries) {
                    if ("__name__".equals(entry.getKey())) {
                        continue;
                    }
                    set.add(entry);
                }
            }
        } else {
            logger.error("query api/v1/targets/series error {}", promserver);
        }


        Set<String> keySet = new HashSet();
        for (Map.Entry<String, Object> entry : set) {
            keySet.add(entry.getKey());
        }

        // 组织返回结果
        Map resultData = new HashMap();
        List values;
        for (String key : keySet) {
            values = new ArrayList<>();
            for (Map.Entry<String, Object> entry : set) {
                if (key.equals(entry.getKey())) {
                    values.add(entry.getValue());
                }
            }
            resultData.put(key, values);
        }
        return resultData;
    }

    /**
     * 处理请求prometheus 的url
     *
     * @param host
     * @param port
     * @param baseUrl
     * @param params
     * @return
     */
    public String buildUrl(String host, Integer port, String baseUrl, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("http://");
        sb.append(host);
        sb.append(":" + port);
        sb.append(baseUrl); //url
        if (params != null && params.size() > 0) {
            sb.append("?");
            Set<Map.Entry<String, Object>> entries = params.entrySet();
            boolean isArray = false;
            for (Map.Entry<String, Object> entry : entries) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String parseValue = "";
                if (value instanceof String) {
                    parseValue = (String) value;
                    isArray = false;
                }

                if (value instanceof Long) {
                    parseValue = (Long) value + "";
                    isArray = false;
                }

                if (value instanceof List) {
                    String temp = "";
                    for (String param : (List<String>) value) {
                        try {
                            temp += (URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(param, "UTF-8") + "&");
                        } catch (Exception e) {
//                            logger.warn("urlencode faild");
                        }
                    }
                    parseValue = temp;
                    isArray = true;
                }
                if (isArray) {
                    sb.append(parseValue);
                } else {
                    try {
                        sb.append((URLEncoder.encode(key, "UTF-8") + "=" + parseValue + "&"));
                    } catch (Exception e) {
//                        logger.warn("urlencode faild");
                    }
                }
            }
            return sb.substring(0, sb.length() - 1);
        }
        return sb.toString();
    }
}
