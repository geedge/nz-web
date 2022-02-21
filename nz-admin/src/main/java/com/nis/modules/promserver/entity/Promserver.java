package com.nis.modules.promserver.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.Dc;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
@TableName("prom_server")
public class Promserver {
	@TableId
	private Integer id;
	@NotBlank(message="参数名不能为空")
	private Integer dcId;
	@NotBlank(message="参数名不能为空")
	private String name;
	@NotBlank(message="参数名不能为空")
	private String host;
	@NotBlank(message="参数名不能为空")
	private Integer type;
	private Integer port;
	/**
	 * agent 请求协议
	 * http/https
	 */
	private String protocol;
	/**
	 * 默认：1
	 * 1：正常
	 * 0：异常
	 * -1：认证失败
	 */
	private Integer status;
	
	@TableField(exist = false)
	private Date checkTime;

	/**
	 * json格式保存状态详情
	 * {
	 *     "prometheus": "UP",
	 *     "confagent": "UP",
	 *     "snmp_exporter": "UP"
	 * }
	 */
	private String statusInfo;


	/**
	 * confagent令牌
	 */
	private String token;

	@TableField(exist = false)
	private Dc dc;

	@Override
	public String toString() {
		return "Promserver{" +
				"id=" + id +
				", dcId=" + dcId +
				", host='" + host + '\'' +
				", type=" + type +
				", port=" + port +
				", protocol=" + protocol +
				", status=" + status +
				", checkTime=" + checkTime +
				", statusInfo='" + statusInfo + '\'' +
				", token='" + token + '\'' +
				'}';
	}
}
