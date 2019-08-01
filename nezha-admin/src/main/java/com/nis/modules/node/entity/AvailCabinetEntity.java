package com.nis.modules.node.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 *  两处 使用该实体  作为返回对象
 * 1. 节点查询可用机柜的返回对象
 * 2. 机柜查询机房可选行列值
 *
 * @author shizhendong
 */
@Data
public class AvailCabinetEntity {

    // 机房ID     公共属性
    private Long roomId;

    //机房名称    公共属性
    private String roomName;

    //节点查询 其中包含起始位置starts 可用起始位置时 使用属性
    private List<NodeCabinetEntity> cabinets;

    //机柜查询 机房可用行列时  使用属性
    private List<Map<String, Object>> roomRows;

}
