package com.nis.modules.sys.entity;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import lombok.Data;

@Data
public class Signature implements Serializable{

	@XmlElement(name="DigestMethod")
	private String digestMethod;
	
	@XmlElement(name="DigestValue")
	private String digestValue;

	@XmlTransient
	public String getDigestMethod() {
		return digestMethod;
	}
	
	@XmlTransient
	public String getDigestValue() {
		return digestValue;
	}
	
	
}
