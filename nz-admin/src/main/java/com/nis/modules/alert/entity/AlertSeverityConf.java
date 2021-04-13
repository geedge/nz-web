package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@TableName("alert_severity_conf")
public class AlertSeverityConf {

    @TableId
    private Integer id;

    private String name;

    private String color;

    private Integer weight;

    private String remark;

    private String methods;

    @TableField(exist = false)
    private List<AlertNotifyMethod> notifyMethods;

    public List<String> toMethodsIds(){
        if(Tool.StrUtil.isNotEmpty(this.getMethods())){
           return Arrays.asList(Tool.StrUtil.split(this.getMethods(),","));
        }
        return Collections.emptyList();
    }

}
