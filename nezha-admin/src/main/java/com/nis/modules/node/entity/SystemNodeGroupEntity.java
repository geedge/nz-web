package com.nis.modules.node.entity;

import lombok.Data;

import java.util.List;

//业务系统 节点组 关联 返回对象实体
@Data
public class SystemNodeGroupEntity {
    //业务系统ID
    private Long systemId;

    //业务系统name
    private String systemName;

    private List<NodeGroupInfoEntity> groups;

    public SystemNodeGroupEntity() {
    }

    public SystemNodeGroupEntity(Long systemId, String systemName, List<NodeGroupInfoEntity> groups) {
        this.systemId = systemId;
        this.systemName = systemName;
        this.groups = groups;
    }
}
