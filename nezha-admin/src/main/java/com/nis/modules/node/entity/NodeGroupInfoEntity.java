package com.nis.modules.node.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 节点组信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@Data
@TableName("nodegroup_info")
@KeySequence(value="seq_nodegroup_info")
public class NodeGroupInfoEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID  主键
     */
    @TableId(type= IdType.INPUT)
    private Long id;

    /**
     * 节点组名称
     */
    private String name;
    /**
     * 业务系统ID
     */
    private Long systemId;
    /**
     * 父级ID 默认为0
     */
    private Long pid;
    /**
     * 所有上级ID
     */
    private String pids;
    /**
     * 备注
     */
    private String remark;
    /**
     * 是否删除，1：删除，0：未删除，默认：0
     */
    private String delFlag;
    /**
     * 操作人id
     */
    private Long operator;
    /**
     * 操作时间
     */
    private Date opTime;


    //能否分配节点 0不能，1可以     必须是叶子节点才能分配节点
    @TableField(exist = false)
    private String allowAss;

    //能否增加子节点组 0不能，1可以  只要跟自己没有直接关联的节点就可以
    @TableField(exist = false)
    private String allowSub;

    //父级名称
    @TableField(exist = false)
    private String pname;
}
