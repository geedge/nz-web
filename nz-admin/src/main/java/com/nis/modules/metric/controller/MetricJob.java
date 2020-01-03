package com.nis.modules.metric.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.LockUtils;
import com.nis.common.utils.R;
import com.nis.modules.endpoint.dao.MetadataLabelDao;
import com.nis.modules.endpoint.dao.MetadataMetricDao;
import com.nis.modules.endpoint.entity.MetadataLabel;
import com.nis.modules.endpoint.entity.MetadataMetric;
import com.nis.modules.endpoint.service.MetadataLabelService;
import com.nis.modules.endpoint.service.MetadataMetricService;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.service.impl.PromserverProxyServiceImpl;
import com.nis.modules.romote.util.DigestUtils;
import com.nis.modules.romote.util.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.util.*;

@RestController
@RequestMapping("/metricJob")
public class MetricJob extends QuartzJobBean {

    private static Logger logger = LoggerFactory.getLogger(MetricJob.class);

    @Autowired
    private PromserverProxyServiceImpl promserverProxyService;

    @Autowired
    private MetadataMetricService metadataMetricService;

    @Autowired
    private MetadataMetricDao metadataMetricDao;

    @Autowired
    private MetadataLabelService metadataLabelService;

    @Autowired
    private MetadataLabelDao metadataLabelDao;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private LockUtils lockUtils;

    /**
     * api 方法
     */
    @Value("${nezha.promApis.metadata}")
    private String apiMetadata;

    @Value("${nezha.promApis.series}")
    private String apiSeries;

    /**
     * metric元数据周期同步方法
     *
     * @return
     */
    @GetMapping
    public R synchronizationMetrics() {
        // 做为锁的value 在释放的时候也是用它去释放 ， 保证谁设置的锁 谁释放锁
        String UID = UUID.randomUUID().toString();
        try {
            // 首先获取锁
            if (lockUtils.lock(Constant.REDIS_KEY_METRIClOCK, UID)) {
                // 获取到锁然后执行
                ValueOperations valueOperations = redisTemplate.opsForValue();

                // 缓存中的 metric 集合
                List<MetadataMetric> cacheMetricList = new ArrayList<>();
                Object cacheMetricObject = valueOperations.get(Constant.REDIS_KEY_METRICS);
                if (cacheMetricObject == null) {
                    // 数据库获取
                    cacheMetricList = metadataMetricService.list();
                    // 放回
                    if (CollectionUtils.isNotEmpty(cacheMetricList)) {
                        valueOperations.set(Constant.REDIS_KEY_METRICS, JSONArray.toJSONString(cacheMetricList));
                    }
                } else {
                    cacheMetricList = JSONObject.parseArray(String.valueOf(cacheMetricObject), MetadataMetric.class);
                }

                // 缓存中的 labels 值
                Map<String, List<MetadataLabel>> cacheLabels = new HashMap<>();
                Object cacheMetricLabels = valueOperations.get(Constant.REDIS_KEY_METRICLABELS);
                if (cacheMetricLabels == null) {
                    // 数据库获取
                    List<MetadataMetric> labelsWithMetric = metadataMetricDao.getLabelsWithMetric();
                    for (MetadataMetric metric : labelsWithMetric) {
                        cacheLabels.put(metric.getMetric(), metric.getLabels());
                    }
                    // 放回
                    if (cacheLabels != null && cacheLabels.size() > 0) {
                        valueOperations.set(Constant.REDIS_KEY_METRICLABELS, JSONArray.toJSONString(cacheLabels));
                    }
                } else {
                    cacheLabels = JSONObject.parseObject(String.valueOf(cacheMetricLabels), Map.class);
                }

                // 需要进行同步的服务集合
                List<Promserver> promservers = promserverService.list(new QueryWrapper<Promserver>().eq("type", 2));

                if (CollectionUtils.isEmpty(promservers)) {
                    logger.error("There are currently no synchronized promservers.");
                    return R.ok("There are currently no synchronized promservers.");
                }

                // 查询所有的 module
                List<Module> moduleList = moduleService.list();

                // 请求 api 的参数
                Map<String, String> matchTarget = new HashMap<>(2);
                Map<String, Object> params = new HashMap<>(2);
                Map<String, Object> matchParam = new HashMap<>(2);
                String host = null;

                for (Promserver promserver : promservers) {
                    host = promserver.getHost();
                    if (StringUtils.isEmpty(host) || !host.matches(Constant.REGEX_IP)) {
                        logger.error("ip format error {}", host);
                        continue;
                    }
            /*// 请求获取所有的 metric
            Map responseMetrics = promserverProxyService.requestPromAPI("http://" + promserver.getHost() + ":" + promserver.getPort() + "/api/v1/label/__name__/values");
            if (responseMetrics == null || responseMetrics.get("error") != null) {
                logger.info("request Prometheus Server values api with error {}", promserver.getHost(), responseMetrics.get("error"));
                continue;
            }
            List<String> metricNameList = (List) responseMetrics.get("data");*/

                    // 请求查询到的 metric 信息
                    List<MetadataMetric> metadataMetrics = new ArrayList<>();

                    for (Module module : moduleList) {
                        matchTarget.put("module", module.getName());
                        String matchTargetStr = JSONObject.toJSONString(matchTarget);
                        matchTargetStr = matchTargetStr.replaceAll("\"(\\w+)\"(\\s*:\\s*)", "$1$2").replaceAll(":\"", "=\"");

                        params.put("match_target", DigestUtils.urlEncode(matchTargetStr));
                        String metadataUrl = this.buildUrl(host, promserver.getPort(), apiMetadata, params);

                        // 请求接口
                        Map result = promserverProxyService.requestPromAPI(metadataUrl);
                        // 有异常的查询
                        if (result == null || result.get("errorType") != null) {
                            logger.error("request Prometheus Server targets metadata api with error {}", module.getName());
                            continue;
                        } else {
                            // 成功查询到数据
                            JSONArray array = (JSONArray) result.get("data");
                            for (int i = 0; i < array.size(); i++) {
                                JSONObject jsonObject = (JSONObject) array.get(i);
                                MetadataMetric metadataMetric = JSONObject.parseObject(jsonObject.toJSONString(), MetadataMetric.class);
                                if (metadataMetric != null) {
                                    metadataMetrics.add(metadataMetric);
                                }
                            }
                        }
                    }

                    // metadata Metric 同步入库
                    if (CollectionUtils.isNotEmpty(metadataMetrics)) {
                        // 去除重复数据
                        metadataMetrics = new ArrayList<>(new HashSet<>(metadataMetrics));
                        // 判断缓存中是否为空
                        if (CollectionUtils.isNotEmpty(cacheMetricList)) {
                            // 数据库中存在 但是新查出来的不在
                            List<MetadataMetric> list = this.listCompare(cacheMetricList, metadataMetrics);
                            if (CollectionUtils.isNotEmpty(list)) {
                                // 1. 删除 metric 数据
                                List ids = new ArrayList();
                                for (MetadataMetric metadataMetric : list) {
                                    ids.add(metadataMetric.getId());
                                }
                                metadataMetricService.removeByIds(ids);
                                // 2. 删除关联label 数据
                                metadataLabelDao.deleteByMetricIds(ids);
                                // 3. 更新缓存数据
                                for (MetadataMetric metric : list) {
                                    cacheLabels.remove(metric.getMetric());
                                }
                            }
                            // 之后新的 metadataMetrics 数据为新增的 数据库中不存在
                            metadataMetrics.removeAll(cacheMetricList);
                            metadataMetricService.saveBatch(metadataMetrics);
                        } else {
                            // 不存在缓存数据 直接入库
                            metadataMetricService.saveBatch(metadataMetrics);
                        }
                    }

                    // 查询当前数据库 metric 信息
                    Map<String, Integer> metricNameAndId = new HashMap<>();
                    List<MetadataMetric> metricList = metadataMetricService.list();

                    if (CollectionUtils.isNotEmpty(metricList)) {
                        // 更新缓存数据
                        valueOperations.set(Constant.REDIS_KEY_METRICS, JSONArray.toJSONString(metricList));
                    }

                    for (MetadataMetric metric : metricList) {
                        // 存放的为 metricName  metricId
                        metricNameAndId.put(metric.getMetric(), metric.getId());
                    }

                    List<MetadataLabel> metadataLabels = new ArrayList<>();

                    for (MetadataMetric metric : metricList) {// 当前 metric 集合
                        String metricName = metric.getMetric();

                        matchParam.put("match[]", metricName);
                        String seriesUrl = this.buildUrl(host, promserver.getPort(), apiSeries, matchParam);

                        // 请求API 获取结果
                        Map result = promserverProxyService.requestPromAPI(seriesUrl);

                        // 当前请求查询得到的 label 集合
                        List<MetadataLabel> lablesByMetric = new ArrayList<>();
                        if (result == null || result.get("errorType") != null) {
                            logger.error("request Prometheus Server series api with error {}", metricName);
                            continue;
                        } else {
                            // 结果集去重 标签名称切标签值相同则去重
                            Set<Map.Entry<String, Object>> set = new HashSet();

                            // 所有标签
                            JSONArray array = (JSONArray) result.get("data");

                            if (array.size() > 0) {
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

                                // 缓存处理 缓存中有当前 metric 的 labels集合
                                if (cacheLabels != null && CollectionUtils.isNotEmpty(cacheLabels.get(metricName))) {
                                    // 缓存中的当前 metric 关联的 label集合
                                    List<MetadataLabel> labels = new ArrayList<>();
                                    labels = JSONObject.parseArray(JSON.toJSONString(cacheLabels.get(metricName)), MetadataLabel.class);
                                    // 这次查询的标签集合
                                    for (Map.Entry<String, Object> entry : set) {
                                        if (metricNameAndId.get(metricName) != null) {
                                            lablesByMetric.add(new MetadataLabel(metricNameAndId.get(metricName), entry.getKey(), entry.getValue().toString()));
                                        }
                                    }
                                    // 使用新查询到的标签集合去除缓存中的标签集合 剩下的保存入库 做增量处理
                                    lablesByMetric.removeAll(labels);
                                    // 更新缓存
                                    labels.addAll(lablesByMetric);
                                    cacheLabels.put(metricName, labels);
                                } else {
                                    // 没有缓存的情况保存数据
                                    for (Map.Entry<String, Object> entry : set) {
                                        if (metricNameAndId.get(metricName) != null) {
                                            lablesByMetric.add(new MetadataLabel(metricNameAndId.get(metricName), entry.getKey(), entry.getValue().toString()));
                                        }
                                    }
                                    // 更新缓存
                                    cacheLabels.put(metricName, lablesByMetric);
                                }
                            }
                        }

                        // 将查询到结果集合汇总
                        metadataLabels.addAll(lablesByMetric);
                    }

                    if (cacheLabels != null && cacheLabels.size() > 0) {
                        // 更新缓存
                        valueOperations.set(Constant.REDIS_KEY_METRICLABELS, JSONArray.toJSONString(cacheLabels));
                    }

                    // 保存 labels 数据
                    if (CollectionUtils.isNotEmpty(metadataLabels)) {
                        metadataLabelService.saveBatch(metadataLabels);
                    }
                }
            } else {
                // 获取锁失败  TODO
            }
        } finally {
            // 释放锁
            lockUtils.unlock(Constant.REDIS_KEY_METRIClOCK, UID);
        }
        return R.ok();
    }

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

    private List<MetadataMetric> listCompare(List<MetadataMetric> o, List<MetadataMetric> n) {
        Map<MetadataMetric, Integer> map = new HashMap<MetadataMetric, Integer>(n.size());
        List<MetadataMetric> differentList = new ArrayList<MetadataMetric>();

        for (MetadataMetric resource : n) {
            map.put(resource, 1);
        }

        for (MetadataMetric resource1 : o) {
            if (map.get(resource1) == null) {
                differentList.add(resource1);
            }
        }
        return differentList;
    }


    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
//        System.out.println("调用同步方法======>" + LocalDateTime.now());
        this.synchronizationMetrics();
//        System.out.println("执行完毕");
    }
}
