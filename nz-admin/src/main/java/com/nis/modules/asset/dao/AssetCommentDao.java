package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.AssetComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 问题管理    dao
 */
@Mapper
@Repository
public interface AssetCommentDao extends BaseMapper<AssetComment> {

    List<AssetComment> queryCommentList(IPage page, @Param("params") Map<String, Object> params);
}
