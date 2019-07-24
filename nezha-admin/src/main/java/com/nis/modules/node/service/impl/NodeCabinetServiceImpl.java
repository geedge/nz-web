package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.node.dao.NodeCabinetDao;
import com.nis.modules.node.entity.NodeCabinetEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.node.entity.NodeRoomEntity;
import com.nis.modules.node.service.NodeCabinetService;
import com.nis.modules.node.service.NodeInfoService;
import com.nis.modules.node.service.NodeRoomService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.StyledEditorKit;
import java.util.List;
import java.util.Map;


@Service("nodeCabinetService")
public class NodeCabinetServiceImpl extends ServiceImpl<NodeCabinetDao, NodeCabinetEntity> implements NodeCabinetService {

    @Autowired
    private NodeRoomService nodeRoomService;

    @Autowired
    private NodeInfoService nodeInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        String name = (String) params.get("name");          // cab name
        //String roomId = (String) params.get("roomId");          // room id

        Long roomId = null;                                 // room id
        if (params.get("roomId") != null) {
            roomId = Long.valueOf(params.get("roomId").toString());
        }

        IPage<NodeCabinetEntity> page = this.page(
                new Query<NodeCabinetEntity>().getPage(params),
                new QueryWrapper<NodeCabinetEntity>()
                        .like(StringUtils.isNotBlank(name), "name", name)
                        .eq((roomId != null), "room_id", roomId)
        );

        for (NodeCabinetEntity entity : page.getRecords()) {
            NodeRoomEntity noderoom = nodeRoomService.getById(entity.getRoomId());
            entity.setRoomName(noderoom.getName());
        }

        return new PageUtils(page);
    }

    @Override
    public NodeCabinetEntity detail(Long id) {
        // 1. 查询自身
        NodeCabinetEntity nodeCabinet = this.getById(id);
        // 2. 查询关联的机房名称
        NodeRoomEntity nodeRoom = nodeRoomService.getById(nodeCabinet.getRoomId());
        nodeCabinet.setRoomName(nodeRoom.getName());
        // 3. 查询关联的节点信息
        List<NodeInfoEntity> nodeInfos = nodeInfoService.list(new QueryWrapper<NodeInfoEntity>().eq("cabinet_id", id));
        nodeCabinet.setNodes(nodeInfos);
        return nodeCabinet;
    }

    /**
     * 机柜名称校验，同机房下名称不能重复
     *
     * @param id     O
     * @param name   R
     * @param roomId R
     * @return
     */
    @Override
    public boolean cabNameCheck(Long id, String name, Long roomId) {
        if (id == null) { // 添加
            return getCount(name, roomId);
        } else { // 修改
            NodeCabinetEntity nodeCabinet = this.getById(id);
            if (name.equals(nodeCabinet.getName())) {
                return true;
            } else {
                return getCount(name, roomId);
            }
        }
    }


    public boolean getCount(String name, Long roomId) {
        int count = this.count(new QueryWrapper<NodeCabinetEntity>()
                .eq("room_id", roomId)
                .eq("name", name));
        if (count > 0) {
            return false;
        }
        return true;
    }
}