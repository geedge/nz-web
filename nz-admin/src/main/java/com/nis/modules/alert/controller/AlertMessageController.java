package com.nis.modules.alert.controller;

import cn.hutool.log.Log;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.service.AlertHandlerService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


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
        R result = R.ok(page);
        Object statFlag = params.getOrDefault("statistics", 0);
        if(Tool.ObjectUtil.equal("1", String.valueOf(statFlag))) {
        	Map<String, List> messageGroupBy = handlerService.queryMessageGroupBy(params);
        	result.put("statistics", messageGroupBy);
        }
		return result;
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
	public void export(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
		Object format = params.get("format");
		String formatStr = format == null ? "1" : format.toString();
		// 校验 FORMAT
		if (!Arrays.asList(Constant.IMPORTFILE_TYPE_INTVALUES).contains(formatStr)) {
			throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
		}
		params.put("format", formatStr);

		handlerService.exportMessage(response, params);
	}
}
