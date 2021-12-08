package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("sys_component")
public class SysComponent {
	@TableId
	private Integer id;
	
	private String serverid;
	
	private String component;
	
	private String host;
	
	private Integer port;
	
	private String protocol;
	
	private Long utime;
	
	@TableField(exist = false)
	private Integer status;
}
