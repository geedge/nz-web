package com.nis.modules.panel.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;

@Data
@TableName("panel")
public class Panel {

    @TableId
    private Integer id;
    /**
     * 图表名
     * */
    private String name;
    /**
     *创建人id
     * */
    private Integer createBy;


    @TableField(exist = false)
    private SysUserEntity creator;

}
