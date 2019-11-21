package com.nis.common.utils;

import java.util.HashMap;

/**
 * 返回数据
 *
 * 错误码、错误内容统一在枚举类RCode中定义， 错误码格式见RCode注释，错误码内容必须用英文，作为国际化的code
 * 自定义的错误类型必须加注释
 */
public class R extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;
	
	public R() {
		put("code", RCode.SUCCESS.getCode());
		put("msg", RCode.SUCCESS.getMsg());
	}
	
	public static R error() {
		return error(RCode.ERROR.getCode(), RCode.ERROR.getMsg());
	}
	
	public static R error(RCode rCode) {
		R r = new R();
		r.put("code", rCode.getCode());
		r.put("msg", rCode.getMsg());
		return r;
	}
	
	private static R error(Integer code, String msg) {
		R r = new R();
		r.put("code", code);
		r.put("msg", msg);
		return r;
	}

	public static R ok(String msg) {
		R r = new R();
		r.put("msg", msg);
		return r;
	}
	
	public static R ok() {
		return new R();
	}
	
	public static R ok(Object data) {
		R r = new R();
		r.put("data", data);
		return r;
	}

	@Override
	public R put(String key, Object value) {
		super.put(key, value);
		return this;
	}
}

