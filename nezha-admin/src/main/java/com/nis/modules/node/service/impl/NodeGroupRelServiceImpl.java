package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.node.dao.NodeGroupRelDao;
import com.nis.modules.node.entity.NodeGroupRelEntity;
import com.nis.modules.node.service.NodeGroupRelService;
import org.springframework.stereotype.Service;

/**
 * 节点 节点组关系表
 * @author shizhendong
 */
@Service("nodeGroupRelService")
public class NodeGroupRelServiceImpl extends ServiceImpl<NodeGroupRelDao, NodeGroupRelEntity> implements NodeGroupRelService {


}