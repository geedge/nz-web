package com.nis.modules.node.entity;


import com.baomidou.mybatisplus.annotation.*;
import jdk.nashorn.internal.objects.annotations.Constructor;
import lombok.Data;

import java.util.Date;
import java.util.Objects;

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

    public SystemNodeRelEntity() {
    }

    public SystemNodeRelEntity(Long systemId, Long uuid, Long operator, Date opTime) {
        this.systemId = systemId;
        this.uuid = uuid;
        this.operator = operator;
        this.opTime = opTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SystemNodeRelEntity)) {
            return false;
        }
        SystemNodeRelEntity that = (SystemNodeRelEntity) o;
        return this.getSystemId().equals(that.getSystemId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getSystemId(), this.getUuid());
    }
}
