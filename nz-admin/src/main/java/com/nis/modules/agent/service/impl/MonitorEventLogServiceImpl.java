package com.nis.modules.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.agent.dao.MonitorEventLogDao;
import com.nis.modules.agent.entity.MonitorEventLog;
import com.nis.modules.agent.service.MonitorEventLogService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MonitorEventLogServiceImpl extends ServiceImpl<MonitorEventLogDao, MonitorEventLog> implements MonitorEventLogService {

    /**
     * keys 为空 所有 value += 1, keys 不为空 指定 value += 1
     * @param keys
     */
    @Override
    public void increment(String... keys) {
        List<MonitorEventLog> confEventList = this.list();
        if(ToolUtil.isNotEmpty(keys)){
            confEventList = confEventList.stream().filter(monitorConfEvent -> Arrays.asList(keys).contains(monitorConfEvent.getEvent())).collect(Collectors.toList());
        }

        for (MonitorEventLog event : confEventList) {
            event.setCounter(event.getCounter() + 1);
        }

        this.saveOrUpdateBatch(confEventList);
    }
}
