package com.nis.modules.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.panel.entity.Chart;

import java.util.List;

public interface ChartService extends IService<Chart> {
    List<Chart> queryCharts(Integer panelId, Integer id, String title);

    void saveChart(Chart chart);

    void validateChart(Integer panelId, Chart chart);

    void updateChart(Chart chart);

    void validatePanel(Integer panelId);

    void delCharts(Integer panelId, Integer[] ids, Integer sync);

    void delChartsByPanelId(Integer... panelId);

    void modify(Integer panelId, Chart chart);
    
    Chart duplicate(Integer panelId,Integer chartId);

    void saveAssetPanelAndChart(Asset asset);

    void updateAssetChartsByNewModel(Asset asset);
}
