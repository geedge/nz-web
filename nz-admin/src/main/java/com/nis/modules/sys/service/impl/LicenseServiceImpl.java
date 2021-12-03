package com.nis.modules.sys.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.LicenseService;
import com.nis.modules.sys.service.SysConfigService;

import Aladdin.HaspStatus;
import cn.hutool.log.Log;

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
	private Long fetureId;

	@Value("${productId}")
	private Integer productId;
	
	@Override
	public Map<String,Object> getLicenseDetail() {
		String licenseInfo = Tool.HaspUtil.getLicenseInfo(productId, vendorCode);
		if(Tool.StrUtil.isBlank(licenseInfo)) {
			throw new NZException(RCode.LICENSE_FILE_IS_EMPTY);
		}
		Map<String, Object> licenseMap = Tool.XmlUtil.xmlToMap(licenseInfo);
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

	@Override
	public boolean installLicense() {
		String license = this.getLicense();
		log.info("license v2c content: {}" ,license);
		if(Tool.StrUtil.isEmpty(license)) {
			log.error("license v2c empty");
			return false;
		}else {
			Document document = Tool.XmlUtil.parseXml(license);
			NodeList nodeList = document.getDocumentElement().getChildNodes();
			for(int i = 0,j = nodeList.getLength();i<j;i++) {
				Node node = nodeList.item(i);
				String v2c = Tool.XmlUtil.toStr(node);
				int status = Tool.HaspUtil.updateKeyWithLicense(v2c, vendorCode);
				if(status == HaspStatus.HASP_STATUS_OK) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public Integer verify() {
		return Tool.HaspUtil.verify(vendorCode,fetureId);
	}

	@Override
	public String getLicense() {
		SysConfigEntity entity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", licenseConfigKey));
		if (entity == null || Tool.StrUtil.isBlank(entity.getParamValue())) {
			return null;
		}
		return entity.getParamValue();
	}
	
}
