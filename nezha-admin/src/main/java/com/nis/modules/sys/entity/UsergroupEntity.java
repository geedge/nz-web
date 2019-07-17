/**

 *

 *

 */

package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nis.common.validator.group.AddGroup;
import com.nis.common.validator.group.UpdateGroup;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 系统用户
 *

 */
@Data
@TableName("user_group")
public class UsergroupEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * 用户组ID
	 */
	@TableId
	private Long id;

	/**
	 * 用户组名
	 */
	@NotBlank(message="用户组名称不能为空", groups = {AddGroup.class, UpdateGroup.class})
	private String name;

	/**
	 * 操作人
	 */
	private Long operator;

	/**
	 * 备注
	 */
	private String remarks;
}
