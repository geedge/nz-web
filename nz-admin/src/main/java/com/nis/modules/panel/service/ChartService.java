package com.nis.modules.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.panel.entity.Chart;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface ChartService extends IService<Chart> {
    List<Chart> queryCharts(Integer panelId, Integer id, String title);

    void validateChart(Integer panelId, Chart chart);

    void saveChart(Integer panelId,Chart chart);

    void updateChart(Integer panelId, Chart chart);

    void delCharts(Integer panelId, Integer[] ids, Integer sync);

    void delChartsByPanelId(Integer... panelId);

    void modify(Integer panelId, Chart chart);
    
    Chart duplicate(Integer panelId,Integer chartId);

    void saveAssetPanelAndChart(Asset asset);

    void updateAssetChartsByNewModel(Asset asset);

    void updateAssetChartsThroughRelatedModel(Asset asset);

    void updateuAllAssetChartsUnderCurrentModel(Integer modelId, List<Asset> assets);

    void getExportTemplateByLanguage(HttpServletResponse response, String language);

    void cancelImport(String seq);

    void export(HttpServletResponse response, Map<String, Object> params);

    Map importChartFromExcelFile(MultipartFile excelFile);
}
