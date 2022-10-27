package com.nis.modules.panel.service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public interface VisualService {

    String getTimeStep(Long start, Long end);

    void exploreSnapshot(HttpServletResponse response, String format, Integer type, Long start, Long end, String language, Integer unit, String limit, String[] expressions) throws IOException;

    Map<String, Object> snapshotResultMap(Long start, Long end, String language);

    String replaceSnapshotTemplate(Map<String, Object> resultMap);

    String createSnapshotTask(String format, Integer panelId, Long start, Long end, String varsJsonStr);

    Map snapshotResultProcess(String taskId);

    void cancelSnapshotTask(String taskId);

    void downloadSnapshotFile(HttpServletResponse response, String language, String taskId);

}
