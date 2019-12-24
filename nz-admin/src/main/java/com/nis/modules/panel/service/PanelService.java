package com.nis.modules.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.panel.entity.Panel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

public interface PanelService extends IService<Panel> {
    PageUtils queryPanels(Map<String, Object> params);

    void delPanels(Integer... ids);

}
