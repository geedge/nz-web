package com.nis.modules.promserver.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

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
	private Integer status;
	private Date checkTime;
}
