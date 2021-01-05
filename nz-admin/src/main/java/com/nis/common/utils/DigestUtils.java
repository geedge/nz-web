package com.nis.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

public class DigestUtils {
	private static final List<String> ENCODE_LIST = Arrays.asList("ISO-8859-1", "GB2312", "UTF-8", "GBK");
	private static final String DEFAULT_URL_ENCODING = "UTF-8";
	
	public static String getEncoding(String text) {
		for(String enc : ENCODE_LIST) {
			try {
				byte[] bytes = text.getBytes(enc);
				String str = StrUtil.str(bytes,enc);
				Arrays.fill(bytes, (byte)0);
				if(StrUtil.equals(text,str)) {
					return enc;
				}
			} catch (UnsupportedEncodingException e) {}
		}
		return null;
	}
	public static String urlEncode(String part) {
		try {
			return URLEncoder.encode(part, DEFAULT_URL_ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new UncheckedExecutionException(e);
		}
	}
	public static String md5Hex(String text) {
		return DigestUtil.md5Hex(text);
	}
}
