package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertSeverityConf;

import java.util.List;
import java.util.Map;

public interface AlertSeverityService extends IService<AlertSeverityConf> {

    List<AlertSeverityConf> queryList(Map<String, Object> params);
    /**
     * 根据条件查询 severityConf
     * @param params
     * @return {severityId:severityConf, ...}
     */
    Map<Integer,AlertSeverityConf> queryMap(Map<String, Object> params);
    
    void validateSeverity(AlertSeverityConf alertSeverityConf, boolean isModify);

    void setWeight(AlertSeverityConf alertSeverityConf);

    void saveWeights(List<AlertSeverityConf> alertSeverityConfs);

    AlertSeverityConf getSeverityById(Integer id);

    void saveSeverity(AlertSeverityConf alertSeverityConf);

    void updateSeverity(AlertSeverityConf alertSeverityConf);

    PageUtils queryPage(Map<String, Object> params);
}
