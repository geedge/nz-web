package com.nis.common.utils;

import java.util.regex.Pattern;

/**
 * 
 * 公用方法工具类
 *
 */
public class CommonUtils {
	
	/**
	 * 端口合法性校验
	 * @param port
	 * @return
	 */
	public static boolean checkPort(String port) {
		String regex = "^([1-9]|[1-9]\\d{3}|[1-6][0-5][0-5][0-3][0-5])$";
		boolean flag = Pattern.matches(regex, port.toString());
		return flag;
	}
}
