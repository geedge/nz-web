package com.nis.modules.dashboard.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.dashboard.entity.VisualChart;

import java.util.List;
import java.util.Map;

public interface VisualChartService extends IService<VisualChart> {

    PageUtils queryPage(Map<String, Object> params);

    VisualChart queryInfo(Integer id);

    Integer saveChart(VisualChart chart);

    Integer updateChart(VisualChart chart);

    void delCharts(List<Integer> idList, Integer sync);

    void modify(VisualChart chart);

    void weights(Integer dashboardId, List<VisualChart> chartList);

    Integer chartTmplSynch(Integer pid, List<Integer> chartIdList);

    void saveRelDashboardAndChart(String type, Integer linkId, String name, Integer tempId, String seq);

    /**
     * 平铺 获取所有 chart 节点
     *
     * @param sourceChartList
     * @return
     */
    List<VisualChart> getAllChartList(List<VisualChart> sourceChartList);

}
