package com.nis.modules.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.panel.entity.ChartElement;
import org.springframework.stereotype.Service;

import java.util.List;

public interface ChartElementService extends IService<ChartElement> {

    void validateElements(Integer chartId, List<ChartElement> elements);

    void delElementByChartIds(List<Integer> ids);

}
