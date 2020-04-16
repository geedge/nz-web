package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertNameMapUserDTO;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.sys.entity.SysUserEntity;

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

	/**
	 * @return true:可用;false:不可用
	 */
	boolean checkName(AlertRuleEntity queryParam);
	

	//void validateTypeLink(Integer type, Integer linkId);

	List<AlertNameMapUserDTO> queryUserByRuleId(Set<Integer> ruleIds);

	//void removeByTypeIds(Integer typeId,List ids);
	
	void validateBulitinRule(List<Integer> id);
}

