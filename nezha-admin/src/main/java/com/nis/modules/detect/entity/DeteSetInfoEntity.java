package com.nis.modules.detect.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 监测设置信息表
 * 
 * @author nezha
 * @email 
 * @date 2019-07-25 16:35:48
 */
@Data
@TableName("dete_set_info")
@KeySequence(value="seq_dete_set_info")
public class DeteSetInfoEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(type=IdType.INPUT)
	private Long id;
	/**
	 * 设置名称，唯一索引
	 */
	private String name;
	/**
	 * 类别id
	 */
	private Long typeId;
	/**
	 * 1:内置，2：用户自定义，默认2
	 */
	private String buildIn;
	/**
	 * 类别定义的参数，多个逗号分隔，参数内逗号需要转义
	 */
	private String params;
	/**
	 * 1:用户组，2：系统内全部人员，3：无限制，默认2
	 */
	private String viewLevel;
	/**
	 * 状态，1：删除，2：停止，3：监控中
	 */
	private String state;
	/**
	 * 备注
	 */
	private String remark;
	/**
	 *  name i18n code值
	 */
	private String i18nCode;
	/**
	 * 操作人
	 */
	private Long operator;
	/**
	 * 操作时间
	 */
	private Date opTime;


	/**
	 * 权限记录
	 */
	@TableField(exist=false)
	private PermissionInfoEntity permission;
	/**
	 * 节点组id
	 * */
	@TableField(exist=false)
	private Long[] nodeGroupIds;
	/**
	 * 节点seqId
	 * */
	@TableField(exist=false)
	private Long[] nodeIds;

	@TableField(exist=false)
	private DeteTypeInfoEntity type;
	@TableField(exist=false)
	private List<NodeGroupInfoEntity> nodeGroups;
	@TableField(exist=false)
	private List<NodeInfoEntity> nodes;

}
