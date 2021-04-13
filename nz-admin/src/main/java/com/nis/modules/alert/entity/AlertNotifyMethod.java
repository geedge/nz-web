package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("alert_notify_method")
public class AlertNotifyMethod {
    @TableId
    private Integer id;

    private String name;

    private String filePath;

    private String account;

    private Integer state;

    private Integer buildIn;
}
