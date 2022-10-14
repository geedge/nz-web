package com.nis.modules.sys.service.impl;

import Aladdin.HaspStatus;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeException;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.LicenseEntity;
import com.nis.modules.sys.entity.Signature;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.LicenseService;
import com.nis.modules.sys.service.SysConfigService;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LicenseServiceImpl implements LicenseService {
	private static final Log log = Log.get();

	@Autowired
	private SysConfigService sysConfigService;

	@Value("${license.config.key:license}")
	private String licenseConfigKey;
	@Value("${license.token.key:license_token}")
	private String licenseTokenKey;
	/**
	 * 系统版本sys_config 表key
	 */
	@Value("${system.version.key:system_version}")
	private String systemVersionKey;

	@Value("${vendorCode}")
	private String vendorCode;

	@Value("${fetureId}")
	private Integer fetureId;

	/**
	 * 获取license 详情
	 */
	@Override
	public Map<String,Object> getLicenseDetail() {
		String licenseInfo = Tool.HaspUtil.getLicenseInfo(vendorCode,fetureId);
		if(Tool.StrUtil.isBlank(licenseInfo)) {
			throw new NZException(RCode.LICENSE_FILE_IS_EMPTY);
		}
		Map<String, Object> licenseMap = Tool.XmlUtil.xmlToMap(licenseInfo);
		
		String v2c = sysConfigService.getValue("license");
		LicenseEntity license = (LicenseEntity) LicenseUtil.convertXmlStrToObject(LicenseEntity.class, v2c);
		if(ToolUtil.isNotEmpty(license)) {
			licenseMap.put("comment", license.getComment());
			licenseMap.put("supportID", license.getSupportID());
			licenseMap.put("organization", license.getOrganization());
			licenseMap.put("type", license.getType());
		}
		return licenseMap;
	}

	/**
	 * 更新license
	 */
	@Override
	public void uploadLicense(String v2c) {
		SysConfigEntity entity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", licenseConfigKey));
		if (entity == null) {
			entity = new SysConfigEntity();
		}
		entity.setParamKey(licenseConfigKey);
		entity.setParamValue(v2c);
		entity.setRemark(Tool.DateUtil.now());
		sysConfigService.saveOrUpdate(entity);
	}

	/**
	 * 获取系统版本
	 * 
	 * @return
	 */
	@Override
	public String getSystemVersion() {
		return sysConfigService.getValue(systemVersionKey);
	}

	/**
	 * 获取 license 指纹
	 * @return
	 */
	@Override
	public String getLicenseC2V() {
		String c2v = Tool.HaspUtil.readC2V(vendorCode,fetureId);
		return c2v;
	}
	
	/**
	 * 安装 license
	 * @throws DocumentException 
	 */
	@Override
	public boolean installLicense(){
		String license = this.getLicense();
		log.info("license v2c content: {}" ,license);
		if(Tool.StrUtil.isEmpty(license)) {
			log.error("license v2c empty");
			return false;
		}else {
			try {
				org.dom4j.Document document = DocumentHelper.parseText(license);
				Element hasps = document.getRootElement().element("hasps");
				if(ToolUtil.isEmpty(hasps)) {
					return false;
				}
				List<org.dom4j.Node> nodeList = hasps.content();
				for(int i = 0,j = nodeList.size();i<j;i++) {
					org.dom4j.Node node = nodeList.get(i);
					String v2c = node.asXML();
					int status = Tool.HaspUtil.updateKeyWithLicense(v2c, vendorCode);
					if(status == HaspStatus.HASP_STATUS_OK || status == HaspStatus.HASP_UPDATE_ALREADY_ADDED) {
						return true;
					}
				}
				return false;
			}catch(Exception e) {
				log.error("installLicense error msg : %s",e.getMessage());
				return false;
			}
		}
	}

	/**
	 * 验证 license
	 */
	@Override
	public Integer verify() {
		return Tool.HaspUtil.verify(vendorCode,fetureId);
	}

	/**
	 * 获取所有 license
	 */
	@Override
	public String getLicense() {
		SysConfigEntity entity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", licenseConfigKey));
		if (entity == null || Tool.StrUtil.isBlank(entity.getParamValue())) {
			return null;
		}
		return entity.getParamValue();
	}

	@Override
	public void validateDigestValue(String v2c) throws DocumentException {
		org.dom4j.Document doc = DocumentHelper.parseText(v2c);
		Element rootElement = doc.getRootElement();
		Element hasps = rootElement.element("hasps");
		String haspsStr = hasps.asXML();
		LicenseEntity entity = (LicenseEntity) LicenseUtil.convertXmlStrToObject(LicenseEntity.class, v2c);
		String digestValue = entity.getSignature().getDigestValue();
		entity.setHasps(haspsStr);
		String genLicenseDigest = entity.genLicenseDigest();
		log.debug("File digestValue : %s , figure out genLicenseDigest : %s", digestValue,genLicenseDigest);
		if(!StrUtil.equals(digestValue, genLicenseDigest)) {
			throw new NZException(RCode.LICENSE_DIGESTVALUE_INVALID);
		}
	}

	@Override
	public String gen(MultipartFile[] files, Map<String, Object> params) {
		String supportID = (String) params.get("supportID");
		String organization = (String) params.get("organization");
		String type = (String) params.get("type");
		String comment = (String) params.get("comment");
		List<String> haspInfos = new ArrayList<String>();
		String license = null;
		try {
			// 提取v2c文件中的hasp info数据
			for(MultipartFile file : files) {
				
					byte[] bytes = file.getBytes();
					String fileLicense = Tool.StrUtil.str(bytes, "utf-8");
					org.dom4j.Document document = DocumentHelper.parseText(fileLicense);
			    	Element ele = document.getRootElement();
			    	if(StrUtil.equals(ele.getName(), "hasp_info")) {
		    			haspInfos.add(ele.asXML());
		    		}
			    	List<Element> elements = ele.elements();
			    	for(Element e : elements) {
			    		if(StrUtil.equals(e.getName(), "hasp_info")) {
			    			haspInfos.add(e.asXML());
			    		}
			    		List<org.dom4j.Node> nodes = e.content();
			    		for(org.dom4j.Node node : nodes) {
			    			if(StrUtil.equals(node.getName(), "hasp_info")) {
			        			haspInfos.add(node.asXML());
			        		}
			    		}
			    	}
			}
			
			if(ToolUtil.isEmpty(haspInfos)) {
				log.debug("license gen haspInfos empty");
				return null;
			}
			// 程序组装v2c文件 返回
			List<String> sortHasp = haspInfos.stream().sorted().collect(Collectors.toList());
			
			org.dom4j.Document document = DocumentHelper.createDocument();
			Element licenseEle = document.addElement("license");
			Element hasps = licenseEle.addElement("hasps");
	
			for(String hasp : haspInfos) {
				org.dom4j.Document haspDoc = DocumentHelper.parseText(hasp);
				org.dom4j.Element haspInfo = haspDoc.getRootElement();
				hasps.add(haspInfo);
			}
			String haspsStr = hasps.asXML();
	
			licenseEle.remove(hasps);
			LicenseEntity licenseEntity = new LicenseEntity();
			licenseEntity.setSupportID(supportID);
			licenseEntity.setOrganization(organization);
			licenseEntity.setType(type);
			licenseEntity.setComment(comment);
			licenseEntity.setHasps(haspsStr);
			String digest = licenseEntity.genLicenseDigest();
	
			Signature signature = new Signature();
			signature.setDigestMethod("sha1");
			signature.setDigestValue(digest);
			licenseEntity.setSignature(signature);
	
			String licenseXml = LicenseUtil.convertObjectToXmlStr(licenseEntity);
	
			org.dom4j.Document licDoc = DocumentHelper.parseText(licenseXml);
			org.dom4j.Element lic = licDoc.getRootElement();
			Element licHasps = lic.element("hasps");
			if(ToolUtil.isNotEmpty(licHasps)) {
				lic.remove(licHasps);
			}
			lic.add(hasps);
			license = licDoc.asXML();
		} catch (Exception e) {
			log.error("license  gen error msg : %s",e.getMessage());
			e.printStackTrace();
		}
		return license;
	}

	/**
	 * 解析license二维码 生成c2v文件
	 */
	public String qrdecode(MultipartFile[] files){
		ValidateUtils validate = ValidateUtils.is(files).notNull(RCode.LICENSE_QRCODE_FILES_ISNULL);
		String[] contents = null;
		String c2vMd5 = null;
		for (MultipartFile file : files) {
			try {
				// 解析二维码
				String decodeStr = Tool.QrCodeUtil.qrDecode(file.getInputStream());
				if(contents == null){
					Integer size = Integer.parseInt(decodeStr.substring(0, 2));
					validate.and(size).equalsIgnoreCase(ToolUtil.toStr(files.length), RCode.LICENSE_QRCODE_FILES_INCORRECT);
					// 二维码顺序从1开始
					contents = new String[size + 1];
					contents[0] = "";
					c2vMd5 = decodeStr.substring(4, 12);
				}
				Integer index = Integer.parseInt(decodeStr.substring(2, 4));
				if(contents[index] != null){
					// 序列重复
					throw new NZException(RCode.LICENSE_QRCODE_FILES_DUPLICATE);
				}
				contents[index] = decodeStr.substring(20);
				// 所有c2v文件md5必须一致   当前c2v文件md5必须与当前内容一致
				validate.and(c2vMd5).equalsIgnoreCase(decodeStr.substring(4, 12), RCode.LICENSE_QRCODE_FILES_ALL_INCONSISTENT)
						.and(decodeStr.substring(12, 20)).equalsIgnoreCase(
								DigestUtils.md5Hex(contents[index]).substring(0, 8),
								RCode.LICENSE_QRCODE_FILES_INCONSISTENT.setParam(file.getOriginalFilename()));
			} catch (NumberFormatException | ArrayIndexOutOfBoundsException | QrCodeException | IOException e){
				throw new NZException(RCode.LICENSE_QRCODE_FILES_INVALIDE.setParam(file.getOriginalFilename()));
			}
		}
		String licenseInfo = Arrays.asList(contents).stream().collect(Collectors.joining());
		// c2v文件md5必须与全内容一致
		validate.and(c2vMd5).equalsIgnoreCase(DigestUtils.md5Hex(licenseInfo).substring(0, 8), RCode.LICENSE_QRCODE_FILES_ALL_INCONSISTENT);
		return licenseInfo;
	}

}
