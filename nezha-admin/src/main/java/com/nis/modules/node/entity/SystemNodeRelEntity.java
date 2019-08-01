package com.nis.modules.node.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 业务系统和节点关联关系实体
 * @author shizhendong
 */
@Data
@TableName("system_node_rel")
@KeySequence(value = "seq_system_node_rel")
public class SystemNodeRelEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 业务系统ID
     */
    private Long systemId;

    /**
     * 全局唯一
     */
    private Long uuid;

    /**
     * 操作人
     */
    private Long operator;


    /**
     * 删除标识  0：正常   1：删除
     */
    @TableLogic
    private String delFlag;

    /**
     * 操作时间
     */
    private Date opTime;


}
