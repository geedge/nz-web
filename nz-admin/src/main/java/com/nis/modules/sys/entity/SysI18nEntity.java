package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * i18n信息表
 */
@Data
@TableName("sys_i18n")
public class SysI18nEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(type=IdType.INPUT)
	private Long id;
	
	/**
	 * 名称
	 */
	private String name;
	/**
	 * i18n code
	 */
	private String code;
	/**
	 * 语言
	 */
	private String lang;
	/**
	 * 翻译值
	 */
	private String value;
	/**
	 * 备注信息
	 */
	private String remark;
	/**
	 * 操作人
	 */
	private Long uby;
	/**
	 * 操作时间
	 */
	private Date utime;
	
	@TableField(exist = false)
	private SysUserEntity uuser;

}
