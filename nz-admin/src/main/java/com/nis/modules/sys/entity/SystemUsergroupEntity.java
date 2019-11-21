/**

 *

 *

 */

package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统用户
 *

 */
@Data
@TableName("system_usergroup_rel")
@KeySequence(value="seq_system_usergroup_rel")
public class SystemUsergroupEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * ID
	 */
	@TableId(type=IdType.INPUT)
	private Long id;

	/**
	 * 业务系统ID
	 */
	private Long systemId;

	/**
	 * 用户组ID
	 */
	private Long usergroupId;

	/**
	 * 删除标识，1：删除，0：未删除
	 */
	private String delFlag;
	
	/**
	 * 操作人
	 */
	private Long operator;
	
	/**
	 * 操作时间
	 */
	private Date opTime;
}
