package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 数据中心    dao
 *
 * @author shizhendong
 * @date 2021-4-6
 */
@Mapper
@Repository
public interface DcDao extends BaseMapper<Dc> {

    List<Dc> queryDcList(IPage page, @Param("params") Map<String, Object> params);

    List<MonitorProject> queryProjectListByDcId(@Param("dcId") Integer dcId);

    List<MonitorModule> queryModuleListByDcId(@Param("dcId") Integer dcId);

    List<MonitorEndpoint> queryEndpointListByDcId(@Param("dcId") Integer dcId);
}
