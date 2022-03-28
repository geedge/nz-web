package com.nis.modules.alert.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.dao.AlertHandlerDao;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service("alertMessageService")
public class AlertMessageServiceImpl extends ServiceImpl<AlertMessageDao, AlertMessageEntity> implements AlertMessageService {

    private Log log = Log.get();

    @Autowired
    private AlertMessageDao alertMessageDao;
    @Autowired
    private AlertHandlerDao msgHandlerDao;
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	@Value("${redis.maxQueueLength:10000}")
    private Long maxQueueLength;//redis队列长度
	
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByAssetIds(List<Integer> assetIdList) {
        List<Long> silencMessageIds = msgHandlerDao.selectAlertSilencMessageIdByAssetIds(assetIdList);
        // 存在静默告警消息，删除 alert_silence_message_rel 表关系
        if (CollectionUtils.isNotEmpty(silencMessageIds)) {
            msgHandlerDao.deleteAlertSilenceMessageRelByMessageIds(silencMessageIds.toArray(new Long[]{}));
        }

        // 删除告警消息
        alertMessageDao.deleteByAssetIds(assetIdList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByModuleIds(List moduleIdList) {
        List<Long> silencMessageIds = msgHandlerDao.selectAlertSilencMessageIdByModuleIds(moduleIdList);
        // 存在静默告警消息，删除 alert_silence_message_rel 表关系
        if (CollectionUtils.isNotEmpty(silencMessageIds)) {
            msgHandlerDao.deleteAlertSilenceMessageRelByMessageIds(silencMessageIds.toArray(new Long[]{}));
        }

        // 删除告警消息
        alertMessageDao.deleteByModuleIds(moduleIdList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByProjectIds(List projectIdList) {
        List<Long> silencMessageIds = msgHandlerDao.selectAlertSilencMessageIdByProjectIds(projectIdList);
        // 存在静默告警消息，删除 alert_silence_message_rel 表关系
        if (CollectionUtils.isNotEmpty(silencMessageIds)) {
            msgHandlerDao.deleteAlertSilenceMessageRelByMessageIds(silencMessageIds.toArray(new Long[]{}));
        }

        // 删除告警消息
        alertMessageDao.deleteByProjectIds(projectIdList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByDcIds(List dcIds) {
        List<Long> silencMessageIds = msgHandlerDao.selectAlertSilencMessageIdByDcIds(dcIds);
        // 存在静默告警消息，删除 alert_silence_message_rel 表关系
        if (CollectionUtils.isNotEmpty(silencMessageIds)) {
            msgHandlerDao.deleteAlertSilenceMessageRelByMessageIds(silencMessageIds.toArray(new Long[]{}));
        }

        // 删除告警消息
        alertMessageDao.deleteByDCIds(dcIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByEndpointIds(List endpointIds) {
        List<Long> silencMessageIds = msgHandlerDao.selectAlertSilencMessageIdByEndpointIds(endpointIds);
        // 存在静默告警消息，删除 alert_silence_message_rel 表关系
        if (CollectionUtils.isNotEmpty(silencMessageIds)) {
            msgHandlerDao.deleteAlertSilenceMessageRelByMessageIds(silencMessageIds.toArray(new Long[]{}));
        }

        // 删除告警消息
        alertMessageDao.deleteByEndpointIds(endpointIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByRuleIds(List ruleIds) {
        List<Long> silencMessageIds = msgHandlerDao.selectAlertSilencMessageIdByRuleIds(ruleIds);
        // 存在静默告警消息，删除 alert_silence_message_rel 表关系
        if (CollectionUtils.isNotEmpty(silencMessageIds)) {
            msgHandlerDao.deleteAlertSilenceMessageRelByMessageIds(silencMessageIds.toArray(new Long[]{}));
        }

        // 删除告警消息
        alertMessageDao.deleteByRuleIds(ruleIds);
    }

	@Override
	public void removeAllData() {
		this.baseMapper.removeAllData();
	}

	@Override
	public void leftPushAlertMessageToRedis(String messages) {
		if(Tool.StrUtil.isBlank(messages)) {
			return;
		}
		ListOperations<String, String> operations = redisTemplate.opsForList();
		operations.leftPush(Constant.ALERT_MESSAGE_RECV, Tool.JSONUtil.toJsonStr(messages));
		Long size = operations.size(Constant.ALERT_MESSAGE_RECV);
		//判断是否超出所设置的队列最大长度
		if(size > maxQueueLength) {
			redisTemplate.opsForList().trim(Constant.ALERT_MESSAGE_RECV, 0, maxQueueLength-1);
			log.debug("trim alert_message_recv size : {}",size - maxQueueLength);
		}
		log.debug("alert_message_recv size : {}",size);
	}

    @Override
    public void cleanExpiredDataByTime(DateTime startCleanAlertMessageTime) {
        alertMessageDao.cleanExpiredDataByTime(startCleanAlertMessageTime);
    }
}
