package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.protobuf.Types.TimeSeries;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.RecordRuleEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
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
	 * 存放 record data 到 redis 需要额外线程消费队列中的内容
	 * 
	 * @param recordData
	 */
	void rightPushRecordToRedis(List<TimeSeries> timeSeriesList);

	void rightPushRecordToRedis(TimeSeries timeSeries);

	void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException;

	void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

	Map importData(MultipartFile excelFile, Map<String, Object> params) throws IOException;
}
