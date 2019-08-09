package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.node.dao.NodeGroupInfoDao;
import com.nis.modules.node.dao.NodeGroupRelDao;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeGroupRelEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.node.service.NodeGroupInfoService;
import com.nis.modules.sys.dao.SystemInfoDao;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("nodeGroupInfoService")
public class NodeGroupInfoServiceImpl extends ServiceImpl<NodeGroupInfoDao, NodeGroupInfoEntity> implements NodeGroupInfoService {

    // 节点组 dao
    @Autowired
    private NodeGroupInfoDao nodegroupInfoDao;

    // 节点组 节点 关联 service
    @Autowired
    private NodeGroupRelServiceImpl nodeGroupRelService;

    // 节点 节点组 关联 dao
    @Autowired
    private NodeGroupRelDao nodeGroupRelDao;

    @Autowired
    private SystemInfoDao systemInfoDao;

    /**
     * 涉及权限   global下列出所有业务系统和其下节点组，普通业务系统只列出自身的节点组；
     *
     * @return
     */
    @Override
    public List<SystemInfoEntity> getSysNodeGroups() {
        // 进行权限判断
        if (ShiroUtils.isGlobal()) {// global 权限 看到所有的节点组信息
            List<SystemInfoEntity> systemInfoEntities = systemInfoDao.querySystemInfosWithNodeGroup(null);
            for (SystemInfoEntity systemInfoEntity : systemInfoEntities) {
                handleEntity(systemInfoEntity.getNodegroups());
            }
            return systemInfoEntities;
        } else { // 其他权限登录 只能看到自己权限下的节点组信息
            List<SystemInfoEntity> systemInfoEntities = systemInfoDao.querySystemInfosWithNodeGroup(ShiroUtils.getCurrSystemId());
            handleEntity(systemInfoEntities.get(0).getNodegroups());
            return systemInfoEntities;
        }
    }

    public void handleEntity(List<NodeGroupInfoEntity> nodegroups) {
        for (NodeGroupInfoEntity entity : nodegroups) {
            if (this.count(new QueryWrapper<NodeGroupInfoEntity>().eq("pid", entity.getId())) < 1) {//证明自己是叶子节点
                entity.setAllowAss("1");
            } else {
                entity.setAllowAss("0");
            }
            //2.2 能否增加子节点组 0不能，1可以  只要跟自己没有直接关联的节点就可以
            if (nodeGroupRelService.count(new QueryWrapper<NodeGroupRelEntity>().eq("group_id", entity.getId())) < 1) {//此节点组下没有关联的节点
                entity.setAllowSub("1");
            } else {
                entity.setAllowSub("0");
            }
        }
    }

    @Override
    public NodeGroupInfoEntity detail(Integer id) {
        NodeGroupInfoEntity nodegroup = nodegroupInfoDao.detail(id);
        return nodegroup;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNodeGroupInfo(NodeGroupInfoEntity nodegroupInfo) {
        if (groupNameCheck(null, nodegroupInfo.getName(), nodegroupInfo.getPid())) {
            if (ShiroUtils.isGlobal()) {
                if (nodegroupInfo.getSystemId() == null) {
                    nodegroupInfo.setSystemId(ShiroUtils.getCurrSystemId());
                }
            } else {
                nodegroupInfo.setSystemId(ShiroUtils.getCurrSystemId());// 不是gelob 全选 添加就是当前的
            }
            NodeGroupInfoEntity parentEntity = this.getById(nodegroupInfo.getPid());
            nodegroupInfo.setOperator(ShiroUtils.getCurrSystemId());
            nodegroupInfo.setOpTime(new Date());
            nodegroupInfo.setPids(parentEntity.getPids() + nodegroupInfo.getPid() + ",");
            save(nodegroupInfo);
        } else {
            throw new NZException(RCode.NODEGROUP_NAME_DUPLICATE);//节点组名称重复
        }
    }

    /**
     * 节点组修改   只能修改节点组的名称
     * id       R
     * name     R
     * remark   O
     *
     * @param nodegroupInfo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNodeGroupInfo(NodeGroupInfoEntity nodegroupInfo) {
        // 1. 根据当前的ID查到当前数据库对象
        NodeGroupInfoEntity nodeGroup = getById(nodegroupInfo.getId());
        nodegroupInfo.setOperator(ShiroUtils.getCurrSystemId());
        // 2. 做名称校验
        if (nodeGroup.getName().equals(nodegroupInfo.getName())) {// 名称没有发生改变
            nodegroupInfoDao.updateById(nodegroupInfo);
        } else {
            if (checkNameExists(nodegroupInfo.getName(), nodeGroup.getPid())) {//  名称不重复
                nodegroupInfoDao.updateById(nodegroupInfo);
            } else {
                throw new NZException(RCode.NODEGROUP_NAME_DUPLICATE);// 节点组名称重复
            }
        }
    }

    /**
     * 删掉所有子节点组及其关联节点 逻辑删除
     *
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 1. 查询此节点组和包含的子节点组
        NodeGroupInfoEntity nodegroupInfo = getById(id);
        List<NodeGroupInfoEntity> list = list(new QueryWrapper<NodeGroupInfoEntity>().like("pids", nodegroupInfo.getPids() + nodegroupInfo.getId()));
        list.add(nodegroupInfo);

        // 2. 删除自身 和 子节点组
        List<Long> ids = list.stream().map(NodeGroupInfoEntity::getId).collect(Collectors.toList());
        nodegroupInfoDao.deleteBatchIds(ids);

        // 3. 删除关联节点
        nodeGroupRelDao.delete(new QueryWrapper<NodeGroupRelEntity>().in("group_id",ids));
    }

    /**
     * 移除对应关系
     *
     * @param id    R  节点组 ID
     * @param uuids R  节点UUIDS
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNodeRel(Long id, Long[] uuids) {
        nodeGroupRelDao.delete(new QueryWrapper<NodeGroupRelEntity>().in("uuid",uuids).eq("group_id", id));
    }

    /**
     * 建立关联关系前 先验证是否重复 重复直接跳过
     *
     * @param id    R  节点组 ID
     * @param uuids R  节点UUIDS
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNodeRel(Long id, Long[] uuids) {

        List<NodeGroupRelEntity> list = new ArrayList<>();

        for (Long uuid : uuids) {
            int count = nodeGroupRelService.count(new QueryWrapper<NodeGroupRelEntity>().eq("group_id", id).eq("uuid", uuid));
            if (count > 0) {
                continue;  // 重复跳过
            }
            list.add(new NodeGroupRelEntity(id, uuid, ShiroUtils.getCurrSystemId(),  new Date()));
        }

        // 在此处添加
        nodeGroupRelService.saveBatch(list);
    }

    /**
     * 查询可以分配给指定节点组的节点
     * page     O
     * limit    O
     * sidx     O
     * order    O
     * groupId  R
     * systemId R
     * ip O
     *
     * @param params
     */
    @Override
    public PageUtils assList(Map<String, Object> params) {
        IPage<NodeInfoEntity> page = new Query<NodeInfoEntity>().getPage(params);
        List<NodeInfoEntity> assNodeList = baseMapper.assList(page, params);
        // 此处暂未做 国际化 字典处理
        page.setRecords(assNodeList);
        return new PageUtils(page);
    }

    /**
     * 节点组名称校验，同父节点下名称不能重复
     *
     * @param id
     * @param name
     * @param pid
     */
    @Override
    public boolean groupNameCheck(Long id, String name, Long pid) {
        if (id == null) {// 添加
            return checkNameExists(name, pid);
        } else {// 修改
            NodeGroupInfoEntity nodegroupInfo = getById(id);
            if (name.equals(nodegroupInfo.getName())) {
                return true;
            } else {
                return checkNameExists(name, pid);
            }
        }
    }

    /**
     * pid下的名称 是否重复判断
     *
     * @param name
     * @param pid
     * @return
     */
    public boolean checkNameExists(String name, Long pid) {
        int count = count(new QueryWrapper<NodeGroupInfoEntity>().eq("pid", pid).eq("name", name));
        return count <= 0;
    }
}