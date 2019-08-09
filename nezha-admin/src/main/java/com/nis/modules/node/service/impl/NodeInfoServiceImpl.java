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
import com.nis.modules.node.dao.NodeRoomDao;
import com.nis.modules.node.entity.*;
import com.nis.modules.node.service.*;
import com.nis.modules.node.util.IpCovert;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    // 业务系统 节点 关联 关系
    @Autowired
    private SystemNodeRelService systemNodeRelService;

    // 节点组service
    @Autowired
    private NodeGroupInfoService nodeGroupInfoService;

    @Autowired
    private NodeRoomDao nodeRoomDao;

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
            // 1.2 获取包含的子节点组
            List<NodeGroupInfoEntity> childe = nodeGroupInfoService.list(new QueryWrapper<NodeGroupInfoEntity>().like("pids", nodeGroup.getPids() + nodeGroup.getId()));
            childe.add(nodeGroup);
            Long[] groupdIds = new Long[childe.size()];
            for (int i = 0; i < childe.size(); i++) {
                groupdIds[i] = childe.get(i).getId();
            }
            page.setRecords(nodeInfoDao.selectListByGroups(page, params, groupdIds));

            // 此处缺少功能  tag tagName tag 从字典表中获取 tagName 为国际化翻译之后的值     szd
            return new PageUtils(page);
        }

        // 2. 节点自身的过滤 包含机房
        page.setRecords(nodeInfoDao.selectListByRoomId(page, params));

        // 此处缺少功能  tag tagName tag 从字典表中获取 tagName 为国际化翻译之后的值     szd
        return new PageUtils(page);
    }

    // 节点详情查询
    @Override
    public NodeInfoEntity detail(Long uuid) {
        NodeInfoEntity nodeInfoEntity = nodeInfoDao.detail(uuid);

        // 此处缺少功能  tag tagName tag 从字典表中获取 tagName 为国际化翻译之后的值     szd
        nodeInfoEntity.setPassword(new String(Base64.getDecoder().decode(nodeInfoEntity.getPassword())));
        return nodeInfoEntity;
    }

    // ip 是否重复校验
    @Override
    public boolean ipCheck(Long uuid, String ip) {
        if (uuid == null) {// 添加
            return checkFieldExists("ip", ip);
        } else {//修改
            NodeInfoEntity nodeInfo = this.getOne(new QueryWrapper<NodeInfoEntity>().eq("uuid", uuid));
            if (ip.equals(nodeInfo.getIp())) {// IP 地址未改变
                return true;
            } else {// IP 地址发生改变
                return checkFieldExists("ip", ip);
            }
        }
    }

    // 名称是否重复校验
    @Override
    public boolean nodeNameCheck(Long uuid, String name) {
        if (uuid == null) {// 添加
            return checkFieldExists("name", name);
        } else {// 修改
            NodeInfoEntity nodeInfo = this.getOne(new QueryWrapper<NodeInfoEntity>().eq("uuid", uuid));
            if (name.equals(nodeInfo.getName())) {// name 未发生改变
                return true;
            } else {
                return checkFieldExists("name", name);
            }
        }
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
            systemNodeRelService.remove(new QueryWrapper<SystemNodeRelEntity>().in("uuid",uuids));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNodeInfo(NodeInfoEntity nodeInfoEntity) {
        // 1. 校验 IP NAME 是否唯一
        if (ipCheck(null, nodeInfoEntity.getIp())) {
            if (nodeNameCheck(null, nodeInfoEntity.getName())) {

                //2. 校验可用机柜
                if (nodeInfoEntity.getCabinetId() != null && nodeInfoEntity.getCabinetId() > 0) {  // 指定了机柜的情况

                    // 查询该机柜的可选起始位置
                    List<Integer> starts = checkPosition(nodeInfoEntity);

                    if (!starts.contains(nodeInfoEntity.getCabinetStart())) {
                        throw new NZException(RCode.NODE_USTART_OCCUPIED); // 所选机柜 U位已被占用 或 不存在该位置
                    }
                }

                //3. IP地址转换 IP整数
                nodeInfoEntity.setIpLong(IpCovert.ipToLong(nodeInfoEntity.getIp()));
                //4. 密码加密 base64
                nodeInfoEntity.setPassword(Base64.getEncoder().encodeToString(nodeInfoEntity.getPassword().getBytes()));
                nodeInfoEntity.setOperator(ShiroUtils.getCurrSystemId());
                nodeInfoEntity.setOpTime(new Date());

                //5. 入库
                nodeInfoDao.save(nodeInfoEntity);
            } else {
                throw new NZException(RCode.NODE_NAME_DUPLICATE);// 名称重复
            }
        } else {
            throw new NZException(RCode.NODE_IPADDRESS_DUPLICATE);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNodeInfo(NodeInfoEntity nodeInfoEntity) {
        // 1. 校验 IP NAME 是否唯一
        if (ipCheck(nodeInfoEntity.getUuid(), nodeInfoEntity.getIp())) {
            if (nodeNameCheck(nodeInfoEntity.getUuid(), nodeInfoEntity.getName())) {

                //2. 校验可用机柜
                if (nodeInfoEntity.getCabinetId() != null && nodeInfoEntity.getCabinetId() > 0) { // 指定了 机柜

                    // 查询该机柜的可选起始位置
                    List<Integer> starts = checkPosition(nodeInfoEntity);

                    if (!starts.contains(nodeInfoEntity.getCabinetStart())) {
                        throw new NZException(RCode.NODE_USTART_OCCUPIED); // 所选机柜 U位已被占用 或 不存在该位置
                    }
                }

                //3 IP地址转换 IP整数
                nodeInfoEntity.setIpLong(IpCovert.ipToLong(nodeInfoEntity.getIp()));
                //4. 密码加密 base64
                nodeInfoEntity.setPassword(Base64.getEncoder().encodeToString(nodeInfoEntity.getPassword().getBytes()));
                nodeInfoEntity.setOperator(ShiroUtils.getCurrSystemId());
                nodeInfoEntity.setOpTime(new Date());

                //5. 入库
                update(nodeInfoEntity, new UpdateWrapper<NodeInfoEntity>().eq("uuid", nodeInfoEntity.getUuid()));
            } else {
                throw new NZException(RCode.NODE_NAME_DUPLICATE);// 名称重复
            }
        } else {
            throw new NZException(RCode.NODE_IPADDRESS_DUPLICATE);// IP地址重复
        }
    }

    // 添加修改时根据 指定的USize获取可用位置的集合
    public List<Integer> checkPosition(NodeInfoEntity nodeInfoEntity) {
        List<AvailCabinetEntity> availCabinetEntities = this.selectCab(nodeInfoEntity.getUuid(), nodeInfoEntity.getuSize());
        for (AvailCabinetEntity entity : availCabinetEntities) {
            for (NodeCabinetEntity cab : entity.getCabinets()) {
                if (cab.getId() - nodeInfoEntity.getCabinetId() == 0) {
                    return cab.getStarts();
                }
            }
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
            NodeInfoEntity updateNodeInfo = this.getOne(new QueryWrapper<NodeInfoEntity>().eq("uuid", uuid));
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

                if (cabinetId == cab.getId()) { // 修改时节点的所属的机柜  应还回节点

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
                        continue;
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
            retAvailCabs.add(availCab);
        }

        return retAvailCabs;
    }


    /**
     * @param column 查询的列
     * @param value  该列的值
     * @return
     */
    public boolean checkFieldExists(String column, String value) {
        int count = count(new QueryWrapper<NodeInfoEntity>().eq(column, value).ne("state", 3));// 名称校验不考虑删除的节点名
        return count <= 0;
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
        page.setRecords(queryNodeBySystemId);
        return new PageUtils(page);
    }
}