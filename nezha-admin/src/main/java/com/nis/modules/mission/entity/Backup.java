package com.nis.modules.mission.entity;

import java.io.Serializable;

import com.alibaba.fastjson.JSON;

import lombok.Data;

@Data
public class Backup implements Serializable{

	private static final long serialVersionUID = 1L;
	private String  backup;
	private String  backupTo;
	private String[] except;
	private String exceptTmpStr;
	private String isAbs;
}

