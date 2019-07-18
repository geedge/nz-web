/**

 *

 *
 * 
 */

package com.nis.common.exception;

import com.nis.common.utils.RCode;

/**
 * 自定义异常
 *
 * @author Mark sunlightcs@gmail.com
 */
public class NZException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
    private String msg;
    private String code = RCode.ERROR;
    
    public NZException(String code) {
    	super(RCode.codeMap.get(code));
		this.code = code;
		this.msg = RCode.codeMap.get(code);
	}
	
	public NZException(String msg, Throwable e) {
		super(msg, e);
		this.msg = msg;
	}
	
	public NZException(String msg, String code) {
		super(msg);
		this.msg = msg;
		this.code = code;
	}
	
	public NZException(String msg, String code, Throwable e) {
		super(msg, e);
		this.msg = msg;
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
}
