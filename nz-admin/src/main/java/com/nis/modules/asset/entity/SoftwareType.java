package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import lombok.Data;

import java.io.Serializable;

/**
 * software type entity
 *
 * @author shizhendong
 * @date 2023-08-10
 */
@Data
@TableName("software_type")
public class SoftwareType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 类别
     */
    private String category;

    /**
     * 图标	base64 image
     */
    private String icon;

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

    public void setName(String name) {
        this.name = Tool.StrUtil.trim(name);
    }

}
