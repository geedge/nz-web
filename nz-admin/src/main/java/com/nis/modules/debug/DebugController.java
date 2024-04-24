package com.nis.modules.debug;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.utils.LocalCacheManager;
import com.nis.common.utils.R;

import cn.hutool.log.Log;

@RestController("/debug")
public class DebugController {

	@Autowired
    private Scheduler scheduler;
	private static final Log log = Log.get();

	@PutMapping("/triggerJob")
	public R triggerJob(String name) throws SchedulerException {
		JobKey key = new JobKey(name);
		boolean checkExists = scheduler.checkExists(key);
		if (checkExists) {
			scheduler.triggerJob(key);
			log.info("trigger job: {}", name);
		}
		return R.ok().putData("exist", checkExists).putData("name", name);
	}

	@PutMapping("/cleanCache")
	public R cleanCache(String prefix, String name) {
		LocalCacheManager.removeCache(prefix, name);
		log.info("clena cache, prefix: {}, name: {}", prefix, name);
		return R.ok();
	}
}
