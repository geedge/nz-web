package com.nis.modules.terminal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.terminal.entity.TerminalSessionEntity;

import java.util.Map;

public interface TerminalSessionService extends IService<TerminalSessionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void killSession(String uuid);
}
