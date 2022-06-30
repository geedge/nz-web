package com.nis.modules.topic.service.impl;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Tool;
import com.nis.modules.report.config.ReportConstant;
import com.nis.modules.topic.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TopicServiceImpl implements TopicService {

    private Log log = Log.get();

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void publishMessage(String topicName, Map<String, Object> contentMap) {
        if (Tool.MapUtil.isEmpty(contentMap)) {
            log.info("Send message is empty,Not publish,topic :{}", topicName);
            return;
        }

        try {
            redisTemplate.convertAndSend(topicName, JSONObject.toJSONString(contentMap));
            log.info("message send success. topic :{} , msg:{}", topicName, JSONObject.toJSONString(contentMap));
        } catch (Exception e) {
            log.error("message send error. topic :{} , msg:{}", topicName, JSONObject.toJSONString(contentMap), e);
        }
    }
}
