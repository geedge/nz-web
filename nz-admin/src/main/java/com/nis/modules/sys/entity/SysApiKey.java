package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_api_key")
public class SysApiKey {

	private Integer id;

	private String name;

	private String token;

	private Integer roleId;

	private Integer createBy;

	private Date createAt;

	private Date expireAt;

	/**
	 * 备注
	 */
	private String remark;

	@TableField(exist = false)
	private SysUserEntity createUser;

	@TableField(exist = false)
	private SysRoleEntity role;
}
