package com.nis.modules.terminal.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.terminal.dao.TerminalLogDao;
import com.nis.modules.terminal.entity.TerminalLog;
import com.nis.modules.terminal.service.TerminalLogService;

@Service("terminalLogService")
public class TerminalLogServiceImpl extends ServiceImpl<TerminalLogDao, TerminalLog> implements TerminalLogService {

}
