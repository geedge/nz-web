package com.nis.common.utils;

import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import cn.hutool.log.Log;
import cn.hutool.system.oshi.OshiUtil;
import com.alibaba.fastjson.JSON;
import com.nis.modules.sys.entity.LicenseEntity;
import com.nis.modules.sys.entity.LicenseEntity.Signature;
import org.w3c.dom.Document;
import oshi.hardware.ComputerSystem;

import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

public class LicenseUtil {
	private static final Log log = Log.get();
	private static String machineIdCache;

	static {
		try {
			ComputerSystem system = OshiUtil.getSystem();
			String hardwareUUID = system.getHardwareUUID();
			String serialNumber = system.getSerialNumber();
			String boardSerialNumber = system.getBaseboard().getSerialNumber();
			if (StrUtil.isAllBlank(hardwareUUID, serialNumber, boardSerialNumber)) {
				machineIdCache = null;
			}
			String token = StrUtil.concat(true, hardwareUUID, serialNumber, boardSerialNumber);
			machineIdCache = SecureUtil.sha1(token);
		} catch (Throwable e) {
			log.warn("Get machine id error", e);
		}
	}
	
	/**
	 * 验证证书签名是否合法
	 * 
	 * @param pk
	 * @return
	 */
	public static boolean verifyLicenseSign(LicenseEntity license, PublicKey pk, String signAlgorithm) {
		Signature signature = license.getSignature();
		if (signature != null
				&& Tool.StrUtil.isAllNotBlank(signature.getDigestValue(), signature.getSignatureValue())) {
			String genLicenseDigest = license.genLicenseDigest();// 计算摘要
			String digestValue = signature.getDigestValue();// 上传证书摘要
			if (!Tool.StrUtil.equals(digestValue, genLicenseDigest)) {//摘要不一致说明license 不合法
				return false;
			}
			// 验证摘要签名是否正确
			String signatureValue = signature.getSignatureValue();
			Sign sign = SecureUtil.sign(SignAlgorithm.valueOf(signAlgorithm));
			sign.setPublicKey(pk);
			try {
				//hex解码
				byte[] signData = Tool.HexUtil.decodeHex(signatureValue);
				return sign.verify(digestValue.getBytes(Constant.DEFAULT_CHARTSET_NAME),signData);
			} catch (UnsupportedEncodingException e) {
			}
		}
		return false;

	}

	/**
	 * 生成签名摘要信息
	 * 
	 * @param key
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String genSign(LicenseEntity license, PrivateKey privateKey, String signAlgorithm)
			throws UnsupportedEncodingException {
		Sign sign = SecureUtil.sign(SignAlgorithm.valueOf(signAlgorithm));
		sign.setPrivateKey(privateKey);
		String content = license.genLicenseDigest();
		byte[] bs = sign.sign(content.getBytes(Constant.DEFAULT_CHARTSET_NAME));
		return Tool.HexUtil.encodeHexStr(bs);
	}

	/**
	 * 生成机器token，限定设备使用
	 * 
	 * @return
	 */
	public static String getMachineID() {
		return machineIdCache;
	}

	/**
	 * xml 格式 license转为 javabean
	 * 
	 * @param licenseXml
	 * @return
	 */
	public static LicenseEntity xmlToLicenseBean(String licenseXml) {
		if (Tool.StrUtil.isNotBlank(licenseXml)) {
			Map<String, Object> map = Tool.XmlUtil.xmlToMap(licenseXml);
			map.forEach((k, v) -> {
				if(Tool.ObjectUtil.isEmpty(v)) {
					map.put(k, null);
				}
			});
			return Tool.BeanUtil.toBean(map, LicenseEntity.class,CopyOptions.create().setIgnoreError(true).setIgnoreNullValue(true));
		}
		return null;
	}

	/**
	 * license javabean 转为xml字符串
	 * 
	 * @param license
	 * @return
	 */
	public static String licenseBeanToXml(LicenseEntity license, boolean isPretty) {
		if (license == null) {
			return null;
		}
		Map<String, Object> beanToMap = Tool.BeanUtil.beanToMap(JSON.toJSON(license));
		Document document = Tool.XmlUtil.mapToXml(beanToMap,"license");
		return Tool.XmlUtil.toStr(document, Constant.DEFAULT_CHARTSET_NAME, isPretty);
	}

}
