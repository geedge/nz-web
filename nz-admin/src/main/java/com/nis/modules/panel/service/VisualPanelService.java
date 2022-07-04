package com.nis.modules.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.panel.entity.VisualPanel;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface VisualPanelService extends IService<VisualPanel> {
	VisualPanel queryInfo(Integer id);
	
    void delPanels(Integer... ids);

    void modify(List<VisualPanel> visualPanelList);

    List<VisualPanel> queryList(Map<String, Object> params);

    void delPanelsAndChartInfoByPanelIds(Set<Integer> visualPanelIds);

    List<VisualPanel> getSequentialPanelList(List<VisualPanel> visualPanels);

    void handlePositionAfterDelete(List<VisualPanel> sequentialVisualPanelanelList, List<Integer> deleteIds);

    void saveVisualPanel(VisualPanel visualPanel);

    void updateVisualPanel(VisualPanel visualPanel);
    
    void getTemplateByLanguage(HttpServletResponse response, String format, String type) throws IOException;

    void cancelImport(String seq);

    void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

    Map importChartData(MultipartFile excelFile, String type, Integer linkId, String language) throws IOException;

    void duplicate(Integer id);

    void snapshot(HttpServletRequest request, HttpServletResponse response, String format, Integer panelId, Long start, Long end, String language);

    String snapshot(String format, Integer panelId, Long start, Long end, String language);
}
