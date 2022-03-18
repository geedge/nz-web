package com.nis.modules.alert.service.impl;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.dao.AlertSilenceDao;
import com.nis.modules.alert.entity.AlertSilenceConf;
import com.nis.modules.alert.entity.AlertSilenceMatcher;
import com.nis.modules.alert.service.AlertHandlerService;
import com.nis.modules.alert.service.AlertSilenceService;

import cn.hutool.core.date.DateTime;
import cn.hutool.log.Log;

@Service("alertSilenceService")
public class AlertSilenceServiceImpl extends ServiceImpl<AlertSilenceDao,AlertSilenceConf> implements AlertSilenceService{
	
	private final static Log logger = Log.get();
	
	@Autowired
	private AlertSilenceDao alertSilenceDao;
	@Autowired
	private AlertHandlerService handlerService;
	
	@Override
	public AlertSilenceConf queryAlertSilence(Integer id) {
		AlertSilenceConf alertSilenceConf = alertSilenceDao.selectById(id);
		return alertSilenceConf;
	}
	
	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		try {
			if (params.get("startAt") != null && !StringUtils.isBlank(params.get("startAt").toString())) {
				Date searchStartAt = DateUtils.parseDate(params.get("startAt").toString(), "yyyy-MM-dd HH:mm:ss");
				params.put("startAt", searchStartAt);
			}
			if (params.get("endAt") != null && !StringUtils.isBlank(params.get("endAt").toString())) {
				Date searchEndAt = DateUtils.parseDate(params.get("endAt").toString(), "yyyy-MM-dd HH:mm:ss");
				params.put("endAt", searchEndAt);
			}
		} catch (ParseException e) {
			logger.error("时间转换异常,参数是 {},异常信息：{}", params, e);
		}
		
		IPage page = new Query(AlertSilenceConf.class).getPage(params);
		List<AlertSilenceConf> alertSilences = alertSilenceDao.selectAlertSilenceConfs(page,params);
		page.setRecords(alertSilences);
		return new PageUtils(page);
	}

	/**
	 * 新增  silence conf 告警静默规则
	 */
	@Transactional
	@Override
	public void saveAlertSilenceConf(AlertSilenceConf conf) {
		DateTime now = Tool.DateUtil.date();
		conf.setUtime(now);
		if(Tool.DateUtil.compare(conf.getEndAt(), now)<0) {
			throw new NZException(RCode.ALERTSILENCE_ENDTIME_MUSTGTNOW);
		}
		String matchers = conf.getMatchers();
		List<AlertSilenceMatcher> matcherList = Tool.JSONUtil.toList(matchers, AlertSilenceMatcher.class);
		if(Tool.CollUtil.isEmpty(matcherList)) {
			throw new NZException(RCode.ALERTSILENCE_MATCHER_ISNULL);
		}
		if(Tool.DateUtil.compare(conf.getStartAt(), now)<0) {//直接生效
			conf.setState(Constant.ALERT_SILENCE_STATE_ACTIVE);
			alertSilenceDao.insert(conf);
			logger.debug("save silenceconf : {}",Tool.JSONUtil.toJsonStr(conf));
			List<AlertSilenceConf> list = Tool.ListUtil.toList(conf);
			//处理已经处于 active 状态的 alert message是否符合静默规则
			handlerService.handleSilenceConfPendingToActive(list);
		}else {
			conf.setState(Constant.ALERT_SILENCE_STATE_PENDING);
			alertSilenceDao.insert(conf);
			logger.debug("save silenceconf : {}",Tool.JSONUtil.toJsonStr(conf));
		}
	}
	
	/**
	 * 更新 silencConf
	 * @param conf
	 */
	@Override
	public void updateAlertSilenceConf(AlertSilenceConf conf) {
		DateTime now = Tool.DateUtil.date();
		conf.setUtime(now);
		if(Tool.DateUtil.compare(conf.getEndAt(), now)<0) {
			throw new NZException(RCode.ALERTSILENCE_ENDTIME_MUSTGTNOW);
		}
		String matchers = conf.getMatchers();
		List<AlertSilenceMatcher> matcherList = Tool.JSONUtil.toList(matchers, AlertSilenceMatcher.class);
		if(Tool.CollUtil.isEmpty(matcherList)) {
			throw new NZException(RCode.ALERTSILENCE_MATCHER_ISNULL);
		}
		AlertSilenceConf current = alertSilenceDao.selectById(conf.getId());
		Integer currState = current.getState();//当前状态
		if(currState == Constant.ALERT_SILENCE_STATE_EXPIRED) {
			conf.setState(Constant.ALERT_SILENCE_STATE_EXPIRED);
			alertSilenceDao.updateById(conf);
			return;
		}
		if(Tool.DateUtil.compare(conf.getStartAt(), now)<0) {
			conf.setState(Constant.ALERT_SILENCE_STATE_ACTIVE);
			alertSilenceDao.updateById(conf);
			if(currState == Constant.ALERT_SILENCE_STATE_ACTIVE) {//当前正处于生效状态
				//将以前 处于静音的 消息失效
				handlerService.handleSilenceConfActiveToExired(Tool.ListUtil.toList(current));
			}
			//根据 更新后的规则匹配消息
			handlerService.handleSilenceConfPendingToActive(Tool.ListUtil.toList(conf));
		}else {
			conf.setState(Constant.ALERT_SILENCE_STATE_PENDING);
			alertSilenceDao.updateById(conf);
			//由   active 变更为 pending
			if(currState ==  Constant.ALERT_SILENCE_STATE_ACTIVE) {
				handlerService.handleSilenceConfActiveToExired(Tool.ListUtil.toList(conf));
			}
		}
	}
	
	/**
	 * 将 silence conf 状态改为  expired
	 * @param conf
	 */
	@Transactional
	@Override
	public void updateAlertSilenceConfExpiredBatch(Integer ... ids) {
		for(Integer id : ids) {
			if(id == null) {
				continue;
			}
			AlertSilenceConf conf = alertSilenceDao.selectById(id);
			if(conf == null) {
				continue;
			}
			DateTime now = Tool.DateUtil.date();
			conf.setEndAt(now);
			conf.setUtime(now);
			if(conf.getState() == Constant.ALERT_SILENCE_STATE_PENDING) {
				conf.setState(Constant.ALERT_SILENCE_STATE_EXPIRED);
				conf.setStartAt(now);
				//alertSilenceDao.updateById(conf);
				alertSilenceDao.deleteById(id);
			} else if(conf.getState() == Constant.ALERT_SILENCE_STATE_ACTIVE) {
				conf.setState(Constant.ALERT_SILENCE_STATE_EXPIRED);
				//alertSilenceDao.updateById(conf);
				alertSilenceDao.deleteById(id);
				//处理 处于静默状态的告警消息
				handlerService.handleSilenceConfActiveToExired(Tool.ListUtil.toList(conf));
			}else {
				conf.setState(Constant.ALERT_SILENCE_STATE_EXPIRED);
				//alertSilenceDao.updateById(conf);
				alertSilenceDao.deleteById(id);
			}
		}
	}

	/**
	 * 告警静音关联消息查询
	 * @param silenceId
	 * @param params
	 * @return
	 */
	@Override
	public PageUtils querySilenceMsgRelObj(Integer silenceId, Map<String, Object> params) {
		IPage page = new Query(AlertSilenceConf.class).getPage(params);
		List<AlertSilenceConf> alertSilences = alertSilenceDao.selectAlertSilenceMessageRel(page, params, silenceId);
		page.setRecords(alertSilences);
		return new PageUtils(page);
	}

}
