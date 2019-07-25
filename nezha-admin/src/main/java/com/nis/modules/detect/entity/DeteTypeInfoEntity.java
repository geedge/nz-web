package com.nis.modules.detect.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@TableName("dete_type_info")
@KeySequence("seq_dete_type_info")
public class DeteTypeInfoEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type=IdType.INPUT)
    private Long id;
    //类别名称
    private String name;
    //查看权限
    private String viewLevel; //1:用户组，2：系统内全部人员，3：无限制，默认2
    //监控方式
    private String method; //1、脚本，2、snmp，3、内置，默认：1
    //监控间隔
    private Integer deteInterval; //监控间隔，单位：s，默认：60
    //内容
    private String content; //1、脚本储存路径，2、oid，可以为空，3、类全路径
    //是否内置
    private String buildIn; //1、隐藏界面不显示，2、内置，不允许修改，3、用户自定义，默认3
    //表名
    private String tableName; //di_name,用户不在填写
    //版本号
    private Integer version;
    //国际化code
    private String i18nCode; //name 的国际化 code，不为空时name显示翻译后的内容，为空直接显示
    //备注
    private String remark; //备注
    //是否删除
    @TableLogic
    private String delFlag; //是否删除，1：删除，0：未删除，默认：0
    //操作人
    private Long operator; //操作人
    //操作时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date opTime;
    //监测类别对应监测元数据
    @TableField(exist = false)
    private List<DeteTypeMetaEntity> deteTypeMetas;
    //监测类别对应参数信息
    @TableField(exist = false)
    private List<DeteTypeParamEntity> deteTypeParams;

}
