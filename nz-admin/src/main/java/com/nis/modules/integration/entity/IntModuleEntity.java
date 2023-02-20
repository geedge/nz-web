package com.nis.modules.integration.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName("int_module")
public class IntModuleEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId
    private Integer id;
    /**
     * 名称
     */
    private String name;

    /**
     * 备注
     */
    private String remark;

    /**
     * 图标
     */
    private String icon;

    /**
     * 配置
     */
    private String configuration;

    /**
     * 图表模板
     */
    private String dashboardTemplate;
    /**
     * 状态
     * 0：隐藏
     * 1：显示
     */
    private Integer status;
    /**
     * 是否内置
     */
    private Integer buildIn;
    /**
     * 用户id
     */
    private Integer uid;
    /**
     * 更新时间戳
     */
    private Long uts;


    /**
     * 集成模块元数据
     */
    @TableField(exist = false)
    private List<IntMetadataEntity> metadatas;

    /**
     * 集成模块告警规则
     */
    @TableField(exist = false)
    private List<IntAlertRuleEntity> alertRules;


}
