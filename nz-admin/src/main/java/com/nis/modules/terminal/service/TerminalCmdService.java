package com.nis.modules.terminal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.terminal.entity.TerminalCmd;

import java.util.Map;

public interface TerminalCmdService extends IService<TerminalCmd> {

    Map queryCmdInfos(Map<String, Object> params);
}