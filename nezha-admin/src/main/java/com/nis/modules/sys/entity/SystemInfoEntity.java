package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.smartvalidate.ann.NotNull;
import com.nis.common.utils.RCode;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 系统用户
 *

 */
@Data
@TableName("system_info")
@KeySequence(value="seq_system_info")
public class SystemInfoEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * 业务系统id
	 */
	@TableId(type=IdType.INPUT)
	private Long id;

	/**
	 * 业务系统名
	 */
	@NotNull(code = RCode.SYS_SYS_NULLNAME)
	private String name;

	/**
	 * 是否内置 1：内置，0：非内置
	 */
	private Integer builtIn;

	/**
	 * 创建人
	 */
	private Long createBy;

	/**
	 * 创建时间
	 */
	private Date createDate;
	
	/**
	 * 更新人
	 */
	private Long updateBy;
	
	/**
	 * 更新时间
	 */
	private Date updateDate;

	/**
	 * 备注
	 */
	private String remarks;
	
	/**
	 * 删除标识  0：正常   1：删除
	 */
	private Integer delFlag;
	
	/**
	 * 关联的用户组
	 */
	@TableField(exist=false)
	private List<UsergroupEntity> usergroups;
	
	@TableField(exist=false)
	private List<Integer> usergroupIds;
	
	@TableField(exist=false)
	private Long usergroupId;
}
