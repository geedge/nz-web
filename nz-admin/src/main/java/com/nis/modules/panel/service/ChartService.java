package com.nis.modules.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.panel.entity.Chart;

import java.util.List;

public interface ChartService extends IService<Chart> {
    List<Chart> queryCharts(Integer panelId, Integer id, String title);

    void saveChart(Chart chart);

    void validateChart(Integer panelId, Chart chart);

    void updateChart(Chart chart);

    void validatePanel(Integer panelId);

    void delCharts(Integer[] ids);

    void delChartsByPanelId(Integer... panelId);
}
