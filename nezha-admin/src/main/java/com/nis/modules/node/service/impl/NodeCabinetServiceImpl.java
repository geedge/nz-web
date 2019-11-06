package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.node.dao.NodeCabinetDao;
import com.nis.modules.node.entity.AvailCabinetEntity;
import com.nis.modules.node.entity.NodeCabinetEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.node.entity.NodeRoomEntity;
import com.nis.modules.node.service.NodeCabinetService;
import com.nis.modules.node.service.NodeInfoService;
import com.nis.modules.node.service.NodeRoomService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service("nodeCabinetService")
public class NodeCabinetServiceImpl extends ServiceImpl<NodeCabinetDao, NodeCabinetEntity> implements NodeCabinetService {

    // 机房 service
    @Autowired
    private NodeRoomService nodeRoomService;

    // 节点 service
    @Autowired
    private NodeInfoService nodeInfoService;

    // 机柜 dao
    @Autowired
    private NodeCabinetDao nodeCabinetDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<NodeCabinetEntity> page = new Query<NodeCabinetEntity>().getPage(params);
        List<NodeCabinetEntity> list = nodeCabinetDao.queryPage(page, params);
        page.setRecords(list);
        return new PageUtils(page);
    }

    @Override
    public NodeCabinetEntity detail(Long id) {
        NodeCabinetEntity nodeCabinet = nodeCabinetDao.detail(id);
        if (nodeCabinet == null) {
            throw new NZException(RCode.NODECABINET_NOT_EXISTS);
        }
        return nodeCabinet;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNodeCabinet(NodeCabinetEntity nodeCabinet) {
        // 1. 校验参数
        this.checkParams(nodeCabinet);

        // 2. 验证机柜名称是否可用
        if (this.cabNameCheck(null, nodeCabinet.getName(), nodeCabinet.getRoomId())) {

            // 3. 判断机房行列是否可用
            NodeCabinetEntity cab = this.getOne(new QueryWrapper<NodeCabinetEntity>()
                    .eq("room_id", nodeCabinet.getRoomId())
                    .eq("room_row", nodeCabinet.getRoomRow())
                    .eq("room_col", nodeCabinet.getRoomCol()));
            if (cab == null) { // 所选机房行列未占用
                nodeCabinet.setOperator(ShiroUtils.getCurrSystemId());
                nodeCabinet.setOpTime(new Date());
                this.save(nodeCabinet);
            } else {
                throw new NZException(RCode.NODEROOM_POSITION_OCCUPIED); // 所选位置已被占用
            }
        } else {
            throw new NZException(RCode.NODECABINET_NAME_DUPLICATE); // 机柜名称重复
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNodeCabinet(NodeCabinetEntity nodeCabinet) {
        // 1. 校验参数
        this.checkParams(nodeCabinet);

        // 2. 验证机柜名称是否可用
        if (this.cabNameCheck(nodeCabinet.getId(), nodeCabinet.getName(), nodeCabinet.getRoomId())) {

            // 3. 判断机房位置是否可用
            NodeCabinetEntity cab = this.getOne(new QueryWrapper<NodeCabinetEntity>()
                    .eq("room_id", nodeCabinet.getRoomId())
                    .eq("room_row", nodeCabinet.getRoomRow())
                    .eq("room_col", nodeCabinet.getRoomCol()));

            if (cab == null || nodeCabinet.getId() - cab.getId() == 0) { // 机房行列未被占用 或 修改时没有改变行列

                // 4. U位是否可用校验
                R r = this.cabUbitCheck(nodeCabinet.getId(), nodeCabinet.getUbit());
                if (r.get("code").equals(RCode.SUCCESS.getCode())) {
                    nodeCabinet.setOperator(ShiroUtils.getCurrSystemId());
                    nodeCabinet.setOpTime(new Date());
                    this.updateById(nodeCabinet);
                } else {
                    throw new NZException(RCode.NODECABINET_UBIT_OCCUPIED); // 机柜U位被占用 修改U位小了
                }

            } else {
                throw new NZException(RCode.NODEROOM_POSITION_OCCUPIED); // 所选位置已被占用
            }

        } else {
            throw new NZException(RCode.NODECABINET_NAME_DUPLICATE); // 机柜名称重复
        }
    }


    /**
     * 删除
     *
     * @param ids R
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long[] ids) {
        for (Long id : ids) {
            // 1. 判断是否存在关联节点
            int count = nodeInfoService.count(new QueryWrapper<NodeInfoEntity>().eq("cabinet_id", id).ne("state", 3));
            if (count > 0) {
                NodeCabinetEntity cabinet = this.getById(id);
                throw new NZException(cabinet.getName() + " related nodes , not deletable ", 204006);
            }
        }
        // 2. 进行逻辑删除
        this.removeByIds(Arrays.asList(ids));
    }


    /**
     * 校验U位数量是否可用
     *
     * @param id   R
     * @param ubit R
     */
    @Override
    public R cabUbitCheck(Long id, Integer ubit) {
        // 1. 校验机柜是否存在
        NodeCabinetEntity cab = this.getById(id);
        if (cab == null) {
            throw new NZException(RCode.NODECABINET_NOT_EXISTS);
        }

        // 2. 根据机柜ID得到其中节点最大存放位置
        List<NodeInfoEntity> list = nodeInfoService.list(new QueryWrapper<NodeInfoEntity>().eq("cabinet_id", id).ne(
                "state", 3).orderByDesc("cabinet_start"));

        Integer occPosition = null;

        if (list != null && list.size() > 0) {
            occPosition = list.get(0).getCabinetStart() + list.get(0).getuSize() - 1;
        } else {
            return R.ok();
        }

        if (ubit >= occPosition) {
            return R.ok();
        } else {
            return R.error(RCode.NODECABINET_UBIT_OCCUPIED).put("data", occPosition);
        }
    }


    /**
     * 查询机柜可选的机房及行号列号
     *
     * @param id O  添加时为 null  修改时为指定Id
     */
    @Override
    public List<AvailCabinetEntity> optionalRoom(Long id) {
        // 修改机柜的时候
        Long roomId = null;
        Integer roomRow = null;
        Integer roomCol = null;
        if (id != null) { // 修改的情况  需要将位置加入现有位置中
            NodeCabinetEntity cab = this.getById(id);
            if (cab == null) {
                throw new NZException(RCode.NODECABINET_NOT_EXISTS);// 机柜不存在异常
            }
            roomId = cab.getRoomId();
            roomRow = cab.getRoomRow();
            roomCol = cab.getRoomCol();
        }

        // 返回对象
        List<AvailCabinetEntity> freePosition = new ArrayList<>();

        // 1.0 得到机房列表
        List<NodeRoomEntity> nodeRooms = nodeRoomService.list();
        // 2.0 遍历
        for (NodeRoomEntity entity : nodeRooms) {

            AvailCabinetEntity availCabinetEntity = new AvailCabinetEntity();

            // 3.0 得到总行列数
            Integer roomRows = entity.getRoomRows();
            Integer roomCols = entity.getRoomCols();

            List<Map<String, Object>> list = new ArrayList<>();
            // 4.0 判断行是否可用     列如果占满表示行不可用
            for (int i = 1; i <= roomRows; i++) {

                // 根据 机房ID 和 机房行号 得到有多少列在被占用
                int count = this.count(new QueryWrapper<NodeCabinetEntity>().eq("room_id", entity.getId()).eq(
                        "room_row", i));

                if (id != null && roomId.equals(entity.getId()) && roomRow - i == 0) {  // 修改的机柜 在这个机房中 在这一行中
                    if (count - roomCols == 0) {  // 如果该行已满 但修改仍要找出该位置
                        count++;                  // ++是为了让下面的判断不生效
                    }
                }

                if (count - roomCols == 0) { // 此行已被占满
                    continue;
                }

                // 5.0 行是可用 开始查找可用列
                List<Integer> colList = new ArrayList<>();
                for (int j = 1; j <= roomCols; j++) {
                    colList.add(j);
                }
                List<Integer> colsByRow = nodeCabinetDao.getColsByRow(entity.getId(), i);

                if (id != null && roomId.equals(entity.getId()) && roomRow - i == 0) {  // 修改的机柜 在这个机房中 在这一行中
                    for (int j = 0; j < colsByRow.size(); j++) {
                        if (colsByRow.get(j) - roomCol == 0) {
                            colsByRow.remove(j);
                        }
                    }
                }

                colList.removeAll(colsByRow);

                Map<String, Object> map = new HashMap<>();
                map.put("roomRow", i);           //  可用行
                map.put("roomCols", colList);    //  可用列

                list.add(map);
            }

            availCabinetEntity.setRoomId(entity.getId());
            availCabinetEntity.setRoomName(entity.getName());
            availCabinetEntity.setRoomRows(list);

            freePosition.add(availCabinetEntity);
        }

        return freePosition;
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

        // 校验机房是否存在
        NodeRoomEntity nodeRoom = nodeRoomService.getById(roomId);
        if (nodeRoom == null) {
            throw new NZException(RCode.NODEROOM_NOT_EXISTS);
        }

        List<NodeCabinetEntity> list = this.list(new QueryWrapper<NodeCabinetEntity>().eq("name", name).eq("room_id", roomId));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
    }

    /**
     * 参数校验
     * 最小值为1
     * 最大值为机房最大行列
     *
     * @param nodeCabinet
     */
    public void checkParams(NodeCabinetEntity nodeCabinet) {

        NodeRoomEntity nodeRoom = nodeRoomService.getById(nodeCabinet.getRoomId());
        // 增加机房校验
        if (nodeRoom == null) {
            throw new NZException(RCode.NODEROOM_NOT_EXISTS);
        }

        // 参数不合理  输入的机房行列小于1
        ValidateUtils.is(nodeCabinet.getRoomRow()).notNull(RCode.NODECABINET_ROW_ISNULL)
                .and(nodeCabinet.getRoomRow()).min(1, RCode.NODECABINET_ROW_INCORRECT);
        ValidateUtils.is(nodeCabinet.getRoomCol()).notNull(RCode.NODECABINET_COL_ISNULL)
                .and(nodeCabinet.getRoomCol()).min(1, RCode.NODECABINET_COL_NCORRECT);

        // 输入的行列超过机房最大行列数
        ValidateUtils.is(nodeCabinet.getRoomRow()).max(nodeRoom.getRoomRows(), RCode.NODECABINET_ROW_MAXNUM);
        ValidateUtils.is(nodeCabinet.getRoomCol()).max(nodeRoom.getRoomCols(), RCode.NODECABINET_COL_MAXNUM);
    }
}