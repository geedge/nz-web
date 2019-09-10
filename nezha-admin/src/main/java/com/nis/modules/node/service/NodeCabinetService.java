package com.nis.modules.node.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.node.entity.AvailCabinetEntity;
import com.nis.modules.node.entity.NodeCabinetEntity;
import org.apache.commons.logging.impl.AvalonLogger;

import java.util.List;
import java.util.Map;

/**
 * 机柜信息表
 *
 * @author shizhendong
 * @email ${email}
 * @date 2019-07-18 09:58:18
 */
public interface NodeCabinetService extends IService<NodeCabinetEntity> {

    PageUtils queryPage(Map<String, Object> params);

    NodeCabinetEntity detail(Long id);

    boolean cabNameCheck(Long id, String name, Long roomId);

    // 查询机柜可选的机房及行号列号
    List<AvailCabinetEntity> optionalRoom(Long id);

    void saveNodeCabinet(NodeCabinetEntity nodeCabinet);

    void updateNodeCabinet(NodeCabinetEntity nodeCabinet);

    void delete(Long[] ids);

    // 校验U位数量是否可用
    R cabUbitCheck(Long id, Integer ubit);
}

