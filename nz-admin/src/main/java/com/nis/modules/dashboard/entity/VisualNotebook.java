package com.nis.modules.dashboard.entity;

import com.alibaba.fastjson.annotation.JSONType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * visual notebook entity
 *
 * @author shizhendong
 * @date 2023-08-16
 */
@Data
@TableName("visual_notebook")
@JSONType(orders = {"name", "type", "remark", "param", "charts"})
public class VisualNotebook implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 笔记类型
     * 可选值：none（默认）, runbook, postmortem, documentation, report, investigation
     */
    private String type;

    /**
     * notebook 参数
     * JSONObject,default {}
     */
    private Object param;

    /**
     * 配置参数
     * JSONArray,default []
     */
    private Object charts;

    /**
     * 是否内置
     * 1:内置，0：非内置，默认：0
     * 内置数据不允许删除
     */
    private Integer buildIn;

    /**
     * 描述
     */
    private String remark;

    /**
     * 导入唯一标识
     */
    private String seq;

    /**
     * 添加时间戳
     */
    private Long cts;

    /**
     * 更新时间戳
     */
    private Long uts;

    /**
     * 创建人ID
     */
    private Integer cid;

    /**
     * 更新人ID
     */
    private Integer uid;

    @TableField(exist = false)
    private Map<Object,Object> createUser;

    @TableField(exist = false)
    private Map<Object,Object> updateUser;

    public void setName(String name) {
        this.name = Tool.StrUtil.trim(name);
    }

}
