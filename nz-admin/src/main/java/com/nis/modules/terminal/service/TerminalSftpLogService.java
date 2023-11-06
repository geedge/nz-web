package com.nis.modules.terminal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.terminal.entity.TerminalSftpLog;

import java.util.Map;

public interface TerminalSftpLogService extends IService<TerminalSftpLog> {

    PageUtils querySftpLogPage(Map<String, Object> params);

    void saveSftpLog(String uuid, String path, Integer sftpOperType);

    void saveUploadFileSftpLog(String uuid, String path, Integer sftpOperType, Long chunkStart);

}
