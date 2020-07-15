package com.nis.modules.terminal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.terminal.dao.TerminalLogDao;
import com.nis.modules.terminal.entity.TerminalLog;
import com.nis.modules.terminal.service.TerminalLogService;

import java.util.List;
import java.util.Map;

@Service("terminalLogService")
public class TerminalLogServiceImpl extends ServiceImpl<TerminalLogDao, TerminalLog> implements TerminalLogService {

    @Autowired
    private SysUserDao userDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) throws Exception{
        if (ToolUtil.isNotAllEmpty(params.get("startTime"), params.get("endTime"))) {
            ToolUtil.validateOpTime((String) params.get("startTime"),(String) params.get("endTime"));
        }
        LambdaQueryWrapper<TerminalLog> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper
                .eq(ToolUtil.isNotEmpty(params.get("protocol")), TerminalLog::getProtocol, params.get("protocol"))
                .eq(ToolUtil.isNotEmpty(params.get("userId")), TerminalLog::getUserId, params.get("userId"))
                .eq(ToolUtil.isNotEmpty(params.get("user")), TerminalLog::getUser, params.get("user"))
                .eq(ToolUtil.isNotEmpty(params.get("host")), TerminalLog::getHost, params.get("host"))
                .like(ToolUtil.isNotEmpty(params.get("cmd")), TerminalLog::getCmd, params.get("cmd"))
                .ge(ToolUtil.isNotEmpty( params.get("startTime")), TerminalLog::getTime, params.get("startTime"))
                .lt(ToolUtil.isNotEmpty( params.get("endTime")), TerminalLog::getTime, params.get("endTime"));
        if(ToolUtil.isEmpty(params.get(Constant.ORDER))) {
        	params.put(Constant.ORDER, "-time");
        }
        IPage<TerminalLog> terminalLogIPage = this.page(new Query<TerminalLog>().getPage(params),queryWrapper);
        List<TerminalLog> records = terminalLogIPage.getRecords();
        records.forEach(terminalLog -> {
            terminalLog.setUserName(userDao.selectById(terminalLog.getUserId()).getUsername());
            terminalLog.setUserId(null);
        });
        return new PageUtils(terminalLogIPage);
    }
}
