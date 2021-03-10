package com.nis.modules.alert.service.impl;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.dao.AlertSilenceDao;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertSilenceConf;
import com.nis.modules.alert.service.AlertSilenceService;
import com.nis.modules.asset.dao.AssetDao;
import com.nis.modules.asset.dao.IdcDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.endpoint.dao.EndpointDao;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.module.dao.ModuleDao;
import com.nis.modules.module.entity.Module;
import com.nis.modules.project.dao.ProjectDao;
import com.nis.modules.project.entity.Project;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;

@Service("alertSilenceService")
public class AlertSilenceServiceImpl extends ServiceImpl<AlertSilenceDao,AlertSilenceConf> implements AlertSilenceService{
	
	private final static Log logger = Log.get();
	
	@Autowired
	private AlertSilenceDao alertSilenceDao;
	
	@Autowired
	private IdcDao idcDao;
	
	@Autowired
	private AssetDao assetDao;
	
	@Autowired
	private ModuleDao moduleDao;
	
	@Autowired
	private ProjectDao projectDao;
	
	@Autowired
	private EndpointDao endpointDao;
	
	@Autowired
	private AlertRuleDao alertRuleDao;
	
	@Autowired
	private AlertMessageDao alertMessageDao;
	
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
		
		IPage<AlertSilenceConf> page = new Query<AlertSilenceConf>().getPage(params);
		List<AlertSilenceConf> alertSilences = alertSilenceDao.selectAlertSilenceConfs(page,params);
		page.setRecords(alertSilences);
		return new PageUtils(page);
	}

	@Override
	public void saveOrUpdateAlertSilence(AlertSilenceConf alertSilenceConf) {
		// 告警规则id 和 linkId 不能同时为空
		if ((ObjectUtil.isEmpty(alertSilenceConf.getRuleId())) && (ObjectUtil.isEmpty(alertSilenceConf.getLinkId())))
		{
			throw new NZException(RCode.ALERTSILENCE_RULE_LINK_IS_NULL);
		}
		// 关联id，存在时 type不能为空
		if(ObjectUtil.isNotEmpty(alertSilenceConf.getLinkId())&& (ObjectUtil.isEmpty(alertSilenceConf.getType()))) 
		{
			throw new NZException(RCode.ALERTSILENCE_TYPE_LINKID_IS_NULL);
		}
		
		// 校验type类型是否正确
		if(ObjectUtil.isNotEmpty(alertSilenceConf.getType())&&(!this.checkType(alertSilenceConf.getType())))
		{
			throw new NZException(RCode.ALERTSILENCE_TYPE_ERROR);
		}
		
		// 根据传入的时间判断静默的状态
	    Date currentTime = DateUtil.getUTCTimeByConfigTimeZone();
	    /**
	     *  判断当前时间与静默设置时间的对比 
	     *  如果当前时间小于开始时间 状态为active
	     *  如果当前时间大于开始时间 小于结束时间 状态为pending
	     *  如果当前时间大于结束时间 状态为expired 
	     */
		if(currentTime.before(alertSilenceConf.getStartAt())) {
			alertSilenceConf.setState(1);
		}else if(currentTime.after(alertSilenceConf.getStartAt())&&currentTime.before(alertSilenceConf.getEndAt())){
			alertSilenceConf.setState(2);
		}else {
			alertSilenceConf.setState(3);
		}
	    
		//更新时间
		alertSilenceConf.setUtime(DateUtil.getUTCTimeByConfigTimeZone());
		
		if(ObjectUtil.isNotEmpty(alertSilenceConf.getId())) {
			alertSilenceDao.updateById(alertSilenceConf);
		}else {
			alertSilenceDao.insert(alertSilenceConf);
		}
		
		// 如果alert message已经存在告警 需要将符合静默的数据状态state改为3
		if(alertSilenceConf.getState().equals(2)) {
			alertMessageDao.updateStateBySilence(alertSilenceConf);
		}		
	}

	@Override
	public void removeByIds(List<Integer> ids) {
		/**
		 * 不直接删除表中数据
		 * 当 silence 状态为 active 时，修改 endtime 为当前时间，并将 state 改为 expired
         * 当 silence 状态为 pending 时，修改 starttime，endtime 为当前时间，并将 state 改为 expired
		 */
		List<AlertSilenceConf> alertSilenceConfs = alertSilenceDao.selectBatchIds(ids);
		for(AlertSilenceConf alertSilenceConf : alertSilenceConfs) {
			if(alertSilenceConf.getState().equals(1)) {
				alertSilenceConf.setEndAt(DateUtil.getUTCTimeByConfigTimeZone());
				alertSilenceConf.setState(3);
			}else if(alertSilenceConf.getState().equals(2)) {
				alertSilenceConf.setStartAt(DateUtil.getUTCTimeByConfigTimeZone());
				alertSilenceConf.setEndAt(DateUtil.getUTCTimeByConfigTimeZone());
				alertSilenceConf.setState(3);
			}
			alertSilenceConf.setUtime(DateUtil.getUTCTimeByConfigTimeZone());
		}
		this.saveOrUpdateBatch(alertSilenceConfs);
	}

	@Override
	public AlertSilenceConf queryAlertSilence(Integer id) {
		AlertSilenceConf alertSilenceConf = alertSilenceDao.selectById(id);

		if(ObjectUtil.isNotEmpty(alertSilenceConf)) {
			
			AlertRuleEntity alertRule = alertRuleDao.selectById(alertSilenceConf.getRuleId());
			alertSilenceConf.setAlertRule(alertRule);
			
			if(ObjectUtil.isNotEmpty(alertSilenceConf.getType())) {
				switch(alertSilenceConf.getType()) {
					case "datacenter":
						Idc idc = idcDao.selectById(alertSilenceConf.getLinkId());
						alertSilenceConf.setDatacenter(idc);
						break;
					case "project":
						Project project = projectDao.selectById(alertSilenceConf.getLinkId());
						alertSilenceConf.setProject(project);
						break;
					case "module":
						Module module = moduleDao.selectById(alertSilenceConf.getLinkId());
						alertSilenceConf.setModule(module);
						break;
					case "asset":
						Asset asset = assetDao.selectById(alertSilenceConf.getLinkId());
						alertSilenceConf.setAsset(asset);
						break;
					case "endpoint":
						Endpoint endpoint = endpointDao.selectById(alertSilenceConf.getLinkId());
						alertSilenceConf.setEndpoint(endpoint);
						break;
				}
			}
		}
		return alertSilenceConf;
	}
	
	public Boolean checkType(String type) {
		Boolean result=true;
		switch(type) {
			case "datacenter":
				break;
			case "project":
				break;
			case "module":
				break;
			case "endpoint":
				break;
			case "asset":
				break;
			default: 
				result=false;
		}
		return result;
	}
	
}
