package com.nis.modules.sys.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.entity.LicenseEntity;
import com.nis.modules.sys.entity.LicenseEntity.Signature;
import com.nis.modules.sys.service.LicenseService;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.log.Log;

@RestController
@RequestMapping("/sys/license")
public class SysLicenseController extends AbstractController {
	private static final Log log = Log.get();
	
	@Autowired
	private LicenseService licenseService;
	
	/**
	 * 私钥存储路径，gen生成证书时使用
	 */
	@Value("${license.privatekey.path:license/license.key}")
	private String prikeyPath;
	
	@Value("${license.fileName:license.xml}")
	private String licenseFileName;
	
	/**
	 * 获取license
	 */
	@RequestMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.LICENSE)
	public R getLicense(){
		LicenseEntity licenseEntity = licenseService.getLicenseEntity();
		return R.ok().putData("license", licenseEntity);
	}
	
	/**
	 * 获取license state
	 */
	@RequestMapping("/state")
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.LICENSE)
	public R getLicenseState(){
		//获取本机token
		String token = licenseService.getLicenseToken();
		R result = R.ok();
		//读取sys_config表中license
		try {
			LicenseEntity licenseEntity = licenseService.getAndSetLicenseCache();
			licenseService.verify(licenseEntity);
		} catch (NZException e) {
			int code = e.getCode();
			String msg = e.getMsg();
			result = R.error(code, msg);
		}
		result.put("token", token);
		return result;
	}

	/**
	 * @throws IOException 
	 * 上传license
	 * @throws  
	 */
	@PostMapping("/upload")
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.LICENSE)
	public R uploadLicense(@RequestParam("file") MultipartFile file) throws IOException {
		ValidateUtils.is(file).notNull(RCode.LICENSE_FILE_IS_NULL);
		byte[] bytes = file.getBytes();
		//获取文件内容
		String xml = null;
		try(ByteArrayInputStream stream = Tool.IoUtil.toStream(bytes);){
			Document document = Tool.XmlUtil.readXML(stream);
			xml = Tool.XmlUtil.toStr(document, Constant.DEFAULT_CHARTSET_NAME, false);
		}
		//将内容转换为 javabean
		LicenseEntity licenseEntity = Tool.LicenseUtil.xmlToLicenseBean(xml);
		//校验证书内容是否缺失
		ValidateUtils.is(licenseEntity).notNull(RCode.LICENSE_FILE_IS_EMPTY)
			.and(licenseEntity.getSignature()).notNull(RCode.LICENSE_FILE_INVALID)
			.and(licenseEntity.getSignature().getSignatureValue()).notNull(RCode.LICENSE_FILE_INVALID);
		//校验证书是否有效
		licenseService.verify(licenseEntity);
		//更新证书
		licenseService.uploadLicenseEntity(licenseEntity);
		return R.ok();
	}

	/**
	 * 下载license
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 * @throws  
	 */
	@RequestMapping("/download")
	public void download(HttpServletResponse response) throws  IOException{
		LicenseEntity licenseEntity = licenseService.getLicenseEntity();
		if(licenseEntity == null) {
			throw new NZException(RCode.LICENSE_FILE_IS_EMPTY);
		}
		String licenseXml = Tool.LicenseUtil.licenseBeanToXml(licenseEntity, true);
		Tool.ResponseUtil.downloadFile(response, licenseFileName,licenseXml.getBytes(Constant.DEFAULT_CHARTSET_NAME));
	}

	/**
	 * 生成 license
	 * @throws IOException 
	*/ 
	@PostMapping("/gen")
	public void genLicense(@RequestBody LicenseEntity license,HttpServletResponse response) throws IOException{
		//验证参数是否为空
		ValidateUtils.is(license).notNull(RCode.LICENSE_PARAM_IS_NULL);
		//计算摘要和签名
		File file = Tool.FileUtil.newFile(prikeyPath);
		log.debug("pk path : {} ,exist : {}", file.getAbsolutePath(),file.exists());
		try (InputStream inputStream = file.exists() ? Tool.FileUtil.getInputStream(prikeyPath)
				: this.getClass().getResourceAsStream(prikeyPath)) {
			DateTime now = Tool.DateUtil.date().setTimeZone(TimeZone.getTimeZone("UTC"));
			license.setGenDate(Tool.DateUtil.format(now, DatePattern.UTC_PATTERN));
			//算config
			String digestLicense = license.genLicenseDigest();
			PrivateKey privateKey = Tool.PemUtil.readPemPrivateKey(inputStream);
			//使用私钥对摘要签名
			String sign = Tool.LicenseUtil.genSign(license, privateKey, licenseService.getSignAlgorithm());
			Signature signature = license.newSignature();
			signature.setDigestValue(digestLicense);
			signature.setSignatureValue(sign);
			//保存签名信息
			license.setSignature(signature);
		}
		//转为xml
		String xmlStr = Tool.LicenseUtil.licenseBeanToXml(license, true);
		//下载文件
		Tool.ResponseUtil.downloadFile(response, licenseFileName, Tool.StrUtil.bytes(xmlStr,Constant.DEFAULT_CHARTSET_NAME));
	}

}
