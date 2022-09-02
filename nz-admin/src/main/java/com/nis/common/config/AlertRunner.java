package com.nis.common.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.nis.modules.alert.backend.AlertHanlderThread;
import com.nis.modules.alert.backend.AlertMessageNotifyThread;
import com.nis.modules.alert.backend.RecordDataConsumer;

import cn.hutool.log.Log;

/**
 * 随 项目启动同时启动 1、告警消息处理线程 2、告警通知线程
 * 
 * @author ThinkPad
 *
 */
@Component
public class AlertRunner implements CommandLineRunner {

	private static final Log log = Log.get();

	@Autowired
	private AlertHanlderThread handlerThread;
	@Autowired
	private AlertMessageNotifyThread notifyThread;
	@Autowired
	private RecordDataConsumer recordDataConsumer;

	private static final ExecutorService threadPool = Executors.newFixedThreadPool(3);

	@Override
	public void run(String... args) throws Exception {
		// 启动 告警消息处理线程
		Future<?> submit = threadPool.submit(handlerThread);
		log.info("启动 {} 成功", "AlertHanlderThread");
		// 启动邮件通知线程
		Future<?> submit1 = threadPool.submit(notifyThread);
		log.info("启动 {} 成功", "AlertMessageNotifyThread");
		// 启动邮件通知线程
		Future<?> submit2 = threadPool.submit(recordDataConsumer);
		log.info("启动 {} 成功", "recordDataConsumer");
	}

}
