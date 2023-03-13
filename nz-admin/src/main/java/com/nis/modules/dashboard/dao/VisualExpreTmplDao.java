package com.nis.modules.dashboard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.dashboard.entity.VisualExpressionTmpl;
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
public interface VisualExpreTmplDao extends BaseMapper<VisualExpressionTmpl> {
}
