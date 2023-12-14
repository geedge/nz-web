package com.nis.modules.alert.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.backend.AlertContext;
import com.nis.modules.alert.dao.AlertHandlerDao;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


@Service("alertMessageService")
public class AlertMessageServiceImpl extends ServiceImpl<AlertMessageDao, AlertMessageEntity> implements AlertMessageService {

    private Log log = Log.get();

    @Autowired
    private AlertMessageDao alertMessageDao;
    @Autowired
    private AlertHandlerDao msgHandlerDao;
	@Value("${redis.maxAlertMessageSize:10000}")
    private Integer maxQueueLength;//redis队列长度
	
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
    public void leftPushAlertMessageToCache(String messages) {
        if (Tool.StrUtil.isBlank(messages)) {
            return;
        }
        AlertContext.ALERT_MESSAGE_RECV_QUEUE.offer(Tool.JSONUtil.toJsonStr(messages));
        int size = AlertContext.ALERT_MESSAGE_RECV_QUEUE.size();
        // 判断是否超出所设置的队列最大长度
        if (size > maxQueueLength) {
            int removeCount = size - maxQueueLength;
            for (int i = 0; i < removeCount; i++) {
                AlertContext.ALERT_MESSAGE_RECV_QUEUE.poll();
            }
            log.debug("trim alert_message_recv size : {}", removeCount);
        }
        log.debug("alert_message_recv size : {}", size);
    }

    @Override
    public void cleanExpiredDataByTime(DateTime startCleanAlertMessageTime) {
        alertMessageDao.cleanExpiredDataByTime(startCleanAlertMessageTime);
    }
    
    /**
     * 根据告警级别统计符合条件的激活状态告警数量
     * @param type 可选值：asset, datacenter, project, module, endpoint, rule
     * @param id
     * @return List<Map<String,Long>>
     */
    @Override
    public List<Map<String,Object>> getAlertsStatBySeverity(String type,Integer id) {
    	return alertMessageDao.getAlertsStatBySeverity(type, id);
    }
    
}
