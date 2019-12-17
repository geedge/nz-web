package com.nis.modules.endpoint.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("metadata_metric")
public class MetadataMetric {
    /**
     * 主键
     * */
    private Integer id;
    /**
     * 指标名称
     * */
    private String metric;
    /**
     * 指标类型
     * */
    private String type;
    /**
     * exporter参数
     * */
    private String help;
    /**
     * 单位
     **/
    private String unit;
}
