package com.nis.modules.terminal.service.impl;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.hutool.http.HttpStatus;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jcraft.jsch.*;
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
import com.nis.modules.terminal.backend.TerminalClient;
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
        log.info("[killSession] [begin] [uuid: {}]", uuid);
        WebSocketSession session = TerminalSession.findSession(uuid);

        // 未找到 websocket session 信息，连接信息已不在，更新 db 中 terminal session 状态
        if (ObjectUtil.isNull(session)) {
            log.warn("[killSession] [failed to get websocket connection through terminal session] [uuid: {}]", uuid);

            TerminalSessionEntity terminalSessionEntity = this.getOne(new LambdaQueryWrapper<TerminalSessionEntity>().eq(TerminalSessionEntity::getUuid, uuid));
            log.info("[killSession] [get the terminal session entity in the db] [uuid: {}] [status: {}]", uuid, ObjectUtil.isNull(terminalSessionEntity) ? null : terminalSessionEntity.getStatus());
            if (ObjectUtil.isNull(terminalSessionEntity)) {
                log.error("[killSession] [failed to get terminal session information] [uuid: {}]", uuid);
                throw new NZException(RCode.TERMINAL_SESSION_NOT_FOUND);
            }

            log.info("[killSession] [update terminal session state] [uuid: {}]", uuid);
            terminalSessionEntity.setEndTime(new Date());
            terminalSessionEntity.setKillUserId(ShiroUtils.getUserId());
            terminalSessionEntity.setStatus(TerminalConstant.SessionStatus.FORCE_QUIT.getValue());
            // update status
            this.saveOrUpdate(terminalSessionEntity);

            // 根据 websocket 找到 terminal session，更新状态，设置结束时间
        } else {
            try {
                TerminalClient terminalClient = TerminalSession.TERMINAL_SESSION.get(session);
                log.info("[killSession] [get connected session] [terminal session: {}]", JSONUtil.toJsonStr(terminalClient));
                String clientId = terminalClient.getClientId();
                TerminalSessionEntity sessionEntity = this.getOne(new LambdaQueryWrapper<TerminalSessionEntity>().eq(TerminalSessionEntity::getUuid, clientId));

                // TerminalSessionEntity sessionEntity = (TerminalSessionEntity) TerminalSession.getTerminalAttributeInfo(uuid).get("terminalSessionEntity");
                if (sessionEntity.getStartTime() == null) {
                    sessionEntity.setStartTime(new Date());
                }

                log.info("[killSession] [update terminal session state] [uuid: {}]", uuid);
                sessionEntity.setEndTime(new Date());
                sessionEntity.setKillUserId(ShiroUtils.getUserId());
                sessionEntity.setStatus(TerminalConstant.SessionStatus.FORCE_QUIT.getValue());
                // update status
                this.saveOrUpdate(sessionEntity);

                try {
                    TerminalSession.remove(session);
                    StringBuilder sb = new StringBuilder();
                    sb.append("\u001B[01;31m\u001B[K Administrator disconnected \u001B[m\u001B[K  \r\n");
                    sb.append("$: ");
                    session.sendMessage(new TextMessage(sb.toString()));
                } catch (IOException e) {
                    log.error(e, "[killSession] [send exit prompt error] [uuid: {}]", uuid);
                }

                // kill 之后，将剩余文件写到数据库中
                TerminalHandler.saveRecordContentAfterClientClosed(uuid);
            } catch (Exception e) {
                log.error(e, "[killSession] [error] [uuid: {}]", uuid);
                throw new NZException(RCode.TERMINAL_STOP_SESSION_FAILED);
            }
        }
        log.info("[killSession] [finshed] [uuid: {}]", uuid);
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

            // 以 asset 为主时，只需校验是否存在 username & authType，其他属性添加 asset 时有校验
            // SSHT & ELNET
            // username & authType required
            String authUsername = asset.getAuthUsername();
            Integer authType = asset.getAuthType();
            ValidateUtils.is(authUsername).notNull(RCode.TERMINAL_CONNECTION_AUTHUSERNAME_ISNULL)
                    .and(authType).notNull(RCode.ASSET_TERMINAL_ACCOUNT_INFO_NOTFOUND);
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
            AssetAsset queryAssetByHost = assetService.getOne(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getManageIp, configEntity.getHost()));
            if (ObjectUtil.isNotNull(queryAssetByHost)) {
                asset.setId(queryAssetByHost.getId());
            }

            // decode auth pin
            String decodeAuthPin = Tool.URLUtil.decode(asset.getAuthPin());
            asset.setAuthPin(decodeAuthPin);

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
                if (e instanceof JSchException) {
                    resultMap.putAll(R.error(400, e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                } else {
                    resultMap.putAll(R.error(RCode.ERROR.getCode(), RCode.ERROR.getMsg()));
                }
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
        ChannelSftp channelSftp = null;
        try {
            boolean isPathDir = sftp.cd(path);
            log.info("[lsOfSftp] [is the path a directory] [path: {}] [isDir: {}]", path, isPathDir);
            if (!isPathDir) {
                log.warn("[lsOfSftp] [cannot display remote folder] [path: {}]", path);
                throw new NZException(RCode.TERMINAL_LS_FOLDER_ERROR);
            }

            log.info("[lsOfSftp] [ls directory] [path: {}]", path);

            channelSftp = sftp.getClient();
            List<ChannelSftp.LsEntry> lsEntries = sftp.lsEntries(path);

            List<Object> list = Tool.ListUtil.list(true);
            for (ChannelSftp.LsEntry lsEntry : lsEntries) {
                SftpATTRS attrs = lsEntry.getAttrs();

                String fullName = StrUtil.EMPTY;
                if (isPathDir) {
                    fullName = new File(path, lsEntry.getFilename()).toString();
                } else {
                    fullName = path;
                }
                boolean isLink = attrs.isLink();
                boolean isDir = attrs.isDir();
                boolean isBlk = attrs.isBlk();
                boolean isFifo = attrs.isFifo();
                boolean isReg = attrs.isReg();
                boolean isSock = attrs.isSock();
                long size = attrs.getSize();
                String linkName = null;
                // 当文件为链接时，需要进一步判断文件类型和链接文件名称
                if(isLink) {
                	try {
                		linkName = channelSftp.readlink(fullName);
                		SftpATTRS stat = channelSftp.stat(fullName);
                		isDir = stat.isDir();
                		isBlk = stat.isBlk();
                		isFifo = stat.isFifo();
                		isReg = stat.isReg();
                		isSock = stat.isSock();
                        size = stat.getSize();
					} catch (Exception e) {
						log.warn(e,"link error: {}", fullName);
					}
                }
                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("name", lsEntry.getFilename())
                        .put("fullName", fullName)
                        .put("permissions", attrs.getPermissions())
                        .put("permissionsString", attrs.getPermissionsString())
                        .put("gid", attrs.getGId())
                        .put("uid", attrs.getUId())
                        .put("extended", attrs.getExtended())
                        .put("size", size)
                        .put("isDir", isDir)
                        .put("isBlk", isBlk)
                        .put("isFifo", isFifo)
                        .put("isLink", isLink)
                        .put("isReg", isReg)
                        .put("isSock", isSock)
                        .put("cts", attrs.getATime())
                        .put("uts", attrs.getMTime())
                        .put("linkName", linkName)
                        .map();

                list.add(map);
            }

            resultData = Tool.MapUtil.builder()
                    .put("list", list)
                    .put("path", path)
                    .map();
        } finally {
            Tool.JschUtil.close(channelSftp);
        }
        return resultData;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mkdirOfSftp(String uuid, String path) {
        Sftp sftp = this.getSftpChannel(uuid);
        try {
            boolean exist = sftp.exist(path);
            log.info("[mkdirOfSftp] [the path exist] [path: {}] [value: {}]", path, exist);
            if (exist) {
                log.warn("[mkdirOfSftp] [The specified file name already exists, please enter another name] [path: {}]", path);
                throw new NZException(RCode.TERMINAL_MKDIR_DIR_ALREADY_EXISTS);
            } else {
                log.info("[mkdirOfSftp] [create path] [path: {}]", path);

                // mkdir
                sftp.mkDirs(path);
                // save log
                sftpLogService.saveSftpLog(uuid, path, TerminalConstant.SftpOperType.MKDIR.getValue());
            }
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
                String fullName = FileUtil.file(path, multipartFile.getOriginalFilename()).toString();
                sftpLogService.saveSftpLog(uuid, fullName, TerminalConstant.SftpOperType.UPLOAD.getValue());

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
    public void renameFileOfSftp(String uuid, String oldPath, String newPath) {
        Sftp sftp = this.getSftpChannel(uuid);
        ChannelSftp channelSftp = null;
        try {
            channelSftp = sftp.getClient();

            boolean exist = sftp.exist(newPath);
            log.info("[renameFileOfSftp] [rename newpath exist] [newPath: {}] [exist: {}]", newPath, exist);
            if (exist && !StrUtil.equals(oldPath, newPath)) {
                log.warn("[renameFileOfSftp] [The specified file name already exists, please enter another name] [path: {}]", newPath);
                throw new NZException(RCode.TERMINAL_MKDIR_DIR_ALREADY_EXISTS);
            }

            // rename
            channelSftp.rename(oldPath, newPath);

            // record sftp log
            // sftpLogService.saveSftpLog(uuid);
        } catch (SftpException e) {
            log.error(e, "[renameFileOfSftp] [rename file error] [uuid: {}] [oldPath: {}] [newPath: {}]", uuid, oldPath, newPath);
            throw new NZException(e.getMessage(), HttpStatus.HTTP_INTERNAL_ERROR);
        } finally {
            // disconnect
            Tool.JschUtil.close(channelSftp);
        }
    }

    @Override
    public Map<Object, Object> duplicateSession(String uuid, Integer width, Integer height, Integer cols, Integer rows) {
        log.info("[duplicateSession] [begin] [uuid: {}]", uuid);
        Map<Object, Object> connObjMap = TerminalSession.TERMINAL_CONNECTED_CACHE.get(uuid);
        log.info("[duplicateSession] [source sesion] [uuid: {}] [exist: {}]", uuid, Tool.MapUtil.isNotEmpty(connObjMap));

        if (Tool.MapUtil.isEmpty(connObjMap)) {
            log.warn("[duplicateSession] [source sesion does not exist] [uuid: {}]", uuid);
            throw new NZException(RCode.TERMINAL_SESSION_NOT_FOUND);
        }

        String duplicateSessionUUID = StrUtil.uuid();
        log.info("[duplicateSession] [source session exists. duplicate session info] [source uuid: {}] [duplicate uuid: {}]", uuid, duplicateSessionUUID);

        // terminal Connection Info
        AssetAsset terminalConnInfo = (AssetAsset) connObjMap.get(TerminalSession.CONNECTION_ASSET_KEY);
        log.info("[duplicateSession] [duplicate session source information] [asset: {}]", JSONUtil.toJsonStr(terminalConnInfo));

        // SSH
        if (TerminalConstant.ProtocolType.SSH.getValue().equals(terminalConnInfo.getAuthProtocol())) {
            try {
                log.info("[duplicateSession] [reopen SSH connection begin.] [duplicate uuid: {}]", duplicateSessionUUID);
                Session session = SshUtil.getSshConnection(terminalConnInfo);
                TerminalSession.TERMINAL_CONNECTED_CACHE.put(duplicateSessionUUID,
                        Tool.MapUtil.builder()
                                .put(TerminalSession.CONNECTION_ASSET_KEY, terminalConnInfo)
                                .put(TerminalSession.CONNECTION_CONNECTION_ENTITY_KEY, session)
                                .map());
                TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP.put(duplicateSessionUUID, Tool.DateUtil.currentSeconds());
                log.info("[duplicateSession] [reopen SSH connection success.] [duplicate uuid: {}]", duplicateSessionUUID);
            } catch (Exception e) {
                log.error(e, "[duplicateSession] [reopen SSH connection error.] [duplicate uuid: {}]", duplicateSessionUUID);
                throw new NZException(RCode.TERMINAL_DUPLICATE_SESSION_FAILED);
            }
        }

        // TELNET
        if (TerminalConstant.ProtocolType.TELNET.getValue().equals(terminalConnInfo.getAuthProtocol())) {
            try {
                log.info("[duplicateSession] [reopen TELNET connection begin.] [duplicate uuid: {}]", duplicateSessionUUID);
                TelnetUtil telnetUtil = new TelnetUtil();
                telnetUtil.connect(terminalConnInfo.getManageIp(), terminalConnInfo.getAuthProtocolPort());
                List<SysConfigEntity> configList = sysConfService.list(new LambdaQueryWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey, "terminal_telnet_user_tip", "terminal_telnet_pin_tip"));
                Map<String, String> systemTipConfigMap = configList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

                String authUserTip = TelnetUtil.getTelenetLoginTip(terminalConnInfo.getAuthUserTip(), systemTipConfigMap, "terminal_telnet_user_tip");
                String authPinTip = TelnetUtil.getTelenetLoginTip(terminalConnInfo.getAuthPinTip(), systemTipConfigMap, "terminal_telnet_pin_tip");
                boolean isLogin = telnetUtil.login(terminalConnInfo.getAuthUsername(), StringUtils.str(Base64.getDecoder().decode(terminalConnInfo.getAuthPin())), authUserTip, authPinTip);
                if (isLogin) {
                    TerminalSession.TERMINAL_CONNECTED_CACHE.put(duplicateSessionUUID,
                            Tool.MapUtil.builder()
                                    .put(TerminalSession.CONNECTION_ASSET_KEY, terminalConnInfo)
                                    .put(TerminalSession.CONNECTION_CONNECTION_ENTITY_KEY, telnetUtil.getTelnetClient())
                                    .map());
                    TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP.put(duplicateSessionUUID, Tool.DateUtil.currentSeconds());
                    log.info("[duplicateSession] [reopen TELNET connection success.] [duplicate uuid: {}]", duplicateSessionUUID);
                } else {
                    log.warn("[duplicateSession] [reopen TELNET connection error.] [duplicate uuid: {}]", duplicateSessionUUID);
                    throw new NZException(RCode.TERMINAL_DUPLICATE_SESSION_FAILED);
                }
            } catch (Exception e) {
                log.error(e, "[duplicateSession] [reopen TELNET connection error.] [duplicate uuid: {}]", duplicateSessionUUID);
                throw new NZException(RCode.TERMINAL_DUPLICATE_SESSION_FAILED);
            }
        }

        // return duplicate session map
        Map<Object, Object> duplicateSessionMap = Tool.MapUtil.builder()
                .put("uuid", duplicateSessionUUID)
                .put("host", terminalConnInfo.getManageIp())
                .put("port", terminalConnInfo.getAuthProtocolPort())
                .put("authProtocol", terminalConnInfo.getAuthProtocol())
                .put("authUsername", terminalConnInfo.getAuthUsername())
                .map();
        log.info("[duplicateSession] [duplicate session finshed] [source uuid: {}] [duplicate uuid: {}]", uuid, duplicateSessionUUID);
        return duplicateSessionMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void downloadOfSftp(String uuid, String path, HttpServletResponse response) {
        Sftp sftp = this.getSftpChannel(uuid);

        StopWatch sw = new StopWatch();
        sw.start();
        long fileSize = 0;
        try {
            boolean exist = sftp.exist(path);
            if (!exist) {
                log.info("[downloadOfSftp] [no such file] [path: {}]", path);
                throw new NZException(RCode.TERMINAL_DOWNLOAD_NO_SUCH_FILE);
            }
            boolean isDir = sftp.cd(path);
            if (isDir) {
                log.info("[downloadOfSftp] [not supported to get directory] [path: {}]", path);
                throw new NZException(RCode.TERMINAL_DOWNLOAD_NOT_SUPPORTED_DIRECTORY);
            }

            List<ChannelSftp.LsEntry> entryList = sftp.lsEntries(path);
            ChannelSftp.LsEntry fileEntity = Tool.CollUtil.getFirst(entryList);
            if (ObjectUtil.isNull(fileEntity)) {
                log.info("[downloadOfSftp] [no such file] [path: {}]", path);
                throw new NZException(RCode.TERMINAL_DOWNLOAD_NO_SUCH_FILE);
            }

            // file size
            fileSize = fileEntity.getAttrs().getSize();

            log.info("[downloadOfSftp] [download file begin] [path: {}] [file size: {}]", path, fileSize);

            String fileName = fileEntity.getFilename();
            response.setCharacterEncoding("utf-8");
            response.setContentType("multipart/form-data");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            response.addHeader("Content-Length", Tool.StrUtil.toString(fileSize));

            // download
            ChannelSftp channelSftp = sftp.getClient();
            channelSftp.get(path, response.getOutputStream());

            // record sftp log
            sftpLogService.saveSftpLog(uuid, path, TerminalConstant.SftpOperType.DOWNLOAD.getValue());
        } catch (Exception e) {
            log.error(e, "[downloadOfSftp] [download file error] [path: {}] [file size: {}]", path, fileSize);
        } finally {
            sw.stop();
            // disconnect
            sftp.getClient().disconnect();
        }
        log.info("[downloadOfSftp] [download file finshed] [uuid: {}] [path: {}] [file size: {}] [Run Time: {}]", uuid, path, fileSize, sw.toString());
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
