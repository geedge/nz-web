package com.nis.modules.alert.service.impl;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.nis.common.utils.Constant;
import com.nis.common.utils.MailUtil;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertNameMapUserDTO;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.sys.entity.SysUserEntity;


@Service("alertMessageService")
public class AlertMessageServiceImpl extends ServiceImpl<AlertMessageDao, AlertMessageEntity> implements AlertMessageService {

	@Value("${spring.mail.open}")
	private Integer sendEmail; //0不通知，1通知  默认通知
	@Autowired
	private MailUtil mailUtil;
	@Autowired
	private AssetService assetService;
	@Autowired
	private AlertRuleService alertRuleService;
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
    
	private static Gson gson = new Gson();
	
    @Override
    public PageUtils queryPage(Map<String, Object> params) throws IllegalAccessException, InvocationTargetException {
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
    	
    	AlertMessageEntity queryEntity = new AlertMessageEntity();
    	BeanUtils.populate(queryEntity, params);
    	
    	IPage<AlertMessageEntity> page = baseMapper.list(new Query<AlertMessageEntity>().getPage(params), queryEntity);
    	
        return new PageUtils(page);
    }

    @Override
    public void saveBatch(AlertMessageReceiveDTO[] receiveData) throws Exception {
    	List<AlertMessageEntity> entities = new ArrayList<>();
    	//取出所有的host，一次查询出assetId，再setId
    	Set<String> hosts = new HashSet<>();
    	Set<String> alertNames = new HashSet<>();
    	
    	for (AlertMessageReceiveDTO dto : receiveData) {
    		//取出labels，计算hash，查询redis看是否有重复
    		Map receiveLabels = dto.getLabels();
    		String md5DigestAsHex = DigestUtils.md5DigestAsHex(gson.toJson(receiveLabels).getBytes());
    		HashOperations<String, String, String> ops = redisTemplate.opsForHash();
    		if (!StringUtils.isBlank(ops.get(Constant.REDIS_KEY_ALERTMESSAGE, md5DigestAsHex))) {
    			continue; //重复则跳过
    		} else {
    			//存入redis
    			ops.put(Constant.REDIS_KEY_ALERTMESSAGE, md5DigestAsHex, gson.toJson(receiveLabels));
    			//转化为入库的实体类
    			AlertMessageEntity entity = AlertMessageReceiveDTO.toAlertMessageEntity(dto);
    			hosts.add(entity.getHost());
    			alertNames.add(entity.getAlertName());
    			entities.add(entity);
    		}
    	}
    	if (!CollectionUtils.isEmpty(entities)) {
	    	List<Asset> queryListByHosts = assetService.queryListByHosts(hosts);
	    	for (AlertMessageEntity entity : entities) {
	    		for (Asset asset : queryListByHosts) {
	    			if (entity.getHost().equals(asset.getHost())) {
	    				entity.setAssetId(Integer.parseInt(asset.getId() + ""));
	    				break;
	    			}
	    		}
	    	}
	    	this.saveBatch(entities);
    	
	    	//邮件通知
	    	if (sendEmail == null || sendEmail == 1) {
	    		List<AlertNameMapUserDTO> alertNameMapEmail = alertRuleService.queryUserByAlertName(alertNames);
	    		for (AlertMessageEntity entity : entities) {
	        		for (AlertNameMapUserDTO dto : alertNameMapEmail) {
	        			if (entity.getAlertName().equals(dto.getAlertName())) {
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

}