package com.nis.modules.sys.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.utils.R;
import com.nis.modules.sys.service.SysBackupService;

/**
 * 系统备份
 */

@RestController
@RequestMapping("/sys/backup")
public class SysBackupController {

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
	public R modify(@RequestBody Map<String, Object> params){
		sysBackupService.save(params);
		return R.ok();
	}
	
	@PostMapping("/list")
	public R backupList() {
		List<Object> result = sysBackupService.queryBackupList();
		return R.ok(result);
	}
	
	@PostMapping
	public void backupData() {
		sysBackupService.backupData();
	}
}
