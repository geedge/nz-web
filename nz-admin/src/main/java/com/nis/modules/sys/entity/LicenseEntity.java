package com.nis.modules.sys.entity;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import lombok.Data;
/**
 * license证书实体
 * @author fang
 *
 */
@Data
public class LicenseEntity implements Serializable,Cloneable{

	private static final long serialVersionUID = 4633547881083628433L;
	@JSONField(ordinal=1)
	private String licenseId;
	@JSONField(ordinal=2)
	private String customer;
	@JSONField(ordinal=3)
	private String contract;
	@JSONField(ordinal=4)
	private String by;
	@JSONField(ordinal=5)
	private String genDate;
	@JSONField(ordinal=6)
	private String startDate;
	@JSONField(ordinal=7)
	private String endDate;
	@JSONField(ordinal=8)
	private String comment;
	@JSONField(ordinal=9)
	private Tokens tokens;
	@JSONField(ordinal=10)
	private Product product;
	@JSONField(ordinal=11)
	private Signature signature;
	
	@Data
	public class Tokens implements Serializable,Cloneable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 4812749370562684544L;
		@JSONField(ordinal=1)
		private String[] token;
	}

	@Data
	public class Product implements Serializable,Cloneable{
		/**
		 * 
		 */
		private static final long serialVersionUID = -2658149618046598534L;
		@JSONField(ordinal=1)
		private String name;
		@JSONField(ordinal=2)
		private String version;
		@JSONField(ordinal=3)
		private Integer maximumAssets;
	}

	@Data
	public class Signature implements Serializable,Cloneable{
		/**
		 * 
		 */
		private static final long serialVersionUID = -2293105833290860463L;
		@JSONField(ordinal=1)
		private String digestValue;
		@JSONField(ordinal=2)
		private String signatureValue;
	}

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
	
	
	@JSONField(serialize=false)
	public Signature newSignature() {
		return new Signature();
	}
	
}