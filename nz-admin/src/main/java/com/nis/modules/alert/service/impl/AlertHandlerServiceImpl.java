package com.nis.modules.alert.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertHandlerDao;
import com.nis.modules.alert.dao.AlertSilenceDao;
import com.nis.modules.alert.entity.*;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.alert.service.AlertHandlerService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.alert.service.AlertSeverityService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetStateConf;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetStateConfService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfService;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("alertMessageHandlerService")
public class AlertHandlerServiceImpl implements AlertHandlerService {
	private static final Log log = Log.get();

	@Autowired
	private AssetAssetService assetAssetService;
	@Autowired
	private AssetStateConfService assetStateService;
	@Autowired
	private MonitorEndpointService endpointService;
	@Autowired
	private MonitorModuleService moduleService;
	@Autowired
	private MonitorProjectService projectService;
	@Autowired
	private AlertRuleService alertRuleService;
	@Autowired
	private AlertSeverityService alertSeverityService;
	@Autowired
	private DcService dcService;
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private AlertHandlerDao msgHandlerDao;
	@Autowired
	private AlertSilenceDao alertSilenceDao;
	@Autowired
	private SysConfService sysConfService;
	/**
	 * alert message 超时时间
	 */
	@Value("${alert.message.resolve.timeout:300}")
	private Integer resolveTimeout;
	

	/**
	 * 更新 message end at 时间
	 */
	@Override
	public void updateMessageEndAt(AlertMessageEntity message) {
		String tableName = message.getTableName();
		String hashKey = message.getHashKey();
		Date endAt = message.getEndAt();
		if (!Constant.ALERT_MESSAGE_TABLE_STATE_MAPPING.containsKey(tableName)) {
			log.warn("alert message tablename is null,content:{}", Tool.JSONUtil.toJsonStr(message));
			throw new NZException(RCode.ERROR);
		}
		if (Constant.ALERT_MESSAGE_ACTIVE_TABLE.equalsIgnoreCase(tableName)) {
			// 更新 redis 超时时间
			redisTemplate.expire(Tool.StrUtil.concat(true, Constant.ALERT_MESSAGE_ACTIVE_PRIX, hashKey),this.getKeyTimeout(endAt), TimeUnit.MILLISECONDS);
		} else if (Constant.ALERT_MESSAGE_SILENCE_TABLE.equalsIgnoreCase(tableName)) {
			redisTemplate.expire(Tool.StrUtil.concat(true, Constant.ALERT_MESSAGE_SILENCE_PRIX, hashKey),this.getKeyTimeout(endAt), TimeUnit.MILLISECONDS);
		}
		// 更新库表
		msgHandlerDao.updateMessageEndAt(message);
	}

	

	/**
	 * 告警规则匹配
	 */
	@SuppressWarnings("all")
	@Override
	public List<AlertSilenceConf> judgeAlertMessageSilence(AlertMessageEntity message) {
		List<AlertSilenceConf> confList = alertSilenceDao.selectList(new QueryWrapper<AlertSilenceConf>().eq("state", Constant.ALERT_SILENCE_STATE_ACTIVE));
		confList = this.judgeAlertMessageSilence(message, confList);
		return confList;
	}

	/**
	 * 判断告警消息 符合 静默规则
	 * @param message
	 * @param confList
	 * @return
	 */
	public List<AlertSilenceConf> judgeAlertMessageSilence(AlertMessageEntity message,List<AlertSilenceConf> confList) {
		if (Tool.CollUtil.isEmpty(confList) || message == null) {
			return null;
		}
		List<AlertSilenceConf> result = Tool.CollUtil.list(true);
		Iterator<AlertSilenceConf> ite = confList.iterator();
		for (; ite.hasNext();) {// 遍历静默配置规则
			AlertSilenceConf next = ite.next();
			boolean b = judgeAlertMessageSilence(message, next);
			if(b) {//匹配
				result.add(next);
			}
		}
		return result;
	}
	
	/**
	 * 判断 告警消息是否匹配 告警规则
	 * @param message
	 * @param conf
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean judgeAlertMessageSilence(AlertMessageEntity message,AlertSilenceConf conf) {
		String matchers = conf.getMatchers();
		HashMap<String, String> labelMap = Tool.JSONUtil.toBean(message.getLabels(), HashMap.class);
		List<AlertSilenceMatcher> matchList = Tool.JSONUtil.toList(matchers, AlertSilenceMatcher.class);
		for (AlertSilenceMatcher match : matchList) {// 遍历静默规则中 match 检查是否每项都匹配
			String name = match.getName();
			String value = match.getValue();
			Integer regex = match.getRegex();
			String mVal = labelMap.get(name);
			if (Tool.StrUtil.isBlank(mVal)) {// 不存在跳内部循环
				return false;
			}
			// 正则匹配
			if (Tool.ObjectUtil.equal(1, regex)) {
				// 正则没有匹配成功
				if (!Tool.ReUtil.isMatch(value, mVal)) {
					return false;
				}
			}else {
				// 完全匹配不成功
				if (!Tool.StrUtil.equals(value, mVal)) {
					return false;
				}
			}
		}
		return true;
	}
	
	
	/**
	 * 批量保存active告警消息
	 * @param msgList
	 */
	@Override
	@Transactional
	public void handleMessageActiveBatch(List<AlertMessageEntity> msgList) {
		if (Tool.CollUtil.isEmpty(msgList)) {
			return;
		}
		String tableName = Constant.ALERT_MESSAGE_ACTIVE_TABLE;
		Integer state = Constant.ALERT_MESSAGE_ACTIVE_STATE;
		for(AlertMessageEntity message : msgList) {//逐个设置 key 缓存，邮件通知
			String hashKey = message.getHashKey();
			Long messageId = message.getId();
			Date endAt = message.getEndAt();
			message.setState(state);
			//缓存 到redis
			redisTemplate.opsForValue().set(Tool.StrUtil.concat(true, Constant.ALERT_MESSAGE_ACTIVE_PRIX, hashKey),String.valueOf(messageId),this.getKeyTimeout(endAt),TimeUnit.MILLISECONDS);
			this.pushMessageNotifyList(message);
		}
		msgHandlerDao.saveMessageBatch(msgList, tableName);//批量保存
	}
	
	/**
	 * 批量保存 Silence 告警消息
	 * @param msgList
	 */
	@Override
	@Transactional
	public void handleMessageSilenceBatch(List<AlertMessageEntity> msgList) {
		if (Tool.CollUtil.isEmpty(msgList)) {
			return;
		}
		String tableName = Constant.ALERT_MESSAGE_SILENCE_TABLE;
		Integer state = Constant.ALERT_MESSAGE_SILENCE_STATE;
		List<AlertSilenceMessageRel> smRels = Tool.ListUtil.list(true);
		for(AlertMessageEntity message : msgList) {//逐个设置 key 缓存，邮件通知
			String hashKey = message.getHashKey();
			Long messageId = message.getId();
			Date endAt = message.getEndAt();
			message.setState(state);
			List<AlertSilenceConf> silenceList = message.getSilenceList();
			if(Tool.CollUtil.isNotEmpty(silenceList)) {
				for(AlertSilenceConf conf : silenceList) {//静默规则与 message 关系
					AlertSilenceMessageRel rel = new AlertSilenceMessageRel();
					rel.setHashKey(message.getHashKey());
					rel.setMessageId(message.getId());
					rel.setSilenceConfId(conf.getId());
					smRels.add(rel);
				}
			}
			//缓存 到redis
			redisTemplate.opsForValue().set(Tool.StrUtil.concat(true, Constant.ALERT_MESSAGE_SILENCE_PRIX, hashKey),String.valueOf(messageId),this.getKeyTimeout(endAt),TimeUnit.MILLISECONDS);
		}
		msgHandlerDao.saveMessageBatch(msgList, tableName);//批量保存
		msgHandlerDao.saveAlertSilenceMessageRel(smRels);
	}
	
	/**
	 * 批量保存 Expired 告警消息
	 * 1、保存到表
	 * @param msgList
	 */
	@Override
	@Transactional
	public void handleMessageExpiredBatch(List<AlertMessageEntity> msgList,Integer fromState) {
		if (Tool.CollUtil.isEmpty(msgList)) {
			return;
		}
		log.debug("fromState: {}, message size: {}",fromState,msgList.size());
		String tableName = Constant.ALERT_MESSAGE_HISTORY_TABLE;
		Integer state = Constant.ALERT_MESSAGE_HISTORY_STATE;
		String fromTable = Constant.ALERT_MESSAGE_STATE_TABLE_MAPPING.get(fromState);
		List<Long> ids = Tool.ListUtil.list(false);
		Iterator<AlertMessageEntity> iterator = msgList.iterator();
		for(;iterator.hasNext();) {//逐个设置 key 缓存，邮件通知
			AlertMessageEntity message = iterator.next();
			String hashKey = message.getHashKey();
			if(Constant.ALERT_MESSAGE_ACTIVE_STATE.equals(fromState)) {
				AlertMessageEntity fromMessage = msgHandlerDao.selectMessageByIdAndTableName(message.getId(), fromTable);
				if(fromMessage == null) {
					iterator.remove();
					continue;
				}
				Tool.BeanUtil.copyProperties(fromMessage, message, "endAt");//复制原内容
				message.setState(state);//修改状态
				hashKey = fromMessage.getHashKey();
				ids.add(fromMessage.getId());
				redisTemplate.delete(Tool.StrUtil.concat(true, Constant.ALERT_MESSAGE_ACTIVE_PRIX, hashKey));
				this.pushMessageNotifyList(fromMessage);
			}
			if(Constant.ALERT_MESSAGE_SILENCE_STATE.equals(fromState)) {
				ids.add(message.getId());
				redisTemplate.delete(Tool.StrUtil.concat(true, Constant.ALERT_MESSAGE_SILENCE_PRIX, hashKey));
			}
		}
		//只有 active 消息失效会保存到 history
		if(Tool.CollUtil.isNotEmpty(msgList) && Constant.ALERT_MESSAGE_ACTIVE_STATE.equals(fromState)) {
			msgHandlerDao.saveMessageBatch(msgList, tableName);//批量保存
		}
		log.debug("message ids: {}",ids.toString());
		if(Tool.CollUtil.isNotEmpty(ids)) {
			if(Constant.ALERT_MESSAGE_ACTIVE_STATE.equals(fromState)) {
				// 删除原来 active 表中数据
				msgHandlerDao.deleteMessageByIdsAndTableName(Constant.ALERT_MESSAGE_ACTIVE_TABLE, ids.toArray(new Long[] {}));
			}
			if(Constant.ALERT_MESSAGE_SILENCE_STATE.equals(fromState)) {
				// 删除原来 active 表中数据
				msgHandlerDao.deleteMessageByIdsAndTableName(Constant.ALERT_MESSAGE_SILENCE_TABLE, ids.toArray(new Long[] {}));
				// 删除 alert_silence_message_rel 表关系
				msgHandlerDao.deleteAlertSilenceMessageRelByMessageIds(ids.toArray(new Long[] {}));
			}
		}
	}
	

	/**
	 * 查询 active || silence 表中 endat 小于 当前时间的告警消息 用于周期线程处理未及时清除的告警消息
	 */
	@Override
	public List<AlertMessageEntity> selectMessagegtNow(String tableName) {
		DateTime now = Tool.DateUtil.date();
		now = Tool.DateUtil.offsetSecond(now, -resolveTimeout);//加上resolve 时间，减少误判几率
		List<AlertMessageEntity> list = msgHandlerDao.selectMessageEndAtltNow(now, tableName);
		return list;
	}

	/**
	 * silence conf 由 active 变更为 expired
	 */
	@Override
	public List<AlertSilenceConf> selectActiveToExpiredSilence() {
		DateTime now = Tool.DateUtil.date();
		List<AlertSilenceConf> list = alertSilenceDao.selectList(new QueryWrapper<AlertSilenceConf>().lambda()
				.eq(AlertSilenceConf::getState, Constant.ALERT_SILENCE_STATE_ACTIVE) //状态为 active
				.lt(AlertSilenceConf::getEndAt, now)); //结束时间小于 当前时间
		return list;
	}

	/**
	 * silence conf 有 pending 变更为 active
	 */
	@Override
	public List<AlertSilenceConf> selectPendingToActiveSilence() {
		DateTime now = Tool.DateUtil.date();
		List<AlertSilenceConf> list = alertSilenceDao.selectList(new QueryWrapper<AlertSilenceConf>().lambda()
				.eq(AlertSilenceConf::getState, Constant.ALERT_SILENCE_STATE_PENDING) //状态为 pending
				.lt(AlertSilenceConf::getStartAt, now) //开始时间小于当前时间
				.gt(AlertSilenceConf::getEndAt, now)); //结束时间大于当前时间
		return list;
	}

	/**
	 * 批量修改 silence conf 状态
	 * @param state
	 * @param silencConfId
	 */
	@Override
	public void updateSilenceConfStateBatch(int state,Integer ... silencConfId) {
		if(Tool.ArrayUtil.isEmpty(silencConfId)) {
			return;
		}
		AlertSilenceConf entity = new AlertSilenceConf();
		entity.setState(state);
		alertSilenceDao.update(entity, new UpdateWrapper<AlertSilenceConf>().lambda().in(AlertSilenceConf::getId, silencConfId));
	}

	/**
	 * 1、alert message 判断是否需要移动到 history 表
	 * 2 、修改 silence conf 状态
	 */
	@Override
	public void handleSilenceConfActiveToExired(List<AlertSilenceConf> silenceList) {
		if(Tool.CollUtil.isEmpty(silenceList)) {
			return ;
		}
		log.debug("告警静默规则状态变更：active -> expired ,silenceConf : {}", Tool.JSONUtil.toJsonStr(silenceList));
		List<Integer> ids = Tool.ListUtil.list(false);
		List<Long> msgIds = Tool.ListUtil.list(false);
		for(AlertSilenceConf silenceConf : silenceList) {
			Integer confId = silenceConf.getId();
			ids.add(confId);
			//查询 只有当前 silenceconf 匹配的 告警规则 的 alert message id
			//当前 silence conf 过期说明 ，alert_message_silence 也已失效
			List<AlertSilenceMessageRel> smRel = msgHandlerDao.selectAlertMessageIdsOneByOneSilencConfId(confId);
			if(Tool.CollUtil.isNotEmpty(smRel)) {
				for(AlertSilenceMessageRel rel : smRel) {
					redisTemplate.delete(Tool.StrUtil.concat(true, Constant.ALERT_MESSAGE_SILENCE_PRIX, rel.getHashKey()));
				}
			}
		}
		//删除 alert_message_silence 
		if(Tool.CollUtil.isNotEmpty(msgIds)) {
			log.debug("silence messsageIds : {}", msgIds.toString());
			msgHandlerDao.deleteMessageByIdsAndTableName(Constant.ALERT_MESSAGE_SILENCE_TABLE,msgIds.toArray(new Long[] {}));
		}
		log.debug("silence confIds : {}", ids.toString());
		msgHandlerDao.deleteAlertSilenceMessageRelBySilenceConfIds(ids.toArray(new Integer[] {}));
		//更新 alert silence conf 状态 为 expired
		this.updateSilenceConfStateBatch(Constant.ALERT_SILENCE_STATE_EXPIRED, ids.toArray(new Integer[] {}));
	}

	@Transactional
	@Override
	public void handleSilenceConfPendingToActive(List<AlertSilenceConf> silenceList) {
		if(Tool.CollUtil.isEmpty(silenceList)) {
			return ;
		}
		log.debug("告警静默规则状态变更：pending -> active ,silenceConf : {}", Tool.JSONUtil.toJsonStr(silenceList));
		//查询数据库中所有 active message 逐一判断
		List<AlertMessageEntity> activeMessage = msgHandlerDao.selectAllActiveMessage();
		List<Long> messageIds = Tool.ListUtil.list(true);
		Iterator<AlertMessageEntity> iterator = activeMessage.iterator();
		DateTime now = Tool.DateUtil.date();
		for( ; iterator.hasNext() ; ) {
			AlertMessageEntity message = iterator.next();
			Date endAt = message.getEndAt();
			if(Tool.DateUtil.compare(endAt, now)<0) {
				iterator.remove();
				continue;
			}
			//判断 alert message 是否匹配到当前 由 pending 转为 active silenceconf
			List<AlertSilenceConf> list = this.judgeAlertMessageSilence(message, silenceList);
			if(Tool.CollUtil.isEmpty(list)) {//当前active 状态的 告警消息 匹配了 silence 配置
				iterator.remove();
				continue;
			}
			message.setSilenceList(list);
			messageIds.add(message.getId());
			redisTemplate.delete(Tool.StrUtil.concat(true, Constant.ALERT_MESSAGE_ACTIVE_PRIX,message.getHashKey()));
		}
		//更新 silence conf 状态
		List<Integer> confIds = Tool.ListUtil.list(false);
		for(AlertSilenceConf conf : silenceList) {
			confIds.add(conf.getId());
		}
		//修改 alert_silence_conf 状态
		this.updateSilenceConfStateBatch(Constant.ALERT_SILENCE_STATE_ACTIVE, confIds.toArray(new Integer[] {}));
		//保存到 alert_message_silence 表及 rel 表
		this.handleMessageSilenceBatch(activeMessage);
		//删除 alert_message_active 表数据
		if(Tool.CollUtil.isNotEmpty(messageIds)) {
			msgHandlerDao.deleteMessageByIdsAndTableName(Constant.ALERT_MESSAGE_ACTIVE_TABLE, messageIds.toArray(new Long[] {}));
		}
	}

	@Override
	public PageUtils queryMessagePage(Map<String, Object> params) {
		//根据状态判断对应的表
		String tableName = Constant.ALERT_MESSAGE_STATE_TABLE_MAPPING.get(Integer.valueOf(params.get("state").toString()));
		params.put("tableName", tableName);
		IPage page = new Query(AlertMessageEntity.class).getPage(params);
		List<AlertMessageEntity> list = msgHandlerDao.queryList(page,params);
		page.setRecords(list);
		return new PageUtils(page);
	}

	/**
	 * 删除告警消息
	 */
	@Override
	public void removeMessageByIds(Integer state, Long ... ids) {
		if(Tool.ArrayUtil.isEmpty(ids)) {
			return;
		}
		String tableName = Constant.ALERT_MESSAGE_STATE_TABLE_MAPPING.get(state);
		msgHandlerDao.deleteMessageByIdsAndTableName(tableName, ids);
		
	}

	@Override
	public void exportMessage(HttpServletResponse response, Map<String, Object> params) {
		List<SysConfigEntity> configList = sysConfService.list();
		Map<String, String> configMap = configList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));
		Map map = JSONObject.parseObject(configMap.get("alert_export_header"), Map.class, Feature.OrderedField);

		// 导出最大条数限制
		int exportMaxLine = Integer.parseInt(configMap.get("export_max_line"));
		int exportRealTotal = 0;

		SXSSFWorkbook workbook = null;
		try {
			String language = (String) params.get("language");
			language = ToolUtil.isEmpty(language) ? "en" : language;

			String sheetName = "AlertMessages";
			workbook = ExcelUtils.createExcel07((Map) map.get(language), sheetName, false);
			SXSSFSheet sheet = workbook.getSheet(sheetName);

			PageUtils page = this.queryMessagePage(params);
			List<AlertMessageEntity> exportAlertMsgList = (List<AlertMessageEntity>) page.getList();
			exportRealTotal = exportAlertMsgList.size();

			// 判断导出条数是否超过配置最大导出量
			if (exportAlertMsgList.size() > exportMaxLine) {
				throw new NZException(RCode.SYS_CONFIG_EXPORT_MAX_LINE);
			}

			this.fillData(exportAlertMsgList, 1, sheet, workbook);

			String fileName = "AlertMessagesExport.xlsx";
			response.setContentType("application/excel");
			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
			workbook.write(response.getOutputStream());
		} catch (IOException | NZException e) {
			try {
				String errMsg;
				if (e instanceof NZException) {
					errMsg = String.format("Configure the maximum number of export: %s, query the total number of export: %s", exportMaxLine, exportRealTotal);
				} else {
					errMsg = "Export error";
				}
				response.sendError(413, errMsg);
			} catch (IOException ex) {
				log.error(ex);
			}
		} finally {
			try {
				if (workbook != null) {
					workbook.close();
				}
			} catch (IOException e) {
				log.error(e);
			}
		}
	}

	private void fillData(List<AlertMessageEntity> list, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook) {
		Row row;
		CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);

		for (AlertMessageEntity alertMessage : list) {

			String startAt = ToolUtil.isNotEmpty(alertMessage.getStartAt()) ? DateUtil.formatDate(alertMessage.getStartAt(), "yyyy-MM-dd HH:mm:ss") : "";
			String endAt = ToolUtil.isNotEmpty(alertMessage.getEndAt()) ? DateUtil.formatDate(alertMessage.getEndAt(), "yyyy-MM-dd HH:mm:ss") : "";

			row = sheet.createRow(rowNum++);
			row.createCell(0).setCellValue(alertMessage.getId());
			row.createCell(1).setCellValue(alertMessage.getAlertRule().getName());
			row.createCell(2).setCellValue(ToolUtil.isNotEmpty(alertMessage.getDc()) ? alertMessage.getDc().getName() : "");
			row.createCell(3).setCellValue(ToolUtil.isNotEmpty(alertMessage.getProject()) ? alertMessage.getProject().getName() : "");
			row.createCell(4).setCellValue(ToolUtil.isNotEmpty(alertMessage.getModule()) ? alertMessage.getModule().getName() : "");
			row.createCell(5).setCellValue(ToolUtil.isNotEmpty(alertMessage.getEndpoint()) ? alertMessage.getEndpoint().getName() : "");
			row.createCell(6).setCellValue(ToolUtil.isNotEmpty(alertMessage.getAsset()) ? alertMessage.getAsset().getName() : "");
			row.createCell(7).setCellValue(ToolUtil.isNotEmpty(alertMessage.getLabels()) ? alertMessage.getLabels() : "");
			row.createCell(8).setCellValue(ToolUtil.isNotEmpty(alertMessage.getSeverity()) ? alertMessage.getSeverity().getName() : "");
			row.createCell(9).setCellValue(alertMessage.getSummary());
			row.createCell(10).setCellValue(alertMessage.getDescription());
			row.createCell(11).setCellValue(startAt);
			row.createCell(12).setCellValue(endAt);
			row.createCell(13).setCellValue(Constant.ALERT_MESSAGE_STATE_NAME_MAPPING.get(alertMessage.getState()));

			// 设置单元格样式
			ExcelUtils.setDataCellStyle(dataStyle, row, 13);
		}
	}
	/**
	 * 计算 active silence key 超时时间
	 * @param endAt
	 * @return
	 */
	public Long getKeyTimeout(Date endAt) {
		return endAt.getTime() - System.currentTimeMillis() + resolveTimeout * 1000;
	}
	
	/**
	 * 将消息发送到通知队列
	 * @param message
	 */
	public void pushMessageNotifyList(AlertMessageEntity message) {
		if (message == null || message.getId() == null) {
			log.debug("messageId is null");
			return;
		}
		Integer ruleId = message.getRuleId();
		AlertRuleEntity ruleEntity = alertRuleService.getById(ruleId);
		if(ruleEntity == null || Tool.StrUtil.isAllNotBlank(ruleEntity.getReceiver(),ruleEntity.getMethod())) {
			log.debug("Receiver & notify method all null");
		}
		//补充message 内容
		message = getAlertMessageLinkObject(message);
		//添加到邮件通知队列
		redisTemplate.opsForList().leftPush(Constant.ALERT_MESSAGE_NOTIFY, Tool.JSONUtil.toJsonStr(message));
	}
	
	/**
	 * 补充alermessage 内容
	 * 
	 * @param dto
	 * @return
	 */
	@Override
	public AlertMessageEntity getAlertMessageLinkObject(AlertMessageEntity message) {
		if (message == null || message.getId() == null) {
			return null;
		}
		// 判断endpoint id 是否存在
		Integer endpointId = message.getEndpointId(), moduleId = message.getModuleId(), 
				projectId = message.getProjectId(), dcId = message.getDcId(), 
				assetId = message.getAssetId(), ruleId = message.getRuleId(),
				severityId = message.getSeverityId();
		if (endpointId != null && message.getEndpoint() == null) {
			// 当 moduleid 不存在，但是 endpoint id存在时，通过库表查询赋值
			MonitorEndpoint ep = endpointService.getById(endpointId);
			message.setEndpointId(endpointId);
			moduleId = ep.getModuleId();
			assetId = ep.getAssetId();
			message.setEndpointId(endpointId);
			message.setEndpoint(ep);
		}

		// 获取 asset id
		if (message.getAsset() == null && assetId != null) {
			AssetAsset asset = assetAssetService.getById(assetId);
			dcId = asset.getDcId();
			message.setAssetId(assetId);
			message.setAsset(asset);
		}
		// 获取dc
		if (message.getDc() == null && dcId != null ) {
			Dc dc = dcService.getById(dcId);
			message.setDcId(dcId);
			message.setDc(dc);
		}
		// 判断 module id 是否存在
		if (message.getModule() == null && moduleId != null ) {
			MonitorModule module = moduleService.getById(moduleId);
			projectId = module.getProjectId();
			message.setModuleId(moduleId);
			message.setModule(module);
		}
		// 判断 module id 是否存在
		if (message.getProject() == null && projectId != null) {
			MonitorProject project = projectService.getById(projectId);
			message.setProjectId(projectId);
			message.setProject(project);
		}
		// 获取alertrule
		if (message.getAlertRule() == null && ruleId != null) {
			AlertRuleEntity ruleEntity = alertRuleService.getById(ruleId);
			message.setRuleId(ruleId);
			message.setAlertRule(ruleEntity);
		}
		//获取  Severity_Id
		if(message.getSeverity() == null && severityId != null) {
			AlertSeverityConf severityConf = alertSeverityService.getById(severityId);
			message.setSeverityId(severityId);
			message.setSeverity(severityConf);
		}
		return message;
	}
	
	/**
	 * 补充alermessage 内容
	 * 
	 * @param dto
	 * @return
	 */
	@Override
	public AlertMessageEntity transAlertMessageEntity(AlertMessageReceiveDTO dto) {
		AlertMessageEntity message = new AlertMessageEntity();
		Map<String, String> labels = dto.getLabels();
		if (labels == null) {
			return null;
		}
		// 判断endpoint id 是否存在
		Integer endpointId = null, moduleId = null, projectId = null, dcId = null, assetId = null, ruleId = null;
		if (Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_ENDPOINT_ID_KEY))) {
			endpointId = Tool.NumberUtil.parseInt(labels.get(Constant.LABEL_ENDPOINT_ID_KEY));
			// 当 moduleid 不存在，但是 endpoint id存在时，通过库表查询赋值
			MonitorEndpoint ep = endpointService.getById(endpointId);
			message.setEndpointId(endpointId);
			moduleId = ep.getModuleId();
			assetId = ep.getAssetId();
			Integer enabled = ep.getEnabled();
			if (Tool.ObjectUtil.equal(enabled, 0)) {// disable直接返回 丢弃告警消息
				if (log.isDebugEnabled())
					log.debug("endpoint state : {}", enabled);
				return null;
			}
			message.setEndpointId(endpointId);
			message.setEndpoint(ep);
		}

		// 获取 asset id
		if (assetId != null || Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_ASSET_ID_KEY))) {
			assetId = assetId != null ? assetId : Tool.NumberUtil.parseInt(labels.get(Constant.LABEL_ASSET_ID_KEY));
			AssetAsset asset = assetAssetService.getById(assetId);
			dcId = asset.getDcId();
			Integer stateId = asset.getStateId();
			AssetStateConf stateConf = assetStateService.getById(stateId);
			Integer alert = stateConf.getAlert();
			if (Tool.ObjectUtil.equal(alert, 0)) {// disable直接返回 丢弃告警消息
				if (log.isDebugEnabled())
					log.debug("asset state : {}", alert);
				return null;
			}
			message.setAssetId(assetId);
			message.setAsset(asset);
		}
		// 获取dc
		if (dcId != null || Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_DC_ID_KEY))) {
			dcId = (dcId != null) ? dcId : Tool.NumberUtil.parseInt(labels.get(Constant.LABEL_DC_ID_KEY));
			Dc dc = dcService.getById(dcId);
			String state = dc.getState();
			if (Tool.StrUtil.equalsIgnoreCase(state, "OFF")) {// disable直接返回 丢弃告警消息
				if (log.isDebugEnabled())
					log.debug("dc state : {}", state);
				return null;
			}
			message.setDcId(dcId);
			message.setDc(dc);
		}
		// 判断 module id 是否存在
		if (moduleId != null || Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_MODULE_ID_KEY))) {
			moduleId = (moduleId != null) ? moduleId : Tool.NumberUtil.parseInt(labels.get(Constant.LABEL_MODULE_ID_KEY));
			MonitorModule module = moduleService.getById(moduleId);
			projectId = module.getProjectId();
			message.setModuleId(moduleId);
			message.setModule(module);
		}
		// 判断 module id 是否存在
		if (projectId != null || Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_PROJECT_ID_KEY))) {
			projectId = (projectId != null) ? projectId
					: Tool.NumberUtil.parseInt(labels.get(Constant.LABEL_PROJECT_ID_KEY));
			MonitorProject project = projectService.getById(projectId);
			message.setProjectId(projectId);
			message.setProject(project);
		}
		// 获取alertrule
		String ruleIdStr = labels.get(Constant.LABEL_RULE_ID_KEY);
		if (Tool.StrUtil.isNotBlank(ruleIdStr)) {
			ruleId = Tool.NumberUtil.parseInt(ruleIdStr);
			AlertRuleEntity ruleEntity = alertRuleService.getById(ruleId);
			message.setRuleId(ruleId);
			message.setAlertRule(ruleEntity);
		}
		//获取  Severity_Id
		String severityIdStr = labels.get(Constant.LABEL_SEVERITY_ID_KEY);
		AlertSeverityConf severityConf = alertSeverityService.getById(severityIdStr);
		message.setSeverityId(Tool.NumberUtil.parseInt(severityIdStr));
		message.setSeverity(severityConf);
		
		// 设置时间
		DateTime endAt = Tool.DateUtil.parse(dto.getEndsAt(), Constant.ALERT_MESSAGE_DATE_FORMAT);
		DateTime startAt = Tool.DateUtil.parse(dto.getStartsAt(), Constant.ALERT_MESSAGE_DATE_FORMAT);
		message.setEndAt(endAt);
		message.setStartAt(startAt);
		// 设置 annotation 信息
		Map<String, String> receiveAnnotations = dto.getAnnotations();
		message.setSummary(receiveAnnotations.get("summary"));
		message.setDescription(receiveAnnotations.get("description"));
		message.setLabels(Tool.JSONUtil.toJsonStr(labels));
		return message;
	}
}
