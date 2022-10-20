package com.nis.modules.alert.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.protobuf.Types.TimeSeries;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.RecordRuleDao;
import com.nis.modules.alert.entity.RecordRuleEntity;
import com.nis.modules.alert.entity.RecordRuleEnum;
import com.nis.modules.alert.service.RecordRuleService;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecordRuleServiceImpl extends ServiceImpl<RecordRuleDao, RecordRuleEntity> implements RecordRuleService {
	private static final Log log = Log.get();

	@Autowired
	private SysUserStarredService starredService;

	@Autowired
	private SysUserDao sysUserDao;

	@Autowired
	private RecordRuleDao recordRuleDao;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired
	private SysConfigService configService;

	@Autowired
	private BasicImportAndExportServices basicImportAndExportServices;

	/**
	 * record list max size
	 */
	@Value("${redis.maxRecordDataSize:100000}")
	private Long maxRecordSize;

	@Override
	public RecordRuleEntity queryRecordRule(Integer id) {
		RecordRuleEntity recordRuleEntity = this.getById(id);

		if (recordRuleEntity.getCid() != null) {
			SysUserEntity userEntity = sysUserDao.selectById(recordRuleEntity.getCid());
			recordRuleEntity.setCreateUser(userEntity);
		}

		if (recordRuleEntity.getUid() != null) {
			SysUserEntity userEntity = sysUserDao.selectById(recordRuleEntity.getUid());
			recordRuleEntity.setUpdateUser(userEntity);
		}

		/// 收藏标识 0：未收藏 1：已收藏
		int count = starredService.count(new LambdaQueryWrapper<SysUserStarredEntity>()
				.eq(SysUserStarredEntity::getTid, recordRuleEntity.getId())
				.eq(SysUserStarredEntity::getType, "recordRule")
				.eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId()));
		recordRuleEntity.setStarred(count == 0 ? 0 : 1);
		return recordRuleEntity;
	}

	@Override
	public PageUtils list(Map<String, Object> params) {

		String ids = (String) params.get("ids");
		if (StrUtil.isNotBlank(ids)) {
			List<String> listIds = Arrays.asList(ids.split(","));
			params.put("ids", listIds);
		}
		params.put("userId", ShiroUtils.getUserId());
		IPage page = new Query(RecordRuleEntity.class).getPage(params);
		List<RecordRuleEntity> recordRules = recordRuleDao.queryList(page, params);

		page.setRecords(recordRules);
		return new PageUtils(page);
	}

	private void validateParam(RecordRuleEntity recordRule) {

		ValidateUtils.is(recordRule.getName()).notNull(RCode.RECORD_RULE_NAME_ISNULL).and(recordRule.getType())
				.notNull(RCode.RECORD_RULE_TYPE_ISNULL).and(recordRule.getExpr()).notNull(RCode.RECORD_RULE_EXPR_ISNULL)
				.and(recordRule.getLabels()).notNull(RCode.RECORD_RULE_LABELS_ISNULL).and(recordRule.getState())
				.notNull(RCode.RECORD_RULE_STATE_ISNULL);
		//校验name labels name 合法性
		ValidateUtils.is(recordRule.getName()).regex(Constant.PROMETHEUS_LABELNAME_EXPRESSION,RCode.RECORD_RULE_NAME_FORMAT_ERROR);
		Map<String, Object> map = JSONUtil.toBean(recordRule.getLabels(), Map.class);
		for (String labelName : map.keySet()) {
			ValidateUtils.is(labelName).regex(Constant.PROMETHEUS_LABELNAME_EXPRESSION, RCode.RECORD_RULE_LABEL_NAME_FORMAT_ERROR);
		}

		// 校验type
		List<Integer> type = Arrays.asList(new Integer[] { 1, 2 });
		if (!type.contains(recordRule.getType())) {
			throw new NZException(RCode.RECORD_RULE_TYPE_ERROR);
		}
		// 校验状态
		List<Integer> status = Arrays.asList(new Integer[] { 0, 1 });
		if (!status.contains(recordRule.getState())) {
			throw new NZException(RCode.RECORD_RULE_STATE_ERROR);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int saveRecordRule(RecordRuleEntity recordRule) {

		// 校验参数
		this.validateParam(recordRule);
		// 检查name
		List<RecordRuleEntity> list = this
				.list(new LambdaQueryWrapper<RecordRuleEntity>().eq(RecordRuleEntity::getName, recordRule.getName()));
		if (Tool.ObjectUtil.isNotEmpty(list) || list.size() > 0) {
			throw new NZException(RCode.RECORD_RULE_NAME_EXIST);
		}
		Long id = ShiroUtils.getUserEntity().getId();
		recordRule.setCid(Integer.parseInt(id.toString()));
		recordRule.setUid(Integer.parseInt(id.toString()));
		recordRule.setCts(System.currentTimeMillis());
		recordRule.setUts(System.currentTimeMillis());
		this.save(recordRule);
		return recordRule.getId();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int updateRecordRule(RecordRuleEntity recordRule) {
		// 校验参数
		this.validateParam(recordRule);

		List<RecordRuleEntity> list = this.list();
		Integer recordRuleId = recordRule.getId();
		// name 唯一
		Map<String, Integer> map = list.stream()
				.collect(Collectors.toMap(RecordRuleEntity::getName, RecordRuleEntity::getId));
		Integer id = map.get(recordRule.getName());
		if (id != null && !id.equals(recordRuleId)) {
			throw new NZException(RCode.RECORD_RULE_NAME_EXIST);
		}
		Long uid = ShiroUtils.getUserEntity().getId();
		recordRule.setUid(Integer.parseInt(uid.toString()));
		recordRule.setUts(System.currentTimeMillis());
		this.updateById(recordRule);
		return recordRule.getId();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteRecordRule(Integer[] ids) {
		List<RecordRuleEntity> list = this
				.list(new LambdaQueryWrapper<RecordRuleEntity>().in(RecordRuleEntity::getId, Arrays.asList(ids)));

		// 是否内置
		List<Integer> id = list.stream().filter(recordRule -> recordRule.getBuildIn() == 1).map(RecordRuleEntity::getId)
				.collect(Collectors.toList());
		if (Tool.ObjectUtil.isNotEmpty(id) || id.size() > 0) {
			throw new NZException(RCode.RECORD_RULE_BUILD_IN_EXIST);
		}
		this.removeByIds(Arrays.asList(ids));
		// 删除对应的收藏记录
		starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>()
				.eq(SysUserStarredEntity::getType, "recordRule").in(SysUserStarredEntity::getTid, Arrays.asList(ids)));
	}

	/**
	 * 存放 record data 到 redis 需要额外线程消费队列中的内容
	 * 
	 * @param recordData
	 */
	@Override
	@SuppressWarnings("all")
	public void rightPushRecordToRedis(List<TimeSeries> timeSeriesList) {
		if (Tool.CollUtil.isEmpty(timeSeriesList)) {
			return;
		}
		redisTemplate.executePipelined(new SessionCallback() {
			@Override
			public Object execute(RedisOperations operations) throws DataAccessException {
				ListOperations opsForList = operations.opsForList();
				timeSeriesList.forEach(ts -> {
					opsForList.rightPush(Constant.RECORD_DATA_CACHE, Tool.HexUtil.encodeHexStr(ts.toByteArray()));
				});
				return null;
			}
		});
		ListOperations opsForList = redisTemplate.opsForList();
		Long size = opsForList.size(Constant.RECORD_DATA_CACHE);
		// 判断是否超出所设置的队列最大长度
		if (size > maxRecordSize) {
			// 删除较老的数据
			opsForList.trim(Constant.RECORD_DATA_CACHE, -maxRecordSize, -1);
			log.warn("trim record_data_cache size : {}", size - maxRecordSize);
		}
		if(log.isDebugEnabled()) {
			log.debug("add record data cache size : {}", timeSeriesList.size());
		}
	}
	
	@Override
	@SuppressWarnings("all")
	public void rightPushRecordToRedis(TimeSeries timeSeries) {
		if (timeSeries == null) {
			return;
		}
		ListOperations opsForList = redisTemplate.opsForList();
		opsForList.rightPush(Constant.RECORD_DATA_CACHE, Tool.HexUtil.encodeHexStr(timeSeries.toByteArray()));
		Long size = opsForList.size(Constant.RECORD_DATA_CACHE);
		// 判断是否超出所设置的队列最大长度
		if (size > maxRecordSize) {
			// 删除较老的数据
			opsForList.trim(Constant.RECORD_DATA_CACHE, -maxRecordSize, -1);
			log.warn("trim record_data_cache size : {}", size - maxRecordSize);
		}
	}

	@Override
	public void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException {
		String sysHeaderJsonStr = configService.getValue(Constant.SYSCONFIG_KEY_RUCORDRULE_IMPORT_HEADER);
		String fileName = "RecordRuleTemplate";
		basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
	}

	@Override
	public void export(HttpServletResponse response, Map<String, Object> params) throws IOException {
		String maxLine = configService.getValue(Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE);
		long exportMaxLine = Integer.parseInt(StrUtil.emptyToDefault(maxLine, "10000"));
		// 导出最大条数限制
		Object limit = params.get(Constant.LIMIT) == null ? Constant.PAGESIZE : params.get(Constant.LIMIT);
		Long pageSize = Long.parseLong(limit.toString());
		if (pageSize == -1) {
			// 导出所有直接限制 pageSize
			params.put(Constant.PAGE, "1");
			params.put(Constant.LIMIT, Long.toString(exportMaxLine));
		} else if (pageSize > exportMaxLine) {
			// 分页超过最大限制 则设置最大限制为 系统配置值，和 pageSize == -1 判断区别在于 pageNo
			params.put(Constant.LIMIT, Long.toString(exportMaxLine));
		}

		// 导出查询与列表查询一致
		PageUtils page = this.list(params);
		List<RecordRuleEntity> list = (List<RecordRuleEntity>) page.getList();

		List<List<String>> exportDataList = this.handleExportDataList(list);

		String sysHeaderJsonStr = configService.getValue(Constant.SYSCONFIG_KEY_RUCORDRULE_IMPORT_HEADER);
		String fileName = "RecordRules";
		String formatStr = params.get("format").toString();
		basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
	}

	private List<List<String>> handleExportDataList(List<RecordRuleEntity> list) {
		List<List<String>> exportDataList = new ArrayList<>();
		for (RecordRuleEntity recordRule : list) {
			List<String> tempList = new ArrayList<>();
			tempList.add(recordRule.getName());
			tempList.add(recordRule.getType().toString());
			tempList.add(recordRule.getExpr());
			tempList.add(recordRule.getInr().toString());
			tempList.add(recordRule.getState().toString());
			tempList.add(recordRule.getLabels());
			tempList.add(recordRule.getRemark());
			exportDataList.add(tempList);
		}
		return exportDataList;
	}

	@Override
	public Map importData(MultipartFile excelFile, Map<String, Object> params) throws IOException {
		String sysHeaderJsonStr = configService.getValue(Constant.SYSCONFIG_KEY_RUCORDRULE_IMPORT_HEADER);
		Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
		List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(excelFile, sysHeaderJsonStr);

		// 表头信息 国际化之后的
		List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

		// 返回统计信息
		Map statMap = this.saveRulesFromImportDataList(importDataList, headerInfos, params);
		return statMap;
	}

	private Map saveRulesFromImportDataList(List<Map<Integer, String>> importDataList, List<String> headerInfos, Map<String, Object> params) {
		String seq = StrUtil.uuid();
		params.put("seq", seq);
		// 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
		String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

		// 创建导入校验对象
		ImportValidateUtil validateUtil = ImportValidateUtil.builder()
				.existed(existed).headerInfos(headerInfos)
				.build();
		Integer addNum = 0, updateNum = 0;
		// 记录导入失败内容
		List<Map<Integer, String>> failContentList = ListUtil.list(true);
		for (int i = 0; i < importDataList.size(); i++) {
			int lineNum = i + 1;
			Map<Integer, String> dataMap = importDataList.get(i);

			//name 校验
			String name = dataMap.get(RecordRuleEnum.NAME.getIndex());
			validateUtil.validateRequired(name, lineNum, RecordRuleEnum.NAME.getIndex());
			validateUtil.validateRegex(name, Constant.PROMETHEUS_LABELNAME_EXPRESSION, lineNum, RecordRuleEnum.NAME.getIndex());
			boolean indexDuplicate = validateUtil.validateIndexDuplicate("record_rule", "name", name, lineNum, RecordRuleEnum.NAME.getIndex());
			// 索引列重复 & 忽略标识，提示忽略本行
			if (BooleanUtil.and(indexDuplicate, StrUtil.equals("2", existed))) {
				validateUtil.ignoreThisRow(lineNum, RecordRuleEnum.NAME.getIndex());
				failContentList.add(dataMap);
				continue;
			}

			//type 校验
			String type = dataMap.get(RecordRuleEnum.TYPE.getIndex());
			validateUtil.validateRequired(type, lineNum, RecordRuleEnum.TYPE.getIndex());
			validateUtil.validateEnumValArr(type, Arrays.asList("1", "2"), lineNum, RecordRuleEnum.TYPE.getIndex());

			//expr
			String expr = dataMap.get(RecordRuleEnum.EXPR.getIndex());
			validateUtil.validateRequired(expr, lineNum, RecordRuleEnum.EXPR.getIndex());

			//inr
			String inr = dataMap.get(RecordRuleEnum.INR.getIndex());

			//state
			String state = dataMap.get(RecordRuleEnum.STATE.getIndex());
			validateUtil.validateRequired(state, lineNum, RecordRuleEnum.STATE.getIndex());
			validateUtil.validateEnumValArr(state, Arrays.asList("0", "1"), lineNum, RecordRuleEnum.STATE.getIndex());

			//labels
			String labels = dataMap.get(RecordRuleEnum.LABELS.getIndex());
			Map<String, String> label = JSONObject.parseObject(labels, Map.class);
			if (MapUtil.isNotEmpty(label) && label.size() != 0){
				for (String key : label.keySet()) {
					validateUtil.validateRegex(key, Constant.PROMETHEUS_LABELNAME_EXPRESSION, lineNum, RecordRuleEnum.LABELS.getIndex());
				}
			}

			// 遇到错误是否继续导入，可选值： 0：不继续    1：继续    默认值：1
			String ignoreError = Tool.ObjectUtil.defaultIfEmpty((String) params.get("ignoreError"), "1");
			List<Map<String, String>> errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
			if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
				// 结束本次导入，自导入行开始 到结尾数据行，提示错误信息
				for (int j = i; j < importDataList.size(); j++) {
					validateUtil.recordImportAttrErr(j + 1, RCode.IMPORT_ERROR_STOP);
					failContentList.add(importDataList.get(j));
				}
				break;
			}
			if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
				String remark = dataMap.get(RecordRuleEnum.REMARK.getIndex());

				RecordRuleEntity recordRule = new RecordRuleEntity();
				recordRule.setName(name);
				recordRule.setType(Integer.valueOf(type));
				recordRule.setExpr(expr);
				recordRule.setInr(inr.length() == 0 ? 0 : Integer.valueOf(inr));
				recordRule.setState(Integer.valueOf(state));
				recordRule.setLabels(labels);
				recordRule.setRemark(remark);
				recordRule.setSeq(params.get("seq").toString());

				try {
					this.saveOrUpdateRowData(recordRule, params);
					// save or update
					Object saveFlag = params.get("save");
					if (ObjectUtil.isNotNull(saveFlag)) {
						addNum++;
					} else {
						updateNum++;
					}
				} catch (Exception e) {
					failContentList.add(dataMap);
					validateUtil.recordImportAttrErr(lineNum, RCode.ERROR);
					log.error("record rule import roll back the transaction failure", e);
				}
			} else {
				failContentList.add(dataMap);
			}
		}
		String sysHeaderJsonStr = configService.getValue(Constant.SYSCONFIG_KEY_RUCORDRULE_IMPORT_HEADER);
		String failedContentFileBase64Str = basicImportAndExportServices.getFailedContentFileBase64Str(failContentList, sysHeaderJsonStr, Constant.ImportFileType.EXCEL.getValue());
		//  返回统计信息
		Map<Object, Object> statMap = Tool.MapUtil.builder()
				.put("seq", seq)
				.put("totalNum", importDataList.size())
				.put("successNum", addNum + updateNum)
				.put("failNum", failContentList.size())
				.put("addNum", addNum)
				.put("updateNum", updateNum)
				.put("failDetail", validateUtil.getImportErrMsg())
				.put("failContent", failedContentFileBase64Str)
				.build();
		return statMap;
	}

	private void saveOrUpdateRowData(RecordRuleEntity recordRule, Map<String, Object> params) {
		MapUtil.removeAny(params, "save", "update");
		// 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
		String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

		Long id = ShiroUtils.getUserEntity().getId();

		RecordRuleEntity oldRecordRule = this.getOne(new LambdaQueryWrapper<RecordRuleEntity>().eq(RecordRuleEntity::getName, recordRule.getName()));
		if (StrUtil.equals("1", existed) && oldRecordRule != null) {
			params.put("update", true);
			// update
			recordRule.setId(oldRecordRule.getId());
			recordRule.setUid(Integer.parseInt(id.toString()));
			recordRule.setUts(System.currentTimeMillis());
			this.updateById(recordRule);
		} else {
			params.put("save", true);
			// insert
			recordRule.setCid(Integer.parseInt(id.toString()));
			recordRule.setUid(Integer.parseInt(id.toString()));
			recordRule.setCts(System.currentTimeMillis());
			recordRule.setUts(System.currentTimeMillis());
			this.save(recordRule);
		}
	}
}

