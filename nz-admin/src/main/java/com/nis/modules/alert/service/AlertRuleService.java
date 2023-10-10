package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertRuleEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
public interface AlertRuleService extends IService<AlertRuleEntity> {

    PageUtils queryPage(Map<String, Object> params);

	void validateBulitinRule(List<Integer> id);

	Integer saveOrUpdateAlertRule(AlertRuleEntity alertRule);

	void delete(Integer[] ids);

	void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException;

	Map importAlertRuleData(MultipartFile excelFile, Map<String, Object> params) throws IOException;

	void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

	void cancelImport(String seq);
	
	AlertRuleEntity queryAlertRule(Integer id);

	/**
	 * 任务是否在执行周期内
	 *
	 * @param alertRule
	 * @return
	 */
	boolean isTaskInSched(AlertRuleEntity alertRule);

}

