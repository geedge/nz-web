package com.nis.modules.detect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.detect.entity.DeteWarnInfoEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeteWarnInfoDao extends BaseMapper<DeteWarnInfoEntity> {

    IPage<DeteWarnInfoEntity> queryList(IPage<DeteWarnInfoEntity> page,@Param("typeId") Long typeId,@Param("setId") Long setId,@Param("name") String name,@Param("isGlobal") Integer isGlobal, PermissionInfoEntity sysPermissionInfo, List<PermissionInfoEntity> permissionInfoVL3);
}
