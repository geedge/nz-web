package com.nis.common.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Base32;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import cn.hutool.crypto.digest.otp.TOTP;

public class MfaUtils {
	
	//生成密钥
	public static String generateSecretKey() {
	    SecureRandom random = new SecureRandom();
	    byte[] bytes = new byte[20];
	    random.nextBytes(bytes);
	    Base32 base32 = new Base32();
	    return base32.encodeToString(bytes);
	}
	
	//将 base32 编码的密钥转换为十六进制，并使用 TotpUtils 根据当前时间将它们转换为 6 位代码。
//	public static String getTOTPCode(String secretKey) {
//	    Base32 base32 = new Base32();
//	    byte[] bytes = base32.decode(secretKey);
//	    String hexKey = Hex.encodeHexString(bytes);
//	    return TOTP.getOTP(hexKey);
//	}
	
	public static int getNowTOTPCode(String secretKey) {
		Instant now = Instant.now();
		byte[] key = Tool.DigestUtil.sha256(secretKey);
		TOTP totp = new TOTP(key);
		return totp.generate(now);
	}
	
	public static int getLastTOTPCode(String secretKey) {
		Instant now = Instant.now();
		Instant lastTime = now.minusSeconds(30);
		byte[] key = Tool.DigestUtil.sha256(secretKey);
		TOTP totp = new TOTP(key);
		return totp.generate(lastTime);
	}
	
	public static int getNextTOTPCode(String secretKey) {
		Instant now = Instant.now();
		Instant nextTime = now.plusSeconds(30);
		byte[] key = Tool.DigestUtil.sha256(secretKey);
		TOTP totp = new TOTP(key);
		return totp.generate(nextTime);
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
	
	//生成二维码图片
	public static void createQRCode(String barCodeData, String filePath, int height, int width)
	        throws WriterException, IOException {
	    BitMatrix matrix = new MultiFormatWriter().encode(barCodeData, BarcodeFormat.QR_CODE,
	            width, height);
	    try (FileOutputStream out = new FileOutputStream(filePath)) {
	        MatrixToImageWriter.writeToStream(matrix, "png", out);
	    }
	}
	
	public static String getAuthToken() {
		return UUID.randomUUID().toString().substring(0,16);
	}
	
	public static List<String> getRecoveryCode() {
		List<String> list = new ArrayList<String>();
		for(int i=0;i<10;i++) {
			list.add(UUID.randomUUID().toString().substring(0,16));
		}
		
		return list;
	}
	
//	public static void main(String[] args) {
//		String authKey = MfaUtils.generateSecretKey();
//		authKey = "FMLRSYLSZKIMODMICYCLJJ6THF6YTLOX";
//		System.out.println(authKey);
////		System.out.println(MfaUtils.getTOTPCode(authKey));
//		System.out.println(TotpUtils.generateTOTP(authKey, System.currentTimeMillis()+"", "6"));
//		
//		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//		Calendar cal = Calendar.getInstance();
//	    cal.add(Calendar.SECOND, 90);
//	    System.out.println("Add one second to current date : " + dateFormat.format(cal.getTime()));
//	    System.out.println(TotpUtils.generateTOTP(authKey, cal.getTimeInMillis()+"", "6"));
//	    
////	    System.out.println(MfaUtils.getTOTPCode(authKey));
//	}
}
