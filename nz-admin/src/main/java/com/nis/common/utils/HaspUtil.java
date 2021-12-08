package com.nis.common.utils;

import com.nis.common.exception.NZException;

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
				log.error("Error: updateLicense Fails with status code :{}",status);
			}
			return licenseInfo;
		}else {
			log.error("Error: with status code :{}",status);
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
				log.error("Error: updateLicense Fails with status code :{}",status);
			}
			return licenseInfo;
		}else {
			hasp = new Hasp(featureId);
			String infos = hasp.getInfo(KEY_SCOPE, KEY_VIEW, vendorCode);
			status = hasp.getLastError();
			log.info("license status: {}", status);
			if(status != HaspStatus.HASP_STATUS_OK) {
				log.error("Error: getLicense c2v Fails with status code :{}",status);
				throw new NZException(RCode.LICENSE_ERROR_CODE.setParam(status));	
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
			log.error("Error: license login  Fails with status code :{}",status);
		}
		hasp.logout();
		return status;
	}

	
	public static void main(String[] args) {
	
		String vendorCode = "AzIceaqfA1hX5wS+M8cGnYh5ceevUnOZIzJBbXFD6dgf3tBkb9cvUF/Tkd/iKu2fsg9wAysYKw7RMAsVvIp4KcXle/v1RaXrLVnNBJ2H2DmrbUMOZbQUFXe698qmJsqNpLXRA367xpZ54i8kC5DTXwDhfxWTOZrBrh5sRKHcoVLumztIQjgWh37AzmSd1bLOfUGI0xjAL9zJWO3fRaeB0NS2KlmoKaVT5Y04zZEc06waU2r6AU2Dc4uipJqJmObqKM+tfNKAS0rZr5IudRiC7pUwnmtaHRe5fgSI8M7yvypvm+13Wm4Gwd4VnYiZvSxf8ImN3ZOG9wEzfyMIlH2+rKPUVHI+igsqla0Wd9m7ZUR9vFotj1uYV0OzG7hX0+huN2E/IdgLDjbiapj1e2fKHrMmGFaIvI6xzzJIQJF9GiRZ7+0jNFLKSyzX/K3JAyFrIPObfwM+y+zAgE1sWcZ1YnuBhICyRHBhaJDKIZL8MywrEfB2yF+R3k9wFG1oN48gSLyfrfEKuB/qgNp+BeTruWUk0AwRE9XVMUuRbjpxa4YA67SKunFEgFGgUfHBeHJTivvUl0u4Dki1UKAT973P+nXy2O0u239If/kRpNUVhMg8kpk7s8i6Arp7l/705/bLCx4kN5hHHSXIqkiG9tHdeNV8VYo5+72hgaCx3/uVoVLmtvxbOIvo120uTJbuLVTvT8KtsOlb3DxwUrwLzaEMoAQAFk6Q9bNipHxfkRQER4kR7IYTMzSoW5mxh3H9O8Ge5BqVeYMEW36q9wnOYfxOLNw6yQMf8f9sJN4KhZty02xm707S7VEfJJ1KNq7b5pP/3RjE0IKtB2gE6vAPRvRLzEohu0m7q1aUp8wAvSiqjZy7FLaTtLEApXYvLvz6PEJdj4TegCZugj7c8bIOEqLXmloZ6EgVnjQ7/ttys7VFITB3mazzFiyQuKf4J6+b/a/Y";
		String c2v = readC2V(vendorCode,4);
		System.out.println(c2v);
		
		
	}
	
	
	
}
