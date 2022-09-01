package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统文件
 */
@Data
@TableName("sys_file")
public class SysFileEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * 文件ID
	 */
	@TableId
	private Integer id;
	/**
	 * 唯一索引
	 */
	private String uuid;
	/**
	 * 文件名称
	 */
	private String name;
	/**
	 * 文件类型
	 */
	private String type;
	/**
	 * 文件大小
	 */
	private Long size;
	/**
	 * 文件MD5
	 */
	private String md5;
	/**
	 * 用户自定义元数据
	 */
	private String meta;
	/**
	 * 存储类型 1: DB 2: S3 3: file
	 */
	private Integer storage;
	/**
	 * 存储路径
	 */
	private String path;
	/**
	 * 时间戳
	 */
	private Long ts;
}
