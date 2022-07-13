package com.nis.common.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.nis.common.exception.NZException;

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
		put("time", new Date());
	}
	
	public static R error() {
		return error(RCode.ERROR.getCode(), RCode.ERROR.getMsg());
	}
	
	public static R error(RCode rCode) {
		R r = new R();
		r.put("code", rCode.getCode());
		r.put("msg", rCode.getMsg());
		r.put("time", new Date());
		return r;
	}
	
	public static R error(Integer code, String msg) {
		R r = new R();
		r.put("code", code);
		r.put("msg", msg);
		r.put("time", new Date());
		return r;
	}

	public static R ok(String msg) {
		R r = new R();
		r.put("msg", msg);
		r.put("time", new Date());
		return r;
	}
	
	public static R ok() {
		return new R();
	}
	
	public static R ok(Object data) {
		R r = new R();
		r.put("data", data);
		r.put("time", new Date());
		return r;
	}

	@Override
	public R put(String key, Object value) {
		super.put(key, value);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public R putData(String key,Object value) {
		Object data = super.getOrDefault("data", new LinkedHashMap<String, Object>());
		if( !(data instanceof Map)) {
			throw new NZException("data put error");
		}
		((Map<String, Object>)data).put(key, value);
		super.put("data", data);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public R putAllData(Map m) {
		super.putAll(m);
		return this;
	}
}

