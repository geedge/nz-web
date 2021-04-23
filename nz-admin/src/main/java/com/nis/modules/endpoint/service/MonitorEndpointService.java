package com.nis.modules.endpoint.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.project.entity.MonitorProject;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface MonitorEndpointService extends IService<MonitorEndpoint> {

    MonitorEndpoint queryEndpointEntity(Integer id);

    PageUtils queryPage(Map<String, Object> params);

    List<MonitorProject> queryEndpointTree(String name);

    void saveOrUpdateBatchEndpointList(List<MonitorEndpoint> endpointList);

    void deleteEndpointByIds(List<Integer> ids);

    void getExportTemplateByLanguage(HttpServletResponse response, String language);

    void cancelImport(String seq);

    Map importEndpoint(MultipartFile excelFile, String language);

    void export(HttpServletResponse response, Map<String, Object> params);

    List<Integer> getSuspendedEndpointIdList(Integer assetId);


    /**
     * agent 组织配置文件所需要的 endpoint 信息,包含 asset module project dc 等
     * 目前获取的是启用状态的 endpoint
     * @return
     */
    List<MonitorEndpoint> getEnableEndpointInfoList();

    List<Map<String,Object>> render(List<Map<String,Object>> params);

}
