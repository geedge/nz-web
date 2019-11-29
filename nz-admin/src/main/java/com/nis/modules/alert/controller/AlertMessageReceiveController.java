package com.nis.modules.alert.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleService;
import com.google.gson.Gson;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;



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
     * 列表
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    public R alerts(@RequestBody AlertMessageReceiveDTO[] receiveData){
    	//dto转化为entity，存库
    	try {
    		alertMessageService.saveBatch(receiveData);
    	} catch (Exception e) {
    		Gson gson = new Gson();
			logger.error("接收的告警信息转化entity失败:" + gson.toJson(receiveData), e);
			return R.error(RCode.ALERTMSG_PARSE_ERROR);
		}

        return R.ok();
    }
}
