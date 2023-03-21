package com.nis.common.utils;

import Aladdin.Hasp;
import Aladdin.HaspStatus;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.exception.NZException;
import com.nis.modules.sys.utils.HaspStatusEnum;

import java.util.Map;

public class HaspUtil {
	private static final Log log = Log.get();
	
	/** Runtime query scope */
	protected final static String KEY_SCOPE = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			+ "<haspscope>\n"
			+ "<license_manager hostname=\"localhost\" />\n"
			+ "</haspscope>\n";
	
	protected final static String KEY_ID_SCOPE = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			+ "<haspscope>\n"
			+ "<hasp id=\"%s\" />\n"
			+ "</haspscope>\n";;
	protected final static String KEY_FEATURE_ID_SCOPE = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			+ "<haspscope>\n"
			+ "<feature id=\"%s\" />\n"
			+ "</haspscope>\n";

	/** Runtime query view */
	protected final static String KEY_VIEW = "<haspformat format=\"host_fingerprint\"/>";



	protected final static String KEY_C2V_FORMAT = "<haspformat format=\"updateinfo\"/>";

	public final static String ALL_FORMAT = "<haspformat>\n" +
			"	<hasp>\n" +
			"		<element name=\"id\"/>\n" +
			"		<element name=\"type\"/>\n" +
			"		<element name=\"configuration\"/>\n" +
			"		<element name=\"clone_protected\"/>\n" +
			"		<element name=\"disabled\"/>\n" +
			"		<element name=\"production_date\"/>\n" +
			"		<element name=\"updatecounter\"/>\n" +
			"		<element name=\"parentid\"/>\n" +
			"		<feature>\n" +
			"			<element name=\"id\"/>\n" +
			"			<element name=\"locked\"/>\n" +
			"			<element name=\"license\"/>\n" +
			"			<element name=\"name\"/>\n" +
			"			<element name=\"concurrency\"/>\n" +
			"			<element name=\"usable\"/>\n" +
			"		</feature>\n" +
			"		<product>\n" +
			"			<element name=\"id\"/>\n" +
			"			<element name=\"name\"/>\n" +
			"		</product>\n" +
			"		<license_manager>\n" +
			"			<element name=\"host_fingerprint\"/>\n" +
			"			<element name=\"hostname\"/>\n" +
			"			<element name=\"osname\"/>\n" +
			"			<element name=\"osversion\"/>\n" +
			"			<element name=\"name\"/>\n" +
			"			<element name=\"time\"/>\n" +
			"			<element name=\"uptime\"/>\n" +
			"			<element name=\"architecture\"/>\n" +
			"			<element name=\"ip\"/>\n" +
			"		</license_manager>\n" +
			"		<vendor>\n" +
			"			<element name=\"id\"/>\n" +
			"			<element name=\"name\"/>\n" +
			"		</vendor>\n" +
			"	</hasp>\n" +
			"</haspformat>";

	public final static String HASP_FORMAT = "<haspformat>\n" +
			"	<hasp>\n" +
			"		<element name=\"id\"/>\n" +
			"		<element name=\"type\"/>\n" +
			"		<element name=\"configuration\"/>\n" +
			"		<element name=\"clone_protected\"/>\n" +
			"		<element name=\"disabled\"/>\n" +
			"		<element name=\"production_date\"/>\n" +
			"		<element name=\"updatecounter\"/>\n" +
			"		<element name=\"parentid\"/>\n" +
			"		<feature>\n" +
			"			<element name=\"id\"/>\n" +
			"			<element name=\"locked\"/>\n" +
			"			<element name=\"license\"/>\n" +
			"			<element name=\"name\"/>\n" +
			"			<element name=\"concurrency\"/>\n" +
			"			<element name=\"usable\"/>\n" +
			"		</feature>\n" +
			"		<product>\n" +
			"			<element name=\"id\"/>\n" +
			"			<element name=\"name\"/>\n" +
			"		</product>\n" +
			"	</hasp>\n" +
			"</haspformat>";
	
	
	public static String getLicenseInfo(String vendorCode,Integer featureId) {
		Hasp hasp = new Hasp(featureId);
		hasp.login(vendorCode);
		int status = hasp.getLastError();
		log.info("license status: {}", status);
		if(status == HaspStatus.HASP_STATUS_OK) {
			String licenseInfo = hasp.getSessionInfo(ALL_FORMAT);
			status = hasp.getLastError();
			log.info("license status: {}", status);
			if (status != HaspStatus.HASP_STATUS_OK) {
				log.error("Error: updateLicense Fails with status code :{}", StrUtil.toString(HaspStatusEnum.getHaspStatusByStatus(status)));
			}else {
				Map<String, Object> xmlToMap = Tool.XmlUtil.xmlToMap(licenseInfo);
				String haspId = Tool.BeanUtil.getProperty(xmlToMap, "hasp.id");
				licenseInfo = hasp.getInfo(String.format(KEY_ID_SCOPE, haspId), ALL_FORMAT, vendorCode);
			}
			return licenseInfo;
		}else {
			log.error("Error: with status code :{}", StrUtil.toString(HaspStatusEnum.getHaspStatusByStatus(status)));
			throw new NZException(RCode.LICENSE_ERROR_CODE.setParam(status));	
		}
	}
	
	/**
	 * Updates the key with the license (v2c)
	 * 
	 * @param v2c
	 * @return
	 * @throws Exception
	 */
	public static int updateKeyWithLicense(String v2c, String vendorCode) {
		Hasp hasp = new Hasp(Hasp.HASP_DEFAULT_FID);
		hasp.update(v2c);
		int status = hasp.getLastError();
		log.info("license status: {}", status);
		if (status != HaspStatus.HASP_STATUS_OK) {
			log.error("Error: updateLicense Fails with status code :{}", StrUtil.toString(HaspStatusEnum.getHaspStatusByStatus(status)));
		}
		return status;
	}
	
	
	/**
	 * 
	 * @param vendorCode
	 * @return
	 * @throws Exception
	 */
	public static String readC2V(String vendorCode,Integer featureId)  {
		Hasp hasp = new Hasp(featureId);
		hasp.login(vendorCode);
		int status = hasp.getLastError();
		log.info("license status: {}", status);
		if(status == HaspStatus.HASP_STATUS_OK) {
			//如果已经安装过license ，读取指纹和 hasp id
			String licenseInfo = hasp.getSessionInfo(KEY_C2V_FORMAT);
			status = hasp.getLastError();
			log.info("license status: {}", status);
			if (status != HaspStatus.HASP_STATUS_OK) {
				log.error("Error: updateLicense Fails with status code :{}",StrUtil.toString(HaspStatusEnum.getHaspStatusByStatus(status)));
			}
			return licenseInfo;
		}else {
			hasp = new Hasp(featureId);
			//先通过 feature id 尝试获取，包含 hasp id
			String infos = hasp.getInfo(KEY_SCOPE, KEY_C2V_FORMAT, vendorCode);
			status = hasp.getLastError();
			if(status != HaspStatus.HASP_STATUS_OK || Tool.StrUtil.isBlank(infos)) {
				// feature 获取失败，只获取机器指纹
				infos = hasp.getInfo(KEY_SCOPE, KEY_VIEW, vendorCode);
				status = hasp.getLastError();
				log.info("license status: {}", status);
				if(status != HaspStatus.HASP_STATUS_OK) {
                    log.error("Error: getLicense c2v Fails with status code :{}", StrUtil.toString(HaspStatusEnum.getHaspStatusByStatus(status)));
					throw new NZException(RCode.LICENSE_ERROR_CODE.setParam(status));
				}
			}
			return infos;
		}
	}
	
	public static int verify(String vendorCode,Integer fetureId) {
		Hasp hasp = new Hasp(fetureId);
		hasp.login(vendorCode);
		int status = hasp.getLastError();
		log.info("license status: {}", status);
		if(status != HaspStatus.HASP_STATUS_OK) {
			log.error("Error: license login  Fails with status code :{}", StrUtil.toString(HaspStatusEnum.getHaspStatusByStatus(status)));
		}
		hasp.logout();
		return status;
	}
	
}
