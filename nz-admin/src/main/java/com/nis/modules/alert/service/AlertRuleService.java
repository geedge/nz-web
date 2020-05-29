package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertNameMapUserDTO;
import com.nis.modules.alert.entity.AlertRuleEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 *
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
public interface AlertRuleService extends IService<AlertRuleEntity> {

    PageUtils queryPage(Map<String, Object> params);

	List<AlertNameMapUserDTO> queryUserByRuleId(Set<Integer> ruleIds);

	void validateBulitinRule(List<Integer> id);

	void saveOrUpdateAlertRule(AlertRuleEntity alertRule);

	void delete(Integer[] ids);
}

