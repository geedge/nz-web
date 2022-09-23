package com.nis.modules.endpoint.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.project.entity.MonitorProject;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MonitorEndpointService extends IService<MonitorEndpoint> {

    MonitorEndpoint queryEndpointEntity(Integer id);

    Map<String,Object> queryPage(Map<String, Object> params);

    List<MonitorProject> queryEndpointTree(String name);

    void deleteEndpointByIds(List<Integer> ids);

    void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException;

    void cancelImport(String seq);

    Map importEndpointData(MultipartFile excelFile, Map<String, Object> params) throws IOException;

    void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

    List<Integer> getSuspendedEndpointIdList(Integer assetId);

    // endpoint 列表，包含 asset dc 数据
    List<MonitorEndpoint> endpointDetailList();

    Integer saveEndpoint(MonitorEndpoint endpoint);

    Integer updateEndpoint(MonitorEndpoint endpoint);

    Map saveBatchEndpoint(List<MonitorEndpoint> endpointList);

    List<Map> render(Map<String, String> params);

    /**
     * agent metrics 组织配置文件所需要的 endpoint 信息,包含 asset module project dc 等
     * 目前获取的是启用状态的 endpoint
     * @return
     */
    List<MonitorEndpoint> getEnableMetricsEndpointInfoListForAgent();

    /**
     * agent logs 组织配置文件所需要的 endpoint 信息,包含 asset module project dc 等
     * 目前获取的是启用状态的 endpoint
     * @return
     */
    List<MonitorEndpoint> getEnableLogsEndpointInfoListForAgent(Integer version);
    
    Map<String,List> queryEndpointGroupBy(Map<String, Object> params);

    void syncTmpl(List<Integer> moduleIds, List<Integer> endpointIds, List<Integer> assetIds);

    void bulkEdit(Map<String, Object> params);

    Map<String, Object> diagnose(Integer id, HttpServletRequest request) throws IOException;
}
