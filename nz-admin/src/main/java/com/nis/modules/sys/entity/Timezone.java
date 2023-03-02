package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_timezone")
public class Timezone {

    @TableId
    private Integer id;

    private String name;

    @TableField(value = "`offset`")
    private Integer offset;

}
