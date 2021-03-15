package com.nis.modules.panel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.panel.entity.ExpressionTmpl;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 表达式模板 dao
 *
 * @author shizhendong
 * @date 2021-03-11
 */
@Mapper
@Repository
public interface ExpreTmplDao extends BaseMapper<ExpressionTmpl> {
}
