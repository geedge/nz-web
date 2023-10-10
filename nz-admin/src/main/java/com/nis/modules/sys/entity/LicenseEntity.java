package com.nis.modules.sys.entity;

import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.nis.common.utils.Tool;
import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;
/**
 * license证书实体
 * @author fang
 *
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "license")  
public class LicenseEntity implements Serializable,Cloneable{

	private static final long serialVersionUID = 4633547881083628433L;

	@JSONField(ordinal=1)
	private String supportID;
	@JSONField(ordinal=2)
	private String organization;
	@JSONField(ordinal=3)
	private String type;
	@JSONField(ordinal=4)
	private String comment;
	@JSONField(ordinal=5)
	private String hasps;
	@JSONField(ordinal=6)
	@XmlElement(name="Signature")
	private Signature signature;

	/**
	 * 返回不包含 Signature 
	 * @return
	 */
	public LicenseEntity genConfig() {
		LicenseEntity clone = Tool.ObjectUtil.clone(this);
		clone.setSignature(null);
		return clone;
	}
	/**
	 * 获取 config string ，用于计算摘要
	 * @return
	 */
	public String genConfigStr() {
		LicenseEntity config = this.genConfig();
		Map<String, Object> beanToMap = Tool.BeanUtil.beanToMap(config);
		beanToMap.forEach((k, v) -> {
			if(Tool.ObjectUtil.isEmpty(v)) {
				beanToMap.put(k, null);
			}
		});
		config = Tool.BeanUtil.toBean(beanToMap, LicenseEntity.class,CopyOptions.create().setIgnoreError(true).setIgnoreNullValue(true));
		return config.toString();
	}
	
	/**
	 * 计算config 摘要
	 * @return
	 */
	public String genLicenseDigest() {
		String configStr = this.genConfigStr();
		return SecureUtil.sha1(configStr);
	}
	
	@XmlTransient
	public Signature getSignature() {
		return signature;
	}
}

