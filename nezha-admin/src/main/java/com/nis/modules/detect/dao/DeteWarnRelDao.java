package com.nis.modules.detect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.detect.entity.DeteWarnRelEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeteWarnRelDao extends BaseMapper<DeteWarnRelEntity> {
    DeteWarnRelEntity queryWarnRel(@Param("warnId")Long warnId);

    List<DeteWarnRelEntity> queryByTypeId(@Param("typeId")Long typeId);
}
