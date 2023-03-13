package com.nis.modules.dashboard.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.dashboard.entity.VisualChartElement;

import java.util.List;

public interface VisualChartElementService extends IService<VisualChartElement> {

    void validateElements(List<VisualChartElement> elements);

    void delElementByChartIds(List<Integer> ids);

    void saveOrUpdateElements(Integer chartId, List<VisualChartElement> chartElements);
}
