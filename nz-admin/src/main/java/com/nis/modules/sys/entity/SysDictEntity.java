/**

 *

 *

 */

package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

/**
 * 数据字典
 *

 */
@Data
@TableName("sys_dict")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysDictEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@TableId
	private Long id;
	/**
	 * 字典名称
	 */
	@NotBlank(message="字典名称不能为空")
	private String name;
	/**
	 * 字典类型
	 */
	@NotBlank(message="字典类型不能为空")
	private String type;
	/**
	 * 字典码
	 */
	@NotBlank(message="字典码不能为空")
	private String code;
	/**
	 * 字典值
	 */
	@NotBlank(message="字典值不能为空")
	private String value;
	/**
	 * 排序
	 */
	private Integer orderNum;
	/**
	 * 备注
	 */
	private String remark;
	/**
	 * 删除标记  -1：已删除  0：正常
	 */
	@TableLogic
	private Integer delFlag;
	
	/**
	 * 操作人
	 */
	private Long operator;
	
	/**
	 * 操作时间
	 */
	private Date opTime;
	
	/**
	 * 国际化code
	 */
	private String i18nCode;

	@TableField(exist = false)
	private SysUserEntity operatorUser;

}
