package com.nis.modules.terminal.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.terminal.config.Constants;
import com.nis.modules.terminal.config.TerminalHandler;
import com.nis.modules.terminal.config.TerminalSession;
import com.nis.modules.terminal.dao.TerminalSessionDao;
import com.nis.modules.terminal.entity.TerminalSessionEntity;
import com.nis.modules.terminal.service.TerminalRecordService;
import com.nis.modules.terminal.service.TerminalSessionService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("terminalSessionService")
public class TerminalSessionServiceImpl extends ServiceImpl<TerminalSessionDao, TerminalSessionEntity> implements TerminalSessionService {

    private Log log = Log.get();

    @Autowired
    private TerminalSessionDao terminalSessionDao;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private TerminalRecordService terminalRecordService;

    /**
     * 处理请求参数
     *
     * @param params
     */
    private void handleQueryDateParam(Map<String, Object> params) {
        try {
            if (params.get("startTime") != null && StringUtils.isNotEmpty(params.get("startTime").toString())) {
                Date searchStartAt = DateUtils.parseDate(params.get("startTime").toString(), "yyyy-MM-dd HH:mm:ss");
                params.put("startTime", searchStartAt);
            } else {
                params.put("startTime", null);
            }
            if (params.get("endTime") != null && StringUtils.isNotEmpty(params.get("endTime").toString())) {
                Date searchEndAt = DateUtils.parseDate(params.get("endTime").toString(), "yyyy-MM-dd HH:mm:ss");
                params.put("endTime", searchEndAt);
            } else {
                params.put("endTime", null);
            }
        } catch (ParseException e) {
            log.error("时间转换异常,参数是 {}", params, e);
        }
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        // 处理请求参数
        this.handleQueryDateParam(params);

        IPage<TerminalSessionEntity> page = new Query<TerminalSessionEntity>().getPage(params);
        List<TerminalSessionEntity> sessionEntityList = terminalSessionDao.queryList(page, params);

        // 查询结果不为空时处理返回结果
        if (CollectionUtils.isNotEmpty(sessionEntityList)) {
            List<SysUserEntity> userList = sysUserService.list();
            Map<Long, String> userIdAndNameMap = userList.stream().collect(Collectors.toMap(SysUserEntity::getUserId, SysUserEntity::getUsername));
            for (TerminalSessionEntity sessionEntity : sessionEntityList) {
                sessionEntity.setKillUserName(userIdAndNameMap.get(sessionEntity.getKillUserId()));
            }
        }

        page.setRecords(sessionEntityList);
        return new PageUtils(page);
    }

    @Override
    public void killSession(String uuid) {
        WebSocketSession session = TerminalSession.findSession(uuid);
        if (session == null) {
            return;
        }

        /**
         * session 已经结束，更新状态，设置结束时间
         */
        TerminalSessionEntity sessionEntity = (TerminalSessionEntity) TerminalSession.getTerminalAttributeInfo(uuid).get("terminalSessionEntity");
        if (sessionEntity.getStartTime() == null) {
            sessionEntity.setStartTime(DateUtil.getUTCTimeByConfigTimeZone());
        }

        sessionEntity.setEndTime(DateUtil.getUTCTimeByConfigTimeZone());
        sessionEntity.setKillUserId(ShiroUtils.getUserId());
        sessionEntity.setStatus(Constants.SessionStatus.FORCE_QUIT.getValue());
        TerminalSessionService terminalSessionService = (TerminalSessionService) TerminalSession.clientAttributeInfo.get("terminalSessionService");
        terminalSessionService.saveOrUpdate(sessionEntity);


        try {
            TerminalSession.remove(session);
            // TerminalHandler.unloginUserHandle(session);
            StringBuilder sb = new StringBuilder();
            sb.append("\u001B[01;31m\u001B[K Administrator disconnected \u001B[m\u001B[K  \r\n");
            sb.append("$: ");
            session.sendMessage(new TextMessage(sb.toString()));
        } catch (IOException e) {
            log.error(String.format("terminal session kill failed,uuid = %s,错误信息 = %s", uuid, e.getMessage()), e);
        }

        // kill 之后，将剩余文件写到数据库中
        TerminalHandler.handleRecodeAfterClientClose(uuid, terminalRecordService);
    }
}
