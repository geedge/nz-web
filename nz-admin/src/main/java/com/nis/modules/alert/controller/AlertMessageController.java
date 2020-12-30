package com.nis.modules.alert.controller;

import cn.hutool.log.Log;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.service.AlertMessageService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
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
    private AlertMessageService alertMessageService;

    /**
     * 列表
     */
    @RequestMapping(method = RequestMethod.GET)
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTMESSAGE)
    @RequiresPermissions({"alert:message:list"})
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page;
		try {
			page = alertMessageService.queryPage(params);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("查询告警信息失败", e);
			throw new NZException(RCode.SYS_ERROR);
		}
    }

	@RequestMapping( method = RequestMethod.PUT)
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ALERTMESSAGE)
	@RequiresPermissions({"alert:message:update"})
    public R update(@RequestBody Map<String, Object> params) {
		ValidateUtils.is(params.get("ids")).notNull(RCode.ALERTMSG_IDS_ISNULL)
				.and(params.get("state")).notNull(RCode.ALERTMSG_STATE_ISNULL)
				.and(params.get("remark")).notNull(RCode.ALERTMSG_REMARK_ISNULL);

		//校验state合法性
		Integer state = (Integer) params.get("state");
		if (!Constant.ALERTMESSAGE_STATE.contains(state)) {
			throw new NZException(RCode.ALERTMSG_STATE_INCORRECT);
		}

		alertMessageService.updateAlertStateByIds(params);
    	return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping( method = RequestMethod.DELETE)
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ALERTMESSAGE)
    @RequiresPermissions({"alert:message:delete"})
    public R delete(String ids) {
    	ValidateUtils.is(ids).notNull();
    	String[] split = ids.split(",");
		alertMessageService.removeByIds(Arrays.asList(split));

        return R.ok();
    }

	/**
	 * alert message导出
	 */
	@GetMapping(value = "/export")
	@SysLog(operation = OperationEnum.EXPORT,type = TypeEnum.ALERTMESSAGE)
	@RequiresPermissions({"alert:message:export"})
	public void export(HttpServletResponse response, @RequestParam Map<String, Object> params){
		alertMessageService.export(response,params);
	}
}
