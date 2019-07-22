package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.modules.node.dao.NodegroupInfoDao;
import com.nis.modules.node.entity.NodeGroupRelEntity;
import com.nis.modules.node.entity.NodegroupInfoEntity;
import com.nis.modules.node.entity.SystemNodeGroupEntity;
import com.nis.modules.node.service.NodegroupInfoService;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.service.impl.SystemInfoServiceImpl;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
public class NodegroupInfoServiceImpl extends ServiceImpl<NodegroupInfoDao, NodegroupInfoEntity> implements NodegroupInfoService {

    @Autowired
    private NodegroupInfoDao nodegroupInfoDao;

    @Autowired
    private SystemInfoServiceImpl systemInfoService;

    @Autowired
    private NodeGroupRelServiceImpl nodeGroupRelService;

    /**
     * 涉及权限   global下列出所有业务系统和其下节点组，普通业务系统只列出自身的节点组；
     *
     * @return
     */
    @Override
    public List<SystemNodeGroupEntity> getSysNodeGroups() {
        // 进行权限判断
        if (ShiroUtils.isGlobal()) {// global 权限 看到所有的节点组信息
            List<SystemNodeGroupEntity> list = new ArrayList<>();
            // 1。 得到系统业务集合
            List<SystemInfoEntity> systemInfoList = systemInfoService.list(new QueryWrapper<SystemInfoEntity>().eq("del_flag", 0));
            // 2. 根据系统业务集合得到包含的节点组集合
            for (SystemInfoEntity item : systemInfoList) {
                List<SystemNodeGroupEntity> sysNodeGroupsBySystemId = getSysNodeGroupsBySystemId(item);
                list.addAll(sysNodeGroupsBySystemId);
            }
            return list;
        } else { // 其他权限登录 只能看到自己权限下的节点组信息
            SystemInfoEntity systemInfoEntity = systemInfoService.getById(ShiroUtils.getCurrSystemId());
            return getSysNodeGroupsBySystemId(systemInfoEntity);
        }
    }

    //根据传入的系统权限角色查询角色包含的节点组信息
    public List<SystemNodeGroupEntity> getSysNodeGroupsBySystemId(SystemInfoEntity systemInfo) {
        List<SystemNodeGroupEntity> list = new ArrayList<>();
        List<NodegroupInfoEntity> nodegroupInfoList = this.list(new QueryWrapper<NodegroupInfoEntity>().eq("system_id", systemInfo.getId()));
        for (NodegroupInfoEntity entity : nodegroupInfoList) {
            // 2.1 能否分配节点 0不能，1可以     必须是叶子节点才能分配节点
            if (this.count(new QueryWrapper<NodegroupInfoEntity>().eq("pid", entity.getId())) < 1) {//证明自己是叶子节点
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
        list.add(new SystemNodeGroupEntity(systemInfo.getId(), systemInfo.getName(), nodegroupInfoList));
        return list;
    }

    @Override
    public NodegroupInfoEntity detail(Integer id) {
        // 1.根据ID查到自己
        NodegroupInfoEntity nodegroup = this.getById(id);
        // 2.根据得到的pid 查到上级对象
        NodegroupInfoEntity parentNodeGroup = this.getById(nodegroup.getPid());
        if (parentNodeGroup == null) { // 有特殊的父级对象没有  例如父级ID为0的对象 因为没有ID为0的元素
            nodegroup.setPname("");
        } else {
            // 3.将上级的name 赋值给返回对象
            nodegroup.setPname(parentNodeGroup.getName());
        }
        return nodegroup;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNodeGroupInfo(NodegroupInfoEntity nodegroupInfo) {
        if (ShiroUtils.isGlobal()) {
            if (nodegroupInfo.getSystemId() == null) {
                nodegroupInfo.setSystemId(ShiroUtils.getCurrSystemId());
            }
        } else {
            nodegroupInfo.setSystemId(ShiroUtils.getCurrSystemId());// 不是gelob 全选 添加就是当前的
        }
        NodegroupInfoEntity parentEntity = getById(nodegroupInfo.getPid());
        nodegroupInfo.setOperator(1l);
        nodegroupInfo.setOpTime(new Date());
        nodegroupInfo.setPids(parentEntity.getPids() + nodegroupInfo.getPid() + ",");
        if (groupNameCheck(null, nodegroupInfo.getName(), nodegroupInfo.getPid())) {
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
     * @param nodegroupInfo
     */
    @Override
    public void updateNodeGroupInfo(NodegroupInfoEntity nodegroupInfo) {

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
            return getCount(name, pid);
        } else {// 修改
            NodegroupInfoEntity nodegroupInfo = getById(id);
            if (name.equals(nodegroupInfo.getName())) {
                return true;
            } else {
                return getCount(name, pid);
            }
        }
    }


    public boolean getCount(String name, Long pid) {
        int count = count(new QueryWrapper<NodegroupInfoEntity>().eq("pid", pid).eq("name", name));
        if (count > 0) {
            return false;
        }
        return true;
    }
}