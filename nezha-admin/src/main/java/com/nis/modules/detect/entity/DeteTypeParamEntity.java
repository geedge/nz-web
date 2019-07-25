package com.nis.modules.detect.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("dete_type_param")
@KeySequence("seq_dete_type_param")
public class DeteTypeParamEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type= IdType.INPUT)
    private Long id;

    private Long typeId;

    private String name;
    //参数索引
    private Integer paramIndex;
    //参数默认值
    private String defVal;

    //是否删除
    @TableLogic
    private String delFlag; //是否删除，1：删除，0：未删除，默认：0
    //操作人
    private Long operator; //操作人
    //操作时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date opTime;
}
