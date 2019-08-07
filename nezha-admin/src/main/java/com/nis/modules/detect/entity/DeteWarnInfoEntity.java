package com.nis.modules.detect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("dete_warn_info")
@KeySequence("seq_dete_warn_info")
public class DeteWarnInfoEntity {
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 告警名称
     * 唯一字段
     * */
    private String name;
    /**
     * 告警字段
     * */
    private Long metaId;

    /**
     * 触发条件
     * 1:阈值触发,
     * 2：变化触发，当本次监测数值与上次不一致时触发，用于配置文件是否修改等,
     * 3：主动，默认：1
     * */
    private String triggerType;
    /**
     *告警模式，1、单台，2、汇总
     * */
    private String mode;
    /**
     * 统计方式
     *1:avg2:count3:sum4:min5:max，默认：1
     * */
    private String statType;
    /**
     *符号
     * 1:>，2:<，3:=，4:equals，5:include，6:exclude，默认：1
     * */
    private String symbol;

    /**
     *阈值
     * */
    private String threshold;
    /**
     *告警提示信息
     * */
    private String tips;
    /**
     *告警级别，1:normal，2:info，3:warning，4:error，默认：3
     * */
    private String level;
    /**
     *回调方式，0:无，1：shell，2：http，默认：0
     * */
    private String callbackType;
    /**
     *回调内容
     */
    private String callbackContent;

    /**
     * 查看权限 1:用户组，2：系统内全部人员，3：无限制，默认2
     * */
    private String viewLevel;

    /**
     *备注
     * */
    private String remark;
    /**
     * 是否删除，1：删除，0：未删除，默认：0
     * */
    @TableLogic
    private String delFlag;

    private Long operator;

    private Date opTime;

    @TableField(exist = false)
    private DeteTypeInfoEntity deteType;

    @TableField(exist = false)
    private List<DeteSetInfoEntity> deteSet;
    @TableField(exist = false)
    private DeteTypeMetaEntity meta;

    @TableField(exist = false)
    private Long deteTypeId;

    @TableField(exist = false)
    private Long[] deteSetIds;

    @TableField(exist = false)
    private String relType;

    @TableField(exist = false)
    private String relIds;


}
