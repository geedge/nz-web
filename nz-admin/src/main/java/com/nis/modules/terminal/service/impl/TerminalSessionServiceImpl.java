package com.nis.modules.terminal.service.impl;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetTypeConf;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetTypeConfService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.terminal.backend.TerminalHandler;
import com.nis.modules.terminal.backend.TerminalSession;
import com.nis.modules.terminal.dao.TerminalSessionDao;
import com.nis.modules.terminal.entity.TerminalConfigDto;
import com.nis.modules.terminal.entity.TerminalSessionEntity;
import com.nis.modules.terminal.service.TerminalSessionService;
import com.nis.modules.terminal.service.TerminalSftpLogService;
import com.nis.modules.terminal.utils.SFTPUploadMonitor;
import com.nis.modules.terminal.utils.TelnetUtil;
import com.nis.modules.terminal.utils.TerminalConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.net.telnet.TelnetClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service("terminalSessionService")
public class TerminalSessionServiceImpl extends ServiceImpl<TerminalSessionDao, TerminalSessionEntity> implements TerminalSessionService {

    private static final Log log = Log.get();

    @Autowired
    private TerminalSessionDao terminalSessionDao;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private AssetTypeConfService assetTypeConfService;

    @Autowired
    private TerminalSftpLogService sftpLogService;

    /**
     * upload progress cache
     */
    private static TimedCache<String, SFTPUploadMonitor> UPLOAD_PROGRESS_CACHE;

    private static Map<String, SftpUploadRunnable> SFTP_UPLOAD_THREAD_MAP = Tool.MapUtil.newConcurrentHashMap();

    @PostConstruct
    public void init() {
        log.info("Setup sftp upload progress cache begin...");
        // 默认 30 分钟失效
        UPLOAD_PROGRESS_CACHE = Tool.CacheUtil.newTimedCache(60000 * 30);
        log.info("Setup sftp upload progress cache finished");
    }

    /**
     * 处理请求参数
     *
     * @param params
     */
    private void handleQueryDateParam(Map<String, Object> params) {
        try {
            if (Tool.ObjectUtil.isNotEmpty(params.get("startTime"))) {
                Date searchStartAt = DateUtils.parseDate(params.get("startTime").toString(), "yyyy-MM-dd HH:mm:ss");
                params.put("startTime", searchStartAt);
            } else {
                params.put("startTime", null);
            }
            if (Tool.ObjectUtil.isNotEmpty(params.get("endTime"))) {
                Date searchEndAt = DateUtils.parseDate(params.get("endTime").toString(), "yyyy-MM-dd HH:mm:ss");
                params.put("endTime", searchEndAt);
            } else {
                params.put("endTime", null);
            }
        } catch (ParseException e) {
            log.error(e, "terminal session handle Query Date Param error, param: {}", Tool.JSONUtil.toJsonStr(params));
        }
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        // 处理请求参数
        this.handleQueryDateParam(params);

        IPage page = new Query(TerminalSessionEntity.class).getPage(params);
        List<TerminalSessionEntity> sessionEntityList = terminalSessionDao.queryList(page, params);

        // 查询结果不为空时处理返回结果
        if (CollectionUtils.isNotEmpty(sessionEntityList)) {
            List<SysUserEntity> userList = sysUserService.list();
            Map<Long, String> userIdAndNameMap = userList.stream().collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getName));
            for (TerminalSessionEntity sessionEntity : sessionEntityList) {
                sessionEntity.setKillUserName(userIdAndNameMap.get(sessionEntity.getKillUserId()));
            }
        }

        page.setRecords(sessionEntityList);
        return new PageUtils(page);
    }

    @Override
    public void killSession(String uuid) {
        WebSocketSession session = TerminalSession.findSession(uuid);
        if (session == null) {
            return;
        }

        /**
         * session 已经结束，更新状态，设置结束时间
         */
        TerminalSessionEntity sessionEntity = (TerminalSessionEntity) TerminalSession.getTerminalAttributeInfo(uuid).get("terminalSessionEntity");
        if (sessionEntity.getStartTime() == null) {
            sessionEntity.setStartTime(new Date());
        }

        sessionEntity.setEndTime(new Date());
        sessionEntity.setKillUserId(ShiroUtils.getUserId());
        sessionEntity.setStatus(TerminalConstant.SessionStatus.FORCE_QUIT.getValue());
        TerminalSessionService terminalSessionService = (TerminalSessionService) TerminalSession.CLIENT_ATTRIBUTE_INFO.get("terminalSessionService");
        terminalSessionService.saveOrUpdate(sessionEntity);


        try {
            TerminalSession.remove(session);
            StringBuilder sb = new StringBuilder();
            sb.append("\u001B[01;31m\u001B[K Administrator disconnected \u001B[m\u001B[K  \r\n");
            sb.append("$: ");
            session.sendMessage(new TextMessage(sb.toString()));
        } catch (IOException e) {
            log.error(String.format("terminal session kill failed,uuid = %s,错误信息 = %s", uuid, e.getMessage()), e);
        }

        // kill 之后，将剩余文件写到数据库中
        TerminalHandler.saveRecordContentAfterClientClosed(uuid);
    }

    @Override
    public void cleanExpiredDataByTime(DateTime startCleanTerminalTime) {
        terminalSessionDao.cleanExpiredDataByTime(startCleanTerminalTime);
    }

    /**
     * validateloginParam
     *
     * @param configEntity
     */
    private void validateloginParam(TerminalConfigDto configEntity) {
        Integer assetId = configEntity.getAssetId();
        if (Tool.ObjectUtil.isNotNull(assetId)) {
            AssetAsset asset = assetService.getById(assetId);
            ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);

            List<Integer> authProtocolEnumList = Arrays.asList(TerminalConstant.ProtocolType.values()).stream().map(TerminalConstant.ProtocolType::getValue).collect(Collectors.toList());
            AssetTypeConf typeConf = assetTypeConfService.getById(asset.getTypeId());
            // 不支持 ssh | telnet
            if (!authProtocolEnumList.contains(typeConf.getAuthProtocol())) {
                throw new NZException(RCode.TERMINAL_ASSET_NOT_SUPPORT);
            }

            // 以 asset 为主时，只需校验是否存在 username，其他属性添加 asset 时有校验
            // SSHT & ELNET
            // username required
            String authUsername = asset.getAuthUsername();
            ValidateUtils.is(authUsername).notNull(RCode.TERMINAL_CONNECTION_AUTHUSERNAME_ISNULL);
        } else {
            // host
            String host = configEntity.getHost();
            ValidateUtils.is(host).notNull(RCode.TERMINAL_CONNECTION_HOST_ISNULL);

            // port can not all empty
            Integer port = configEntity.getPort();
            Integer authProtocolPort = configEntity.getAuthProtocolPort();
            if (Tool.ObjectUtil.isAllEmpty(port, authProtocolPort)) {
                throw new NZException(RCode.TERMINAL_CONNECTION_HOST_ISNULL);
            }

            List<Integer> authProtocolEnumList = Arrays.asList(TerminalConstant.ProtocolType.values()).stream().map(TerminalConstant.ProtocolType::getValue).collect(Collectors.toList());
            Integer authProtocol = configEntity.getAuthProtocol();
            if (!authProtocolEnumList.contains(authProtocol)) {
                throw new NZException(RCode.TERMINAL_CONNECTION_AUTHPROTOCOL_INVALIDED);
            }

            // username required
            String authUsername = configEntity.getAuthUsername();
            ValidateUtils.is(authUsername).notNull(RCode.TERMINAL_CONNECTION_AUTHUSERNAME_ISNULL);

            // SSH
            if (TerminalConstant.ProtocolType.SSH.getValue().equals(authProtocol)) {
                List<Integer> authTypeEnumList = Arrays.asList(TerminalConstant.SshAuthType.values()).stream().map(TerminalConstant.SshAuthType::getType).collect(Collectors.toList());
                Integer authType = configEntity.getAuthType();
                if (!authTypeEnumList.contains(authType)) {
                    throw new NZException(RCode.TERMINAL_CONNECTION_AUTHTYPE_INVALIDED);
                }

                if (Tool.ObjectUtil.equal(authType, TerminalConstant.SshAuthType.PIN.getType())) {
                    String authPin = configEntity.getAuthPin();
                    ValidateUtils.is(authPin).notNull(RCode.TERMINAL_CONNECTION_AUTHPIN_ISNULL);
                }
                if (Tool.ObjectUtil.equal(authType, TerminalConstant.SshAuthType.KEY.getType())) {
                    String authPriKey = configEntity.getAuthPriKey();
                    ValidateUtils.is(authPriKey).notNull(RCode.TERMINAL_CONNECTION_AUTHPRIKEY_ISNULL);
                }
            }

            // TELNET
            if (TerminalConstant.ProtocolType.TELNET.getValue().equals(authProtocol)) {
                String authPin = configEntity.getAuthPin();
                ValidateUtils.is(authPin).notNull(RCode.TERMINAL_CONNECTION_AUTHPIN_ISNULL);
            }

            // allow to be empty
            String authPinTip = configEntity.getAuthPinTip();
            String authUserTip = configEntity.getAuthUserTip();
        }
    }

    /**
     * get Terminal Connection Info Entity
     *
     * @param configEntity
     * @return
     */
    private AssetAsset getTerminalConnectionInfoEntity(TerminalConfigDto configEntity) {
        // terminal Connection Info
        AssetAsset terminalConnectionInfo = new AssetAsset();
        if (Tool.ObjectUtil.isNotNull(configEntity.getAssetId())) {
            AssetAsset asset = assetService.getById(configEntity.getAssetId());
            AssetTypeConf typeConf = assetTypeConfService.getById(asset.getTypeId());
            asset.setAuthProtocol(typeConf.getAuthProtocol());

            // clone
            terminalConnectionInfo = ObjectUtil.clone(asset);
        } else {
            AssetAsset asset = new AssetAsset();
            // copy Properties val
            Tool.BeanUtil.copyProperties(configEntity, asset, true);

            // different name attribute set val
            asset.setManageIp(configEntity.getHost());

            Integer port = configEntity.getPort();
            // priority config authProtocolPort
            Integer authProtocolPort = configEntity.getAuthProtocolPort();
            asset.setAuthProtocolPort(ObjectUtil.defaultIfNull(authProtocolPort, port));

            // 尝试用 host 补充 assetId
            AssetAsset queryAssetByHost = assetService.getOne(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getManageIp, configEntity));
            if (ObjectUtil.isNotNull(queryAssetByHost)) {
                asset.setId(queryAssetByHost.getId());
            }

            // clone
            terminalConnectionInfo = ObjectUtil.clone(asset);
        }
        return terminalConnectionInfo;
    }

    @Override
    public Map<Object, Object> login(TerminalConfigDto configEntity) {
        // validate login Param
        this.validateloginParam(configEntity);

        // uuid
        String uuid = Tool.StrUtil.uuid();

        // terminal Connection Info
        AssetAsset terminalConnInfo = this.getTerminalConnectionInfoEntity(configEntity);
        log.info("Open terminal, uuid: {}, connection Info: {}", uuid, JSONUtil.toJsonStr(terminalConnInfo));

        Map<Object, Object> resultMap = Tool.MapUtil.builder().map();

        Integer authProtocol = terminalConnInfo.getAuthProtocol();
        // SSH
        if (TerminalConstant.ProtocolType.SSH.getValue().equals(authProtocol)) {
            try {
                log.info("Open SSH terminal begin. uuid: {}", uuid);
                Session session = SshUtil.getSshConnection(terminalConnInfo);
                // connected terminal cache session info
                TerminalSession.TERMINAL_CONNECTED_CACHE.put(uuid,
                        Tool.MapUtil.builder()
                                .put(TerminalSession.CONNECTION_ASSET_KEY, terminalConnInfo)
                                .put(TerminalSession.CONNECTION_CONNECTION_ENTITY_KEY, session)
                                .map());
                // connected last active time
                TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP.put(uuid, Tool.DateUtil.currentSeconds());
                resultMap.putAll(R.ok());
                log.info("Open SSH terminal success. uuid: {}", uuid);
            } catch (Exception e) {
                log.error(e, "Open SSH terminal error. uuid: {}", uuid);
                resultMap.putAll(R.error(400, e.getMessage()));
            }
        }

        // TELNET
        if (TerminalConstant.ProtocolType.TELNET.getValue().equals(authProtocol)) {
            try {
                log.info("Open TELNET terminal begin. uuid: {}", uuid);
                TelnetUtil telnetUtil = new TelnetUtil();
                telnetUtil.connect(terminalConnInfo.getManageIp(), terminalConnInfo.getAuthProtocolPort());

                String authUsername = terminalConnInfo.getAuthUsername();
                String pin = StringUtils.str(Base64.getDecoder().decode(terminalConnInfo.getAuthPin()));

                List<SysConfigEntity> configList = sysConfService.list(new LambdaQueryWrapper<SysConfigEntity>()
                        .in(SysConfigEntity::getParamKey, "terminal_telnet_user_tip", "terminal_telnet_pin_tip"));
                Map<String, String> systemTipConfigMap = configList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

                String authUserTip = terminalConnInfo.getAuthUserTip();
                String authPinTip = terminalConnInfo.getAuthPinTip();

                authUserTip = TelnetUtil.getTelenetLoginTip(authUserTip, systemTipConfigMap, "terminal_telnet_user_tip");
                authPinTip = TelnetUtil.getTelenetLoginTip(authPinTip, systemTipConfigMap, "terminal_telnet_pin_tip");
                boolean isLogin = telnetUtil.login(authUsername, pin, authUserTip, authPinTip);
                if (isLogin) {
                    log.info("Open TELNET terminal success. uuid: {}", uuid);
                    TelnetClient telnetClient = telnetUtil.getTelnetClient();
                    // connected terminal cache telnet client info
                    TerminalSession.TERMINAL_CONNECTED_CACHE.put(uuid,
                            Tool.MapUtil.builder()
                                    .put(TerminalSession.CONNECTION_ASSET_KEY, terminalConnInfo)
                                    .put(TerminalSession.CONNECTION_CONNECTION_ENTITY_KEY, telnetClient)
                                    .map());
                    // connected last active time
                    TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP.put(uuid, Tool.DateUtil.currentSeconds());
                    resultMap.putAll(R.ok());
                } else {
                    log.warn("Open TELNET terminal fail. uuid: {}", uuid);
                    resultMap.putAll(R.error(400, "Login incorrect"));
                }
            } catch (Exception e) {
                log.error(e, "Open TELNET terminal error. uuid: {}", uuid);
                resultMap.putAll(R.error(400, e.getMessage()));
            }
        }

        // client info
        Map<Object, Object> terminalConnInfoMap = Tool.MapUtil.builder()
                .put("uuid", uuid)
                .put("host", terminalConnInfo.getManageIp())
                .put("port", terminalConnInfo.getAuthProtocolPort())
                .put("authProtocol", terminalConnInfo.getAuthProtocol())
                .put("authUsername", terminalConnInfo.getAuthUsername())
                .map();
        resultMap.put("data", terminalConnInfoMap);
        return resultMap;
    }

    /**
     * get Sftp Channel
     *
     * @param uuid
     * @return
     */
    private Sftp getSftpChannel(String uuid) {
        Map<Object, Object> connObjMap = TerminalSession.TERMINAL_CONNECTED_CACHE.get(uuid);
        if (ObjectUtil.isNull(connObjMap)) {
            throw new NZException(RCode.TERMINAL_SESSION_NOT_FOUND);
        }

        Session session = (Session) connObjMap.get(TerminalSession.CONNECTION_CONNECTION_ENTITY_KEY);
        if (ObjectUtil.isNull(session)) {
            throw new NZException(RCode.TERMINAL_TELNET_TRANSFER);
        }

        // sftp channel
        Sftp sftp = new Sftp(session);
        // connected last active time
        TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP.put(uuid, Tool.DateUtil.currentSeconds());
        return sftp;
    }

    @Override
    public Map<Object, Object> lsOfSftp(String uuid, String path) {
        Map<Object, Object> resultData;
        Sftp sftp = this.getSftpChannel(uuid);
        try {
            List<ChannelSftp.LsEntry> lsEntries = sftp.lsEntries(path);

            List<Object> list = Tool.ListUtil.list(true);
            for (ChannelSftp.LsEntry lsEntry : lsEntries) {
                SftpATTRS attrs = lsEntry.getAttrs();
                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("name", lsEntry.getFilename())
                        .put("fullName", StrUtil.concat(true, path, File.separator, lsEntry.getFilename()))
                        .put("permissions", attrs.getPermissions())
                        .put("permissionsString", attrs.getPermissionsString())
                        .put("size", attrs.getSize())
                        .put("isDir", attrs.isDir())
                        .put("isBlk", attrs.isBlk())
                        .put("isFifo", attrs.isFifo())
                        .put("isLink", attrs.isLink())
                        .put("isReg", attrs.isReg())
                        .put("isSock", attrs.isSock())
                        .put("cts", attrs.getATime())
                        .put("uts", attrs.getMTime())
                        .map();

                list.add(map);
            }

            resultData = Tool.MapUtil.builder()
                    .put("list", list)
                    .put("path", path)
                    .map();
        } finally {
            // disconnect
            sftp.getClient().disconnect();
        }
        return resultData;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mkdirOfSftp(String uuid, String path) {
        Sftp sftp = this.getSftpChannel(uuid);
        try {
            sftp.mkDirs(path);

            sftpLogService.saveSftpLog(uuid, path, TerminalConstant.SftpOperType.MKDIR.getValue());
        } finally {
            // disconnect
            sftp.getClient().disconnect();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rmOfSftp(String uuid, String path) {
        Sftp sftp = this.getSftpChannel(uuid);
        try {
            boolean cd = sftp.cd(path);
            if (cd) {
                throw new NZException(RCode.TERMINAL_CANNOT_DELETE_DIR);
            }
            // delFile
            sftp.delFile(path);

            sftpLogService.saveSftpLog(uuid, path, TerminalConstant.SftpOperType.RM.getValue());
        } finally {
            // disconnect
            sftp.getClient().disconnect();
        }
    }

    @Override
    public String uploadOfSftp(String uuid, String path, MultipartFile multipartFile) {
        Sftp sftp = this.getSftpChannel(uuid);
        String tid = StrUtil.uuid();

        boolean exist = sftp.exist(path);
        if (!exist) {
            log.warn("upload file directory does not exist create a file directory, path: {}", path);
            this.mkdirOfSftp(uuid, path);
        }

        boolean isDir = sftp.cd(path);
        if (isDir) {
            File tempFile = Tool.FileUtil.newFile(Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, multipartFile.getOriginalFilename()));
            try {
                // copy stream
                FileUtils.copyInputStreamToFile(multipartFile.getInputStream(), tempFile);

                // upload thread
                SftpUploadRunnable sftpUploadRunnable = new SftpUploadRunnable(tid, path, sftp, tempFile);
                SFTP_UPLOAD_THREAD_MAP.put(tid, sftpUploadRunnable);

                // record log
                sftpLogService.saveSftpLog(uuid, StrUtil.concat(true, path, File.separator, multipartFile.getOriginalFilename()), TerminalConstant.SftpOperType.UPLOAD.getValue());

                // run upload
                Tool.ThreadUtil.execAsync(sftpUploadRunnable);
            } catch (IOException e) {
                log.error(e);
                FileUtil.del(tempFile);
            }
        } else {
            // disconnect
            sftp.getClient().disconnect();
            throw new NZException(RCode.TERMINAL_UPLOAD_DIRECTORY_ERROR);
        }
        return tid;
    }

    @Override
    public Map<Object, Object> querySftpUploadProcess(String tid) {
        SFTPUploadMonitor sftpUploadMonitor = UPLOAD_PROGRESS_CACHE.get(tid);
        if (ObjectUtil.isNull(sftpUploadMonitor)) {
            throw new NZException(RCode.TERMINAL_UPLOAD_PROGRESS_NOT_FOUND);
        }

        Map<Object, Object> map = Tool.MapUtil.builder()
                .put("tid", tid)
                // type   暂时支持上传，不支持下载
                .put("type", TerminalConstant.SftpOperType.UPLOAD.getValue())
                .put("total", sftpUploadMonitor.getTotalSize())
                .put("done", sftpUploadMonitor.getUploadProgress())
                .put("sts", sftpUploadMonitor.getStartTime())
                .put("cts", System.currentTimeMillis())
                .map();
        return map;
    }

    @Override
    public void cancelUpload(String tid) {
        SftpUploadRunnable uploadRunnable = SFTP_UPLOAD_THREAD_MAP.get(tid);
        if (ObjectUtil.isNotNull(uploadRunnable)) {
            // interrupt
            uploadRunnable.interrupt();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void downloadOfSftp(String uuid, String path, HttpServletResponse response) {
        Sftp sftp = this.getSftpChannel(uuid);
        File tempFile = Tool.FileUtil.file(Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, Tool.FileUtil.getName(path)));
        OutputStream os = null;

        StopWatch sw = new StopWatch();
        sw.start();
        long length = 0;
        try {
            boolean exist = sftp.exist(path);
            if (!exist) {
                throw new NZException(RCode.TERMINAL_DOWNLOAD_NO_SUCH_FILE);
            }
            boolean isDir = sftp.cd(path);
            if (isDir) {
                throw new NZException(RCode.TERMINAL_DOWNLOAD_NOT_SUPPORTED_DIRECTORY);
            }

            log.info("download file begin...  uuid: {}, path: {}", uuid, path);
            // download
            sftp.download(path, tempFile);
            length = tempFile.length();

            String fileName = Tool.FileUtil.getName(path);
            response.setCharacterEncoding("utf-8");
            response.setContentType("multipart/form-data");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            // 发送给客户端的数据
            os = response.getOutputStream();
            byte[] readBytes = IoUtil.readBytes(FileUtil.getInputStream(tempFile));
            os.write(readBytes);
            os.flush();

            sftpLogService.saveSftpLog(uuid, path, TerminalConstant.SftpOperType.DOWNLOAD.getValue());
        } catch (Exception e) {
            log.error(e, "download file error. uuid: {}, path: {}", uuid, path);
        } finally {
            sw.stop();
            // disconnect
            sftp.getClient().disconnect();

            IoUtil.close(os);
            Tool.FileUtil.del(tempFile);
        }
        log.info("download file finshed  uuid: {}, path: {}, total length: {}, Run Time: {}", uuid, path, length, sw.toString());
    }

    private class SftpUploadRunnable extends Thread {

        private String tid;
        private String path;

        private Sftp sftp;
        private File tempFile;

        public SftpUploadRunnable(String tid, String path, Sftp sftp, File tempFile) {
            this.tid = tid;
            this.path = path;

            this.sftp = sftp;
            this.tempFile = tempFile;
        }

        @Override
        public void run() {
            StopWatch sw = new StopWatch();
            sw.start();
            try {
                // SFTP Upload Monitor   query upload progress
                SFTPUploadMonitor sftpUploadMonitor = new SFTPUploadMonitor(tempFile.length());
                UPLOAD_PROGRESS_CACHE.put(tid, sftpUploadMonitor);

                log.info("upload file begin... tid: {}, path: {}", tid, path);
                // upload
                sftp.put(Tool.FileUtil.getAbsolutePath(tempFile), path, sftpUploadMonitor, Sftp.Mode.OVERWRITE);
            } catch (Exception e) {
                log.error(e);
            } finally {
                sw.stop();

                FileUtil.del(tempFile);

                // disconnect
                sftp.getClient().disconnect();

                // del tid & thread mapping
                SFTP_UPLOAD_THREAD_MAP.remove(tid);
            }
            log.info("upload file finshed tid: {}, path: {}, length: {}, Run Time: {}", tid, path, tempFile.length(), sw.toString());
        }

        @Override
        public void interrupt() {
            super.interrupt();
            log.warn("upload file thread is interrupted. tid: {}, path: {}", tid, path);

            // del temp file
            FileUtil.del(tempFile);

            try {
                // disconnect channel
                sftp.getClient().disconnect();
            } catch (Exception e) {
                log.error(e, "sftp close error, thread interrupting. tid: {}, path: {}", tid, path);
            }

            // del tid & thread mapping
            SFTP_UPLOAD_THREAD_MAP.remove(tid);

            // progress
            SFTPUploadMonitor sftpUploadMonitor = UPLOAD_PROGRESS_CACHE.get(tid);
            log.warn("upload file thread is interrupted. tid: {}, progress: {}, path: {}", tid, sftpUploadMonitor.getUploadProgress() + '%', path);
        }
    }
}
