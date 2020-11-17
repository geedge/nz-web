package com.nis.modules.endpoint.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("endpoint_state")
public class EndpointState {
	private Integer endpointId;
	private Date createTime;
	private Integer state;
}
