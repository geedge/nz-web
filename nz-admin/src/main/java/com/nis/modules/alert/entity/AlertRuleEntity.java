package com.nis.modules.alert.entity;

import cn.hutool.core.lang.TypeReference;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author fang
 * 
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
	 * 单位
	 */
	private Integer unit;
	/**
	 * 持续时间,单位s
	 */
	private Integer last;
	/**
	 * 告警摘要
	 */
	private String summary;
	/**
	 * 告警详细描述
	 */
	private String description;

	/**
	 * 是否自动失效 默认：1 1：开启自动失效判断 0：关闭自动失效判断
	 */
	private Integer autoExpired;

	/**
	 * 定时是否开启 默认：0 1：开启 0：关闭
	 */
	private Integer schedEnable;

	/**
	 * 定时开启天（星期几） 默认："1,2,3,4,5,6,7" 多个逗号分隔 例：1,2,3,4,5,6,7 sched_enable = 1 时，不能为空
	 */
	private String schedDays;

	/**
	 * 定时开启 开始时间 默认：00:00
	 */
	private String schedStime;

	/**
	 * 定时开启 关闭时间 默认：23:59
	 */
	private String schedEtime;

	/**
	 * active通知开关 默认：1 1：开启 0：关闭
	 */
	private Integer notifyActive;

	/**
	 * expired通知开关 默认：1 1：开启 0：关闭
	 */
	private Integer notifyExpired;

	/**
	 * trouble shooting
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
	 * 告警规则状态 默认：开启 0：关闭 1：开启
	 */
	private Integer state;

	/**
	 * 告警 规则类型 1：metrics 2：logs 3：snmp trap
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
	 * 自定义标签，格式: [{name:"abc",value:"value1"}]
	 */
	private String extLabels;
	/**
	 * 收藏标识： 1：收藏 0：未收藏
	 */
	@TableField(exist = false)
	private Integer starred;

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

	/**
	 * 多阈值告警条件
	 */
	@TableField("`condition`")
	private String condition;
	
	@TableField(exist = false)
	private List alert;

	public void setName(String name) {
		this.name = Tool.StrUtil.trim(name);
	}

	/**
	 * 计算 alert rule hash id
	 *
	 * @return
	 */
	public String getAlertRuleHashId() {
		String content = new StringBuilder(AlertRuleEntity.class.getName()).append(id).append(name).append(expr)
				.append(unit).append(last).append(summary).append(description).append(autoExpired).append(schedEnable)
				.append(schedDays).append(schedStime).append(schedEtime).append(notifyActive).append(notifyExpired)
				.append(receiver).append(method).append(buildIn).append(seq).append(state).append(type).append(inr)
				.append(extLabels).append(timeout).append(Tool.JSONUtil.toJsonStr(condition)).toString();
		return Tool.DigestUtil.sha1Hex(content);
	}

	/**
	 * 获取 完整表达式 内容
	 * 1、当只有配置了一个告警阈值，返回  [expr] [operator] [value] 
	 * 2、当配置多个告警阈值，返回 [expr]
	 * @return
	 */
	public String getExpression() {
		List<ConditionItem> items = this.getConditionItems();
		if (items.size() == 1) {
			ConditionItem item = items.get(0);
			return String.format("%s %s %s", this.getExpr(), item.getOperator(), item.getValue());
		}
		return this.getExpr();
	}

	public int getExpiredTimeout() {
		Integer autoExpired = this.getAutoExpired();// 是否开启自动失效
		autoExpired = (autoExpired == null) ? 0 : autoExpired;
		Integer timeout = this.getTimeout();// 自动失效超时时间
		timeout = (timeout == null) ? 0 : timeout * autoExpired;
		return timeout;
	}

	public List<Map<String,String>> getExtLabelList(){
		if(Tool.StrUtil.isBlank(this.extLabels)) {
			return Tool.ListUtil.empty();
		}
		TypeReference<List<Map<String, String>>> tr = new TypeReference<List<Map<String,String>>>() {};
		return Tool.JSONUtil.toBean(this.extLabels, tr.getType(), true);
	}
	
	/**
	 * 获取告警阈值条件
	 * @return
	 */
	public List<ConditionItem> getConditionItems() {
		//1、string to bean
		TypeReference<List<ConditionItem>> tr = new TypeReference<List<ConditionItem>>() {};
		this.condition = Tool.StrUtil.emptyToDefault(this.condition, "[]");
		List<ConditionItem> items = Tool.JSONUtil.toBean(this.condition, tr.getType(), true);
		//2、根据 告警优先级 排序
		items.sort( (i1,i2) -> {
			return i1.getWeight() - i2.getWeight();
		});
		return items;
	}

	@Data
	public static class ConditionItem {
		private Integer id;
		private Integer weight;
		private String operator;
		private String value;

		public boolean validate(String val) {
			boolean result = false;
			switch (operator) {
				case "==":
					result = (Tool.NumberUtil.parseDouble(val) == Tool.NumberUtil.parseDouble(value));
					break;
				case "!=":
					result = (Tool.NumberUtil.parseDouble(val) != Tool.NumberUtil.parseDouble(value));
					break;
				case ">":
					result = (Tool.NumberUtil.parseDouble(val) > Tool.NumberUtil.parseDouble(value));
					break;
				case "<":
					result = (Tool.NumberUtil.parseDouble(val) < Tool.NumberUtil.parseDouble(value));
					break;
				case ">=":
					result = (Tool.NumberUtil.parseDouble(val) >= Tool.NumberUtil.parseDouble(value));
					break;
				case "<=":
					result = (Tool.NumberUtil.parseDouble(val) <= Tool.NumberUtil.parseDouble(value));
					break;
				case "~=":
					result = Pattern.matches(operator, val.toString());
					break;
				default:
					break;
			}
			return result;
		}
	}

}
