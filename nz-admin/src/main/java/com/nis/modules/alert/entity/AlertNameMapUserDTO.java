package com.nis.modules.alert.entity;

import java.util.List;

import com.nis.modules.sys.entity.SysUserEntity;

import lombok.Data;

/**
 * alertname和需要邮件通知的user的映射类
 */
@Data
public class AlertNameMapUserDTO {
	private Integer ruleId;
	private Integer receiver; //usergroupId
	private List<SysUserEntity> users;
}
