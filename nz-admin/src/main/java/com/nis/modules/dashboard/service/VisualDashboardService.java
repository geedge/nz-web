package com.nis.modules.dashboard.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.dashboard.entity.VisualDashboard;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface VisualDashboardService extends IService<VisualDashboard> {

    VisualDashboard queryInfo(Integer id);

    void delDashboards(Integer... ids);

    void modify(List<VisualDashboard> visualDashboardList);

    PageUtils queryList(Map<String, Object> params);

    void delDashboardsAndChartInfoByDashboardIds(Set<Integer> dashboardIds);

    void saveVisualDashboard(VisualDashboard visualDashboard);

    void updateVisualDashboard(VisualDashboard visualDashboard);

    void getTemplateByLanguage(HttpServletResponse response, String format, String type) throws IOException;

    void cancelImport(String seq);

    void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

    Map importChartData(MultipartFile excelFile, Map<String, Object> params) throws IOException;

    void duplicate(Integer id);

    void snapshot(HttpServletResponse response, String format, Integer dashboardId, Long start, Long end, String language, String vars) throws IOException;

    String genSnapshotByTemplate(Integer dashboardId, Long start, Long end, String lang, String vars);

    List<VisualDashboard> getTemplateSyncDashboard(Integer dashboardId, Integer tmplId, Integer modelId, Integer moduleId);

    VisualDashboard queryDashboardIncludeChartInfo(Integer dashboardId);

    void addReportConfigInCache(VisualDashboard dashboard);

    void updateReportConfigInCache(VisualDashboard oldDashboard, VisualDashboard newDashboard);

    // template dashboard , add assetInfo or endpointInfo chart
    void addLinkObjectInfoChart(VisualDashboard dashboard);
}
