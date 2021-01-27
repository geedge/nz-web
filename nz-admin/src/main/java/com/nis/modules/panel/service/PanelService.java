package com.nis.modules.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.panel.entity.Panel;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PanelService extends IService<Panel> {
    void delPanels(Integer... ids);

    void modify(Panel panel);

    void modify(List<Panel> panelList);

    List<Panel> queryList(Map<String, Object> params);

    void delPanelsAndChartInfoByPanelIds(Set<Integer> panelIds);

    List<Panel> getSequentialPanelList(List<Panel> panels);

    void handlePositionAfterDelete(List<Panel> sequentialPanelList, List<Integer> deleteIds);

    void savePanel(Panel panel);

    void updatePanel(Panel panel);
}
