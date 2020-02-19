package com.nis.modules.module.entity;

import javax.validation.constraints.NotBlank;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.project.entity.Project;

import lombok.Data;

@Data
@TableName("module")
public class Module {
	@TableId
	private Integer id;
	@TableField(exist = false)
	private Project project;
	@NotBlank(message="参数名不能为空")
	private Integer projectId;
	@NotBlank(message="参数名不能为空")
	private String name;
	@NotBlank(message="参数名不能为空")
	private Integer port;
	private String param;
	@NotBlank(message="参数名不能为空")
	private String path;
	private String remark;
	private Integer buildIn;
	private String seq;
}
