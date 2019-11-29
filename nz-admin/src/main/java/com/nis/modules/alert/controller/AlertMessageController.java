package com.nis.modules.alert.controller;

import java.util.Arrays;
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
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
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
@RequestMapping("alert/message")
public class AlertMessageController {
	
	private final static Logger logger = LoggerFactory.getLogger(AlertMessageController.class);
    
	@Autowired
    private AlertMessageService alertMessageService;

    /**
     * 列表
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page;
		try {
			page = alertMessageService.queryPage(params);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("查询告警信息失败", e);
			return R.error();
		}
    }

    /**
     * 修改
     */
    @RequestMapping(value = "", method = RequestMethod.PUT)
    public R update(@RequestBody AlertMessageEntity alertMessage) {
    	ValidateUtils.is(alertMessage.getIds()).notNull(RCode.ALERTMSG_IDS_ISNULL)
    					.and(alertMessage.getState()).notNull(RCode.ALERTMSG_STATE_ISNULL);
    	//校验state合法性
    	if (!Constant.ALERTMESSAGE_STATE.contains(alertMessage.getState())) {
    		return R.error(RCode.ALERTMSG_STATE_INCORRECT);
    	}
    	alertMessageService.updateByIds(alertMessage);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public R delete(String ids) {
    	ValidateUtils.is(ids).notNull();
    	String[] split = ids.split(",");
		alertMessageService.removeByIds(Arrays.asList(split));

        return R.ok();
    }

}
