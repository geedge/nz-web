package com.nis.modules.sys.job;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.SysBackupLog;
import com.nis.modules.sys.entity.SysComponent;
import com.nis.modules.sys.service.SysBackupLogService;
import com.nis.modules.sys.service.SysBackupService;
import com.nis.modules.sys.service.SysComponentService;
import com.nis.modules.sys.service.SysConfService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@DisallowConcurrentExecution
public class SysBackFileSyncJob extends QuartzJobBean {

    private static final Log log = Log.get();

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    private final RestTemplate restTemplate;
    private final SysConfService sysConfService;
    private final SysBackupService sysBackupService;
    private final SysComponentService sysComponentService;
    private final SysBackupLogService sysBackupLogService;

    public SysBackFileSyncJob(RestTemplate restTemplate, SysConfService sysConfService, SysBackupService sysBackupService, SysComponentService sysComponentService, SysBackupLogService sysBackupLogService) {
        this.restTemplate = restTemplate;
        this.sysConfService = sysConfService;
        this.sysBackupService = sysBackupService;
        this.sysComponentService = sysComponentService;
        this.sysBackupLogService = sysBackupLogService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Thread.currentThread().setName("SysBackFileSyncJob");

        log.info("[SysBackFileSyncJob] [begin]");

        StopWatch sw = StopWatch.create("SysBackFileSyncJob");
        sw.start();
        try {
            // sync system backup file
            this.syncSysBackFile(Constant.SERVER_ID);
        } catch (Exception e) {
            log.error(e, "[SysBackFileSyncJob] [error]");
        } finally {
            sw.stop();
        }

        log.info("[SysBackFileSyncJob] [finshed] [Run Time: {}]", sw.prettyPrint());
    }

    /**
     * sync system backup file
     */
    private void syncSysBackFile(String serverId) {
        log.info("[syncSysBackFile] [begin] [serverId: {}]", serverId);

        SysComponent anotherWebCompoent = sysComponentService.getOne(new LambdaQueryWrapper<SysComponent>()
                .eq(SysComponent::getComponent, Constant.NZ_WEB_COMPONENT_NAME)
                .ne(SysComponent::getServerid, serverId));
        log.info("[syncSysBackFile] [anotherWebCompoent] [exist: {}]", ObjectUtil.isNotNull(anotherWebCompoent));

        if (ObjectUtil.isNull(anotherWebCompoent)) {
            log.info("[syncSysBackFile] [another web component not found] [no need to sync]");
        } else {
            String backupConfigJsonStr = sysConfService.getValue("backup_config");
            Map backupConfigMap = JSONUtil.toBean(backupConfigJsonStr, Map.class);
            Integer retention = Tool.MapUtil.getInt(backupConfigMap, "retention", -1);
            log.info("[syncSysBackFile] [retention {}]", retention);

            // 所有操作日志
            List<SysBackupLog> logList = sysBackupLogService.list(new LambdaQueryWrapper<SysBackupLog>().eq(SysBackupLog::getState, 1));
            // 备份文件日志
            List<SysBackupLog> backupLogList = logList.stream()
                    .filter(backupLog -> ObjectUtil.equal(Constant.SysBackupTypeEnum.BACK_UP.getValue(), backupLog.getType()))
                    .collect(Collectors.toList());
            // 删除文件日志
            List<String> deleteLogFilenameList = logList.stream()
                    .filter(backupLog -> ObjectUtil.equal(Constant.SysBackupTypeEnum.DELETE.getValue(), backupLog.getType()))
                    .map(SysBackupLog::getFilename)
                    .collect(Collectors.toList());

            // 最终保留的备份文件
            List<String> backupFileListFromLog = backupLogList.stream()
                    .filter(backupLog -> BooleanUtil.negate(deleteLogFilenameList.contains(backupLog.getFilename())))
                    .sorted(Comparator.comparing(SysBackupLog::getId).reversed())
                    .limit(ObjectUtil.equal(-1, retention) ? logList.size() : retention)
                    .map(SysBackupLog::getFilename)
                    .collect(Collectors.toList());
            log.info("[syncSysBackFile] [backupFileListFromLog] [size: {}]", Tool.CollUtil.size(backupFileListFromLog));
            if (log.isDebugEnabled()) {
                log.debug("[syncSysBackFile] [backupFileListFromLog] [size: {}] [info: {}]", Tool.CollUtil.size(backupFileListFromLog), JSONUtil.toJsonStr(backupFileListFromLog));
            }

            // local file
            List<Object> localBackupFileList = sysBackupService.queryBackupList();
            localBackupFileList = Tool.CollUtil.defaultIfEmpty(localBackupFileList, Tool.CollUtil.list(true));
            log.info("[syncSysBackFile] [localBackupFileList] [size: {}]", Tool.CollUtil.size(localBackupFileList));

            List<String> localBackupFilenameList = localBackupFileList.stream().map(tmap -> Tool.MapUtil.getStr((Map) tmap, "fileName")).collect(Collectors.toList());
            List<String> needSyncBackupFilenameList = backupFileListFromLog.stream()
                    .filter(filename -> BooleanUtil.negate(localBackupFilenameList.contains(filename)))
                    .collect(Collectors.toList());
            log.info("[syncSysBackFile] [needSyncBackupFilenameList] [size: {}]", Tool.CollUtil.size(needSyncBackupFilenameList));
            if (log.isDebugEnabled()) {
                log.debug("[syncSysBackFile] [needSyncBackupFilenameList] [size: {}] [info: {}]", Tool.CollUtil.size(needSyncBackupFilenameList), JSONUtil.toJsonStr(needSyncBackupFilenameList));
            }

            String backupFilePath = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);
            boolean directoryExist = FileUtil.isDirectory(backupFilePath);
            log.info("[syncSysBackFile] [backup_file_path] [exist: {}]", directoryExist);
            if (BooleanUtil.negate(directoryExist)) {
                log.info("[syncSysBackFile] [create backup_file_path] [path: {}]", backupFilePath);
                FileUtil.mkdir(backupFilePath);
            }

            // sync backup file from another web compoent
            this.syncBackupFileFromAnotherWebCompoent(anotherWebCompoent, needSyncBackupFilenameList);

            // sync delete backup file from log
            this.syncDeleteBackupFileFromLog(deleteLogFilenameList, localBackupFilenameList);

        }
        log.info("[syncSysBackFile] [finshed]");
    }


    /**
     * build sync backup file url
     *
     * @param sysComponent
     * @param filename
     * @return
     */
    private String buildSyncBackupFileUrl(SysComponent sysComponent, String filename) {
        UrlBuilder urlBuilder = new UrlBuilder();
        urlBuilder.setScheme(sslEnabled ? "https" : "http");
        urlBuilder.setHost(sysComponent.getHost());
        urlBuilder.setPort(sysComponent.getPort());
        urlBuilder.setPath(UrlPath.of("/sys/backup/sync", Charset.forName("UTF-8")));

        UrlQuery urlQuery = new UrlQuery(true);
        urlQuery.add("serverid", sysComponent.getServerid());
        urlQuery.add("type", "download");
        urlQuery.add("filename", filename);
        urlBuilder.setQuery(urlQuery);
        String urlStr = urlBuilder.build();
        return urlStr;
    }

    /**
     * sync backup file from another web compoent
     *
     * @param sysComponent
     * @param needSyncBackupFilenameList
     */
    private void syncBackupFileFromAnotherWebCompoent(SysComponent sysComponent, List<String> needSyncBackupFilenameList) {
        String backupFilePath = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);
        for (String syncBackupFilename : needSyncBackupFilenameList) {
            String urlStr = this.buildSyncBackupFileUrl(sysComponent, syncBackupFilename);
            try {
                ResponseEntity<byte[]> exchange = restTemplate.exchange(urlStr, HttpMethod.GET, null, byte[].class);
                log.info("[syncBackupFileFromAnotherWebCompoent] [sync url: {}] [responseStatus: {}]", urlStr, exchange.getStatusCodeValue());
                if (Tool.ObjectUtil.equal(HttpStatus.OK.value(), exchange.getStatusCodeValue())) {
                    byte[] bytes = exchange.getBody();
                    int length = bytes.length;
                    try {
                        Tool.FileUtil.writeBytes(bytes, FileUtil.file(backupFilePath, syncBackupFilename));
                        log.info("[syncBackupFileFromAnotherWebCompoent] [sync backup success] [filename: {}] [length: {}]", syncBackupFilename, length);
                    } catch (Exception e) {
                        log.error(e, "[syncBackupFileFromAnotherWebCompoent] [write backup file error] [filename: {}] [length: {}]", syncBackupFilename, length);
                    }
                } else {
                    log.warn("[syncBackupFileFromAnotherWebCompoent] [request error] [url: {}] [responseStatus: {}] [msg: {}]", urlStr, exchange.getStatusCodeValue(), StrUtil.str(exchange.getBody(), "UTF-8"));
                }
            } catch (Exception e) {
                log.error(e, "[syncBackupFileFromAnotherWebCompoent] [error] [url: {}]", urlStr);
            }
        }
    }

    /**
     * sync delete backup file from log
     *
     * @param deleteLogFilenameList
     * @param localBackupFilenameList
     */
    private void syncDeleteBackupFileFromLog(List<String> deleteLogFilenameList, List<String> localBackupFilenameList) {
        List<String> syncDeleteFilenameList = localBackupFilenameList.stream()
                .filter(localBackupFilename -> deleteLogFilenameList.contains(localBackupFilename))
                .collect(Collectors.toList());
        log.info("[syncDeleteBackupFileFromLog] [delete file list] [size: {}]", Tool.CollUtil.size(syncDeleteFilenameList));

        String backupFilePath = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);
        for (String delFilename : syncDeleteFilenameList) {
            String sqlFilePath = Tool.StrUtil.concat(true, backupFilePath, File.separator, delFilename);
            try {
                FileUtil.del(sqlFilePath);
            } catch (Exception e) {
                log.error(e, "[syncDeleteBackupFileFromLog] [error] [filename: {}]", delFilename);
            }
        }
    }

}
