package com.nis.modules.integration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.integration.entity.IntMetadataEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface IntMetadataDao extends BaseMapper<IntMetadataEntity> {

    List<IntMetadataEntity> queryList(IPage page, @Param("params") Map params);
}
