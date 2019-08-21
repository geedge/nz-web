package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * i18n信息表
 * 
 * @author song
 * @email 
 * @date 2019-08-05 14:35:21
 */
@Data
@TableName("sys_i18n")
@KeySequence(value="seq_sys_i18n")
public class SysI18nEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(type=IdType.INPUT)
	private Long id;
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
	@JsonIgnore
	private Long operator;
	/**
	 * 操作时间
	 */
	@JsonIgnore
	private Date opTime;
	@JsonIgnore
	@TableLogic
	private String delFlag;

}
