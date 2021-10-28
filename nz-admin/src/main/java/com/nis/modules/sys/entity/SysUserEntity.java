package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nis.common.validator.group.AddGroup;
import com.nis.common.validator.group.UpdateGroup;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 系统用户
 */
@Data
@TableName("sys_user")
public class SysUserEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * 用户ID
	 */
	@TableId
	private Long id;

	/**
	 * 用户名
	 */
	@NotBlank(message="登录用户名不能为空", groups = {AddGroup.class, UpdateGroup.class})
	private String username;
	
	/**
	 * 密码
	 */
	@NotBlank(message="密码不能为空", groups = AddGroup.class)
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String pin;

	/**
	 * 盐
	 */
	@JsonIgnore
	private String salt;

	/**
	 * 邮箱
	 */
	@NotBlank(message="邮箱不能为空", groups = {AddGroup.class, UpdateGroup.class})
	@Email(message="邮箱格式不正确", groups = {AddGroup.class, UpdateGroup.class})
	private String email;

	/**
	 * 状态  0：禁用   1：正常
	 */
	private Integer status;
	
	/**
	 * 语言
	 */
	private String lang;
	
	/**
	 * 角色ID列表
	 */
	@TableField(exist = false)
	private String roleIds;

	/**
	 * 用户组ID列表
	 */
	@TableField(exist=false)
	private List<Long> usergroupIds;
	@TableField(exist=false)
	private String userIds;
	
	private String mobile;
	
	/**
	 * 创建时间
	 */
	private Date createAt;
	
	private Integer createBy;

	private String name;
	
	/**
	 * 来源
	 */
	private String source;

	private String lastLoginIp;
	
	private Date lastLoginTime;
	
	private Integer mfaLevel;//是否开启认证
	@TableField(exist = false)
	private Integer mfaAuthEnable;//全局认证开关
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String otpKey;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String recoveryCode;
	
	/**
	 * 用户关联角色列表
	 */
	@TableField(exist = false)
	private List<SysRoleEntity> roles;

	@TableField(exist = false)
	private String userName;

	@TableField(exist = false)
	private Integer apiKeyId;
	
	@TableField(exist = false)
	private String authToken;
	
	@TableField(exist = false)
	private String authCode;//验证码
	
	/**
	 * 主题
	 */
	private Integer theme; 
}
