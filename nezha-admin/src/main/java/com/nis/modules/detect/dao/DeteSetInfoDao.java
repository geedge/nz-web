package com.nis.modules.detect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.detect.entity.DeteSetInfoEntity;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;
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

    List<DeteSetInfoEntity> queryList(@Param("name") String name, @Param("typeId") Long typeId
            , @Param("start") Integer start, @Param("pageSize") Integer pageSize, @Param("user") Long userId
            , @Param("system") Long system,@Param("sidx") String sidx,@Param("order") String order);

    int queryCount(@Param("name") String name, @Param("typeId") Long typeId
            , @Param("start") Integer start, @Param("pageSize") Integer pageSize, @Param("user") Long userId
            , @Param("system") Long system, @Param("sidx") String sidx,@Param("order") String order);
    List<NodeGroupInfoEntity> queryNodeGroups(@Param("setId")Long id);

    List<NodeInfoEntity> queryNodes(@Param("setId")Long id);

    DeteSetInfoEntity queryById(@Param("id")Long id);
}
