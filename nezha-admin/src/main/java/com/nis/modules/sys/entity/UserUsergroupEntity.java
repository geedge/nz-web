package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * 
 * @author song
 * @email 
 * @date 2019-08-01 14:56:46
 */
@Data
@TableName("user_usergroup")
@KeySequence(value="seq_user_usergroup")
public class UserUsergroupEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId(type=IdType.INPUT)
	private Long id;
	/**
	 * 
	 */
	private Long userId;
	/**
	 * 
	 */
	private Long groupId;
	/**
	 * 1：删除，0：未删除
	 */
	private String delFlag;
	/**
	 * 
	 */
	private Long operator;
	/**
	 * 
	 */
	private Date opTime;

}
