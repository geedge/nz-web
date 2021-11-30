package com.nis.common.utils;

import com.nis.common.exception.NZException;

import Aladdin.Hasp;
import Aladdin.HaspStatus;
import cn.hutool.log.Log;

public class HaspUtil {
	private static final Log log = Log.get();
	
	/** Record created indication on WS PUT calls */
	public final static int CREATED = 201;
	/** Required header for WS calls */
	private final static String VERSION = "application/vnd.ems.v12";
	/** Runtime query scope */
	protected final static String KEY_SCOPE = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			+ "<haspscope>\n"
			+ "<license_manager hostname=\"localhost\" />\n"
			+ "</haspscope>\n";
	/** SL-Legacy C2V not used for V2CP activation**/
	protected final static String SCOPE_WITHOUT_SL_LEGACY = 
		      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"+
		      "<haspscope>"+
		      " <license_manager hostname=\"localhost\" >"+
		      " <hasp key_type=\"~SL-Legacy\" />"+
		      " </license_manager>"+
		      "</haspscope>";
	/** Runtime query view */
	protected final static String KEY_VIEW = "<haspformat format=\"host_fingerprint\"/>";

	protected final static String KEY_VIEW_FOR_V2CP = "<haspformat root=\"hasp_info\">  "
			+ "<hasp>      "
			+ "  <attribute name=\"id\" /> "
			+ "  <attribute name=\"type\" />    " + "</hasp>" + "</haspformat>";

	protected final static String KEYID_SCOPE1 = "	<haspscope>     <hasp id=\"";

	protected final static String KEYID_SCOPE2 = "\" /></haspscope>";

	protected final static String KEY_C2V_FORMAT = "<haspformat format=\"updateinfo\"/>";

	/** WS uri */
	public static final String PK_LOGIN_URL = "ems/v21/ws/loginByProductKey.ws";
	public static final String REGISTER_URL = "ems/v21/ws/customer.ws";
	public static final String V2CP_URL = "/ems/v78/ws/activation/target.ws";

	/** Error message constants */
	public static final String MSG_BAD_CONNECTION = "Sentinel EMS is not available for the specified URL";
	public static final String FAILS_WITH_CODE = "Fails with status code ";
	public static final String TOO_MANY_KEYS = "Too many keys connected.";
	public static final String NO_KEYS = "No key connected.";
	public static final String ERROR_CODE = "Error - code ";
	public static final String SERVER_RETURNED_ERROR = "server returned error code";

	
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
		String infos = hasp.getInfo(KEY_SCOPE, KEY_VIEW, vendorCode);
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
