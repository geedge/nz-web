package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.protobuf.Types.TimeSeries;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.RecordRuleEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface RecordRuleService extends IService<RecordRuleEntity> {

    RecordRuleEntity queryRecordRule(Integer id);

    PageUtils list(Map<String, Object> params);

    int saveRecordRule(RecordRuleEntity recordRule);

    int updateRecordRule(RecordRuleEntity recordRule);

    void deleteRecordRule(Integer[] ids);

	/**
	 * 存放 record data 到本地 cache 需要额外线程消费队列中的内容
	 * 
	 * @param recordData
	 */
	void rightPushRecordToCache(List<TimeSeries> timeSeriesList);

	void rightPushRecordToCache(TimeSeries timeSeries);

	void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException;

	void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

	Map importData(MultipartFile excelFile, Map<String, Object> params) throws IOException;
}
