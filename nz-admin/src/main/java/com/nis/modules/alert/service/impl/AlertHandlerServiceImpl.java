package com.nis.modules.alert.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
	private SysConfigService sysConfigService;
	@Autowired
	private BasicImportAndExportServices basicImportAndExportServices;

	@Value("${redis.maxQueueLength:100}")
    private Long maxQueueLength;//redis队列长度
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
                log.debug("fromMessage {}", fromMessage != null ? JSONObject.toJSONString(fromMessage) : "null");
                if(fromMessage == null) {
					iterator.remove();
					continue;
				}
                fromMessage.setState(state); // 修改状态
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
	public List<AlertMessageEntity> selectMessagegtNow(String tableName,Integer limit) {
		DateTime now = Tool.DateUtil.date();
		now = Tool.DateUtil.offsetSecond(now, -resolveTimeout);//加上resolve 时间，减少误判几率
		List<AlertMessageEntity> list = msgHandlerDao.selectMessageEndAtltNow(now, tableName,limit);
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
					msgIds.add(rel.getMessageId());
				}
			}
		}
		//删除 alert_message_silence 
		log.debug("silence messsageIds : {}", msgIds.toString());
		if(Tool.CollUtil.isNotEmpty(msgIds)) {
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
	 * 根据查询条件 返回告警消息统计
	 * @param params
	 * @return
	 */
	@Override
	@SuppressWarnings("all")
	public Map<String,List> queryMessageGroupBy(Map<String,Object> params) {
		Map<String,List> result = Tool.MapUtil.newHashMap();
		QueryWrapper wrapper = new QueryWrapper().select("id","name");
		wrapper.orderByAsc("name");
		//dc
		List<Map<String, Object>> dcList = dcService.listMaps(wrapper);
		List<Map<String, Object>> dcGroupList = msgHandlerDao.queryAlertMessageGroupBy("am.dc_id", "dc.name", params);
		this.alertMessageGroupBysetNum(dcList, dcGroupList);
		result.put("dc", dcList);
		//project
		List<Map<String, Object>> projectList = projectService.listMaps(wrapper);
		List<Map<String, Object>> projecGrouptList = msgHandlerDao.queryAlertMessageGroupBy("am.project_id", "p.name", params);
		this.alertMessageGroupBysetNum(projectList, projecGrouptList);
		result.put("project", projectList);
		//module
		List<Map<String, Object>> moduleList = moduleService.listMaps(wrapper);
		List<Map<String, Object>> moduleGroupList = msgHandlerDao.queryAlertMessageGroupBy("am.module_id", "m.name", params);
		this.alertMessageGroupBysetNum(moduleList, moduleGroupList);
		result.put("module", moduleList);
		//rule
		List<Map<String, Object>> ruleList = alertRuleService.listMaps(wrapper);
		List<Map<String, Object>> ruleGroupList = msgHandlerDao.queryAlertMessageGroupBy("am.rule_id", "ar.name", params);
		this.alertMessageGroupBysetNum(ruleList, ruleGroupList);
		result.put("rule", ruleList);
		//priority
		wrapper.select("id","name","color");
		List<Map<String, Object>> severityList = alertSeverityService.listMaps(wrapper);
		List<Map<String, Object>> severityGroupList = msgHandlerDao.queryAlertMessageGroupBy("am.severity_id", "alsc.name", params);
		this.alertMessageGroupBysetNum(severityList, severityGroupList);
		result.put("severity", severityList);	
		return result;
	}
	
    private void alertMessageGroupBysetNum(List<Map<String,Object>> allList,List<Map<String,Object>> groupList) {
    	if(Tool.CollUtil.isEmpty(groupList)) {
    		return;
    	}
    	Map<String,Object> groupMap = new HashMap<String, Object>();
    	for(Map<String,Object> m : groupList) {
    		groupMap.put(String.valueOf(m.get("id")), m.get("num"));
    	}
    	for(Map<String,Object> m : allList) {
    		m.put("num", groupMap.getOrDefault(String.valueOf(m.get("id")), 0));
    	}
		/* 排序会导致筛选条件跳动 先注释
		allList.sort((Map<String,Object> m1, Map<String,Object> m2) -> {
			Integer i1 = Integer.valueOf(String.valueOf(m1.getOrDefault("num", 0)));
			Integer i2 = Integer.valueOf(String.valueOf(m2.getOrDefault("num", 0)));
			return Integer.compare(i2, i1);
		});*/
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
	public void exportMessage(HttpServletResponse response, Map<String, Object> params) throws IOException {
		String maxLine = sysConfigService.getValue(Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE);
		long exportMaxLine = Integer.parseInt(StrUtil.emptyToDefault(maxLine, "10000"));
		// 导出最大条数限制
		Object limit = params.get(Constant.LIMIT) == null ? Constant.PAGESIZE : params.get(Constant.LIMIT);
		Long pageSize = Long.parseLong(limit.toString());
		if (ObjectUtil.isEmpty(pageSize) || pageSize == -1 || pageSize > exportMaxLine) {
			params.put(Constant.LIMIT, Long.toString(exportMaxLine));
		}
		PageUtils page = this.queryMessagePage(params);
		List<AlertMessageEntity> list = (List<AlertMessageEntity>) page.getList();

		List<List<String>> exportDataList = this.handleExportDataList(list);

		String sysHeaderJsonStr = sysConfigService.getValue("alert_export_header");
		String fileName = "AlertMessages";
		String formatStr = params.get("format").toString();
		basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
	}

	private List<List<String>> handleExportDataList(List<AlertMessageEntity> list) {
		List<List<String>> exportDataList = new ArrayList<>();

		for (AlertMessageEntity alertMessage : list) {
			List<String> tempList = new ArrayList<>();

			String startAt = ToolUtil.isNotEmpty(alertMessage.getStartAt()) ? DateUtil.formatDate(alertMessage.getStartAt(), "yyyy-MM-dd HH:mm:ss") : "";
			String endAt = ToolUtil.isNotEmpty(alertMessage.getEndAt()) ? DateUtil.formatDate(alertMessage.getEndAt(), "yyyy-MM-dd HH:mm:ss") : "";

			tempList.add(ObjectUtil.defaultIfNull(alertMessage.getId(), "").toString());
			tempList.add(alertMessage.getAlertRule().getName());
			tempList.add(ToolUtil.isNotEmpty(alertMessage.getDc()) ? alertMessage.getDc().getName() : "");
			tempList.add(ToolUtil.isNotEmpty(alertMessage.getProject()) ? alertMessage.getProject().getName() : "");
			tempList.add(ToolUtil.isNotEmpty(alertMessage.getModule()) ? alertMessage.getModule().getName() : "");
			tempList.add(ToolUtil.isNotEmpty(alertMessage.getEndpoint()) ? alertMessage.getEndpoint().getName() : "");
			tempList.add(ToolUtil.isNotEmpty(alertMessage.getAsset()) ? alertMessage.getAsset().getName() : "");
			tempList.add(ToolUtil.isNotEmpty(alertMessage.getLabels()) ? alertMessage.getLabels() : "");
			tempList.add(ToolUtil.isNotEmpty(alertMessage.getSeverity()) ? alertMessage.getSeverity().getName() : "");
			tempList.add(alertMessage.getSummary());
			tempList.add(alertMessage.getDescription());
			tempList.add(startAt);
			tempList.add(endAt);
			tempList.add(Constant.ALERT_MESSAGE_STATE_NAME_MAPPING.get(alertMessage.getState()));
			exportDataList.add(tempList);
		}
		return exportDataList;
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
		//判断是否超出所设置的队列最大长度
		Long size = redisTemplate.opsForList().size(Constant.ALERT_MESSAGE_NOTIFY);
		if(size > maxQueueLength) {
			redisTemplate.opsForList().trim(Constant.ALERT_MESSAGE_NOTIFY, 0, maxQueueLength-1);
		}
	}
	
	/**
	 * 补充alermessage 内容
	 * 
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
			if(ep != null) {
				message.setEndpointId(endpointId);
				moduleId = ep.getModuleId();
				assetId = ep.getAssetId();
				message.setEndpointId(endpointId);
				message.setEndpoint(ep);
			}
		}

		// 获取 asset id
		if (message.getAsset() == null && assetId != null) {
			AssetAsset asset = assetAssetService.getById(assetId);
			if(asset != null) {
				dcId = asset.getDcId();
				message.setAssetId(assetId);
				message.setAsset(asset);
			}
		}
		// 获取dc
		if (message.getDc() == null && dcId != null ) {
			Dc dc = dcService.getById(dcId);
			if(dc != null) {
				message.setDcId(dcId);
				message.setDc(dc);
			}
		}
		// 判断 module id 是否存在
		if (message.getModule() == null && moduleId != null ) {
			MonitorModule module = moduleService.getById(moduleId);
			if(module != null) {
				projectId = module.getProjectId();
				message.setModuleId(moduleId);
				message.setModule(module);
			}
		}
		// 判断 module id 是否存在
		if (message.getProject() == null && projectId != null) {
			MonitorProject project = projectService.getById(projectId);
			if(project != null) {
				message.setProjectId(projectId);
				message.setProject(project);
			}
		}
		// 获取alertrule
		if (message.getAlertRule() == null && ruleId != null) {
			AlertRuleEntity ruleEntity = alertRuleService.getById(ruleId);
			if(ruleEntity != null) {
				message.setRuleId(ruleId);
				message.setAlertRule(ruleEntity);
			}
		}
		//获取  Severity_Id
		if(message.getSeverity() == null && severityId != null) {
			AlertSeverityConf severityConf = alertSeverityService.getById(severityId);
			if(severityConf != null) {
				message.setSeverityId(severityId);
				message.setSeverity(severityConf);
			}
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
		// 确定 endpoint 两种途径  1 ，根据 endpoint id 获取  2. 根据 module name + endpoint name 获取 ， 因为 module name 唯一  endpoint name 同一个 Module 下唯一
		if (Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_ENDPOINT_ID_KEY)) || (Tool.StrUtil.isNotEmpty(labels.get(Constant.LABEL_ENDPOINT_NAME_KEY)) && Tool.StrUtil.isNotEmpty(labels.get(Constant.LABEL_MODULE_NAME_KEY)))) {
			endpointId = Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_ENDPOINT_ID_KEY)) ? Tool.NumberUtil.parseInt(labels.get(Constant.LABEL_ENDPOINT_ID_KEY)) : null;
			MonitorEndpoint ep = null;
			if (endpointId != null) {
				ep = endpointService.getById(endpointId);
			}
			String endpointName = labels.get(Constant.LABEL_ENDPOINT_NAME_KEY);
			String moduleName = labels.get(Constant.LABEL_MODULE_NAME_KEY);
			if (Tool.StrUtil.isNotEmpty(endpointName) && Tool.StrUtil.isNotEmpty(moduleName) && ep == null) {
				MonitorModule module = moduleService.getOne(new LambdaQueryWrapper<MonitorModule>().eq(MonitorModule::getName, moduleName));
				ep = endpointService.getOne(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getModuleId, module.getId()).eq(MonitorEndpoint::getName, endpointName));
			}
			
			if(ep != null) {
				// 当 moduleid 不存在，但是 endpoint id存在时，通过库表查询赋值
				message.setEndpointId(ep.getId());
				moduleId = ep.getModuleId();
				assetId = ep.getAssetId();
				/*Integer enabled = ep.getEnabled();
				if (Tool.ObjectUtil.equal(enabled, 0)) {// disable直接返回 丢弃告警消息
					if (log.isDebugEnabled())
						log.debug("endpoint state : {}", enabled);
					return null;
				}*/
				message.setEndpointId(ep.getId());
				message.setEndpoint(ep);
			}
			
		}

		// 获取 asset id
		if (assetId != null || Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_ASSET_ID_KEY)) || Tool.StrUtil.isNotEmpty(labels.get(Constant.LABEL_ASSET_NAME_KEY))) {
			Integer aid = Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_ASSET_ID_KEY)) ? Tool.NumberUtil.parseInt(labels.get(Constant.LABEL_ASSET_ID_KEY)) : null;
			assetId = (aid != null) ? aid : assetId;
			AssetAsset asset = null;
			if (assetId != null) {
				asset = assetAssetService.getById(assetId);
			}
			// 通过 assetId 找不到的，在尝试用 assetNameStr 获取 ，因为 assetNameStr 同样唯一
			String assetNameStr = labels.get(Constant.LABEL_ASSET_NAME_KEY);
			if (Tool.StrUtil.isNotEmpty(assetNameStr) && asset == null) {
				asset = assetAssetService.getOne(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getName, assetNameStr));
			}
			if(asset != null) {
				dcId = asset.getDcId();
				Integer stateId = asset.getStateId();
				AssetStateConf stateConf = assetStateService.getById(stateId);
				Integer alert = stateConf.getAlert();
				if (Tool.ObjectUtil.equal(alert, 0)) {// disable直接返回 丢弃告警消息
					if (log.isDebugEnabled())
						log.debug("asset state : {}", alert);
					return null;
				}
				message.setAssetId(asset.getId());
				message.setAsset(asset);
			}
		}
		// 获取dc
		if (dcId != null || Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_DC_ID_KEY)) || Tool.StrUtil.isNotEmpty(labels.get(Constant.LABEL_DC_NAME_KEY))) {
			Integer did = Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_DC_ID_KEY)) ? Tool.NumberUtil.parseInt(labels.get(Constant.LABEL_DC_ID_KEY)) : null;
			dcId = (did != null) ? did : dcId;
			Dc dc = null;
			if (dcId != null) {
				dc = dcService.getById(dcId);
			}
			// 通过 dcId 找不到的，在尝试用 dcname 获取 ，因为 dc name 同样唯一
			String dcNameStr = labels.get(Constant.LABEL_DC_NAME_KEY);
			if (Tool.StrUtil.isNotEmpty(dcNameStr) && dc == null) {
				dc = dcService.getOne(new LambdaQueryWrapper<Dc>().eq(Dc::getName, dcNameStr));
			}
			
			if(dc != null) {
				String state = dc.getState();
				if (Tool.StrUtil.equalsIgnoreCase(state, "OFF")) {// disable直接返回 丢弃告警消息
					if (log.isDebugEnabled())
						log.debug("dc state : {}", state);
					return null;
				}
				message.setDcId(dc.getId());
				message.setDc(dc);
			}
		}
		// 判断 module id 是否存在
		if (moduleId != null || Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_MODULE_ID_KEY)) || Tool.StrUtil.isNotEmpty(labels.get(Constant.LABEL_MODULE_NAME_KEY))) {
			Integer mid = Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_MODULE_ID_KEY)) ? Tool.NumberUtil.parseInt(labels.get(Constant.LABEL_MODULE_ID_KEY)) : null;
			moduleId = (mid != null) ? mid : moduleId;
			MonitorModule module = null;
			if (moduleId != null) {
				module = moduleService.getById(moduleId);
			}
			// 通过 moduleId 找不到的，在尝试用 moduleName 获取 ，因为 module name 同样唯一
			String moduelNameStr = labels.get(Constant.LABEL_MODULE_NAME_KEY);
			if (Tool.StrUtil.isNotEmpty(moduelNameStr) && module == null) {
				module = moduleService.getOne(new LambdaQueryWrapper<MonitorModule>().eq(MonitorModule::getName, moduelNameStr));
			}
			if(module != null) {
				projectId = module.getProjectId();
				message.setModuleId(module.getId());
				message.setModule(module);
			}
		}
		// 判断 module id 是否存在
		if (projectId != null || Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_PROJECT_ID_KEY)) || Tool.StrUtil.isNotEmpty(labels.get(Constant.LABEL_PROJECT_NAME_KEY))) {
			Integer pid = Tool.NumberUtil.isNumber(labels.get(Constant.LABEL_PROJECT_ID_KEY)) ? Tool.NumberUtil.parseInt(labels.get(Constant.LABEL_PROJECT_ID_KEY)) : null;
			projectId = (pid != null) ? pid : projectId;
			MonitorProject project = null;
			if (projectId != null) {
				project = projectService.getById(projectId);
			}
			// 通过 projectId 找不到的，在尝试用 projectName 获取 ，因为 projectName 同样唯一
			String projectNameStr = labels.get(Constant.LABEL_PROJECT_NAME_KEY);
			if (Tool.StrUtil.isNotEmpty(projectNameStr) && project == null) {
				project = projectService.getOne(new LambdaQueryWrapper<MonitorProject>().eq(MonitorProject::getName, projectNameStr));
			}
			if(project != null) {
				message.setProjectId(project.getId());
				message.setProject(project);
			}
		}
		// 获取alertrule
		String ruleIdStr = labels.get(Constant.LABEL_RULE_ID_KEY);
		if (Tool.NumberUtil.isNumber(ruleIdStr)) {
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
		DateTime endAt = Tool.DateUtil.parseUTC(dto.getEndsAt());
		DateTime startAt = Tool.DateUtil.parseUTC(dto.getStartsAt());
		message.setEndAt(endAt);
		message.setStartAt(startAt);
		// 设置 annotation 信息
		Map<String, String> receiveAnnotations = dto.getAnnotations();
		message.setSummary(receiveAnnotations.get("summary"));
		message.setDescription(receiveAnnotations.get("description"));
		message.setLabels(Tool.JSONUtil.toJsonStr(labels));
		return message;
	}
	
	@Override
	public void updateCloseExpiredMessageEndAt(String tableName,Date endAt) {
		msgHandlerDao.updateCloseExpiredMessageEndAt(tableName, endAt);
	}
}
