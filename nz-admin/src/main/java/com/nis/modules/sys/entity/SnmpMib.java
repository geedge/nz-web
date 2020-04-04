package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.ModelWithMib;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * snmp mib
 */
@Data
@TableName("snmp_mib")
public class SnmpMib implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Integer id;
    /**
     * 名称
     */
    private String name;

    /**
     * 二进制文件名称
     */
    private String fileName;

    /**
     * 文件内容
     */
    private byte[] content;

    /**
     *   关联设备型号
     */
    private String models;
    /**
     * 关联设备的详细信息
     */
    @TableField(exist = false)
    private List<ModelWithMib> modelsDetail;
    /**
     * 备注信息
     */
    private String remark;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 更新日期
     */
    private Date updateAt;


    /**
     * Mib tree
     */
    private String tree;

    /**
     * 更新人
     */
    @TableField(exist = false)
    private SysUserEntity updateUser;
}
