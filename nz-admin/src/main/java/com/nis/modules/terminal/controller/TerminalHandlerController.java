package com.nis.modules.terminal.controller;

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
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

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
    public R resize(@RequestHeader(name = "Authorization") String token,
                    @RequestBody TerminalConfigDto dto) {
        Integer cols = dto.getCols();
        Integer height = dto.getHeight();
        Integer rows = dto.getRows();
        Integer width = dto.getWidth();
        if (Tool.ObjectUtil.hasEmpty(cols, height, rows, width)) {
            return R.error(RCode.TERMINAL_PARAM_ISNULL);
        }
        try {
            List<TerminalClient> terminalClients = TerminalSession.getAllTerminalClient(token);
            for (TerminalClient terminalClient : terminalClients) {
                // resize
                terminalClient.resize(cols, rows, width, height);
            }
        } catch (Exception e) {
            log.error(e);
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
    @SysLog(operation = OperationEnum.UNKNOWN, type = TypeEnum.TERMINAL)
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
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.TERMINAL)
    public Map<Object, Object> login(@RequestBody TerminalConfigDto configEntity) {
        Map<Object, Object> map = terminalSessionService.login(configEntity);
        return map;
    }

    @PostMapping("/login/duplicate")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.TERMINAL)
    public R loginDuplicate(@RequestBody TerminalConfigDto terminalConfigDto) {
        ValidateUtils.is(terminalConfigDto.getUuid()).notNull(RCode.TERMINAL_UUID_ISNULL);

        String uuid = terminalConfigDto.getUuid();
        Integer width = terminalConfigDto.getWidth();
        Integer height = terminalConfigDto.getHeight();
        Integer cols = terminalConfigDto.getCols();
        Integer rows = terminalConfigDto.getRows();

        return R.ok(
                terminalSessionService.duplicateSession(uuid, width, height, cols, rows)
        );
    }

    @PostMapping("/sftp/ls")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TERMINAL)
    public R ls(@RequestBody Map<String, String> param) {
        String uuid = param.get("uuid");
        String path = param.get("path");
        ValidateUtils.is(uuid).notNull(RCode.TERMINAL_UUID_ISNULL);
        return R.ok(
                terminalSessionService.lsOfSftp(uuid, Tool.StrUtil.emptyToDefault(path, "/"))
        );
    }

    @PostMapping("/sftp/mkdir")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.TERMINAL)
    public R mkdir(@RequestBody Map<String, String> param) {
        String uuid = param.get("uuid");
        String path = param.get("path");
        ValidateUtils.is(uuid).notNull(RCode.TERMINAL_UUID_ISNULL)
                .and(path).notNull(RCode.TERMINAL_PATH_ISNULL);

        terminalSessionService.mkdirOfSftp(uuid, path);
        return R.ok();
    }

    @PostMapping("/sftp/rm")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.TERMINAL)
    public R rm(@RequestBody Map<String, String> param) {
        String uuid = param.get("uuid");
        String path = param.get("path");
        ValidateUtils.is(uuid).notNull(RCode.TERMINAL_UUID_ISNULL)
                .and(path).notNull(RCode.TERMINAL_PATH_ISNULL);

        terminalSessionService.rmOfSftp(uuid, path);
        return R.ok();
    }

    @PutMapping("/sftp/upload")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.TERMINAL)
    public R upload(@RequestParam(value = "file") MultipartFile file,
                    @RequestParam String uuid, @RequestParam String path) {

        String tid = terminalSessionService.uploadOfSftp(uuid, path, file);
        return R.ok().putData("tid", tid);
    }


    @GetMapping("/sftp/process/{tid}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TERMINAL)
    public R querySftpUploadProcess(@PathVariable("tid") String tid) {
        return R.ok(
                terminalSessionService.querySftpUploadProcess(tid)
        );
    }

    @DeleteMapping("/sftp/cancel/{tid}")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.TERMINAL)
    public R cancelUpload(@PathVariable("tid") String tid) {
        terminalSessionService.cancelUpload(tid);
        return R.ok();
    }

    @PostMapping("/sftp/download")
    @SysLog(operation = OperationEnum.UNKNOWN, type = TypeEnum.TERMINAL)
    public void download(@RequestBody Map<String, String> param, HttpServletResponse response) {
        String uuid = param.get("uuid");
        String path = param.get("path");
        ValidateUtils.is(uuid).notNull(RCode.TERMINAL_UUID_ISNULL)
                .and(path).notNull(RCode.TERMINAL_PATH_ISNULL);

        terminalSessionService.downloadOfSftp(uuid, path, response);
    }

    @PutMapping("/sftp/rename")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.TERMINAL)
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
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.TERMINAL)
    public R querySftpLog(@RequestParam Map<String, Object> params) {
        PageUtils page = terminalSftpLogService.querySftpLogPage(params);
        return R.ok(page);
    }

}
