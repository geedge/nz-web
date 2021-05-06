package com.nis.modules.endpoint.entity;

import lombok.Data;
import lombok.ToString;

import java.util.Objects;

@Data
@ToString
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetadataMetric)) return false;
        MetadataMetric that = (MetadataMetric) o;
        return Objects.equals(getMetric(), that.getMetric()) &&
                Objects.equals(getType(), that.getType()) &&
                Objects.equals(getHelp(), that.getHelp()) &&
                Objects.equals(getUnit(), that.getUnit());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetric(), getType(), getHelp(), getUnit());
    }
}
