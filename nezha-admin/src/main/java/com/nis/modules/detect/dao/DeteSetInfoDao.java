package com.nis.modules.detect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.detect.entity.DeteSetInfoEntity;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.entity.UserGroupEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 监测设置信息表
 * 
 * @author nezha
 * @date 2019-07-25 16:35:48
 */
@Mapper
public interface DeteSetInfoDao extends BaseMapper<DeteSetInfoEntity> {
	
	List<DeteSetInfoEntity> queryByPermission(PermissionInfoEntity entity);

    List<NodeGroupInfoEntity> queryNodeGroups(@Param("setId")Long id);

    List<NodeInfoEntity> queryNodes(@Param("setId")Long id);

    DeteSetInfoEntity queryById(@Param("id")Long id);

    List<UserGroupEntity> queryUserGroup(@Param("setId")Long id);

    List<SystemInfoEntity> querySystem(@Param("setId")Long id);


    IPage<DeteSetInfoEntity> queryList(IPage<DeteSetInfoEntity> page, @Param("name") String name, @Param("typeId") Long typeId,@Param("isGlobal") Integer isGlobal, @Param("sysPermissionInfo")PermissionInfoEntity syspermissionInfo,@Param("permissionInfoVL3") List<PermissionInfoEntity> permissionInfoVL3);
    List<Long> queryAllowedIds(@Param("isGlobal") Integer isGlobal, @Param("sysPermissionInfo")PermissionInfoEntity syspermissionInfo,@Param("permissionInfoVL1") List<PermissionInfoEntity> permissionInfoVL1);
}
