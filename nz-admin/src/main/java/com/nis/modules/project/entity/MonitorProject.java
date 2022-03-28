package com.nis.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.module.entity.MonitorModule;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("monitor_project")
public class MonitorProject implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 应用名称
     */
    private String name;

    /**
     * 描述
     */
    private String remark;

    /**
     * 是否内置 1:内置，0：非内置，默认：0 内置数据不允许删除
     */
    private Integer buildIn;

    private String seq;

    @TableField(exist = false)
    private List<Integer> alertStat = new ArrayList<Integer>();

    /**
     * module 列表
     */
    @TableField(exist = false)
    private List<MonitorModule> children;

    /**
     * module数量
     */
    @TableField(exist = false)
    private Integer moduleNum;

    /**
     * endpoint数量
     */
    @TableField(exist = false)
    private Integer endpointNum;

    /**
     * asset数量
     */
    @TableField(exist = false)
    private Integer assetNum;

    /**
     * alert数量
     */
    @TableField(exist = false)
    private Integer alertNum;

    @TableField(exist = false)
    private List alert;
}
