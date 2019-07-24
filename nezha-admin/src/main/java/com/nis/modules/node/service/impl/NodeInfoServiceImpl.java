package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.node.dao.NodeGroupRelDao;
import com.nis.modules.node.dao.NodeInfoDao;
import com.nis.modules.node.entity.*;
import com.nis.modules.node.service.NodeCabinetService;
import com.nis.modules.node.service.NodeInfoService;
import com.nis.modules.node.service.NodeRoomService;
import com.nis.modules.node.service.SystemNodeRelService;
import com.nis.modules.node.util.IpCovert;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.service.SystemInfoService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service("nodeInfoService")
public class NodeInfoServiceImpl extends ServiceImpl<NodeInfoDao, NodeInfoEntity> implements NodeInfoService {

    // 节点 dao
    @Autowired
    private NodeInfoDao nodeInfoDao;

    // 节点 节点组 关联 关系
    @Autowired
    private NodeGroupRelDao nodeGroupRelDao;

    // 机柜service
    @Autowired
    private NodeCabinetService nodeCabinetService;

    // 机房service
    @Autowired
    private NodeRoomService nodeRoomService;

    // 业务系统service
    @Autowired
    private SystemInfoService systemInfoService;

    // 业务系统 节点 关联 关系
    @Autowired
    private SystemNodeRelService systemNodeRelService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        //ip地址
        String ip = (String) params.get("ip");
        //名称
        String name = (String) params.get("name");
        //设备状态
        String state = (String) params.get("state");
        //设备类型
        String type = (String) params.get("type");
        //设备标签
        String tag = (String) params.get("tag");
        //机柜ID
        Long cabinetId = null;
        if (cabinetId != null) {
            cabinetId = Long.valueOf(params.get("cabinetId").toString());
        }

        QueryWrapper<NodeInfoEntity> queryWrapper = new QueryWrapper<NodeInfoEntity>()
                .like(StringUtils.isNotBlank(ip), "ip", ip)
                .like(StringUtils.isNotBlank(name), "name", name)
                .eq(StringUtils.isNotBlank(state), "state", state)
                .eq(StringUtils.isNotBlank(type), "type", type)
                .eq(StringUtils.isNotBlank(tag), "tag", tag)
                .eq((cabinetId != null), "cabinet_id", cabinetId)
                .ne("state", 3);// state 3 是删除状态
        IPage<NodeInfoEntity> page = this.page(new Query<NodeInfoEntity>().getPage(params), queryWrapper);

        // 处理返回结果
        for (NodeInfoEntity entity : page.getRecords()) {
            handlerEntity(entity);
        }
        // 此处缺少功能  tag tagName tag 从字典表中获取 tagName 为国际化翻译之后的值     szd
        return new PageUtils(page);
    }

    // 节点详情查询
    @Override
    public NodeInfoEntity detail(Long uuid) {
        NodeInfoEntity nodeInfoEntity = getOne(new QueryWrapper<NodeInfoEntity>()
                .eq((uuid != null), "uuid", uuid)
                .ne("state", 3));//state 为3表示删除

        handlerEntity(nodeInfoEntity);
        // 此处缺少功能  tag tagName tag 从字典表中获取 tagName 为国际化翻译之后的值     szd
        return nodeInfoEntity;
    }

    //处理返回结果  添加 机柜 机房 业务系统相关信息
    public void handlerEntity(NodeInfoEntity nodeInfoEntity) {
        // 1.得到所属机柜
        NodeCabinetEntity cabinet = nodeCabinetService.getById(nodeInfoEntity.getCabinetId());
        nodeInfoEntity.setCabinetInfo(cabinet);
        // 2.得到所属机房名称
        NodeRoomEntity nodeRoom = nodeRoomService.getById(cabinet.getRoomId());
        nodeInfoEntity.setRoomName(nodeRoom.getName());
        // 3.得到此节点所在的所有业务系统
        List<SystemInfoEntity> sysList = new ArrayList<>();
        List<SystemNodeRelEntity> list = systemNodeRelService.list(new QueryWrapper<SystemNodeRelEntity>().eq("uuid", nodeInfoEntity.getUuid()).eq("del_flag", 0));
        for (SystemNodeRelEntity entity : list) {
            sysList.add(systemInfoService.getById(entity.getSystemId()));
        }
        nodeInfoEntity.setSystemInfos(sysList);
    }

    // ip 是否重复校验
    @Override
    public boolean ipCheck(Long uuid, String ip) {
        if (uuid == null) {// 添加
            return getCount("ip", ip);
        } else {//修改
            NodeInfoEntity nodeInfo = detail(uuid);
            if (ip.equals(nodeInfo.getIp())) {// IP 地址未改变
                return true;
            } else {// IP 地址发生改变
                return getCount("ip", ip);
            }
        }
    }

    // 名称是否重复校验
    @Override
    public boolean nodeNameCheck(Long uuid, String name) {
        if (uuid == null) {// 添加
            return getCount("name", name);
        } else {// 修改
            NodeInfoEntity nodeInfo = detail(uuid);
            if (name.equals(nodeInfo.getName())) {// name 未发生改变
                return true;
            } else {
                return getCount("name", name);
            }
        }
    }

    // 节点状态变更 (上线/下线/删除)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeState(Long[] uuids, String state) {
        nodeInfoDao.changeState(uuids, state);
        if ("3".equals(state)) { // 删除节点
            NodeGroupRelEntity nodeGroup = new NodeGroupRelEntity();
            nodeGroup.setDelFlag("1");
            SystemNodeRelEntity nodeRelEntity = new SystemNodeRelEntity();
            nodeRelEntity.setDelFlag("1");
            for (Long uuid : uuids) {
                // 1. 逻辑删除 和 节点组 之间的关系
                nodeGroupRelDao.update(nodeGroup, new UpdateWrapper<NodeGroupRelEntity>().eq("uuid", uuid));
                // 2. 逻辑删除 和 业务修通 之间的关系
                systemNodeRelService.update(nodeRelEntity, new UpdateWrapper<SystemNodeRelEntity>().eq("uuid", uuid));
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNodeInfo(NodeInfoEntity nodeInfoEntity) {
        // 1. 校验 IP NAME 是否唯一
        if (ipCheck(null, nodeInfoEntity.getIp()) && nodeNameCheck(null, nodeInfoEntity.getName())) {
            //2. IP地址转换 IP整数
            nodeInfoEntity.setIpLong(IpCovert.ipToLong(nodeInfoEntity.getIp()));
            //3. 密码加密 base64
            nodeInfoEntity.setPassword(Base64.getEncoder().encodeToString(nodeInfoEntity.getPassword().getBytes()));
            nodeInfoEntity.setOperator(1L);
            nodeInfoEntity.setOpTime(new Date());
            //4. 入库
            nodeInfoDao.save(nodeInfoEntity);
        } else {
            throw new NZException(RCode.NODE_IPADDRESS_DUPLICATE);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNodeInfo(NodeInfoEntity nodeInfoEntity) {
        // 1. 校验 IP NAME 是否唯一
        if (ipCheck(nodeInfoEntity.getUuid(), nodeInfoEntity.getIp()) && nodeNameCheck(nodeInfoEntity.getUuid(), nodeInfoEntity.getName())) {
            //2. IP地址转换 IP整数
            nodeInfoEntity.setIpLong(IpCovert.ipToLong(nodeInfoEntity.getIp()));
            //3. 密码加密 base64
            nodeInfoEntity.setPassword(Base64.getEncoder().encodeToString(nodeInfoEntity.getPassword().getBytes()));
            nodeInfoEntity.setOperator(1L);
            nodeInfoEntity.setOpTime(new Date());
            //4. 入库
            update(nodeInfoEntity, new UpdateWrapper<NodeInfoEntity>()
                    .eq("uuid", nodeInfoEntity.getUuid()));
        } else {
            throw new NZException(RCode.NODE_IPADDRESS_DUPLICATE);
        }
    }


    /**
     * @param column 查询的列
     * @param value  该列的值
     * @return
     */
    public boolean getCount(String column, String value) {
        int count = count(new QueryWrapper<NodeInfoEntity>().eq(column, value).ne("state", 3));// 名称校验不考虑删除的节点名
        if (count > 0) {//不是唯一的  已经存在
            return false;
        }
        return true;
    }
}