package com.nis.modules.sys.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.sys.service.SysBackupService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 系统备份
 */

@RestController
@RequestMapping("/sys/backup")
public class SysBackupController {

    private static final Log log = Log.get();

    @Autowired
    private SysBackupService sysBackupService;

    /**
     * 查询列表
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYS_BACKUP)
    public R info(@RequestParam Map<String, Object> params) {
        Object result = sysBackupService.queryInfo();
        return R.ok(result);
    }

    /**
     * 修改备份
     */
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.SYS_BACKUP)
    public R modify(@RequestBody Map<String, Object> params) {
        sysBackupService.save(params);
        return R.ok();
    }

    @GetMapping("/list")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYS_BACKUP)
    public R backupList() {
        List<Object> result = sysBackupService.queryBackupList();
        return R.ok(result);
    }

    @PostMapping
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.SYS_BACKUP)
    public R backupData(@RequestBody(required = false) Map<String, String> requestBody) {
        // get lock
        LeaderElectionDao dao = LeaderElectionDao.getInstance();

        String uuid = Tool.StrUtil.uuid();
        dao.attemptLeadership(Constant.SYS_BACKUP_LOCK, uuid, TimeUnit.SECONDS.toMillis(300));
        try {
            if (dao.isLeader(Constant.SYS_BACKUP_LOCK, uuid)) {
                log.info("[backupData] [Get lock and start system data backup]");

                StopWatch sw = new StopWatch();
                sw.start();

                String remark = requestBody.get("remark");
                sysBackupService.backupData(remark);
                sw.stop();
                log.info("[backupData] [System data backup finished] [Run time: {}]", sw.toString());

                return R.ok();
            } else {
                log.warn("[backupData] [System data backup is in progress please try again later]");
                return R.error(RCode.SYS_BACKUP_IN_PROGRESS);
            }
        } catch (Exception e) {
            log.error(e, "[backupData] [backup error]");
            throw new NZException(RCode.SYS_BACKUP_ERROR);
        } finally {
            dao.forceReElection(Constant.SYS_BACKUP_LOCK, uuid);
        }
    }


    /**
     * 系统备份恢复接口
     *
     * @param params
     * @return
     */
    @PutMapping("/restore")
    @SysLog(operation = OperationEnum.RESET, type = TypeEnum.SYS_BACKUP)
    public R restore(@RequestBody(required = false) Map<String, Object> params) {
        ValidateUtils.is(params).notNull(RCode.SYS_BACKUP_FILENAME_ISNULL);
        String filename = (String) params.get("filename");
        String pin = (String) params.get("pin");

        ValidateUtils.is(filename).notNull(RCode.SYS_BACKUP_FILENAME_ISNULL)
                .and(pin).notNull(RCode.SYS_USER_PASSWORD_ISNULL);

        sysBackupService.restore(filename, pin);
        return R.ok();
    }


    /**
     * 系统备份删除接口
     *
     * @param filename
     * @return
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.SYS_BACKUP)
    public R delete(String filename) {
        if (Tool.StrUtil.isEmpty(filename)) throw new NZException(RCode.SYS_BACKUP_FILENAME_ISNULL);

        sysBackupService.removeBackFileByFilename(filename);
        return R.ok();
    }

    /**
     * 系统备份下载接口
     *
     * @param filename
     */
    @GetMapping("/download")
    @SysLog(operation = OperationEnum.DOWNLOAD, type = TypeEnum.SYS_BACKUP)
    public void download(HttpServletResponse response, String filename) throws IOException {
        if (Tool.StrUtil.isEmpty(filename)) throw new NZException(RCode.SYS_BACKUP_FILENAME_ISNULL);
        sysBackupService.downloadBackFileByFilename(response, filename);
    }


    /**
     * unpack backup file
     */
    @PostMapping("/unpack")
    @SysLog(operation = OperationEnum.DOWNLOAD, type = TypeEnum.SYS_BACKUP)
    public void unpackBackupFile(@RequestParam(value = "file") MultipartFile file, HttpServletResponse response) {
        // validate file format
        String filename = file.getOriginalFilename();
        String fileExtension = FilenameUtils.getExtension(filename);
        if (!StrUtil.startWith(filename, "NZ-") || ObjectUtil.notEqual("bak", fileExtension)) {
            throw new NZException(RCode.SYS_BACKUP_UNPACK_FILE_FORMAT_ERROR);
        }

        sysBackupService.unpackBackupFile(file, response);
    }


    /**
     * sync backup file
     *
     * @param serverid
     * @param type
     * @param filename
     * @param response
     */
    @GetMapping("/sync")
    public void syncBackupFile(@RequestParam(required = false) String serverid,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) String filename,
                               HttpServletResponse response) {
        ValidateUtils.is(serverid).notNull(RCode.SYS_BACKUP_SYNC_SERVERID_ISNULL).and(type).notNull(RCode.SYS_BACKUP_SYNC_TYPE_ISNULL);

        List<String> enumTypeList = Tool.ListUtil.of("list", "download");
        if (!enumTypeList.contains(type)) {
            throw new NZException(RCode.SYS_BACKUP_SYNC_TYPE_INVALID);
        }

        if (StrUtil.equals("download", type)) {
            ValidateUtils.is(filename).notNull(RCode.SYS_BACKUP_SYNC_FILENAME_ISNULL);
        }

        sysBackupService.syncBackupFile(serverid, type, filename, response);
    }

}
