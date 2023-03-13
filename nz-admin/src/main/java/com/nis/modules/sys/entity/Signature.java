package com.nis.modules.sys.entity;

import lombok.Data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

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
