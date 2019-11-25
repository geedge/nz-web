/**

 *

 *

 */

package com.nis.common.exception;

import org.apache.shiro.authz.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nis.common.utils.R;
import com.nis.common.utils.RCode;

/**
 * 异常处理器
 *

 */
@RestControllerAdvice
public class NZExceptionHandler {
	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * 处理自定义异常
	 */
	@ExceptionHandler(NZException.class)
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public R handleNZException(NZException e){
		R r = new R();
		r.put("code", e.getCode());
		r.put("msg", e.getMsg());
		return r;
	}

	@ExceptionHandler(DuplicateKeyException.class)
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public R handleDuplicateKeyException(DuplicateKeyException e){
		logger.error(e.getMessage(), e);
		return R.error(RCode.SYS_DB_DUPLICATERECORD);
	}

	@ExceptionHandler(AuthorizationException.class)
	@ResponseStatus(value=HttpStatus.UNAUTHORIZED)
	public R handleAuthorizationException(AuthorizationException e){
		logger.error(e.getMessage(), e);
		return R.error(RCode.SYS_DB_AUTH);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	public R handleException(Exception e){
		logger.error(e.getMessage(), e);
		return R.error().put("msg", e);
	}
}
