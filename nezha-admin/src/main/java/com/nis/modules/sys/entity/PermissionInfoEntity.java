package com.nis.modules.sys.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("permission_info")
@KeySequence(value="seq_permission_info")
public class PermissionInfoEntity implements Serializable {
	
	private static final long serialVersionUID = -458579718666963915L;
	
	@TableId(type=IdType.INPUT)
	private Long id;
	
	private String type; //1.dete_type,2.dete_set,3.dete_warn,4.mission; 暂时只用2、4
	
	private Long typeId;
	
	private String viewLevel;
	
	private Long relId;
	
	private String delFlag;
	
	private Long operator;
	
	private Date opTime;
}
