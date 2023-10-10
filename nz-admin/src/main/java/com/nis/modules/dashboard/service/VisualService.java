package com.nis.modules.dashboard.service;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public interface VisualService {

    String getTimeStep(Long start, Long end);

    void exploreSnapshot(HttpServletResponse response, String format, Integer type, Long start, Long end, String language, Integer unit, String limit, String direction, String[] expressions) throws IOException;

    Map<String, Object> snapshotResultMap(Long start, Long end, String language);

    String replaceSnapshotTemplate(Map<String, Object> resultMap);

    String createDashboardSnapshotTask(String format, Integer dashboardId, Long start, Long end, String varsJsonStr);

    String createNotebookSnapshotTask(String format, Integer notebookId, Long start, Long end);

    Map querySnapshotResultProcess(String type, String taskId);

    void cancelSnapshotTask(String type, String taskId);

    void downloadSnapshotFile(HttpServletResponse response, String type, String language, String taskId);

    void syncTemplateDashboard(Integer id, Integer tmplId, Integer modelId, Integer moduleId);

    Integer createDashboardUsingTemplate(String name, String type, String description, Integer tmplId, Integer link);
}
