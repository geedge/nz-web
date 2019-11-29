package com.nis.modules.sys.entity;

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
 * @date 2019-11-28 18:07:30
 */
@Data
@TableName("user_group")
public class UserGroupEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Integer id;
	/**
	 * 用户组名
	 */
	private String name;
	/**
	 * 用户组描述
	 */
	private String description;

}
