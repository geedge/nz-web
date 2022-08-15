package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface BasicMapper extends BaseMapper {

    /**
     * 指定表名、字段名、字段值
     * SELECT id,NAME FROM ${tableName} WHERE ${col} = #{value}
     *
     * @param tableName
     * @param col
     * @param value
     * @return
     */
    List<Object> selectList(@Param("tableName") String tableName, @Param("col") String col, @Param("value") String value);

}
