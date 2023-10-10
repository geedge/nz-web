/**

 *

 *

 */

package com.nis.common.exception;

import cn.hutool.log.Log;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.service.SysI18nService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.shiro.authz.AuthorizationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 异常处理器
 *

 */
@RestControllerAdvice
public class NZExceptionHandler {
	private Log logger = Log.get();

	@Autowired
	private SysI18nService sysI18nService;
	
	/**
	 * 处理自定义异常
	 */
	@ExceptionHandler(NZException.class)
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public R handleNZException(NZException e){
		R r = new R();
		r.put("code", e.getCode());
		String msg = "";
		if(ToolUtil.isNotEmpty(e.getRCode())) {
			msg = sysI18nService.queryValue(e.getRCode().toString(),e.getParam());
		}
		// 如果还是未找到 再尝试使用 sys_i18n name 搜索 条件为 RCode code 码
		if (Tool.StrUtil.isEmpty(msg)) {
			msg = sysI18nService.queryValueByName(Tool.StrUtil.toString(e.getCode()));
		}
		if (Tool.StrUtil.isEmpty(msg) && (ToolUtil.isEmpty(e.getRCode()) || ToolUtil.equals(msg, e.getRCode().toString()))) {
			r.put("msg", e.getMsg());
		} else {
			r.put("msg", msg);
		}
		return r;
	}

	@ExceptionHandler(DuplicateKeyException.class)
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public R handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request){
		logger.error(e,"Request uri: {}", request.getRequestURI());
		String msg = sysI18nService.queryValueByName(RCode.SYS_DB_DUPLICATERECORD.getCode().toString());
		return R.error(RCode.SYS_DB_DUPLICATERECORD.getCode(), msg);
	}

	@ExceptionHandler(AuthorizationException.class)
	@ResponseStatus(value=HttpStatus.UNAUTHORIZED)
	public R handleAuthorizationException(AuthorizationException e, HttpServletRequest request){
		logger.error(e,"Request uri: {}", request.getRequestURI());
		return R.error(RCode.SYS_DB_AUTH);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	public R handleException(Exception e, HttpServletRequest request){
		if(e instanceof ClientAbortException) {
			return null;
		}
		logger.error(e,"Request uri: {}", request.getRequestURI());
		return R.error().put("msg", e.getMessage());
	}
}
