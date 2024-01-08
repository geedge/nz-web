package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_lock")
public class SysLock {

    @TableId
    private String lockKey;

    private String lockType;

    private String lockValue;

    private Long version;

}
