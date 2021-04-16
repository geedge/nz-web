package com.nis.modules.sys.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("sys_api_key")
public class SysApiKey {
	
	private Integer id;
	
	private String name;
	
	private String token;
	
	private Integer roleId;
	
	private Integer createBy;
	
	private Date createAt;
	
	private Date expireAt;
}
