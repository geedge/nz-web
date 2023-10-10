package com.nis.common.utils;

import de.taimos.totp.TOTP;
import org.apache.commons.codec.binary.Base32;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;



public class MfaUtils {
	
	//生成密钥
	public static String generateSecretKey() {
	    SecureRandom random = new SecureRandom();
	    byte[] bytes = new byte[20];
	    random.nextBytes(bytes);
	    Base32 base32 = new Base32();
	    return base32.encodeToString(bytes);
	}
	
	/**
	 * 校验 密钥 和 otp code 是否匹配
	 * @param secretKey
	 * @param otpCode
	 * @return
	 */
	public static boolean validateTOTPCode(String secretKey,String otpCode) {
		Base32 base = new Base32();
		byte[] decode = base.decode(secretKey);
		String encodeHexStr = Tool.HexUtil.encodeHexStr(decode);
		boolean validate = TOTP.validate(encodeHexStr, otpCode);
		return validate;
	}

	
	//使用的二维码。首先我们需要从谷歌接收条码数据。它需要一些特殊的格式，所以这里是我们生成这些数据的方法。
	public static String getAuthenticatorBarCode(String secretKey, String account) {
	    try {
	    	String issuer = "NEZHA";
	        return "otpauth://totp/"
	                + URLEncoder.encode(issuer + ":" + account, "UTF-8").replace("+", "%20")
	                + "?secret=" + URLEncoder.encode(secretKey, "UTF-8").replace("+", "%20")
	                + "&issuer=" + URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
	    } catch (UnsupportedEncodingException e) {
	        throw new IllegalStateException(e);
	    }
	}

	
	public static String getAuthToken() {
		return UUID.randomUUID().toString();
	}
	
	public static List<String> getRecoveryCode() {
		List<String> list = new ArrayList<String>();
		for(int i=0;i<10;i++) {
			list.add(Tool.IdUtil.simpleUUID().substring(0,16));
		}
		return list;
	}
	
}
