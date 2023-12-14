package com.nis.modules.alert.controller;

import cn.hutool.log.Log;
import com.nis.common.exception.NZException;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.alert.service.AlertMessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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
	private AlertMessageService alertMessageService;
	
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
		    alertMessageService.leftPushAlertMessageToCache(receiveStr);
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
		    alertMessageService.leftPushAlertMessageToCache(receiveStr);
    	} catch (RuntimeException e) {
    		logger.error(e, "alert message receive error,host:{}, msg:{}", request.getRemoteAddr(),receiveStr);
			throw new NZException(RCode.ALERTMSG_PARSE_ERROR);
		}
        return R.ok();
    }
    
}
