package com.nis.modules.endpoint.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MonitorEndpointDao extends BaseMapper<MonitorEndpoint> {

    List<MonitorEndpoint> queryPage(IPage page, @Param("params") Map<String, Object> params);

    List<Integer> getSuspendedEndpointIdList(@Param("assetId")Integer assetId);

    /**
     * agent 组织配置文件所需要的 endpoint 信息,包含 asset module project dc 等
     * 目前获取的是启用状态的 endpoint
     * @return
     */
    List<MonitorEndpoint> getEnableMetricsEndpointInfoListForAgent();

    List<MonitorEndpoint> endpointDetailList();

    /**
     * agent logs 组织配置文件所需要的 endpoint 信息,包含 asset module project dc 等
     * 目前获取的是启用状态的 endpoint
     * @return
     */
    List<MonitorEndpoint> getEnableLogsEndpointInfoListForAgent();
}
