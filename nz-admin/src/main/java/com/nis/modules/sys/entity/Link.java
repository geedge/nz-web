package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("link")
public class Link  implements Serializable{
	@TableId
	private Integer id;
	private String name;
	private Long createBy;
	private String url;
	private Integer buildIn;
	private Integer weight;
	
	@TableField(exist = false)
	private SysUserEntity creator;
}
