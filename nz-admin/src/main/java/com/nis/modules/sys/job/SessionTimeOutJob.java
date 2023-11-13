package com.nis.modules.sys.job;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.terminal.backend.TerminalHandler;
import com.nis.modules.terminal.backend.TerminalSession;
import com.nis.modules.terminal.entity.TerminalSessionEntity;
import com.nis.modules.terminal.service.TerminalSessionService;
import com.nis.modules.terminal.utils.TerminalConstant;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class SessionTimeOutJob extends QuartzJobBean {
	
	private Log log = Log.get();
	
	@Autowired
	private TerminalSessionService terminalSessionService;

	@SuppressWarnings("unchecked")
	@Override
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		// 检查 HA_LOCK
		LeaderElectionDao dao = LeaderElectionDao.getInstance();
		boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
		log.info("[SessionTimeOutJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
		if (BooleanUtil.negate(isLeader)) return;

		Thread.currentThread().setName("SessionTimeOutJob");
		// 获取redis中所有的token信息
		Map<String, SysUserEntity> shiroSessionInfoMap = ShiroUtils.getShiroSessionInfoMap();
		
		// 获取所有登录用户的信息
		List<SysUserEntity> users = new ArrayList<SysUserEntity>(shiroSessionInfoMap.values());
		List<Long> onlineUserIds = users.stream().map(SysUserEntity::getId).collect(Collectors.toList());
		
		// 获取terminal session表中所有正在连接的用户信息
		List<TerminalSessionEntity> tableSessions = terminalSessionService.list(new QueryWrapper<TerminalSessionEntity>().lambda().eq(TerminalSessionEntity::getStatus, 0));
		List<Long> allUserTerminals = tableSessions.stream().map(TerminalSessionEntity::getUserId).collect(Collectors.toList());
		Map<Long, List<TerminalSessionEntity>> userIdAndTerminal = tableSessions.stream().collect(Collectors.groupingBy(TerminalSessionEntity::getUserId));
		
		List<Long> removeUserIds = allUserTerminals.stream().filter(item -> !onlineUserIds.contains(item)).collect(Collectors.toList());
		
		List<TerminalSessionEntity> updateDatas = new ArrayList<TerminalSessionEntity>();
		// 将登录超时的用户terminal连接关闭
		for(Long userId : removeUserIds) {
			List<TerminalSessionEntity> userTerminals = userIdAndTerminal.get(userId);
			for(TerminalSessionEntity userTerminal : userTerminals) {
				userTerminal.setEndTime(new Date());
				userTerminal.setStatus(TerminalConstant.SessionStatus.FORCE_QUIT.getValue());
				updateDatas.add(userTerminal);
				WebSocketSession session = TerminalSession.findSession(userTerminal.getUuid());
				try {
					if(ToolUtil.isNotEmpty(session)) {
						TerminalSession.remove(session);
					}
		        } catch (RuntimeException e) {
		            log.error(String.format("terminal session kill failed,uuid = %s,错误信息 = %s", userTerminal.getUuid(), e.getMessage()), e);
		        }
				// kill 之后，将剩余文件写到数据库中
				TerminalHandler.saveRecordContentAfterClientClosed(userTerminal.getUuid());
			}
		}
		if(ToolUtil.isNotEmpty(updateDatas)) {
			terminalSessionService.updateBatchById(updateDatas);
		}
	}

}
