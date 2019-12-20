package com.nis.modules.endpoint.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.util.Objects;

@Data
@TableName("metadata_label")
@ToString
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

    /**
     * 无参构造
     */
    public MetadataLabel() {
    }

    /**
     * 有参构造
     * @param metricId
     * @param name
     * @param value
     */
    public MetadataLabel(Integer metricId, String name, String value) {
        this.metricId = metricId;
        this.name = name;
        this.value = value;
    }

    public MetadataLabel(Integer id, Integer metricId, String name, String value) {
        this.id = id;
        this.metricId = metricId;
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetadataLabel)) return false;
        MetadataLabel that = (MetadataLabel) o;
        return Objects.equals(getMetricId(), that.getMetricId()) &&
                Objects.equals(getName(), that.getName()) &&
                Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetricId(), getName(), getValue());
    }
}
