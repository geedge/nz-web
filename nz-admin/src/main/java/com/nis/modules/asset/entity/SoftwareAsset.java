package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * software asset entity
 *
 * @author shizhendong
 * @date 2023-08-10
 */
@Data
@TableName("software_asset")
public class SoftwareAsset implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 硬件资产id
     */
    private Integer assetId;

    /**
     * 业务系统ID
     */
    private Integer projectId;

    /**
     * 软件类型ID
     */
    private Integer typeId;

    /**
     * 配置参数
     */
    private Object params;

    /**
     * 描述
     */
    private String remark;

    /**
     * 是否内置
     * 1:内置，0：非内置，默认：0
     * 内置数据不允许删除
     */
    private Integer buildIn;

    /**
     * 添加时间戳
     */
    private Long cts;

    /**
     * 更新时间戳
     */
    private Long uts;


    @TableField(exist = false)
    private Map<Object,Object> asset;

    @TableField(exist = false)
    private Map<Object,Object> project;

    @TableField(exist = false)
    private Map<Object,Object> type;

    public void setName(String name) {
        this.name = Tool.StrUtil.trim(name);
    }


}
