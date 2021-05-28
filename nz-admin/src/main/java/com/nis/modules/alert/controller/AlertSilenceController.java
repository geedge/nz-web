package com.nis.modules.alert.controller;

import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.alert.entity.AlertSilenceConf;
import com.nis.modules.alert.service.AlertSilenceService;

import cn.hutool.log.Log;

@RestController
@RequestMapping("alert/silence")
public class AlertSilenceController {
	
	private final static Log logger = Log.get();
	
	@Autowired 
	private AlertSilenceService alertSilenceService;
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTSILENCE)
	public R list(@RequestParam Map<String,Object> params) {
		logger.debug("alertsilence query params is {}",JSON.toJSONString(params));
		PageUtils page = alertSilenceService.queryPage(params);
		return R.ok(page);
	}
	
	@GetMapping("/{id}")
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTSILENCE)
	public R queryAlertSilenceConf(@PathVariable("id") Integer id) {
		AlertSilenceConf result = alertSilenceService.queryAlertSilence(id);
		return R.ok(result);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.ALERTSILENCE)
	@RequiresPermissions({"alert:silence:save"})
	public R save(@RequestBody AlertSilenceConf alertSilenceConf) {
		ValidateUtils.is(alertSilenceConf.getStartAt()).notNull(RCode.ALERTSILENCE_STARTTIME_ISNULL)
		.and(alertSilenceConf.getEndAt()).notNull(RCode.ALERTSILENCE_ENDTIME_ISNULL)
		.and(alertSilenceConf.getName()).notNull(RCode.ALERTSILENCE_NAME_ISNULL).maxLength(64)
		.and(alertSilenceConf.getRemark()).maxLength(256);
		logger.debug("alertsilence save param is {}",JSON.toJSONString(alertSilenceConf));
		
		alertSilenceService.saveAlertSilenceConf(alertSilenceConf);
		return R.ok().putData("id", alertSilenceConf.getId());
	}
	
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ALERTSILENCE)
	@RequiresPermissions({"alert:silence:update"})
	public R modify(@RequestBody AlertSilenceConf alertSilenceConf) {
		ValidateUtils.is(alertSilenceConf.getStartAt()).notNull(RCode.ALERTSILENCE_STARTTIME_ISNULL)
		.and(alertSilenceConf.getEndAt()).notNull(RCode.ALERTSILENCE_ENDTIME_ISNULL)
		.and(alertSilenceConf.getName()).notNull(RCode.ALERTSILENCE_NAME_ISNULL).maxLength(64)
		.and(alertSilenceConf.getRemark()).maxLength(256);
		
		logger.debug("alertsilence modify param is {}",JSON.toJSONString(alertSilenceConf));
		
		alertSilenceService.updateAlertSilenceConf(alertSilenceConf);
		return R.ok().putData("id", alertSilenceConf.getId());
	}
	
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ALERTSILENCE)
	@RequiresPermissions({"alert:silence:delete"})
	public R remove(@RequestParam Integer... ids) {
		if(Tool.ArrayUtil.isAllNull(ids)) {
			throw new NZException(RCode.ALERTSILENCE_ID_IS_NOT_NULL);
		}
		alertSilenceService.updateAlertSilenceConfExpiredBatch(ids);
		return R.ok();
	}
}
