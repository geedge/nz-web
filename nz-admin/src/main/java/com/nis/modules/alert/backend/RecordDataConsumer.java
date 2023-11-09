package com.nis.modules.alert.backend;

import cn.hutool.core.date.StopWatch;
import cn.hutool.log.Log;
import com.google.protobuf.InvalidProtocolBufferException;
import com.nis.common.protobuf.Types.TimeSeries;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.election.dao.LeaderElectionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * record data 消费线程 1、从 redis中获取 record data 2、处理内容 3、rewrite 发送到
 * prometheus/cortex
 * 
 * @author ThinkPad
 *
 */
@Component
public class RecordDataConsumer implements Runnable {
	private static final Log log = Log.get();

	private static final String THREAD_NAME = "RecordDataConsumer";
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	/**
	 * 最大 发送间隔，单位 ： ms
	 */
	@Value("${record.maxSendIntervalMs:5000}")
	private long maxSendIntervalMs;
	/**
	 * 一次最大发送数量
	 */
	@Value("${record.maxSamplesPerSend:1000}")
	private long maxSamplesPerSend;

	/*
	 * 最后一次发送时间
	 */
	private volatile long lastSendTimeMillis = Tool.DateUtil.current();

	@Override
	public void run() {
		Thread.currentThread().setName(THREAD_NAME);
		log.info("thread start");
		while (true) {
			try {
				// 检查 HA_LOCK
				LeaderElectionDao dao = LeaderElectionDao.getInstance();
				if (Tool.BooleanUtil.negate(dao.isLeader())) {
					Tool.ThreadUtil.sleep(maxSendIntervalMs);
					log.debug("sleep {}ms ", maxSendIntervalMs);
					continue;
				}

				// 当前队列数量
				ListOperations<String, Object> opsForList = redisTemplate.opsForList();
				Long cacheSize = 0L;
				if (redisTemplate.hasKey(Constant.RECORD_DATA_CACHE)) {
					cacheSize = opsForList.size(Constant.RECORD_DATA_CACHE);
					log.debug("record_data_cache size : {}", cacheSize);
				}

				// 判断是否需要发送
				// 数量 或 时间 其中一个条件满足即可发送
				long interval = Tool.DateUtil.current() - lastSendTimeMillis;
				if (cacheSize == 0 || !(cacheSize >= maxSamplesPerSend || interval >= maxSendIntervalMs)) {
					long sleepMs = Tool.NumberUtil.max((long) Math.ceil(maxSendIntervalMs / 10), 100); // 最小睡眠时间 100ms
					log.debug("wait next time,sleep {}ms, current cache size: {}, last interval: {}ms", sleepMs,
							cacheSize, interval);
					Tool.ThreadUtil.sleep(sleepMs);
					continue;
				}
				long size = (cacheSize > maxSamplesPerSend) ? maxSamplesPerSend : cacheSize;
				// 符合发送条件
				log.info("send start,current cache size: {}, size: {}, last interval: {}ms", cacheSize, size, interval);
				StopWatch sw = StopWatch.create("record");
				sw.start("readData");
				List<TimeSeries> timeSeriesList = this.leftPopRecordData(size);
				sw.stop();
				sw.start("transform");
				sw.stop();
				sw.start("request");
				// 发送请求
				ResponseEntity<String> responseEntity = AgentUtil.remoteWrite(timeSeriesList);
				HttpStatusCode statusCode = responseEntity.getStatusCode();
				if (statusCode.isError()) {
					log.warn("remote write send, status: {}, msg: {}", responseEntity.getStatusCode(),
							responseEntity.getBody());
				}
				sw.stop();
				if (log.isDebugEnabled()) {
					log.debug("耗时统计:{}", sw.prettyPrint());
				}

				log.info("send end, cacheSize: {}, interval: {}ms, timeSeriesSize: {}, statusCode: {}", cacheSize,
						interval, timeSeriesList.size(), statusCode.value());
				// 更新
				lastSendTimeMillis = Tool.DateUtil.current();
			} catch (Throwable e) {
				if (!(e instanceof QueryTimeoutException)) {
					log.error(e, "thread error");
				}
			}
		}
	}

	/**
	 * 从redis 中批量 获取 record data
	 * 
	 * @param size
	 * @return
	 */
	@SuppressWarnings("all")
	private List<TimeSeries> leftPopRecordData(Long size) throws InvalidProtocolBufferException {
		List<TimeSeries> result = Tool.ListUtil.list(false);
		List<String> cacheList = (List) redisTemplate.executePipelined((SessionCallback) operations -> {
			ListOperations opsForList = operations.opsForList();
			for (int i = 0; i < size; i++) {
				opsForList.leftPop(Constant.RECORD_DATA_CACHE);
			}
			return null;
		});

		if (cacheList.size() > 0) {
			for (int i = 0, j = cacheList.size(); i < j; i++) {
				TimeSeries timeSeries = TimeSeries.parseFrom(Tool.HexUtil.decodeHex(cacheList.get(i)));
				result.add(timeSeries);
			}
		}
		return result;
	}

}
