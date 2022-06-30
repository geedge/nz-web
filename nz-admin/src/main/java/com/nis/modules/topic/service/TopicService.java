package com.nis.modules.topic.service;

import java.util.Map;

public interface TopicService {

    /**
     * @param topicName  名称
     * @param contentMap 内容
     */
    void publishMessage(String topicName, Map<String, Object> contentMap);

}
