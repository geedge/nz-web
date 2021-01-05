package com.nis.modules.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("project")
public class Project {
	@TableId
	private Integer id;
	@NotBlank(message="参数名不能为空")
	private String name;
	private String remark;
	private Integer buildIn;
	private String seq;
	@TableField(exist = false)
	private List<Integer> alertStat = new ArrayList<Integer>();
}
