package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.node.dao.NodeInfoDao;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.node.service.NodeInfoService;
import com.nis.modules.node.util.IpCovert;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Date;
import java.util.Map;


@Service
public class NodeInfoServiceImpl extends ServiceImpl<NodeInfoDao, NodeInfoEntity> implements NodeInfoService {

    @Autowired
    private NodeInfoDao nodeInfoDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        //ip地址
        String ip = (String) params.get("ip");
        //设备状态
        String state = (String) params.get("state");
        //设备类型
        String type = (String) params.get("type");
        //设备标签
        String tag = (String) params.get("tag");
        //机柜ID
        Long cabinetId = (Long) params.get("cabinetId");
        //排序字段
        String sidx = (String) params.get("sidx");
        String order = (String) params.get("order");
        QueryWrapper<NodeInfoEntity> queryWrapper = new QueryWrapper<NodeInfoEntity>()
                .like(StringUtils.isNotBlank(ip), "ip", ip)
                .eq(StringUtils.isNotBlank(state), "state", state)
                .eq(StringUtils.isNotBlank(type), "type", type)
                .eq(StringUtils.isNotBlank(tag), "tag", tag)
                .eq((cabinetId != null), "cabinet_id", cabinetId)
                .orderBy(StringUtils.isNotBlank(sidx), "ASC".equals(order), sidx);
        IPage<NodeInfoEntity> page = this.page(new Query<NodeInfoEntity>().getPage(params), queryWrapper);

        return new PageUtils(page);
    }

    // 节点详情查询
    @Override
    public NodeInfoEntity detail(Long uuid) {
        NodeInfoEntity nodeInfoEntity = getOne(new QueryWrapper<NodeInfoEntity>()
                .eq((uuid != null), "uuid", uuid)
                .ne("state", 3));//state 为3表示删除
        //此处还要 set  roomName CabinetInfo  systemInfo
        return nodeInfoEntity;
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
        int count = count(new QueryWrapper<NodeInfoEntity>().eq(column, value));
        if (count > 0) {//不是唯一的  已经存在
            return false;
        }
        return true;
    }
}