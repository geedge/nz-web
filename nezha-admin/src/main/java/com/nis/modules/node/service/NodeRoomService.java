package com.nis.modules.node.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.node.entity.NodeRoomEntity;

import java.util.Map;

/**
 * 机房信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
public interface NodeRoomService extends IService<NodeRoomEntity> {

    PageUtils queryPage(Map<String, Object> params);

    NodeRoomEntity detail(Long id);

    void saveNodeRoom(NodeRoomEntity nodeRoom);

    boolean roomNameCheck(Long id, String name);

    void delete(Long[] ids);

    void updateNodeRoom(NodeRoomEntity nodeRoom);

    R roomSizeCheck(Long id, Integer roomRows, Integer roomCols);
}

