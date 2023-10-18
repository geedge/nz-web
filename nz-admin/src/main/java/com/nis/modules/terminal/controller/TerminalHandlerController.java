package com.nis.modules.terminal.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.common.xss.XssUtil;
import com.nis.modules.terminal.backend.TerminalClient;
import com.nis.modules.terminal.backend.TerminalSession;
import com.nis.modules.terminal.entity.TerminalConfigDto;
import com.nis.modules.terminal.service.TerminalCmdService;
import com.nis.modules.terminal.service.TerminalRecordService;
import com.nis.modules.terminal.service.TerminalSessionService;
import com.nis.modules.terminal.service.TerminalSftpLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/terminal")
public class TerminalHandlerController {

    private static final Log log = Log.get();

    @Autowired
    private TerminalCmdService terminalCmdService;

    @Autowired
    private TerminalRecordService terminalRecordService;

    @Autowired
    private TerminalSftpLogService terminalSftpLogService;

    @Autowired
    private TerminalSessionService terminalSessionService;

    @Value("${nezha.uploadTempPath}")
    private String configUploadPath;

    @PostMapping(value = "resize")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.TERMINAL)
    public R resize(@RequestHeader(name = "Authorization", required = true) String token,
                    @RequestBody TerminalConfigDto dto) {
        Integer cols = dto.getCols();
        Integer height = dto.getHeight();
        Integer rows = dto.getRows();
        Integer width = dto.getWidth();
        if (Tool.ObjectUtil.hasEmpty(cols, height, rows, width)) {
            return R.error(RCode.TERMINAL_PARAM_ISNULL);
        }

        log.info("[resize] [begin]");
        try {
            List<TerminalClient> allTerminalClients = TerminalSession.TERMINAL_SESSION.values().stream().distinct().collect(Collectors.toList());
            List<TerminalClient> resizeClients = allTerminalClients.stream().filter(terminalClient -> Tool.StrUtil.equals(token, terminalClient.getToken())).collect(Collectors.toList());
            log.info("[resize] [all session: {}] [token: {}] [resize session: {}]", allTerminalClients.size(), token, resizeClients.size());

            for (TerminalClient terminalClient : resizeClients) {
                // resize
                terminalClient.resize(cols, rows, width, height);
            }
            log.info("[resize] [finshed]");
        } catch (Exception e) {
            log.error(e, "[resize] [error.]");
            return R.error();
        }
        return R.ok();
    }

    /**
     * terminal审计日志会话查询接口 默认按照 id 倒序
     *
     * @param params
     * @return
     */
    @GetMapping("/session")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TERMINAL)
    public R querySessionPage(@RequestParam Map<String, Object> params) {
        params = (Map<String, Object>) XssUtil.filterParameters((Map) params);
        PageUtils page = terminalSessionService.queryPage(params);
        return R.ok(page);
    }

    /**
     * terminal审计日志命令查询接口 按照  id asc
     *
     * @param params
     * @return
     */
    @GetMapping("/cmd")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TERMINAL)
    public R queryCmdPage(@RequestParam Map<String, Object> params) {
        params = (Map<String, Object>) XssUtil.filterParameters((Map) params);
        Map map = terminalCmdService.queryCmdInfos(params);
        return R.ok(map);
    }

    @PutMapping("/kill")
    @RequiresPermissions({"terminal:kill"})
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.TERMINAL)
    public R killTerminal(@RequestBody Map<String, String> params) {
        ValidateUtils.is(params.get("uuid")).notNull(RCode.TERMINAL_UUID_ISNULL);

        terminalSessionService.killSession(params.get("uuid"));
        return R.ok();
    }

    @GetMapping("/record")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TERMINAL)
    public R queryTerminalRecord(@RequestParam Map<String, Object> params) {
        params = (Map<String, Object>) XssUtil.filterParameters((Map) params);
        Map map = terminalRecordService.queryTerminalRecordInfos(params);
        return R.ok(map);
    }

    @PostMapping("/login")
    @SysLog(operation = OperationEnum.LOGIN, type = TypeEnum.TERMINAL)
    public Map<Object, Object> login(@RequestHeader(name = "Authorization") String token,
                                     @RequestBody TerminalConfigDto configEntity) {
        Map<Object, Object> map = terminalSessionService.login(token, configEntity);
        return map;
    }

    @PostMapping("/login/duplicate")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.TERMINAL)
    public R loginDuplicate(@RequestHeader(name = "Authorization") String token,
                            @RequestBody TerminalConfigDto terminalConfigDto) {
        ValidateUtils.is(terminalConfigDto.getUuid()).notNull(RCode.TERMINAL_UUID_ISNULL);

        String uuid = terminalConfigDto.getUuid();
        Integer width = terminalConfigDto.getWidth();
        Integer height = terminalConfigDto.getHeight();
        Integer cols = terminalConfigDto.getCols();
        Integer rows = terminalConfigDto.getRows();

        return R.ok(
                terminalSessionService.duplicateSession(token, uuid, width, height, cols, rows)
        );
    }

    @PostMapping("/sftp/ls")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SFTP)
    public R ls(@RequestBody Map<String, String> param) {
        String uuid = param.get("uuid");
        String path = param.get("path");
        ValidateUtils.is(uuid).notNull(RCode.TERMINAL_UUID_ISNULL);
        return R.ok(
                terminalSessionService.lsOfSftp(uuid, Tool.StrUtil.emptyToDefault(path, "/"))
        );
    }

    @PostMapping("/sftp/mkdir")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.SFTP)
    public R mkdir(@RequestBody Map<String, String> param) {
        String uuid = param.get("uuid");
        String path = param.get("path");
        ValidateUtils.is(uuid).notNull(RCode.TERMINAL_UUID_ISNULL)
                .and(path).notNull(RCode.TERMINAL_PATH_ISNULL);

        terminalSessionService.mkdirOfSftp(uuid, path);
        return R.ok();
    }

    @PostMapping("/sftp/rm")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.SFTP)
    public R rm(@RequestBody Map<String, String> param) {
        String uuid = param.get("uuid");
        String path = param.get("path");
        ValidateUtils.is(uuid).notNull(RCode.TERMINAL_UUID_ISNULL)
                .and(path).notNull(RCode.TERMINAL_PATH_ISNULL);

        terminalSessionService.rmOfSftp(uuid, path);
        return R.ok();
    }

    @PutMapping("/sftp/upload")
    //@SysLog(operation = OperationEnum.UPLOAD, type = TypeEnum.SFTP)
    public R upload(@RequestParam(value = "chunkFile") MultipartFile file,
                    @RequestParam String uuid, @RequestParam String path, @RequestParam String fileName, @RequestParam Integer replace, @RequestParam Long totalSize, @RequestParam Long chunkSize, @RequestParam Long chunkStart) {
        ValidateUtils.is(uuid).notNull(RCode.TERMINAL_UUID_ISNULL)
                .and(path).notNull(RCode.TERMINAL_PATH_ISNULL)
                .and(fileName).notNull(RCode.TERMINAL_UPLOAD_FILENAME_ISNULL)
                .and(totalSize).notNull(RCode.TERMINAL_UPLOAD_TOTAL_SIZE_ISNULL)
                .and(chunkSize).notNull(RCode.TERMINAL_UPLOAD_CHUNK_SIZE_ISNULL)
                .and(chunkStart).notNull(RCode.TERMINAL_UPLOAD_CHUNK_START_ISNULL);
        replace = ObjectUtil.defaultIfNull(replace, 1);

        Long uploadSize = terminalSessionService.uploadOfSftp(uuid, path, file, fileName, replace, totalSize, chunkSize, chunkStart);
        return R.ok().putData("uploadSize", uploadSize);
    }


    @GetMapping("/sftp/process/{tid}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SFTP)
    public R querySftpUploadProcess(@PathVariable("tid") String tid) {
        return R.ok(
                terminalSessionService.querySftpUploadProcess(tid)
        );
    }

    @DeleteMapping("/sftp/cancel/{tid}")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.SFTP)
    public R cancelUpload(@PathVariable("tid") String tid) {
        terminalSessionService.cancelUpload(tid);
        return R.ok();
    }

    @PostMapping("/sftp/download")
    @SysLog(operation = OperationEnum.DOWNLOAD, type = TypeEnum.SFTP)
    public void download(@RequestBody Map<String, String> param, HttpServletResponse response) {
        String uuid = param.get("uuid");
        String path = param.get("path");
        ValidateUtils.is(uuid).notNull(RCode.TERMINAL_UUID_ISNULL)
                .and(path).notNull(RCode.TERMINAL_PATH_ISNULL);

        terminalSessionService.downloadOfSftp(uuid, path, response);
    }

    @PutMapping("/sftp/rename")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.SFTP)
    public R rename(@RequestBody Map<String, String> param) {
        String uuid = param.get("uuid");
        String oldPath = param.get("oldPath");
        String newPath = param.get("newPath");

        ValidateUtils.is(uuid).notNull(RCode.TERMINAL_UUID_ISNULL)
                .and(oldPath).notNull(RCode.TERMINAL_PATH_ISNULL)
                .and(newPath).notNull(RCode.TERMINAL_NEW_PATH_ISNULL);

        terminalSessionService.renameFileOfSftp(uuid, oldPath, newPath);
        return R.ok();
    }


    @GetMapping("/sftp/log")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SFTP)
    public R querySftpLog(@RequestParam Map<String, Object> params) {
        PageUtils page = terminalSftpLogService.querySftpLogPage(params);
        return R.ok(page);
    }

    @GetMapping("/sftp/upload/check")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.SFTP)
    public R uploadCheck(@RequestParam String uuid, @RequestParam String path, @RequestParam String fileName) {

        ValidateUtils.is(uuid).notNull(RCode.TERMINAL_UUID_ISNULL)
                .and(path).notNull(RCode.TERMINAL_PATH_ISNULL)
                .and(fileName).notNull(RCode.TERMINAL_UPLOAD_FILENAME_ISNULL);

        Map data = terminalSessionService.uploadCheckOfSftp(uuid, path, fileName);
        return R.ok(data);
    }

}
