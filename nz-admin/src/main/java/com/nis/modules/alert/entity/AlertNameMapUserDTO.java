package com.nis.modules.alert.entity;

import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;

import java.util.List;

/**
 * alertname和需要邮件通知的user的映射类
 */
@Data
public class AlertNameMapUserDTO {
	private Integer ruleId;
	private Integer receiver; //usergroupId
	private List<SysUserEntity> users;
}
