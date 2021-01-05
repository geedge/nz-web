package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 区域表
 *
 * @author th
 * @date 2019-11-21
 */
@Data
@TableName("sys_area")
public class SysArea implements Serializable{
	@TableId
	private Long id;
	/**
	 *名称
	 */
	private String name; 
	
	/**
	 * 父id
	 */
	private Integer pid;
	
	/**
	 * 所有父id
	 */
	private String pids;
	
	/**
	 * 经度
	 */
	private Double longitude;
	
	/**
	 * 维度
	 */
	private Double latitude;
	
	private String i18n;
	
	@TableField(exist = false)
	private List<SysArea> children;
}
