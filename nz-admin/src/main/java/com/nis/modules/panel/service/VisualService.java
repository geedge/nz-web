package com.nis.modules.panel.service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public interface VisualService {

    String getTimeStep(Long start, Long end);

    void exploreSnapshot(HttpServletResponse response, String format, Integer type, Long start, Long end, String language, Integer unit, String limit, String direction, String[] expressions) throws IOException;

    Map<String, Object> snapshotResultMap(Long start, Long end, String language);

    String replaceSnapshotTemplate(Map<String, Object> resultMap);
}
