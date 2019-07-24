package com.nis.modules.node.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.SystemNodeGroupEntity;

import java.util.List;
import java.util.Map;

/**
 * 节点组信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
public interface NodeGroupInfoService extends IService<NodeGroupInfoEntity> {

    List<SystemNodeGroupEntity> getSysNodeGroups();

    NodeGroupInfoEntity detail(Integer id);

    void saveNodeGroupInfo(NodeGroupInfoEntity nodegroupInfo);

    boolean groupNameCheck(Long id, String name, Long pid);

    void updateNodeGroupInfo(NodeGroupInfoEntity nodegroupInfo);

    void delete(Long id);

    void deleteNodeRel(Long id, Long[] uuids);

    void saveNodeRel(Long id, Long[] uuids);

    PageUtils assList(Map<String, Object> params);
}

