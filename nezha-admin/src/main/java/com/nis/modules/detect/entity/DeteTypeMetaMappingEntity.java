package com.nis.modules.detect.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("dete_type_meta_mapping")
@KeySequence("seq_dete_type_meta_mapping")
public class DeteTypeMetaMappingEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type= IdType.INPUT)
    private Long id;
    //映射类型
    private String type;
    //原值
    private String val;
    //映射后值
    private String mapping;
    //是否删除
    @TableLogic
    private String delFlag; //是否删除，1：删除，0：未删除，默认：0
    //操作人
    private Long operator; //操作人
    //操作时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date opTime;

}
