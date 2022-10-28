package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertNotifyMethod;

import java.util.List;
import java.util.Map;

public interface AlertNotifyMethodService extends IService<AlertNotifyMethod> {
    boolean isExist(String methods);

    PageUtils queryPage(Map<String, Object> params);

    void validateNotifyMethod(AlertNotifyMethod alertNotifyMethod,Boolean isModify);

    void deleteByIds(List<String> idList);
}
