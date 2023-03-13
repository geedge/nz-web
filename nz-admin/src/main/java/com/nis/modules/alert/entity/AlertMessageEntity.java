package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 
 * 
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
@Data
@TableName("alert_message_active")
public class AlertMessageEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * 表
	 */
	@TableField(exist = false)
	public String tableName;

	/**
	 * 主键
	 */
	@TableId
	private Long id;
	/**
	 * 主键们  eg:[1,2,3]
	 */
	@TableField(exist = false)
	private String ids;
	/**
	 * 告警规则id
	 */
	private Integer ruleId;
	/**
	 * 告警主机对象
	 */
	@TableField(exist = false)
	private AssetAsset asset;
	/**
	 * 告警级别
	 */
	private Integer severityId;
	/**
	 * 告警摘要
	 */
	private String summary;
	/**
	 * 告警描述
	 */
	private String description;
	/**
	 * 告警labels信息
	 */
	private String labels;
	/**
	 * 开始时间
	 */
	private Date startAt;
	/**
	 * 结束时间
	 */
	private Date endAt;
	/**
	 * 最后一次接收到 告警时间
	 */
	private Date lastAt;
	/**
	 * 状态
	 */
	private Integer state;
	/**
	 * 关联project id
	 */
	private Integer projectId;
	/**
	 * 关联module Id
	 */
	private Integer moduleId;
	/**
	 * 关联endpoint_id
	 */
	private Integer endpointId;
	/**
	 * 关联asset id
	 */
	private Integer assetId;
	/**
	 * 关联dc
	 */
	private  Integer dcId;

	/**
	 * redis hash key
	 * */
	private String hashKey;

	/**
	 * remark 修改时使用此参数 记录手动关闭的备注信息
	 */
	private String remark;

	@TableField(exist = false)
	private AlertRuleEntity alertRule;

	/**
	 * 对应的project
	 */
	@TableField(exist = false)
	private MonitorProject project;

	/**
	 * module
	 */
	@TableField(exist = false)
	private MonitorModule module;

	/**
	 * endpoint
	 */
	@TableField(exist = false)
	private MonitorEndpoint endpoint;

	/**
	 * dc
	 */
	@TableField(exist = false)
	private Dc dc;

	@TableField(exist = false)
	private AlertSeverityConf severity;
	
	@TableField(exist = false)
	private List<AlertSilenceConf> silenceList;

	/**
	 * 确认标识
	 */
	private  Integer ack;

}
