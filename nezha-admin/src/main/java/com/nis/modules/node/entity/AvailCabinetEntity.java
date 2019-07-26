package com.nis.modules.node.entity;

import lombok.Data;

import java.util.List;

/**
 * 节点查询可用机柜的返回对象
 *
 * @author shizhendong
 */
@Data
public class AvailCabinetEntity {

    // 机房ID
    private Long roomId;

    //机房名称
    private String roomName;

    //机柜集合  其中包含可用位置的起始位置 数组 starts
    private List<NodeCabinetEntity> cabinets;

}
