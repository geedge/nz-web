package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统用户组
 *
 */
@Data
@TableName("user_group")
@KeySequence(value="seq_user_group")
public class UserGroupEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * 用户组ID
	 */
	@TableId(type=IdType.INPUT)
	private Long id;

	/**
	 * 用户组名
	 */
	private String name;

	/**
	 * 操作人
	 */
	private Long operator;

	/**
	 * 备注
	 */
	private String remark;
	
	private Date opTime;
	
	private String delFlag;
}
