package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.node.dao.NodeCabinetDao;
import com.nis.modules.node.dao.NodeRoomDao;
import com.nis.modules.node.entity.NodeCabinetEntity;
import com.nis.modules.node.entity.NodeRoomEntity;
import com.nis.modules.node.service.NodeCabinetService;
import com.nis.modules.node.service.NodeRoomService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;


@Service("nodeRoomService")
public class NodeRoomServiceImpl extends ServiceImpl<NodeRoomDao, NodeRoomEntity> implements NodeRoomService {

    // 机柜 dao
    @Autowired
    private NodeCabinetDao nodeCabinetDao;

    // 机房 dao
    @Autowired
    private NodeRoomDao nodeRoomDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        String name = (String) params.get("name");    // room  name
        String area = (String) params.get("area");    // room area

        IPage<NodeRoomEntity> page = this.page(
                new Query<NodeRoomEntity>().getPage(params),
                new QueryWrapper<NodeRoomEntity>()
                        .like(StringUtils.isNotBlank(name), "name", name)
                        .eq(StringUtils.isNotBlank(area), "area", area)
        );
        return new PageUtils(page);
    }

    @Override
    public NodeRoomEntity detail(Long id) {
        NodeRoomEntity nodeRoom = this.getById(id);
        return nodeRoom;
    }


    @Override
    public void saveNodeRoom(NodeRoomEntity nodeRoom) {
        if (roomNameCheck(null, nodeRoom.getName())) { // 名称唯一校验
            nodeRoom.setOperator(0l);
            nodeRoom.setOpTime(new Date());
            this.save(nodeRoom);
        } else {
            throw new NZException(RCode.NODEROOM_NAME_DUPLICATE);// 名称重复异常
        }
    }

    /**
     * 修改时前后端都需要校验行列是否已被机柜占用
     *
     * @param nodeRoom
     */
    @Override
    public void updateNodeRoom(NodeRoomEntity nodeRoom) {
        if (roomNameCheck(nodeRoom.getId(), nodeRoom.getName())) { // 名称唯一校验
            nodeRoom.setOpTime(new Date());

            // 修改前校验机房的大小是否可用
            roomSizeCheck(nodeRoom.getId(), nodeRoom.getRoomRows(), nodeRoom.getRoomCols());

            this.updateById(nodeRoom);
        } else {
            throw new NZException(RCode.NODEROOM_NAME_DUPLICATE);// 名称重复异常
        }
    }

    /**
     * 机房删除   删除时需要校验是否存在关联的机柜
     *
     * @param id R
     */
    @Override
    public void delete(Long id) {
        // 1. 校验是否存在关联的机柜信息
        List<NodeCabinetEntity> nodeCabinets = nodeCabinetDao.selectList(new QueryWrapper<NodeCabinetEntity>().eq("room_id", id));
        if (nodeCabinets.size() > 0) {
            throw new NZException(RCode.NODEROOM_EXISTS_CABINET);// 机房下存在机柜 不可以删除
        }
        // 2 . 进行逻辑删除
        nodeRoomDao.deleteById(id);
    }


    /**
     * 机房名称校验
     *
     * @param id   O
     * @param name R
     * @return
     */
    @Override
    public boolean roomNameCheck(Long id, String name) {
        if (id == null) { // 新增
            return getCount(name);
        } else { // 修改
            NodeRoomEntity nodeRoom = getById(id);
            if (name.equals(nodeRoom.getName())) { // 修改时未发生改变
                return true;
            } else {
                return getCount(name);
            }
        }
    }

    /**
     * 机房修改时行列校验，行、列数不能小于已被机柜占用的数
     *
     * @param id       R
     * @param roomRows R
     * @param roomCols R
     * @return
     */
    @Override
    public void roomSizeCheck(Long id, Integer roomRows, Integer roomCols) {
        // 1. 根据当前的 roomId 拿到机柜的最大行列数
        Map<String, Integer> map = nodeCabinetDao.getCabMaxRowCol(id);
        // 2. 进行判断是否可修改
        if (map != null) {
            if (roomRows < map.get("maxRows")) {
                throw new NZException(RCode.NODEROOM_ROW_OCCUPIED); // 输入行小于已使用的行
            }
            if (roomCols < map.get("maxCols")) {
                throw new NZException(RCode.NODEROOM_COL_OCCUPIED); // 输入列小于已使用的列
            }
        }
    }

    public boolean getCount(String name) {
        int count = this.count(new QueryWrapper<NodeRoomEntity>().eq("name", name));// 获取目前非删除的名称个数
        if (count > 0) {
            return false;
        }
        return true;
    }


}