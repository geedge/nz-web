package com.nis.modules.endpoint.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("metadata_label")
public class MetadataLabel {
    /**
     * 主键
     * */
    private Integer id;
    /**
     * 指标id
     * */
    private Integer metricId;
    /**
     * label名称
     * */
    private String name;
    /**
     * label值
     * */
    private String value;
}
