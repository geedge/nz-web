package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

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

    /**
     * 高级搜索 -> keyword 查询，返回 name list
     *      支持分页
     *      按 name 模糊搜索
     *      支持 asset project module endpoint dc rule
     * @param page
     * @param params
     * @return
     */
    List<String> selectKeywordsByTableName(IPage page, Map<String, Object> params);

}
