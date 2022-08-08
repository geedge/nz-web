package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.RecordRuleEntity;

import java.util.Map;

public interface RecordRuleService extends IService<RecordRuleEntity> {

    RecordRuleEntity queryRecordRule(Integer id);

    PageUtils list(Map<String, Object> params);

    int saveRecordRule(RecordRuleEntity recordRule);

    int updateRecordRule(RecordRuleEntity recordRule);

    void deleteRecordRule(Integer[] ids);
}
