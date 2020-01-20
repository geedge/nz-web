package com.nis.modules.promserver.entity;

import java.util.Date;

import javax.validation.constraints.NotBlank;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("prom_server")
public class Promserver {
	@TableId
	private Integer id;
	@NotBlank(message="参数名不能为空")
	private Integer idcId;
	@NotBlank(message="参数名不能为空")
	private String host;
	@NotBlank(message="参数名不能为空")
	private Integer type;
	private Integer port;
	private Boolean status;
	private Date lastUpdate;
}
