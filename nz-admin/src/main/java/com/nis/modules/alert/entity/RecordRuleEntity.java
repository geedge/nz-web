package com.nis.modules.alert.entity;

import java.io.Serializable;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.SysUserEntity;

import lombok.Data;

@Data
@TableName("record_rule")
public class RecordRuleEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键id
	 */
	@TableId
	private Integer id;
	/**
	 * 规则名称
	 */
	private String name;

	/**
	 * 运算表达式
	 */
	private String expr;

	/**
	 * 类型
	 */
	private Integer type;

	/**
	 * 执行周期
	 */
	private Integer inr;

	/**
	 * 标签
	 */
	private String labels;
	/**
	 * 规则状态
	 */
	private Integer state;
	/**
	 * 是否内置
	 */
	private Integer buildIn;
	/**
	 * 导入唯一标识
	 */
	private String seq;

	/**
	 * 创建人
	 */
	private Integer cid;

	/**
	 * 创建时间
	 */
	private Long cts;

	/**
	 * 更新人
	 */
	private Integer uid;

	/**
	 * 更新时间
	 */
	private Long uts;
	/**
	 * 描述
	 */
	private String remark;

	/**
	 * 收藏标识： 1：收藏 0：未收藏
	 */
	@TableField(exist = false)
	private Integer starred;

	/**
	 * 创建人对象
	 */
	@TableField(exist = false)
	private SysUserEntity createUser;

	/**
	 * 更新人对象
	 */
	@TableField(exist = false)
	private SysUserEntity updateUser;

	/**
	 * 获取 record rule hash 值
	 * 
	 * @return
	 */
	public String getRecordRuleHashId() {
		String content = new StringBuilder().append(this.id).append(this.name).append(this.type).append(this.expr)
				.append(this.labels).append(this.inr).append(this.state).toString();
		return Tool.DigestUtil.sha1Hex(content);
	}
	
}
