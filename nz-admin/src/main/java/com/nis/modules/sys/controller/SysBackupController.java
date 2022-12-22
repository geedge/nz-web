package com.nis.modules.sys.controller;

import cn.hutool.log.Log;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.service.SysBackupService;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 系统备份
 */

@RestController
@RequestMapping("/sys/backup")
public class SysBackupController {

	Log log = Log.get();

    @Autowired
    private SysBackupService sysBackupService;

	@Autowired
	private RedisLock redisLock;

    /**
	 * 查询列表
	 */
	@GetMapping
	public R info(@RequestParam Map<String, Object> params){
		Object result = sysBackupService.queryInfo();
		return R.ok(result);
	}
	
	/**
	 * 修改备份
	 */
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.SYSTEM)
	public R modify(@RequestBody Map<String, Object> params){
		sysBackupService.save(params);
		return R.ok();
	}

	@GetMapping("/list")
	public R backupList() {
		List<Object> result = sysBackupService.queryBackupList();
		return R.ok(result);
	}

	@PostMapping
	public R backupData() {
		// get lock
		String uuid = Tool.StrUtil.uuid();
		try {
			if (redisLock.lock(Constant.SYS_BACKUP_LOCK, uuid, 5L)) {
				log.info("[backupData] [Get lock and start system data backup]");

				StopWatch sw = new StopWatch();
				sw.start();

				sysBackupService.backupData();
				sw.stop();
				log.info("[backupData] [System data backup finished] [Run time: {}]", sw.toString());

				return R.ok();
			} else {
				log.warn("[backupData] [System data backup is in progress please try again later]");
				return R.error(RCode.SYS_BACKUP_IN_PROGRESS);
			}
		} catch (Exception e) {
			log.error(e, "[backupData] [backup error]");
			throw new NZException(RCode.SYS_BACKUP_ERROR);
		} finally {
			redisLock.unlock(Constant.SYS_BACKUP_LOCK, uuid);
		}
	}


	/**
	 * 系统备份恢复接口
	 *
	 * @param params
	 * @return
	 */
	@PutMapping("/restore")
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.SYSTEM)
	public R restore(@RequestBody(required = false) Map<String, Object> params) {
		ValidateUtils.is(params).notNull(RCode.SYS_BACKUP_FILENAME_ISNULL);
		String filename = (String) params.get("filename");
		if (Tool.StrUtil.isEmpty(filename)) throw new NZException(RCode.SYS_BACKUP_FILENAME_ISNULL);

		// TODO
		return R.ok();
	}


	/**
	 * 系统备份删除接口
	 *
	 * @param filename
	 * @return
	 */
	@DeleteMapping
	public R delete(String filename) {
		if (Tool.StrUtil.isEmpty(filename)) throw new NZException(RCode.SYS_BACKUP_FILENAME_ISNULL);

		sysBackupService.removeBackFileByFilename(filename);
		return R.ok();
	}

	/**
	 * 系统备份下载接口
	 * @param filename
	 */
	@GetMapping("/download")
	public void download(HttpServletResponse response, String filename) throws IOException {
		if (Tool.StrUtil.isEmpty(filename)) throw new NZException(RCode.SYS_BACKUP_FILENAME_ISNULL);
		sysBackupService.downloadBackFileByFilename(response, filename);
	}

}
