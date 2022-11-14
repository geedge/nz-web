package com.nis.modules.sys.controller;

import cn.hutool.log.Log;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.service.SysBackupService;
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
		try {
			sysBackupService.backupData();
		} catch (Exception e) {
			log.error("data backup error", e);
			throw new NZException(RCode.SYS_BACKUP_ERROR);
		}
		return R.ok();
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
