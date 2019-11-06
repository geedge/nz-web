/**
 * Copyright &copy; 2012-2016 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.nis.modules.dashboard.entity;

import java.io.Serializable;

import org.hibernate.validator.constraints.Length;

import lombok.Data;



/**
 * user_sqlEntity
 * @author th
 * @version 2019-05-09
 */
@Data
public class UserSql implements Serializable {
	private String sql;		// 用户自定义sql查询语句
	private Integer type;		// 图表类型
	private String vTime;		// 时间变量字段
	private String describe;   //查询数据描述
	
	
	
}