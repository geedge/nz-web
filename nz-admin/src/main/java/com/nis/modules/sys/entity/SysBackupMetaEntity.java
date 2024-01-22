package com.nis.modules.sys.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class SysBackupMetaEntity implements Serializable{

	private static final long serialVersionUID = 1L;
	private long ts;
	private String remark;
	
}
