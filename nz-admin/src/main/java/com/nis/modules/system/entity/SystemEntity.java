package com.nis.modules.system.entity;

import javax.validation.constraints.NotBlank;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("system")
public class SystemEntity {
	@TableId
	private Integer id;
	@NotBlank(message="参数名不能为空")
	private String name;
	private String remark;
}
