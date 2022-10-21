package com.nis.modules.vsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("vsys_info")
public class VsysInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * task id
     */
    private Integer taskId;

    /**
     * vsys id
     */
    private Integer vsysId;

    /**
     * 设备组
     */
    private Object deviceTag;

    /**
     * 参数
     */
    private String params;

    /**
     * 是否有效 1：是 0：否
     * 默认：1
     */
    private Integer state;

    /**
     * 是否内置	 1：内置  0：非内置
     * 默认：0
     */
    private Integer buildIn;

    /**
     * 时间戳
     */
    private Long ts;


}
