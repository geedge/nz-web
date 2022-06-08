package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_user_starred")
public class SysUserStarredEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Integer id;

    private Integer uid;

    /**
     * 支持类型:datacenter,project,module,endpoint,alertRule,asset,panel,chart,cabinet,ipamSubnet
     */
    private String type;

    private Integer tid;

    private Long cts;

    /**
     * 收藏名称 例如: type = project , name =project.name
     */
    @TableField(exist = false)
    private String name;

}
