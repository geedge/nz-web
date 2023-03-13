package com.nis.modules.dashboard.service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public interface VisualService {

    String getTimeStep(Long start, Long end);

    void exploreSnapshot(HttpServletResponse response, String format, Integer type, Long start, Long end, String language, Integer unit, String limit, String direction, String[] expressions) throws IOException;

    Map<String, Object> snapshotResultMap(Long start, Long end, String language);

    String replaceSnapshotTemplate(Map<String, Object> resultMap);

    String createSnapshotTask(String format, Integer dashboardId, Long start, Long end, String varsJsonStr);

    Map snapshotResultProcess(String taskId);

    void cancelSnapshotTask(String taskId);

    void downloadSnapshotFile(HttpServletResponse response, String language, String taskId);

    void syncTemplateDashboard(Integer id, Integer tmplId, Integer modelId, Integer moduleId);

    Integer createDashboardUsingTemplate(String name, String type, String description, Integer tmplId, Integer link);
}
