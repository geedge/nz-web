package com.nis.modules.detect.entity;

import lombok.Data;

import java.util.List;
@Data
public class DeteTypeMetaMapppingVO {

    private String type;
    private List<DeteTypeMetaMappingEntity> mappings;

    public void setMappings(List<DeteTypeMetaMappingEntity> mappings) {
        this.mappings = mappings;
    }
}
