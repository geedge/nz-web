package com.nis.modules.report.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class ReportTopicConfig {

    @Autowired
    private LettuceConnectionFactory connectionFactory;

    @Bean
    public ReportConsumerListener reportConsumerListener() {
        return new ReportConsumerListener();
    }

    @Bean
    public ChannelTopic reportTopic() {
        return new ChannelTopic(ReportConstant.REPORT_TOPIC_NAME);
    }

    /**
     * 注册消息监听容器,使监听器和 topic 进行绑定
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(reportConsumerListener(), reportTopic());
        return container;
    }

}
