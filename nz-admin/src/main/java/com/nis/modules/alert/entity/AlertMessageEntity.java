package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.module.entity.Module;
import com.nis.modules.project.entity.Project;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * 
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
@Data
@TableName("alert_message")
public class AlertMessageEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId
	private Integer id;
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
	private Asset asset;
	/**
	 * 告警级别
	 */
	private String severity;
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
	 * 关联idc
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

	/**
	 * linkId对应实体类
	 * */
	@TableField(exist = false)
	private Object linkObject;

	@TableField(exist = false)
	private AlertRuleEntity alertRule;

	/**
	 * 对应的project
	 */
	@TableField(exist = false)
	private Project project;

	/**
	 * module
	 */
	@TableField(exist = false)
	private Module module;

	/**
	 * endpoint
	 */
	@TableField(exist = false)
	private Endpoint endpoint;

	/**
	 * idc
	 */
	@TableField(exist = false)
	private Idc dc;

	/**
	 *  告警规则表达式
	 */
	@TableField(exist = false)
	private String expr;
}
