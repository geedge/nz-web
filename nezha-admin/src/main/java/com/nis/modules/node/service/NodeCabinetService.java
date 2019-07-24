package com.nis.modules.node.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.node.entity.NodeCabinetEntity;

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
}

