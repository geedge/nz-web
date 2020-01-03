package com.nis.modules.alert.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.project.service.ProjectService;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.*;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertNameMapUserDTO;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.sys.entity.SysUserEntity;

import javax.mail.SendFailedException;


@Service("alertMessageService")
public class AlertMessageServiceImpl extends ServiceImpl<AlertMessageDao, AlertMessageEntity> implements AlertMessageService {
	private Logger logger= LoggerFactory.getLogger(AlertMessageServiceImpl.class);
	@Value("${spring.mail.open}")
	private Integer sendEmail; //0不通知，1通知  默认通知
	@Autowired
	private MailUtil mailUtil;
	@Autowired
	private AssetService assetService;
	@Autowired
	private ModuleService moduleService;
	@Autowired
	private ProjectService projectService;
	@Autowired
	private AlertRuleService alertRuleService;
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private AlertMessageDao alertMessageDao;
    
	private static Gson gson = new Gson();
	
    @Override
    public PageUtils queryPage(Map<String, Object> params)  {
    	try {
			if (params.get("startAt") != null && !StringUtils.isBlank(params.get("startAt").toString())) {
				Date searchStartAt = DateUtils.parseDate(params.get("startAt").toString(), "yyyy-MM-dd HH:mm:ss");
				params.put("startAt", searchStartAt);
			}
			if (params.get("endAt") != null && !StringUtils.isBlank(params.get("endAt").toString())) {
				Date searchEndAt = DateUtils.parseDate(params.get("endAt").toString(), "yyyy-MM-dd HH:mm:ss");
				params.put("endAt", searchEndAt);
			}

		} catch (ParseException e) {}

    	Integer searchId=resolveParamToInteger(params,"id");
    	params.put("id",searchId);

    	Integer searchType=resolveParamToInteger(params,"type");
		params.put("type",searchType);

    	Integer searchLinkId=resolveParamToInteger(params,"linkId");
		params.put("linkId",searchLinkId);

    	Integer searchState=resolveParamToInteger(params,"state");
		params.put("state",searchState);

		IPage<AlertMessageEntity> page=this.page(new Query<AlertMessageEntity>().getPage(params));

		List<AlertMessageEntity> alertMessages=this.baseMapper.list(page,params);

		for (AlertMessageEntity am:alertMessages){
			if(am.getType()!=null){
				IService service=getServiceImpl(am.getType());
				Object linkObject = service.getById(am.getLinkId());

				am.setLinkObject(linkObject);
			}
		}
		page.setRecords(alertMessages);

        return new PageUtils(page);
    }

    @Override
    public void saveBatch(AlertMessageReceiveDTO[] receiveData)  {
    	logger.info("receive promethues alert message {}",receiveData==null?null:Arrays.asList(receiveData).toString());
    	List<AlertMessageEntity> entities = new ArrayList<>();
    	Set<Integer> ruleIds = new HashSet<>();
		redisTemplate.executePipelined(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection redis) throws DataAccessException {
				for (AlertMessageReceiveDTO dto : receiveData) {
					try{
						AlertMessageEntity entity = AlertMessageReceiveDTO.toAlertMessageEntity(dto);
						ruleIds.add(entity.getRuleId());
						entities.add(entity);
						HashMap<String, Object> map = new HashMap<>();
						map.put("id",entity.getRuleId());
						map.put("time",new Date().getTime());
						//存在更新时间，不存在set
						redis.hSet(Constant.REDIS_KEY_ALERTMESSAGE.getBytes(),entity.getHashKey().getBytes(),JSON.toJSONString(map).getBytes());
					}catch (Exception e){
						throw new NZException(RCode.ALERTMSG_SAVE_ERROR);
					}
				}
				return null;
			}
		});
		logger.debug("数据格式化完成 : {}",entities.toString());
    	if (!CollectionUtils.isEmpty(entities)) {
	    	this.saveBatch(entities);
	    	//邮件通知
	    	if (sendEmail == null || sendEmail == 1) {
	    		List<AlertNameMapUserDTO> alertNameMapEmail = alertRuleService.queryUserByRuleId(ruleIds);
	    		for (AlertMessageEntity entity : entities) {
	        		for (AlertNameMapUserDTO dto : alertNameMapEmail) {
	        			if (entity.getRuleId().intValue()==dto.getRuleId().intValue()) {
	        				List<String> toSendEmails = new ArrayList<>();
	        				List<SysUserEntity> users = dto.getUsers();
	        				for (SysUserEntity user : users) {
	        					if (!StringUtils.isBlank(user.getEmail())) {
	        						toSendEmails.add(user.getEmail());
	        					}
	        				}
							//TODO 邮件内容/格式 待定
							mailUtil.sendHTML(toSendEmails, entity.getSummary(), entity.getDescription());

							break;
	        			}
	        		}
	        	}
	    	}
    	}
    }
    
	@Override
	public boolean updateByIds(AlertMessageEntity alertMessage) {
		String ids = alertMessage.getIds();
		Object[] splitedIds = ids.split(",");
		baseMapper.update(null, new UpdateWrapper<AlertMessageEntity>().in("id", splitedIds).set("state", alertMessage.getState()));
		return true;
	}
	/**
	 * 根据type获取相应service
	 * */
	private IService getServiceImpl(Integer typeId){
		Constant.AlertType alertType = Constant.AlertType.getAlertType(typeId);
		if(alertType==null){
			throw new NZException(RCode.ALERTRULE_TYPE_INVALIDE);
		}
		switch (alertType){
			case ALERT_TYPE_PROJECT:
				return projectService;
			case ALERT_TYPE_MODULE:
				return moduleService;
			case ALERT_TYPE_ASSET:
				return assetService;
		}
		return null;
	}

	private Integer resolveParamToInteger(Map<String,Object> params,String paramName){
		Integer param=null;
		try {
			if(params.get(paramName)!=null&&StringUtils.isNotBlank(params.get(paramName).toString())){
				param=Integer.parseInt(params.get(paramName).toString());
			}
		}catch (NumberFormatException e){}

		return param;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeByTypeIds(Integer typeId, List ids) {
		alertMessageDao.deleteByTypeIds(typeId, ids);
	}

}