package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 维修记录实体
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Data
@TableName("maintenance_record")
public class MaintenanceRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 资产id
     */
    private Integer assetId;

    /**
     * 维修内容
     */
    private String content;

    /**
     * 添加日期
     */
    private Date createAt;

    /**
     * 添加人 user表关联
     */
    private Long createBy;

    /**
     * 更新日期
     */
    private Date updateAt;

    /**
     * 更新人
     */
    private Long updateBy;
}