package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("snmp_credential")
public class SnmpCredential {

	private Integer id;
	
	private String name;
	
	private String remark;
	
	private Integer type;
	
	private String config;
	
}
