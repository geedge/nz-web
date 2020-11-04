package com.nis.modules.alert.controller;

import com.nis.common.exception.NZException;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.alert.service.AlertMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;


/**
 * 
 *
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
@RestController
@RequestMapping("api/v1/alerts")
public class AlertMessageReceiveController {

	private final static Logger logger = LoggerFactory.getLogger(AlertMessageReceiveController.class);
	
	@Autowired
    private AlertMessageService alertMessageService;

	/**
	 * 接收 prometheus 推送过来的告警消息
	 * @param receiveStr
	 * @param request
	 * @return
	 */
    @PostMapping
    public R alerts(@RequestBody String receiveStr, HttpServletRequest request){
    	try {
		    alertMessageService.saveBatch(receiveStr, request);
    	} catch (Exception e) {
		    logger.error(request.getRemoteAddr() + " 推送过来的 promethues 告警消息处理异常，消息是：" + receiveStr + " 异常信息是：" + e.getMessage(), e);
			throw new NZException(RCode.ALERTMSG_PARSE_ERROR);
		}

        return R.ok();
    }
}
