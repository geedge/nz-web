package com.nis.modules.alert.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;

import cn.hutool.log.Log;


/**
 * 
 * 接收prometheus 推送的告警消息
 * 1、直接存到 redis
 * 2、后续 例外线程在redis 中读取解析之后再处理
 */
@RestController
@RequestMapping("api")
public class AlertMessageReceiveController {

	private final static Log logger = Log.get();

	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	@Value("${redis.maxQueueLength:100}")
    private Long maxQueueLength;//redis队列长度
	
	/**
	 * 接收 prometheus 推送过来的告警消息
	 * @param receiveStr
	 * @param request
	 * @return
	 */
    @PostMapping("v1/alerts")
    public R alertsV1(@RequestBody String receiveStr, HttpServletRequest request){
    	try {
    		if(logger.isDebugEnabled()) {
    			logger.debug("alert receive: {}", receiveStr);
    		}
    		ListOperations<String, String> operations = redisTemplate.opsForList();
    		operations.leftPush(Constant.ALERT_MESSAGE_RECV, receiveStr);
    		Long size = operations.size(Constant.ALERT_MESSAGE_RECV);
    		//判断是否超出所设置的队列最大长度
    		if(size > maxQueueLength) {
    			redisTemplate.opsForList().trim(Constant.ALERT_MESSAGE_RECV, 0, maxQueueLength-1);
    			logger.debug("alert_message_recv size : {}",maxQueueLength);
    		}else {
    			logger.debug("alert_message_recv size : {}",size);
    		}
    	} catch (RuntimeException e) {
    		logger.error(e, "alert message receive error,host:{}, msg:{}", request.getRemoteAddr(),receiveStr);
			throw new NZException(RCode.ALERTMSG_PARSE_ERROR);
		}
        return R.ok();
    }
    
    @PostMapping("v2/alerts")
    public R alertsV2(@RequestBody String receiveStr, HttpServletRequest request){
    	try {
    		if(logger.isDebugEnabled()) {
    			logger.debug("alert receive: {}", receiveStr);
    		}
    		ListOperations<String, String> operations = redisTemplate.opsForList();
    		operations.leftPush(Constant.ALERT_MESSAGE_RECV, receiveStr);
    		Long size = operations.size(Constant.ALERT_MESSAGE_RECV);
    		//判断是否超出所设置的队列最大长度
    		if(size > maxQueueLength) {
    			redisTemplate.opsForList().trim(Constant.ALERT_MESSAGE_RECV, 0, maxQueueLength-1);
    			logger.debug("alert_message_recv size : {}",maxQueueLength);
    		}else {
    			logger.debug("alert_message_recv size : {}",size);
    		}
    	} catch (RuntimeException e) {
    		logger.error(e, "alert message receive error,host:{}, msg:{}", request.getRemoteAddr(),receiveStr);
			throw new NZException(RCode.ALERTMSG_PARSE_ERROR);
		}
        return R.ok();
    }
    
}
