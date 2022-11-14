package com.nis.modules.terminal.service;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.terminal.entity.TerminalConfigDto;
import com.nis.modules.terminal.entity.TerminalSessionEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface TerminalSessionService extends IService<TerminalSessionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void killSession(String uuid);

    /**
     * 清理过期的 terminal session cmd record 数据
     * @param startCleanTerminalTime
     */
    void cleanExpiredDataByTime(DateTime startCleanTerminalTime);

    Map<Object, Object> login(TerminalConfigDto configEntity);

    Map<Object, Object> lsOfSftp(String uuid, String path);

    void mkdirOfSftp(String uuid, String path);

    void rmOfSftp(String uuid, String path);

    String uploadOfSftp(String uuid, String path, MultipartFile file);

    Map<Object, Object> querySftpUploadProcess(String tid);

    void downloadOfSftp(String uuid, String path, HttpServletResponse response);

    void cancelUpload(String tid);

}
