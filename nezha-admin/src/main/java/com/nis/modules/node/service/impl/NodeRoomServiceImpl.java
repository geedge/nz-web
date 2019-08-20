package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service("nodeRoomService")
public class NodeRoomServiceImpl extends ServiceImpl<NodeRoomDao, NodeRoomEntity> implements NodeRoomService {

    // 机柜 dao
    @Autowired
    private NodeCabinetDao nodeCabinetDao;

    // 机柜 service
    @Autowired
    private NodeCabinetService nodeCabinetService;

    // 字典service
    @Autowired
    private SysDictService sysDictService;

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
        // 查询地区名称
        Map<String, Object> map = new HashMap<>();
        map.put("name", "addr");
        List<SysDictEntity> sysDictes = sysDictService.queryList(map);
        for (NodeRoomEntity room : page.getRecords()) {
            for (SysDictEntity dict : sysDictes) {
                if (room.getAddr().toString().equals(dict.getCode())) {
                    room.setAddrName(dict.getValue());
                    break;
                }
            }
        }
        return new PageUtils(page);
    }

    @Override
    public NodeRoomEntity detail(Long id) {
        NodeRoomEntity nodeRoom = this.getById(id);

        // 查询地区名称
        Map<String, Object> map = new HashMap<>();
        map.put("name", "addr");
        List<SysDictEntity> sysDictes = sysDictService.queryList(map);
        for (SysDictEntity entity : sysDictes) {
            if (entity.getCode().equals(nodeRoom.getAddr().toString())) {
                nodeRoom.setAddrName(entity.getValue());
                break;
            }
        }
        return nodeRoom;
    }


    @Override
    public void saveNodeRoom(NodeRoomEntity nodeRoom) {
        if (this.roomNameCheck(null, nodeRoom.getName())) { // 名称唯一校验
            nodeRoom.setOperator(ShiroUtils.getCurrSystemId());
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
        if (this.roomNameCheck(nodeRoom.getId(), nodeRoom.getName())) { // 名称唯一校验

            // 修改前校验机房的大小是否可用
            this.roomSizeCheck(nodeRoom.getId(), nodeRoom.getRoomRows(), nodeRoom.getRoomCols());


            nodeRoom.setOperator(ShiroUtils.getCurrSystemId());
            nodeRoom.setOpTime(new Date());

            this.updateById(nodeRoom);
        } else {
            throw new NZException(RCode.NODEROOM_NAME_DUPLICATE);// 名称重复异常
        }
    }

    /**
     * 机房删除   删除时需要校验是否存在关联的机柜
     *
     * @param ids R
     */
    @Override
    public void delete(Long[] ids) {
        // 1. 校验是否存在关联的机柜信息
        for (Long id : ids) {
            int count = nodeCabinetService.count(new QueryWrapper<NodeCabinetEntity>().eq("room_id", id));
            if (count > 0) {
                NodeRoomEntity nodeRoom = this.getById(id);
                throw new NZException(nodeRoom.getName() + " related cabinets , not deletable ", 203004); // 机房下存在机柜信息
            }
        }
        // 2. 进行逻辑删除
        this.removeByIds(Arrays.asList(ids));
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
        List<NodeRoomEntity> list = this.list(new QueryWrapper<NodeRoomEntity>().eq("name", name));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId() == id)) {
            flag = true;
        }
        return flag;
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
        NodeRoomEntity room = this.getById(id);

        if (room == null) {
            throw new NZException(RCode.NODEROOM_NOT_EXISTS);
        }

        // 1. 根据当前的 roomId 拿到机柜的最大行列数
        NodeCabinetEntity cab = nodeCabinetDao.getCabMaxRowCol(id);

        if (cab != null && cab.getMaxRows() != null && cab.getMaxCols() != null) {
            if (roomRows < cab.getMaxRows()) {
                throw new NZException(RCode.NODEROOM_ROW_OCCUPIED); // 输入行小于已使用的行
            }
            if (roomCols < cab.getMaxCols()) {
                throw new NZException(RCode.NODEROOM_COL_OCCUPIED); // 输入列小于已使用的列
            }
        }
    }

}