package com.nis.modules.node.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.node.entity.AvailCabinetEntity;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeGroupRelEntity;
import com.nis.modules.node.entity.NodeInfoEntity;

import java.util.List;
import java.util.Map;

/**
 * 节点信息表
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
public interface NodeInfoService extends IService<NodeInfoEntity> {
    PageUtils queryPage(Map<String, Object> params);

    // 节点详情查询
    NodeInfoEntity detail(Long uuid);

    // ip 是否重复校验
    R ipCheck(Long uuid, String ip);

    // 名称是否重复校验
    boolean nodeNameCheck(Long uuid, String name);

    // 节点状态变更 (上线/下线/删除)
    void changeState(Long[] uuids, String state);

    // 保存
    void saveNodeInfo(NodeInfoEntity nodeInfoEntity);

    // 修改
    void updateNodeInfo(NodeInfoEntity nodeInfoEntity);

    // 查询可用可用 机房 下 机柜 的 起始位置
    List<AvailCabinetEntity> selectCab(Long uuid, Integer uSize);
    
    // 查询业务系统的节点列表
    PageUtils queryPageBySystemId(Map<String, Object> params);

    public void getTagName(List<NodeInfoEntity> nodeInfoEntities);

	PageUtils querySelectableNodeBySystemId(Map<String, Object> params);

    /**
     * 根据节点UUID 获取所属节点组集合
     * @param uuid
     * @return
     */
    List<NodeGroupRelEntity> getGroupsById(Long uuid);
}

