package com.nis.modules.detectType.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
@Data
@TableName("dete_type_meta")
@KeySequence("seq_dete_type_meta")
public class DeteTypeMetaEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type= IdType.INPUT)
    private Long id;
    //显示名称
    private String name; //显示名称，唯一索引
    //类别id
    private Long typeId;
    //字段分类
    private String fieldType; //字段分类，1:Tag 数据库添加索引 ,2:field 普通字段, 3:temp 临时字段，数据传输不入库, 4:cal 计算字段，根据field，temp计算得出
    //数据类型
    private String dataType; //数据类型，1:counter，需要根据上一个监测周期的数据差值计算2：gauge瞬时值 3：string 字符类型
    //数据长度
    private Integer dataLenth;
    @TableField(exist = false)
    private Integer decimalPointLimit=2;
    //排序
    private Integer sort;
    //snmp oid
    private String oid;
    //单位
    private String unit;
    //字典类型
    private Long dicType; //与DETE_TYPE_META_MAPPING表的type关联
    //数据库字段名称
    private String fieldName; //数据库字段名称，不需要用户填写，使用name值去除特殊字符
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

}
