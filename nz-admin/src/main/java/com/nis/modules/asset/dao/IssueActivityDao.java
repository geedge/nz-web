package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.IssueActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 问题动态管理    dao
 */
@Mapper
@Repository
public interface IssueActivityDao extends BaseMapper<IssueActivity> {

    List<IssueActivity> queryIssueActivityList(IPage page, @Param("params") Map<String, Object> params);
}
