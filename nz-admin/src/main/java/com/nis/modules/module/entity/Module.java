package com.nis.modules.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.project.entity.Project;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotBlank;

import org.springframework.data.annotation.Transient;

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
	private String labels;
	@NotBlank(message="参数名不能为空")
	private String path;
	private String remark;
	private Integer buildIn;
	private String seq;

	/**
	 * modulle 类型 可选值 ： http 、snmp
	 */
	private String type;

	/**
	 * 	snmp配置参数
	 */
	private String snmpParam;

	/**
	 * snmp_exporter需要的配置部分， 由web端根据snmp_param生成
	 */
	private String snmpYml;
	
	@TableField(exist = false)
	private List<Integer> endpointStat=new ArrayList<Integer>();
	
	@TableField(exist = false)
	private List<Integer> alertStat=new ArrayList<Integer>();
}
