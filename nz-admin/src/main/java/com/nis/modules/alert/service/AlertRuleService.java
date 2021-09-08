package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertRuleEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
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


	void getExportTemplateByLanguage(HttpServletResponse response, String language);

	Map importAlertFromExcelFile(MultipartFile excelFile, String language);

	void export(HttpServletResponse response, Map<String, Object> params);

	void cancelImport(String seq);
	
	AlertRuleEntity queryAlertRule(Integer id);
}

