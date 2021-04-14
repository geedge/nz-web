package com.nis.modules.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("monitor_conf_event")
public class MonitorConfEvent implements Serializable{

	private static final long serialVersionUID = 1L;

	private String table;

	private Integer value;

}
