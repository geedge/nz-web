package com.nis.modules.sys.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.panel.entity.Node;

import lombok.Data;

@Data
@TableName("link")
public class Link extends Node implements Serializable{
	private String name;
	private Long createBy;
	private String url;
	private Integer buildIn;
	
	@TableField(exist = false)
	private SysUserEntity creator;
}
