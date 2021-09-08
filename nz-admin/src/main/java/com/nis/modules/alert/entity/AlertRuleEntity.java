package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 *
 *
 * @author song
 * @email
 * @date 2019-11-25 10:11:01
 */
@Data
@TableName("alert_rule")
public class AlertRuleEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键id
	 */
	@TableId
	private Integer id;
	/**
	 * 告警名称
	 */
	private String name;

	/**
	 * 运算表达式
	 */
	private String expr;

	/**
	 * 比较符号
	 */
	private String operator;

	/**
	 * 阈值
	 */
	private Double threshold;

	/**
	 * 单位
	 */
	private Integer unit;
	/**
	 * 持续时间,单位s
	 */
	private Integer last;
	/**
	 * 告警级别
	 */
	private Integer severityId;
	/**
	 * 告警摘要
	 */
	private String summary;
	/**
	 * 告警详细描述
	 */
	private String description;

	/**
	 * 是否自动失效	默认：1
	 * 1：开启自动失效判断
	 * 0：关闭自动失效判断
	 */
	private Integer autoExpired;

	/**
	 *定时是否开启	默认：0
	 * 1：开启
	 * 0：关闭
	 */
	private Integer schedEnable;

	/**
	 *定时开启天（星期几）
	 * 默认："1,2,3,4,5,6,7"
	 * 多个逗号分隔
	 * 例：1,2,3,4,5,6,7
	 * sched_enable = 1 时，不能为空
	 */
	private String schedDays;

	/**
	 * 定时开启 开始时间 默认：00:00
	 */
	private String schedStime;

	/**
	 *定时开启 关闭时间 默认：23:59
	 */
	private String schedEtime;

	/**
	 *active通知开关 默认：1
	 * 1：开启
	 * 0：关闭
	 */
	private Integer notifyActive;

	/**
	 *expired通知开关	    默认：1
	 * 1：开启
	 * 0：关闭
	 */
	private Integer notifyExpired;

	/**
	 *trouble shooting
	 */
	private String trbShot;

	/**
	 * 通知用户ids 可多个
	 */
	private String receiver;
	/**
	 * 告警通知方式
	 */
	private String method;

	/**
	 * 是否为内置规则
	 */
	private Integer buildIn;

	/**
	 * 导入唯一标识
	 */
	private String seq;

	/**
	 * 告警规则状态
	 * 默认：开启
	 * 0：关闭
	 * 1：开启
	 */
	private Integer state;

	/**
	 * 告警 规则类型
	 * 1：metrics
	 * 2：logs
	 * 3：snmp trap
	 */
	private Integer type;
	
	/**
	 * 执行周期
	 */
	private Integer inr;
	
	/**
	 * 执行超时时间
	 */
	private Integer timeout;
	
	/**
	 * 告警消息统计
	 */
	@TableField(exist = false)
	private Integer alertNum;

	/**
	 * 通知的用户信息
	 */
	@TableField(exist = false)
	private List<SysUserEntity> receivers;
	/**
	 * 通知方式
	 */
	@TableField(exist = false)
	private List<AlertNotifyMethod> methods;

	@TableField(exist = false)
	private AlertSeverityConf severity;

	
	/**
	 * 计算 alert rule hash id
	 * @param entity
	 * @return
	 */
	public  String getAlertRuleHashId() {
		return Tool.DigestUtil.md5Hex(this.toString());
	}
	
	/**
	 * 获取 完整表达式 内容
	 * @return
	 */
	public String getExpression() {
		return String.format("%s %s %s", this.getExpr(),this.getOperator(),this.getThreshold());
	}
	
}
