package com.nis.modules.terminal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.terminal.entity.TerminalRecord;

import java.util.Map;

public interface TerminalRecordService extends IService<TerminalRecord> {

    Map queryTerminalRecordInfos(Map<String, Object> params);
}