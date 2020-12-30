/**

 *

 *

 */

package com.nis.modules.job.task;

import cn.hutool.log.Log;
import org.springframework.stereotype.Component;

/**
 * 测试定时任务(演示Demo，可删除)
 *
 * testTask为spring bean的名称
 *

 */
@Component("testTask")
public class TestTask implements ITask {
	private Log logger = Log.get();

	@Override
	public void run(String params){
		logger.debug("TestTask定时任务正在执行，参数为：{}", params);
	}
}
