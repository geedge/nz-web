package com.nis.modules.overview.dao;

import com.nis.modules.overview.entity.EndpointStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface OverviewDao {

    // 关联并且type = per prometheus status 为可用的 prometheus 关联 dc的名称集合
    List<String> getIdcNameByPerPromeServer();

    List<EndpointStat> getEndpointInfoByDc();
}