package com.nis.modules.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.agent.entity.MonitorEventLog;

public interface MonitorEventLogService extends IService<MonitorEventLog> {

	void increment(String... keys);

	Integer getByEvent(String event);

}
