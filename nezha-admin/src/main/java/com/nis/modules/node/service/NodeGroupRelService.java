package com.nis.modules.node.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.node.entity.NodeGroupRelEntity;

import java.util.List;
import java.util.Map;

/**
 * 节点节点组关系表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
public interface NodeGroupRelService extends IService<NodeGroupRelEntity> {

    List<NodeGroupRelEntity> queryBySystemId(Long id);

    Integer queryCount(Long nodeGroupId, String type);
}

