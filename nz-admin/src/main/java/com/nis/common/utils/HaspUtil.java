package com.nis.common.utils;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;

import Aladdin.Hasp;
import Aladdin.HaspStatus;
import cn.hutool.log.Log;

public class HaspUtil {
	private static final Log log = Log.get();
	
	/** Runtime query scope */
	protected final static String KEY_SCOPE = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			+ "<haspscope>\n"
			+ "<license_manager hostname=\"localhost\" />\n"
			+ "</haspscope>\n";
	/** Runtime query view */
	protected final static String KEY_VIEW = "<haspformat format=\"host_fingerprint\"/>";

	protected final static String KEY_VIEW_FOR_V2CP = "<haspformat root=\"hasp_info\">  "
			+ "<hasp>      "
			+ "  <attribute name=\"id\" /> "
			+ "  <attribute name=\"type\" />    " + "</hasp>" + "</haspformat>";

	protected final static String KEYID_SCOPE1 = "	<haspscope>     <hasp id=\"";

	protected final static String KEYID_SCOPE2 = "\" /></haspscope>";

	protected final static String KEY_C2V_FORMAT = "<haspformat format=\"updateinfo\"/>";


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
	
	public final static String PRODUCT_SCOPE = "<haspscope>\n" +
			"   <product id=\"%s\" />\n" +
			"</haspscope>";
	
	public static String getLicenseInfo(Integer productId, String vendorCode) {
		ValidateUtils.is(productId).notNull();
		Hasp hasp = new Hasp(Hasp.HASP_DEFAULT_FID);
		String licenseInfo = hasp.getInfo(String.format(PRODUCT_SCOPE, productId), HASP_FORMAT, vendorCode);
		int status = hasp.getLastError();
		if (status != HaspStatus.HASP_STATUS_OK) {
			log.error("Error: updateLicense Fails with status code :{}",status);
		}
		return licenseInfo;
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
		if (status != HaspStatus.HASP_STATUS_OK) {
			log.error("Error: updateLicense Fails with status code :{}",status);
		}
		return status;
	}
	
	
	/**
	 * 
	 * @param vendorCode
	 * @return
	 * @throws Exception
	 */
	public static String readC2vFromKey(String vendorCode)  {
		Hasp hasp = new Hasp(Hasp.HASP_DEFAULT_FID);
		String infos = hasp.getInfo(PRODUCT_SCOPE, KEY_VIEW, vendorCode);
		int status = hasp.getLastError();
		log.info("reading C2v from key status code = " + status);
		if(status != HaspStatus.HASP_STATUS_OK) {
			log.error("Error: getLicense c2v Fails with status code :{}",status);
			throw new NZException(RCode.LICENSE_ERROR_CODE.setParam(status));	
		}
		return infos;
	}
	
	public static int verify(String vendorCode,Long fetureId) {
		Hasp hasp = new Hasp(fetureId);
		hasp.login(vendorCode);
		int status = hasp.getLastError();
		if(status != HaspStatus.HASP_STATUS_OK) {
			log.error("Error: license login  Fails with status code :{}",status);
		}
		hasp.logout();
		return status;
	}

}
