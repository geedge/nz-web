package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("alert_severity_conf")
public class AlertSeverityConf {

    @TableId
    private Integer id;

    private String name;

    private String color;

    private Integer weight;

    private String remark;

}
