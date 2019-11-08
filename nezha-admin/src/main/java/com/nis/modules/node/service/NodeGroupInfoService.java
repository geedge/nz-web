package com.nis.modules.node.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.sys.entity.SystemInfoEntity;

import java.util.List;
import java.util.Map;

/**
 * 节点组信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
public interface NodeGroupInfoService extends IService<NodeGroupInfoEntity> {

    List<SystemInfoEntity> getSysNodeGroups();

    NodeGroupInfoEntity detail(Long id);

    void saveNodeGroupInfo(NodeGroupInfoEntity nodegroupInfo);

    boolean groupNameCheck(Long id, String name, Long pid, Long systemId);

    void updateNodeGroupInfo(NodeGroupInfoEntity nodegroupInfo);

    void delete(Long id);

    void deleteNodeRel(Long id, Long[] uuids);

    void saveNodeRel(Long id, Long[] uuids);

    PageUtils assList(Map<String, Object> params);

    boolean isCurrentSystem(Long[] ids);

    // 根据当前节点组ID获取包含的节点个数
    Integer getNodeCount(Long id);

    // 节点组 分配 / 移除 节点
    void assignNode(Long id, Long[] addUuids, Long[] removeUuids);
}

