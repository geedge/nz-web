package com.nis.modules.node.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.node.entity.NodegroupInfoEntity;
import com.nis.modules.node.entity.SystemNodeGroupEntity;

import java.util.List;

/**
 * 节点组信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
public interface NodegroupInfoService extends IService<NodegroupInfoEntity> {

    List<SystemNodeGroupEntity> getSysNodeGroups();

    NodegroupInfoEntity detail(Integer id);

    void saveNodeGroupInfo(NodegroupInfoEntity nodegroupInfo);

    boolean groupNameCheck(Long id, String name, Long pid);

    void updateNodeGroupInfo(NodegroupInfoEntity nodegroupInfo);
}

