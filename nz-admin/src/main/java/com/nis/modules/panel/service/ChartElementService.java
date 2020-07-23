package com.nis.modules.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.panel.entity.ChartElement;

import java.util.List;

public interface ChartElementService extends IService<ChartElement> {

    void validateElements(List<ChartElement> elements);

    void delElementByChartIds(List<Integer> ids);

    void saveOrUpdateElements(Integer chartId, List<ChartElement> chartElements);
}
