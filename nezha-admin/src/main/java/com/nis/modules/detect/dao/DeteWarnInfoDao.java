package com.nis.modules.detect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.detect.entity.DeteWarnInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeteWarnInfoDao extends BaseMapper<DeteWarnInfoEntity> {
    List<DeteWarnInfoEntity> queryList(@Param("typeId")Long typeId,@Param("setId") Long setId,@Param("name") String name,@Param("user") Long userId
            ,@Param("system") Long currSystemId,@Param("sidx") String sidx,@Param("order") String order,@Param("start") int start,@Param("pageSize") Integer pageSize);

    int queryCount(@Param("typeId")Long typeId,@Param("setId") Long setId,@Param("name") String name,@Param("user") Long userId
            ,@Param("system") Long currSystemId);


}
