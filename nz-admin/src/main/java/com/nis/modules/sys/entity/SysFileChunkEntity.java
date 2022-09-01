package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统文件存储数据
 */
@Data
@TableName("sys_file_chunk")
public class SysFileChunkEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * ID
	 */
	@TableId
	private Integer id;
	/**
	 * 文件ID
	 */
	private Integer fileId;
	/**
	 * 数据块
	 */
	private byte[] chunk;

}
