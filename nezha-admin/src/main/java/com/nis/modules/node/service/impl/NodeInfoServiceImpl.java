package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.node.dao.NodeGroupRelDao;
import com.nis.modules.node.dao.NodeInfoDao;
import com.nis.modules.node.dao.NodeRoomDao;
import com.nis.modules.node.entity.*;
import com.nis.modules.node.service.NodeCabinetService;
import com.nis.modules.node.service.NodeGroupInfoService;
import com.nis.modules.node.service.NodeInfoService;
import com.nis.modules.node.service.SystemNodeRelService;
import com.nis.modules.node.util.IpCovert;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.sun.org.apache.xalan.internal.lib.NodeInfo;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
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

    // 业务系统 节点 关联 关系
    @Autowired
    private SystemNodeRelService systemNodeRelService;

    // 节点组service
    @Autowired
    private NodeGroupInfoService nodeGroupInfoService;

    @Autowired
    private NodeRoomDao nodeRoomDao;

    // 字典service
    @Autowired
    private SysDictService sysDictService;

    // 机柜service
    @Autowired
    private NodeCabinetService nodeCabinetService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        // 获取当前业务系统 ID
        Long systemId = ShiroUtils.getCurrSystemId();
        params.put("systemId", systemId);

        // 1.  groupId 存在  以 group 关联 为主
        IPage<NodeInfoEntity> page = new Query<NodeInfoEntity>().getPage(params);
        if (StringUtils.isNotBlank((String) params.get("groupId"))) {
            // 1.1 查询出选中节点组的子节点组的ID
            Long groupId = Long.valueOf(params.get("groupId").toString());
            NodeGroupInfoEntity nodeGroup = nodeGroupInfoService.getOne(new QueryWrapper<NodeGroupInfoEntity>().eq("id", groupId));

            if (nodeGroup == null) {
                throw new NZException(RCode.NODEGROUP_NOT_EXISTS);
            }

            // 1.2 获取包含的子节点组
            List<NodeGroupInfoEntity> childe = nodeGroupInfoService.list(new QueryWrapper<NodeGroupInfoEntity>().like("pids", nodeGroup.getPids() + nodeGroup.getId()));
            childe.add(nodeGroup);
            Long[] groupdIds = new Long[childe.size()];
            for (int i = 0; i < childe.size(); i++) {
                groupdIds[i] = childe.get(i).getId();
            }
            List<NodeInfoEntity> nodeInfoEntities = nodeInfoDao.selectListByGroups(page, params, groupdIds);
            // 1.3 获取 tagName 的值
            this.getTagName(nodeInfoEntities);
            page.setRecords(nodeInfoEntities);
            return new PageUtils(page);
        }

        // 2. 节点自身的过滤 包含机房
        List<NodeInfoEntity> nodeInfoEntities = nodeInfoDao.selectListByRoomId(page, params);
        // 获取 tagName 的值
        this.getTagName(nodeInfoEntities);
        page.setRecords(nodeInfoEntities);
        return new PageUtils(page);
    }

    public void getTagName(List<NodeInfoEntity> nodeInfoEntities) {
        Map<String, Object> map = new HashMap();
        map.put("type", Constant.DICT_NODE_TAG_TYPE + Constant.NODE_TYPE_SERVER); // 节点类型为 server
        List<SysDictEntity> serverTags = sysDictService.queryList(map);

        map.put("type", Constant.DICT_NODE_TAG_TYPE + Constant.NODE_TYPE_NET); // 节点类型为 Net
        List<SysDictEntity> netTags = sysDictService.queryList(map);

        for (NodeInfoEntity nodeInfo : nodeInfoEntities) {
            if (Constant.NODE_TYPE_SERVER.equals(nodeInfo.getType())) { // 节点类型为 server
                for (SysDictEntity entity : serverTags) {
                    if (entity.getCode().equals(nodeInfo.getTag())) {
                        nodeInfo.setTagName(entity.getValue());
                        break;
                    }
                }
            } else if (Constant.NODE_TYPE_NET.equals(nodeInfo.getType())) { // 节点类型为 net
                for (SysDictEntity entity : netTags) {
                    if (entity.getCode().equals(nodeInfo.getTag())) {
                        nodeInfo.setTagName(entity.getValue());
                        break;
                    }
                }
            } else {
                throw new NZException("Nodes with UUID " + nodeInfo.getUuid() + " type not found", 201012);
            }
        }
    }

    // 节点详情查询
    @Override
    public NodeInfoEntity detail(Long uuid) {
        NodeInfoEntity nodeInfoEntity = nodeInfoDao.detail(uuid);
        if (nodeInfoEntity == null) {
            throw new NZException(RCode.NODE_NOT_EXISTS);
        }

        String tag = nodeInfoEntity.getTag();
        Map<String, Object> map = new HashMap();

        if (Constant.NODE_TYPE_SERVER.equals(nodeInfoEntity.getType())) {
            map.put("type", Constant.DICT_NODE_TAG_TYPE + Constant.NODE_TYPE_SERVER); // 节点类型为 server
        } else if (Constant.NODE_TYPE_NET.equals(nodeInfoEntity.getType())) {
            map.put("type", Constant.DICT_NODE_TAG_TYPE + Constant.NODE_TYPE_NET);    // 节点类型为 Net
        } else {
            throw new NZException(RCode.NODE_TYPE_NOTFOUND); // 节点类型未找到
        }

        // tag 值不是必须的 ,在tag值存在的时候进行校验
        if (tag != null && StringUtils.isNotBlank(tag)) {
            List<SysDictEntity> sysDictEntities = sysDictService.queryList(map);
            boolean flag = false;
            for (SysDictEntity entity : sysDictEntities) {
                if (tag.equals(entity.getCode())) {
                    nodeInfoEntity.setTagName(entity.getValue());
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                throw new NZException(RCode.NODE_TAG_NOTFOUND);
            }
        }

        if (nodeInfoEntity != null && StringUtils.isNotBlank(nodeInfoEntity.getPassword())) {
            nodeInfoEntity.setPassword(new String(Base64.getDecoder().decode(nodeInfoEntity.getPassword())));
        }
        return nodeInfoEntity;
    }

    //  IP校验，分三种情况：1.校验不通过；2.校验通过；3.ip已在其他业务系统里存在，此时返回node实体，询问用户是否关联；
    @Override
    public R ipCheck(Long uuid, String ip) {
        if (ShiroUtils.isGlobal()) { // global
            if (checkFieldExist(uuid, "ip", ip)) {  // 通过
                return R.ok();
            } else {                                         // 不通过
                return R.error(RCode.NODE_IPADDRESS_DUPLICATE);
            }
        } else { // 当前业务系统
            if (checkFieldExist(uuid, "ip", ip)) {   // 通过
                return R.ok();
            } else {                                          // 其他业务系统已存在 返回详情
                NodeInfoEntity node = this.getOne(new QueryWrapper<NodeInfoEntity>().eq("ip", ip));
                NodeInfoEntity nodeDetail = this.detail(node.getUuid());
                List<SystemInfoEntity> systemInfo = nodeDetail.getSystemInfo();
                for (SystemInfoEntity entity : systemInfo) {
                    if (entity.getId().equals(ShiroUtils.getCurrSystemId())) {
                        return R.error(RCode.NODE_IPADDRESS_DUPLICATE); // 本系统已存在该IP
                    }
                }
                return R.ok(nodeDetail);
            }
        }
    }

    // 名称是否重复校验
    @Override
    public boolean nodeNameCheck(Long uuid, String name) {
        return checkFieldExist(uuid, "name", name);
    }

    public boolean checkFieldExist(Long uuid, String column, String colValue) {
        List<NodeInfoEntity> list = this.list(new QueryWrapper<NodeInfoEntity>().eq(column, colValue).ne("state", 3));
        boolean flag = false;

        if (list == null || list.size() == 0 || (list != null && list.size() == 1 && list.get(0).getUuid() == uuid)) {
            flag = true;
        }
        return flag;
    }

    // 节点状态变更 (上线/下线/删除)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeState(Long[] uuids, String state) {
        nodeInfoDao.changeState(uuids, state);
        if ("3".equals(state)) { // 删除节点

            // 1. 逻辑删除 和 节点组 之间的关系
            nodeGroupRelDao.delete(new QueryWrapper<NodeGroupRelEntity>().in("uuid", uuids));

            // 2. 逻辑删除 和 业务系统 之间的关系
            systemNodeRelService.remove(new QueryWrapper<SystemNodeRelEntity>().in("uuid", uuids));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNodeInfo(NodeInfoEntity nodeInfoEntity) {
        if (ShiroUtils.isGlobal()) {
            // 校验IP
            if (this.checkFieldExist(null, "ip", nodeInfoEntity.getIp())) {

                // 正常入库流程
                saveNode(nodeInfoEntity);

                // 校验是否已经存在关联关系
                SystemNodeRelEntity entity = systemNodeRelService.getOne(new QueryWrapper<SystemNodeRelEntity>().eq(
                        "system_id",
                        Constant.GLOBAL_ID).eq("uuid", nodeInfoEntity.getUuid()));
                if (entity != null) {
                    throw new NZException(RCode.NODE_SYSTEM_REL_EXISTS);  // 节点和业务系统已经关联
                }

                // 维护系统中间表  只维护global
                SystemNodeRelEntity nodeRelEntity = new SystemNodeRelEntity(Constant.GLOBAL_ID, nodeInfoEntity.getUuid(), Constant.GLOBAL_ID, new Date());
                systemNodeRelService.save(nodeRelEntity);
            } else {
                // ip 已存在 抛异常
                throw new NZException(RCode.NODE_IPADDRESS_DUPLICATE);
            }
        } else {
            // 校验IP
            if (this.checkFieldExist(null, "ip", nodeInfoEntity.getIp())) {

                // 正常入库流程
                saveNode(nodeInfoEntity);

                Long systemId = ShiroUtils.getCurrSystemId();
                // 校验是否已经存在关联关系
                SystemNodeRelEntity entity = systemNodeRelService.getOne(new QueryWrapper<SystemNodeRelEntity>().eq(
                        "system_id", systemId).eq("uuid", nodeInfoEntity.getUuid()));
                if (entity != null) {
                    throw new NZException(RCode.NODE_SYSTEM_REL_EXISTS);  // 节点和业务系统已经关联
                }
                // 维护系统中间表   维护global 和 自身业务系统 关系
                List<SystemNodeRelEntity> list = new ArrayList<>();

                Date date = new Date();
                SystemNodeRelEntity currSystem = new SystemNodeRelEntity(systemId, nodeInfoEntity.getUuid(), systemId
                        , date);
                SystemNodeRelEntity global = new SystemNodeRelEntity(Constant.GLOBAL_ID, nodeInfoEntity.getUuid(), systemId, date);
                list.add(currSystem);
                list.add(global);
                systemNodeRelService.saveBatch(list);
            } else {

                // 如果此节点和自身业务系统存在关系 则不让添加
                NodeInfoEntity node = this.getOne(new QueryWrapper<NodeInfoEntity>().eq("ip", nodeInfoEntity.getIp()));
                List<SystemNodeRelEntity> systemInfo = systemNodeRelService.list(new QueryWrapper<SystemNodeRelEntity>().eq("uuid", node.getUuid()));
                for (SystemNodeRelEntity entity : systemInfo) {
                    if (entity.getSystemId() - ShiroUtils.getCurrSystemId() == 0) {
                        throw new NZException(RCode.NODE_SYSTEM_REL_EXISTS); // 节点和业务系统已经关联
                    }
                }
                // ip已经存在 和自身没有关联 则建立关联
                SystemNodeRelEntity nodeRelEntity = new SystemNodeRelEntity(ShiroUtils.getCurrSystemId(), node.getUuid(),
                        ShiroUtils.getCurrSystemId(), new Date());
                systemNodeRelService.save(nodeRelEntity);
            }
        }
    }

    // 添加节点正常操作  名称校验 机柜校验 密码加密 入库 并返回当前的UUID序列值
    @Transactional(rollbackFor = Exception.class)
    public void saveNode(NodeInfoEntity nodeInfoEntity) {
        if (this.nodeNameCheck(null, nodeInfoEntity.getName())) {

            //2. 校验可用机柜
            if (nodeInfoEntity.getCabinetId() != null && nodeInfoEntity.getCabinetId() > 0) {  // 指定了机柜的情况

                NodeCabinetEntity cab = nodeCabinetService.getById(nodeInfoEntity.getCabinetId());
                if (cab == null) {
                    throw new NZException(RCode.NODECABINET_NOT_EXISTS);
                }

                // 查询该机柜的可选起始位置
                List<Integer> starts = this.checkPosition(nodeInfoEntity);
                if (starts == null) {
                    throw new NZException(RCode.NODECABINET_NOT_HAVE_POSITION); // 所选机柜 U位已被占用 或 不存在该位置
                }
                if (nodeInfoEntity.getCabinetStart() == null) {
                    throw new NZException(RCode.NODE_USTART_ISNULL);
                }
                if (starts.size() > 0 && !starts.contains(nodeInfoEntity.getCabinetStart())) {
                    throw new NZException(RCode.NODE_USTART_OCCUPIED); // 所选机柜 U位已被占用 或 不存在该位置
                }
            } else { // 没有指定机柜的情况 不可以填写起始位置
                if (nodeInfoEntity.getCabinetStart() != null) {
                    throw new NZException(RCode.NODECABINET_ID_ISNULL); // 指定机柜起始位置,需要指定机柜ID
                }
            }

            // 校验节点类型和节点标签
            this.checkTypeAndTag(nodeInfoEntity);

            //3. IP地址转换 IP整数
            nodeInfoEntity.setIpLong(IpCovert.ipToLong(nodeInfoEntity.getIp()));
            if (nodeInfoEntity.getPassword() != null && StringUtils.isNotBlank(nodeInfoEntity.getPassword())) {
                //4. 密码加密 base64
                nodeInfoEntity.setPassword(Base64.getEncoder().encodeToString(nodeInfoEntity.getPassword().getBytes()));
            }
            nodeInfoEntity.setOperator(ShiroUtils.getCurrSystemId());
            nodeInfoEntity.setOpTime(new Date());

            //5. 入库
            nodeInfoDao.save(nodeInfoEntity);
        } else {
            throw new NZException(RCode.NODE_NAME_DUPLICATE);// 名称重复
        }
    }

    // 校验节点类型和节点标签
    public void checkTypeAndTag(NodeInfoEntity nodeInfoEntity) {
        Map<String, Object> map = new HashMap();

        if (Constant.NODE_TYPE_SERVER.equals(nodeInfoEntity.getType())) {
            map.put("type", Constant.DICT_NODE_TAG_TYPE + Constant.NODE_TYPE_SERVER); // 节点类型为 server
            if (nodeInfoEntity.getSnmpPort() != null) {
                throw new NZException(RCode.NODE_SERVER_CANNOT_HAVE_SNMPPORT);
            }
            if (nodeInfoEntity.getSnmpCommunity() != null) {
                throw new NZException(RCode.NODE_SERVER_CANNOT_HAVE_SNMPCOMMUNITY);
            }
        } else if (Constant.NODE_TYPE_NET.equals(nodeInfoEntity.getType())) {
            map.put("type", Constant.DICT_NODE_TAG_TYPE + Constant.NODE_TYPE_NET);    // 节点类型为 Net
            if (nodeInfoEntity.getSnmpPort() == null) {
                nodeInfoEntity.setSnmpPort(161);
            }
            if (StringUtils.isBlank(nodeInfoEntity.getSnmpCommunity())) {
                nodeInfoEntity.setSnmpCommunity("public");
            }
        } else {
            throw new NZException(RCode.NODE_TYPE_NOTFOUND); // 节点类型未找到
        }

        // 如果存在tag则校验tag是否存在
        if (nodeInfoEntity.getTag() != null && StringUtils.isNotBlank(nodeInfoEntity.getTag())) {
            // 得到相应的tag 集合
            List<SysDictEntity> sysDictEntities = sysDictService.queryList(map);
            boolean flag = false;
            for (SysDictEntity entity : sysDictEntities) {
                if (entity.getCode().equals(nodeInfoEntity.getTag())) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                throw new NZException(RCode.NODE_TAG_NOTFOUND); // 节点标签未找到
            }
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNodeInfo(NodeInfoEntity nodeInfoEntity) {
        if (ShiroUtils.isGlobal()) { // global 系统下
            // 1.校验IP
            if (this.checkFieldExist(nodeInfoEntity.getUuid(), "ip", nodeInfoEntity.getIp())) {
                // 正常修改操作
                this.updateNode(nodeInfoEntity);
            } else {
                // ip 已存在 抛异常
                throw new NZException(RCode.NODE_IPADDRESS_DUPLICATE);
            }
        } else {
            // 校验IP
            if (this.checkFieldExist(nodeInfoEntity.getUuid(), "ip", nodeInfoEntity.getIp())) {
                // 正常修改操作
                this.updateNode(nodeInfoEntity);
            } else {
                // 维护关系
                NodeInfoEntity node = this.getOne(new QueryWrapper<NodeInfoEntity>().eq("ip", nodeInfoEntity.getIp()));
                List<SystemNodeRelEntity> systemInfo = systemNodeRelService.list(new QueryWrapper<SystemNodeRelEntity>().eq("uuid", node.getUuid()));
                for (SystemNodeRelEntity entity : systemInfo) {
                    if (entity.getSystemId() - ShiroUtils.getCurrSystemId() == 0) {
                        throw new NZException(RCode.NODE_SYSTEM_REL_EXISTS); // 节点和业务系统已经关联
                    }
                }
                // ip已经存在 和自身没有关联 则建立关联
                SystemNodeRelEntity nodeRelEntity = new SystemNodeRelEntity(ShiroUtils.getCurrSystemId(), node.getUuid(),
                        ShiroUtils.getCurrSystemId(), new Date());
                systemNodeRelService.save(nodeRelEntity);
            }
        }
    }

    // 正常修改流程
    public void updateNode(NodeInfoEntity nodeInfoEntity) {
        if (this.nodeNameCheck(nodeInfoEntity.getUuid(), nodeInfoEntity.getName())) {

            // 3. 校验可用机柜
            if (nodeInfoEntity.getCabinetId() != null && nodeInfoEntity.getCabinetId() > 0) { // 指定了 机柜
                // 指定机柜的情况下 必须指定机柜起始位置
                if (nodeInfoEntity.getCabinetStart() == null) {
                    throw new NZException(RCode.NODE_USTART_ISNULL);
                }

                NodeCabinetEntity cab = nodeCabinetService.getById(nodeInfoEntity.getCabinetId());
                if (cab == null) {
                    throw new NZException(RCode.NODECABINET_NOT_EXISTS);
                }

                // 查询该机柜的可选起始位置
                List<Integer> starts = this.checkPosition(nodeInfoEntity);

                if (starts == null) {
                    throw new NZException(RCode.NODECABINET_NOT_HAVE_POSITION); // 所选机柜 U位已被占用 或 不存在该位置
                }
                if (!starts.contains(nodeInfoEntity.getCabinetStart())) {
                    throw new NZException(RCode.NODE_USTART_OCCUPIED); // 所选机柜 U位已被占用 或 不存在该位置
                }
            }

            // 校验节点类型和标签是否正确
            this.checkTypeAndTag(nodeInfoEntity);

            //3 IP地址转换 IP整数
            nodeInfoEntity.setIpLong(IpCovert.ipToLong(nodeInfoEntity.getIp()));
            //4. 密码加密 base64
            if (nodeInfoEntity.getPassword() != null && StringUtils.isNotBlank(nodeInfoEntity.getPassword())) {
                nodeInfoEntity.setPassword(Base64.getEncoder().encodeToString(nodeInfoEntity.getPassword().getBytes()));
            }
            nodeInfoEntity.setOperator(ShiroUtils.getCurrSystemId());
            nodeInfoEntity.setOpTime(new Date());

            //5. 入库
            update(nodeInfoEntity, new UpdateWrapper<NodeInfoEntity>().eq("uuid", nodeInfoEntity.getUuid()));
        } else {
            throw new NZException(RCode.NODE_NAME_DUPLICATE);// 名称重复
        }
    }

    // 添加修改时根据 指定的USize获取可用位置的集合
    public List<Integer> checkPosition(NodeInfoEntity nodeInfoEntity) {
        if (nodeInfoEntity.getuSize() != null && nodeInfoEntity.getuSize() > 0) { // 指定机柜 且 指定U位正确情况下
            List<AvailCabinetEntity> availCabinetEntities = this.selectCab(nodeInfoEntity.getUuid(), nodeInfoEntity.getuSize());
            for (AvailCabinetEntity entity : availCabinetEntities) {
                for (NodeCabinetEntity cab : entity.getCabinets()) {
                    if (cab.getId() - nodeInfoEntity.getCabinetId() == 0) {
                        return cab.getStarts();
                    }
                }
            }
        } else {
            throw new NZException(RCode.NODE_USIZE_NOTTRUE); // U位不正确
        }
        return null;
    }

    /**
     * 查询可用机柜 返回机房 机柜 和 可用下标
     *
     * @param uuid  O
     * @param uSize R
     * @return
     */
    @Override
    public List<AvailCabinetEntity> selectCab(Long uuid, Integer uSize) {
        //定义返回集合
        List<AvailCabinetEntity> retAvailCabs = new ArrayList<>();

        // 1.0 得到所有机房  有机柜的机房
        List<NodeRoomEntity> nodeRooms = nodeRoomDao.queryRoomCabinets();

        // 所有包含节点的机房
        List<NodeRoomEntity> list = nodeRoomDao.queryRoomsNodes();

        Map<Long, List<NodeInfoEntity>> map = new HashMap<>();
        for (NodeRoomEntity entity : list) { // 占用节点的机房集合
            if (entity != null && entity.getCabinets() != null && entity.getCabinets().size() > 0) {// 找到占用节点的机柜信息
                for (NodeCabinetEntity cab : entity.getCabinets()) {
                    map.put(cab.getId(), cab.getNodes());
                }
            }
        }

        for (NodeRoomEntity entity : nodeRooms) {
            for (NodeCabinetEntity cab : entity.getCabinets()) {
                List<NodeInfoEntity> nodeInfoEntities = map.get(cab.getId());
                if (nodeInfoEntities != null && nodeInfoEntities.size() > 0) {// 该机柜中存在节点
                    cab.setNodes(nodeInfoEntities);
                }
            }
        }

        Long cabinetId = null;
        if (uuid != null) { // 修改的情况  需要将自身节点补充到所属机柜的位置中去  再次重新计算可用位置
            NodeInfoEntity updateNodeInfo = this.getOne(new QueryWrapper<NodeInfoEntity>().eq("uuid", uuid).ne("state", 3));
            if (updateNodeInfo == null) {
                throw new NZException(RCode.NODE_NOT_EXISTS); // 节点不存在
            }
            cabinetId = updateNodeInfo.getCabinetId();
        }

        for (NodeRoomEntity nodeRoom : nodeRooms) {
            //定义返回格式
            AvailCabinetEntity availCab = new AvailCabinetEntity();
            availCab.setRoomId(nodeRoom.getId());
            availCab.setRoomName(nodeRoom.getName());

            // 1.1 查询每个机房下所有的机柜
            List<NodeCabinetEntity> nodeCabinets = nodeRoom.getCabinets();

            // 2.0 遍历机柜 得到下面的所有节点
            for (NodeCabinetEntity cab : nodeCabinets) {

                // 2.1 得到每个机柜下包含的节点信息
                List<NodeInfoEntity> nodeInfos = cab.getNodes();
                // 2.2 获取当前机柜的全部位置
                Integer[] allPosition = new Integer[cab.getUbit()];
                for (int i = 0; i < allPosition.length; i++) {
                    allPosition[i] = i + 1;
                } //操作之后  allPosition 中的数据格式应为 1,2,3,4 ...  从1开始到uBit   也就是所有位置

                List<Integer> occList = new ArrayList<>(); // 记录的是整个机柜下 节点所占用的位置

                if (cabinetId != null && cabinetId - cab.getId() == 0) { // 修改时节点的所属的机柜  应还回节点

                    // 2.3 根据得到的节点信息 得到占用的位置
                    for (NodeInfoEntity nodeInfo : nodeInfos) {
                        if (nodeInfo.getUuid() == uuid) { // 修改的情况下
                            continue;
                        }
                        for (int i = nodeInfo.getCabinetStart(); i < nodeInfo.getCabinetStart() + nodeInfo.getuSize(); i++) {
                            //2.4 这里 i 就是要从 allPostion 中剔除的数据  例如 5,7  那这里的I 就是 5 ,6
                            occList.add(i);
                        }
                    }

                } else {

                    for (NodeInfoEntity nodeInfo : nodeInfos) {
                        for (int i = nodeInfo.getCabinetStart(); i < nodeInfo.getCabinetStart() + nodeInfo.getuSize(); i++) {
                            //2.4 这里 i 就是要从 allPostion 中剔除的数据  例如 5,7  那这里的I 就是 5 ,6
                            occList.add(i);
                        }
                    }

                }
                // 3.0 根据全部的位置 去除已经占用的位置 得到空闲的位置
                List<Integer> freePosition = new ArrayList<>(Arrays.asList(allPosition));
                freePosition.removeAll(occList); // freePosition 为剩余的位置 例如 1,2,3,4,(5,6,) 7,8,(9),10
                // 4.0 在此处处理一下可用位置
                String convertStr = this.convertStr(freePosition);// 处理之后的格式  '1-4,7,10'
                //5.0  拿到处理好的格式之后进行判断U位规格
                String[] handleArr = convertStr.split(",");  //   格式例如 [ "1-4" , "7"  , "10"]

                // 6.0 记录符合返回段的数据
                List<String> availPostion = new ArrayList<>();
                if (uSize > 1) {
                    //进行U位大小的判断 查询可用位置
                    for (String str : handleArr) {
                        if (str.contains("-")) { // 如果包含 ‘-’ 绝对是连续 不包含就是单条的 此处条件为uSize > 1
                            String[] split = str.split("-");// 继续分割 找到可用的起始位置 5,7
                            Integer u = Integer.valueOf(split[1]) - Integer.valueOf(split[0]) + 1;//得到这个范围的位置个数 1-4 为4个位置 4-1 = 3
                            if (u < uSize) {// 这个连续段的大小不符合U位
                                continue;
                            } else {
                                availPostion.add(str); // 输入的U位大小符合这个范围段
                            }
                        }
                    }

                    // 6.1 此处对可用的范围段做出相应的处理  availPostion中记录的是范围段   多个返回段  6-10   13-17 .... 这种
                    List<Integer> endList = new ArrayList<>();
                    if (ArrayUtils.isNotEmpty(allPosition)) {
                        for (String str : availPostion) {
                            String[] split = str.split("-");
                            for (int i = Integer.valueOf(split[0]); i < Integer.valueOf(split[1]); i++) { // 1 4  2
                                if (Integer.valueOf(split[1]) - i + 1 >= uSize) {//从后往前判断可以从哪些开始
                                    //可以开始的位置   记录并返回前台
                                    endList.add(i);
                                }
                                continue;
                            }
                        }
                    }
                    // 此处的endList 为开始下标了
                    cab.setStarts(endList);
                } else {
                    //这种情况就是用户输入的U位是1  可以把所有的位置都返回  都可以选择 如果存在可用位置的情况下  如果不存在返回 Null 合理
                    cab.setStarts(freePosition);
                }
            }
            availCab.setCabinets(nodeCabinets);

            // 如果机房下所有机柜都不含可用位置 则不反悔此机房
            if (availCab.getCabinets() != null && availCab.getCabinets().size() > 0) {
                retAvailCabs.add(availCab);
            }
        }

        return retAvailCabs;
    }

    //判断是否连续的  并用字符串的形式返回
    public String convertStr(List<Integer> list) {
        int state = 0;
        String result = "";
        for (int i = 0; i < list.size(); i++) {
            if (i == list.size() - 1) state = 2;
            if (state == 0) {
                if (list.get(i + 1) == list.get(i) + 1) {
                    result += list.get(i);
                    result += "-";
                    state = 1;
                } else {
                    result += list.get(i);
                    result += ",";
                }
            } else if (state == 1) {
                if (list.get(i + 1) != list.get(i) + 1) {
                    result += list.get(i);
                    result += ",";
                    state = 0;
                }
            } else {
                result += list.get(i);
            }
        }
        return result;
    }

    @Override
    public PageUtils queryPageBySystemId(Map<String, Object> params) {
        IPage<NodeInfoEntity> page = new Query<NodeInfoEntity>().getPage(params);
        List<NodeInfoEntity> queryNodeBySystemId = nodeInfoDao.queryNodeBySystemId(page, (Long) params.get("id"));
        this.getTagName(queryNodeBySystemId);
        page.setRecords(queryNodeBySystemId);
        return new PageUtils(page);
    }

    @Override
    public PageUtils querySelectableNodeBySystemId(Map<String, Object> params) {
        IPage<NodeInfoEntity> page = new Query<NodeInfoEntity>().getPage(params);
        List<NodeInfoEntity> queryNodeBySystemId = nodeInfoDao.querySelectableNodeBySystemId(page, (Long) params.get("id"));
        this.getTagName(queryNodeBySystemId);
        page.setRecords(queryNodeBySystemId);
        return new PageUtils(page);
    }
}