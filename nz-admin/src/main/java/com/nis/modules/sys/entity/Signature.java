package com.nis.modules.sys.entity;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import lombok.Data;

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
