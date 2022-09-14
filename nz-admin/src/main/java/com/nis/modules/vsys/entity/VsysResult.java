package com.nis.modules.vsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("vsys_result")
public class VsysResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 任务id
     */
    private Integer taskId;

    /**
     * 资产id
     */
    private Integer assetId;

    /**
     * vsys id
     */
    private Integer vsysId;

    /**
     * 任务类型
     * 1：add
     * 2：update
     * 3：delete
     */
    private Integer type;

    /**
     * 状态
     * 0：失败
     * 1：成功
     */
    private Integer state;

    /**
     * 执行信息
     */
    private String msg;

    /**
     * 执行第几次	默认：1
     */
    private Integer num;

    /**
     * 时间戳
     */
    private Long ts;

}
