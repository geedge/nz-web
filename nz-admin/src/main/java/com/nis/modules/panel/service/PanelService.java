package com.nis.modules.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.panel.entity.Panel;

import java.util.Map;

public interface PanelService extends IService<Panel> {
    PageUtils queryPanels(Map<String, Object> params);

    void delPanels(Integer... ids);

    boolean validatePanelName(Integer id, String name);

    void saveOrUpdatePanel(Panel panel);
}
