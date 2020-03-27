package com.nis.modules.terminal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.terminal.entity.TerminalLog;

import java.util.Map;

public interface TerminalLogService extends IService<TerminalLog> {

    PageUtils queryPage(Map<String, Object> params) throws Exception;
}
