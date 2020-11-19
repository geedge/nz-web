package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.alert.entity.AlertNotificationScript;

import java.util.List;
import java.util.Map;

public interface AlertNotifiScriptService extends IService<AlertNotificationScript> {

    List<AlertNotificationScript> queryList(Map<String, Object> params);

    void saveOrUpdateNotifiScript(AlertNotificationScript alertNotificationScript);

    void deleteScripts(Integer[] ids);
}

