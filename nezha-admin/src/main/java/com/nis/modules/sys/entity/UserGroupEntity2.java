package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 系统用户组
 * 此实体类仅用来接收查询结果，创建此类是因为多个实体类之间若存在对方的嵌套变量，启动会出错；
 * 如：UserGroupEntity里定义变量"List<SystemInfoEntity> systems"，SystemInfoEntity里定义变量"List<UserGroupEntity> usergroups"，则无法启动
 */
@Data
public class UserGroupEntity2 implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * 用户组ID
	 */
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
	
	@TableField(exist = false)
	private List<SysUserEntity> users;
	
	@TableField(exist = false)
	private List<SystemInfoEntity> systems;

}
