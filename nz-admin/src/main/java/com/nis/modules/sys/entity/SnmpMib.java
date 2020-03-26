package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

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
     * 更新人
     */
    @TableField(exist = false)
    private SysUserEntity updateUser;
}
