package com.nis.modules.terminal.job;

import java.util.List;
import java.util.stream.Collectors;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.terminal.config.TerminalSession;
import com.nis.modules.terminal.entity.TerminalCmd;
import com.nis.modules.terminal.entity.TerminalRecord;
import com.nis.modules.terminal.entity.TerminalSessionEntity;
import com.nis.modules.terminal.service.TerminalCmdService;
import com.nis.modules.terminal.service.TerminalRecordService;
import com.nis.modules.terminal.service.TerminalSessionService;

import cn.hutool.core.date.DateTime;
import cn.hutool.log.Log;

@DisallowConcurrentExecution
public class TerminalCleanRecordJob extends QuartzJobBean {
	
	private Log log = Log.get();
	
	@Autowired
    private TerminalRecordService terminalRecordService;
	
	@Autowired
	private TerminalSessionService terminalSessionService;
	
	@Autowired
	private TerminalCmdService terminalCmdService;
	
	@Autowired
	private SysConfigService sysConfigService;
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		// 获取配置文件中terminal_record_local_retention的值
		SysConfigEntity sysConfig = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, "terminal_record_local_retention"));
		String value = sysConfig.getParamValue();
		Integer day = Integer.valueOf(value);
		
		//获取当前时间 day天以前的时间
		DateTime nowDate = Tool.DateUtil.date();
		DateTime offsetSecond = Tool.DateUtil.offsetSecond(nowDate,-(day*24*60*60));
		
		List<TerminalSessionEntity> list = terminalSessionService.list(new QueryWrapper<TerminalSessionEntity>().lambda().le(TerminalSessionEntity::getStartTime, offsetSecond));
		
		if(ToolUtil.isNotEmpty(list)) {
			log.debug("remove terminal session info {}",JSON.toJSON(list));
			List<String> uuids = list.stream().map(TerminalSessionEntity::getUuid).collect(Collectors.toList());
			terminalCmdService.remove(new QueryWrapper<TerminalCmd>().lambda().in(TerminalCmd::getUuid, uuids));
			terminalRecordService.remove(new QueryWrapper<TerminalRecord>().lambda().in(TerminalRecord::getUuid, uuids));
			terminalSessionService.remove(new QueryWrapper<TerminalSessionEntity>().lambda().in(TerminalSessionEntity::getUuid, uuids));
		}
		log.debug("remove terminal end");
	}
}
