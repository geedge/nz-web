package com.nis.modules.alert.controller;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.alert.service.AlertHandlerService;

import cn.hutool.log.Log;


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

	private final static Log logger = Log.get();

	@Autowired
	private AlertHandlerService handlerService;

    /**
     * 列表
     */
    @RequestMapping(method = RequestMethod.GET)
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTMESSAGE)
    public R list(@RequestParam Map<String, Object> params){
    	logger.debug("query param : {}", Tool.JSONUtil.toJsonStr(params));
    	//state不能为空
    	boolean containsstate = Constant.ALERT_MESSAGE_STATE_TABLE_MAPPING.containsKey(Integer.valueOf(params.get("state").toString()));
    	if(!containsstate) {
    		throw new NZException(RCode.ERROR);
    	}
        PageUtils page = handlerService.queryMessagePage(params);
		return R.ok(page);
    }


    /**
     * 删除
     */
    @RequestMapping( method = RequestMethod.DELETE)
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ALERTMESSAGE)
    @RequiresPermissions({"alert:message:delete"})
    public R delete(Long[] ids,Integer state) {
    	ValidateUtils.is(ids).notNull();
    	boolean containsstate = Constant.ALERT_MESSAGE_STATE_TABLE_MAPPING.containsKey(state);
    	if(!containsstate) {
    		throw new NZException(RCode.ERROR);
    	}
    	handlerService.removeMessageByIds(state,ids);
        return R.ok();
    }

	/**
	 * alert message导出
	 */
	@GetMapping(value = "/export")
	@SysLog(operation = OperationEnum.EXPORT,type = TypeEnum.ALERTMESSAGE)
	public void export(HttpServletResponse response, @RequestParam Map<String, Object> params){
		handlerService.exportMessage(response,params);
	}
}
