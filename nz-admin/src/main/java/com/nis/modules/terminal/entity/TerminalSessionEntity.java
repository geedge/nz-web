package com.nis.modules.terminal.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * terminal_session
 *
 * @author shizhendong
 * @date 2021-02-01
 */
@Data
@TableName("terminal_session")
public class TerminalSessionEntity implements Serializable {

	@TableId
	private Integer id;

	/**
	 * session id
	 */
	private String uuid;

	/**
	 * 资产 id
	 */
	private Integer assetId;

	/**
	 * kill_user_id
	 */
	private String host;

	/**
	 * 端口
	 */
	private Integer port;

	/**
	 * 协议
	 */
	private String protocol;

	/**
	 * 认证类型，1：密码，2：私钥证书
	 * ssh可选值：1、2
	 * telnet可选值：1
	 */
	private Integer authType;

	/**
	 * 登录主机的用户名
	 */
	private String loginUser;

	/**
	 * nezha系统内用户id
	 */
	private Long userId;

	/**
	 * 客户端ip地址
	 */
	private String remoteAddr;

	/**
	 * 开始时间
	 */
	private Date startTime;

	/**
	 * 结束时间
	 */
	private Date endTime;

	/**
	 * 0：连接中
	 * 1：发送失败（没有通过ssh或telnet发送成功）
	 * 2：已结束
	 * 3：强制退出
	 * 4：未知错误
	 */
	private Integer status;

	/**
	 * 强制关闭用户id
	 */
	private Long killUserId;

	/**
	 * 系统用户
	 */
	@TableField(exist = false)
	private String username;

	/**
	 * 强制退出踢出用户名
	 */
	@TableField(exist = false)
	private String killUserName;

	@Override
	public String toString() {
		return "TerminalSessionEntity{" +
				"id=" + id +
				", uuid='" + uuid + '\'' +
				", assetId=" + assetId +
				", host='" + host + '\'' +
				", port=" + port +
				", protocol='" + protocol + '\'' +
				", authType=" + authType +
				", loginUser='" + loginUser + '\'' +
				", userId=" + userId +
				", remoteAddr='" + remoteAddr + '\'' +
				", startTime=" + startTime +
				", endTime=" + endTime +
				", status=" + status +
				", killUserId=" + killUserId +
				", username='" + username + '\'' +
				", killUserName='" + killUserName + '\'' +
				'}';
	}
}