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
    private int code = RCode.ERROR.getCode();
    private Object[] param = new Object[]{};
    private RCode rCode;
    
    public NZException(RCode rCode) {
    	this.code = rCode.getCode();
    	this.msg = rCode.getMsg();
    	this.param = rCode.getParam();
    	this.rCode = rCode;
    }
    
    public NZException(String msg) {
    	super(msg);
		this.msg = msg;
	}
	
	public NZException(String msg, Throwable e) {
		super(msg, e);
		this.msg = msg;
	}
	
	public NZException(String msg, int code) {
		super(msg);
		this.msg = msg;
		this.code = code;
	}
	
	public NZException(String msg, int code, Throwable e) {
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

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
	
	public Object[] getParam() {
		return param;
	}

	public void setParam(Object[] param) {
		this.param = param;
	}

	public RCode getRCode() {
		return rCode;
	}

	public void setrCode(RCode rCode) {
		this.rCode = rCode;
	}
}
