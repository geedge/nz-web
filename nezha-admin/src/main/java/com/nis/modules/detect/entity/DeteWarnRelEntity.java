package com.nis.modules.detect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("dete_warn_rel")
@KeySequence("seq_dete_warn_rel")
public class DeteWarnRelEntity {
    @TableId(type= IdType.INPUT)
    private Long id;
    /**
     *告警id
     * */
    private Long warnId;

    /**
     *关联方式，1、type，2、set
     * */
    private String relType;
    /**
     *关联id
     * */
    private Long relId;

    @TableLogic
    private String delFlag;

    /**
     * 操作人
     */
    private Long operator;
    /**
     * 操作时间
     */
    private Date opTime;

    @TableField(exist = false)
    private String relIds;
}
