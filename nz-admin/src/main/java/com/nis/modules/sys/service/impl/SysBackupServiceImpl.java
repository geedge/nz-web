package com.nis.modules.sys.service.impl;

import cn.hutool.core.compress.Gzip;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.SysBackupLog;
import com.nis.modules.sys.entity.SysComponent;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.*;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.mariadb.jdbc.MariaDbBlob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("sysBackupService")
public class SysBackupServiceImpl implements SysBackupService {

    private static final Log log = Log.get();

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysComponentService sysComponentService;

    @Autowired
    private SysBackupLogService sysBackupLogService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${nezha.exportMaxSize:10000}")
    private Integer exportMaxSize;

    @Override
    public Object queryInfo() {
        String backupConfigJsonStr = sysConfService.getValue("backup_config");
        Object resultObj = JSON.parseObject(backupConfigJsonStr, Object.class);
        return resultObj;
    }

    @Override
    public void save(Map<String, Object> params) {
        String backupConfigJsonStr = sysConfService.getValue("backup_config");
        if (ToolUtil.isNotEmpty(backupConfigJsonStr)) {
            sysConfService.update(new LambdaUpdateWrapper<SysConfigEntity>().set(SysConfigEntity::getParamValue, JSON.toJSONString(params)).eq(SysConfigEntity::getParamKey, "backup_config"));

            Map oldParamMap = JSONObject.parseObject(StrUtil.emptyToDefault(backupConfigJsonStr, StrUtil.EMPTY_JSON), LinkedHashMap.class);
            if (!oldParamMap.equals(params)) {
                redisTemplate.convertAndSend(Constant.BACKUP_TOPIC_NAME, JSONObject.toJSONString(params));
            }
        } else {
            // 新增
            SysConfigEntity config = new SysConfigEntity();
            config.setParamKey("backup_config");
            config.setParamValue(JSON.toJSONString(params));
            config.setStatus("1");
            sysConfService.save(config);

            redisTemplate.convertAndSend(Constant.BACKUP_TOPIC_NAME, JSONObject.toJSONString(params));
        }
    }

    @Override
    public List<Object> queryBackupList() {
        List<Object> result = null;
        String path = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);
        boolean directory = FileUtil.isDirectory(path);
        if (directory) {
            List<File> loopFiles = FileUtil.loopFiles(path);
            if (ToolUtil.isNotEmpty(loopFiles)) {
                List<File> sortFile = loopFiles.stream().sorted(Comparator.comparing(File::lastModified).reversed()).collect(Collectors.toList());
                result = new ArrayList<Object>();
                for (File file : sortFile) {
                    Map<String, Object> data = new HashMap<String, Object>();
                    data.put("fileName", file.getName());
                    data.put("time", DateUtil.date(file.lastModified()));
                    data.put("size", file.length());
                    data.put("remark", "");
                    try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                        long fileSize = file.length();
                        long startPosition = fileSize - 2 * 1024;
                        startPosition = startPosition < 0 ? 0 : startPosition;

                        raf.seek(startPosition);
                        byte[] buffer = new byte[2 * 1024];
                        raf.readFully(buffer);
                        data.put("remark", StrUtil.trim(StrUtil.str(buffer, "UTF-8")));
                    } catch (IOException e) {
                        log.error(e, "[queryBackupList] [read remark error] [file: {}]", file.getName());
                    }
                    result.add(data);
                }
            }
        }
        return result;
    }

    @Override
    public void backupData(String remark) throws Exception {
        // BACK_UP log
        SysBackupLog backupLog = SysBackupLog.buildBasicLogEntity(Constant.SysBackupTypeEnum.BACK_UP.getValue());
        try {
            String tempFilePerfix = IdUtil.fastSimpleUUID();
            log.info("[backupData] [begin] [tempFilePerfix: {}]", tempFilePerfix);

            // preconditions
            String nzBackupFilePath = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);
            boolean directoryExist = FileUtil.isDirectory(nzBackupFilePath);
            log.info("[backupData] [backup_file_path] [exist: {}]", directoryExist);
            if (BooleanUtil.negate(directoryExist)) {
                log.info("[backupData] [create backup_file_path] [path: {}]", nzBackupFilePath);
                FileUtil.mkdir(nzBackupFilePath);
            }

            String nzBackupFileName = StrUtil.concat(true, "NZ-", DateUtil.format(new Date(), "yyyyMMdd'T'HHmmss"), ".bak");
            backupLog.setFilename(nzBackupFileName);

            // export db to sql file
            this.exportDbToSQLFile(tempFilePerfix);

            /**
             * secure writer backup file
             * sqlFile -> gzip -> aes -> append remark str -> NZ-date.bak
             */
            this.secureWriterBackupFile(tempFilePerfix, nzBackupFileName, remark);

            // backup file rotate
            this.backupFileRotate();

            log.info("[backupData] [finshed] [tempFilePerfix: {}]", tempFilePerfix);
        } catch (Exception e) {
            backupLog.setState(0);
            backupLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, Constant.MYSQL_TEXT_MAXLENGTH));
            throw e;
        } finally {
            try {
                // save sys backup log
                sysBackupLogService.save(backupLog);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    @Override
    public void removeBackFileByFilename(String filename) {
        String path = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);

        String sqlFilePath = Tool.StrUtil.concat(true, path, File.separator, filename);
        boolean exist = FileUtil.exist(sqlFilePath);
        if (exist) {
            boolean isFile = FileUtil.isFile(sqlFilePath);
            if (isFile) {
                SysBackupLog backupLog = SysBackupLog.buildBasicLogEntity(Constant.SysBackupTypeEnum.DELETE.getValue());
                backupLog.setFilename(filename);
                try {
                    // 是文件 则删除
                    boolean del = FileUtil.del(sqlFilePath);
                    log.info(String.format("System backup file delete, filename :%s, result : %s", filename, del));
                } catch (IORuntimeException e) {
                    log.error(e, "[removeBackFileByFilename] [error] [filename: {}]", filename);
                    backupLog.setState(0);
                    backupLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, Constant.MYSQL_TEXT_MAXLENGTH));
                } finally {
                    try {
                        // save sys backup log
                        sysBackupLogService.save(backupLog);
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }
        } else {
            // 文件不存在
            throw new NZException(RCode.SYS_BACKUP_FILENAME_NOTEXIST);
        }
    }

    @Override
    public void downloadBackFileByFilename(HttpServletResponse response, String filename) throws IOException {
        String path = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);

        String sqlFilePath = Tool.StrUtil.concat(true, path, File.separator, filename);
        boolean exist = FileUtil.exist(sqlFilePath);
        if (exist) {
            exist = FileUtil.isFile(sqlFilePath);
            if (exist) {
                ResponseUtil.downloadFile(response, FileUtil.file(sqlFilePath));
            }
        }
        if (!exist) {
            // 文件不存在
            throw new NZException(RCode.SYS_BACKUP_FILENAME_NOTEXIST);
        }
    }

    /**
     * unpack backup file
     *
     * @param file
     * @param response
     */
    @Override
    public void unpackBackupFile(MultipartFile file, HttpServletResponse response) {
        log.info("[unpackBackupFile] [begin]");
        File tempSecureFile = null, readSqlFile = null;
        try {
            String tempFilePath = Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, IdUtil.fastSimpleUUID(), ".bak");
            tempSecureFile = FileUtil.file(tempFilePath);

            FileUtils.copyInputStreamToFile(file.getInputStream(), tempSecureFile);

            // 返回文件名称
            String filename = file.getOriginalFilename();
            String unpackFileName = FilenameUtils.getBaseName(filename);

            // secure reader backup file
            readSqlFile = this.secureReaderBackupFile(tempSecureFile, unpackFileName);
            ResponseUtil.downloadFile(response, readSqlFile);
            log.info("[unpackBackupFile] [finshed]");
        } catch (Exception e) {
            log.error(e, "[unpackBackupFile] [error] [file: {}]", file.getOriginalFilename());
            throw new NZException(RCode.SYS_BACKUP_UNPACK_ERROR);
        } finally {
            FileUtil.del(tempSecureFile);
            FileUtil.del(readSqlFile);
        }
    }

    /**
     * sync backup file
     *
     * @param serverid
     * @param type
     * @param filename
     * @param response
     */
    @Override
    public void syncBackupFile(String serverid, String type, String filename, HttpServletResponse response) {
        SysComponent sysComponent = sysComponentService.getOne(new LambdaUpdateWrapper<SysComponent>().eq(SysComponent::getServerid, serverid));
        log.info("[syncBackupFile] [get sysComponent] [serverid: {}] [component exist: {}]", serverid, ObjectUtil.isNotNull(sysComponent));
        if (ObjectUtil.isNull(sysComponent)) {
            log.warn("[syncBackupFile] [get sysComponent error] [serverid: {}]", serverid);
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "System component not found.");
            } catch (IOException e) {
                log.error(e);
            }
            return;
        }

        // query back file list
        if (StrUtil.equals("list", type)) {
            try {
                String path = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);
                List<File> loopFiles = FileUtil.loopFiles(path);
                List<File> sortFile = loopFiles.stream().sorted(Comparator.comparing(File::lastModified).reversed()).collect(Collectors.toList());

                List<Map<Object, Object>> resultList = Tool.ListUtil.list(true);
                for (File file : sortFile) {
                    Map<Object, Object> map = Tool.MapUtil.builder().put("fileName", file.getName()).put("size", file.length()).put("md5", DigestUtils.md5Hex(FileUtil.getInputStream(file))).build();
                    resultList.add(map);
                }

                response.getWriter().write(JSON.toJSONString(R.ok().putData("list", resultList)));
            } catch (IOException e) {
                log.error(e, "[syncBackupFile] [error] [type: {}]", type);
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error");
                } catch (IOException ex) {
                    log.error(ex);
                }
            }
        } else if (StrUtil.equals("download", type)) {
            try {
                String path = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);
                String sqlFilePath = Tool.StrUtil.concat(true, path, File.separator, filename);
                boolean exist = FileUtil.exist(sqlFilePath);
                log.info("[syncBackupFile] [download file] [filename: {}] [exist: {}]", filename, exist);
                if (exist) {
                    ResponseUtil.downloadFile(response, FileUtil.file(sqlFilePath));
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "File Not Found");
                }
            } catch (IOException e) {
                log.error(e, "[syncBackupFile] [error] [type: {}] [filename: {}]", type, filename);
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error");
                } catch (IOException ex) {
                    log.error(ex);
                }
            }
        }
    }

    @Override
    public synchronized void restore(String filename, String pin) {
        Long userId = ShiroUtils.getUserId();
        SysUserEntity user = sysUserService.getById(userId);

        // validate pin
        String tempPinForValidate = ShiroUtils.sha256(pin, user.getSalt());
        if (ObjectUtil.notEqual(user.getPin(), tempPinForValidate)) {
            throw new NZException(RCode.SYS_LOGIN_ACCOUNTAUTH);
        }

        String path = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);
        String sqlFilePath = Tool.StrUtil.concat(true, path, File.separator, filename);

        boolean exist = FileUtil.exist(sqlFilePath);
        log.info("[restore] [download file] [filename: {}] [exist: {}]", filename, exist);
        if (exist) {
            // sys_restore_filename
            redisTemplate.opsForValue().set("sys_restore_filename", Tool.AesUtil.encrypt(filename, Constant.AES_SECRET_KEY), 300, TimeUnit.SECONDS);

            // send restart notify
            redisTemplate.convertAndSend(Constant.RESTORE_TOPIC_NAME, Constant.SERVER_ID);
        } else {
            log.warn("[restore] [file not found] [filename: {}]", filename);
            throw new NZException(RCode.SYS_BACKUP_FILENAME_NOTEXIST);
        }
    }

    @Override
    public void restore(String filename) {
        String path = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);
        String sqlFilePath = Tool.StrUtil.concat(true, path, File.separator, filename);
        boolean exist = FileUtil.exist(sqlFilePath);
        log.info("[restore] [restore file] [filename: {}] [exist: {}]", filename, exist);
        if (BooleanUtil.negate(exist)) {
            log.warn("[restore] [file not found] [filename: {}]", filename);
            return;
        }

        // secure reader backup file
        File readSqlFile = null;
        try {
            String unpackFileName = StrUtil.concat(true, IdUtil.fastSimpleUUID());
            readSqlFile = this.secureReaderBackupFile(FileUtil.file(sqlFilePath), unpackFileName);
        } catch (Exception e) {
            log.error(e, "[restore] [secure reader backup file error] [filename: {}]", filename);
            throw new NZException(RCode.SYS_BACKUP_UNPACK_ERROR);
        }

        // perform restore action
        try {
            this.performRestoreAction(readSqlFile);
        } catch (Exception e) {
            log.error(e, "[restore] [error] [filename: {}]", filename);
            throw new NZException(RCode.SYS_BACKUP_RESTORE_ERROR);
        }
    }

    /**
     * export db to sql file
     *
     * @param tempFilePerfix
     * @throws IOException
     * @throws SQLException
     */
    private void exportDbToSQLFile(String tempFilePerfix) throws IOException, SQLException {
        log.info("[exportDbToSQLFile] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();

        Connection conn = null;
        Statement stmtInfo = null, stmtData = null;
        ResultSet rsInfo = null, rsData = null;
        try {
            conn = dataSource.getConnection();
            stmtInfo = conn.createStatement();
            String dbName = conn.getCatalog();

            // 获取所有表名称
            List<String> tableNames = new ArrayList<>();
            rsInfo = stmtInfo.executeQuery(String.format("SHOW FULL TABLES FROM `%s` WHERE TABLE_TYPE = 'BASE TABLE'", dbName));
            // 遍历所有表
            while (rsInfo.next()) {
                String tableName = rsInfo.getString(1);
                tableNames.add(tableName);
            }    //end for tables

            Tool.IoUtil.close(rsInfo);
            Tool.IoUtil.close(stmtInfo);

            if (ToolUtil.isNotEmpty(tableNames)) {
                //将不需要备份表过滤
                String backupExcludeTables = sysConfService.getValue("backup_exclude_tables");
                if (StrUtil.isNotEmpty(backupExcludeTables)) {
                    List<String> excludeTableNames = StrUtil.split(backupExcludeTables, ",");
                    tableNames = tableNames.stream().filter(tableName -> !excludeTableNames.contains(tableName)).collect(Collectors.toList());
                }

                String tempSqlFilePath = Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, tempFilePerfix, ".sql");
                BufferedWriter writer = FileUtil.getWriter(tempSqlFilePath, "UTF-8", false);

                // 头内容
                writer.write("SET NAMES utf8mb4;");
                writer.newLine();
                writer.write("SET FOREIGN_KEY_CHECKS = 0;");
                writer.newLine();
                writer.flush();

                // 导出表数据
                for (String tableName : tableNames) {
                    this.dbBackExportTable(conn, tableName, writer, false);
                }

                // 导出存储过程信息
//                stmtInfo = conn.createStatement();
//                rsInfo = stmtInfo.executeQuery(String.format("SELECT `SPECIFIC_NAME` from `INFORMATION_SCHEMA`.`ROUTINES` WHERE `ROUTINE_SCHEMA` = '%s' AND ROUTINE_TYPE = 'PROCEDURE'; ", dbName));
//                while (rsInfo.next()) {
//                    String procName = rsInfo.getString(1);
//
//                    // 存储过程结构
//                    stmtData = conn.createStatement();
//                    rsData = stmtData.executeQuery(String.format("SHOW CREATE PROCEDURE `%s`", procName));
//                    if (!rsData.next()) {
//                        continue;
//                    }
//                    writer.newLine();
//                    writer.newLine();
//                    writer.write(String.format("/* Procedure structure for procedure `%s` */", procName));
//                    writer.newLine();
//                    writer.write(String.format("/*!50003 DROP PROCEDURE IF EXISTS  `%s` */;", procName));
//                    writer.newLine();
//                    writer.write("DELIMITER $$");
//                    writer.newLine();
//                    writer.append("/*!50003 ").append(rsData.getString(3)).append(" */$$");
//                    writer.newLine();
//                    writer.write("DELIMITER ;");
//
//                    Tool.IoUtil.close(rsData);
//                    Tool.IoUtil.close(stmtData);
//                }

                // 尾内容
                writer.newLine();
                writer.newLine();
                writer.write("SET FOREIGN_KEY_CHECKS = 1;");
                writer.flush();
                writer.close();
            }
        } finally {
            Tool.IoUtil.close(stmtData);
            Tool.IoUtil.close(rsData);
            Tool.IoUtil.close(stmtInfo);
            Tool.IoUtil.close(rsInfo);
            Tool.IoUtil.close(conn);
            sw.stop();
        }
        log.info("[exportDbToSQLFile] [finshed] [Run time: {}]", sw.toString());
    }

    /**
     * 导出表数据
     *
     * @param conn
     * @param tableName
     * @param writer
     * @param bulkFlag  是否将数据放在一起
     * @throws SQLException
     * @throws IOException
     */
    private void dbBackExportTable(Connection conn, String tableName, BufferedWriter writer, boolean bulkFlag) throws SQLException, IOException {
        Statement stmt = null;
        ResultSet rs = null;
        /* 表结构 */
        stmt = conn.createStatement();
        rs = stmt.executeQuery(String.format("SHOW CREATE TABLE `%s`", tableName));
        if (!rs.next()) {
            return;
        }
        writer.newLine();
        writer.newLine();
        writer.write(String.format("DROP TABLE IF EXISTS `%s`;", tableName));
        writer.newLine();
        String createTableSql = rs.getString(2);
        createTableSql = StrUtil.replace(createTableSql, "ROW_FORMAT=COMPACT", "ROW_FORMAT=DYNAMIC");
        writer.write(createTableSql + ";");
        writer.newLine();
        Tool.IoUtil.close(rs);
        Tool.IoUtil.close(stmt);

        /* 导出表数据 */
        // 先获取记录数
        stmt = conn.createStatement();
        rs = stmt.executeQuery(String.format("SELECT COUNT(1) FROM `%s`", tableName));
        int rowCount = rs.next() ? rs.getInt(1) : 0;
        if (0 >= rowCount) {
            writer.flush();
            return;
        }

        stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        /**
         * If the fetch size specified is zero, the JDBC driver ignores the value and is free to make its own best guess as to what the fetch size should be.
         * The default value is set by the Statement object that created the result set.
         * The fetch size may be changed at any time.
         * @see #setFetchSize
         * @link https://docs.oracle.com/javase/8/docs/api/java/sql/ResultSet.html#setFetchSize-int-
         */
        // stmt.setFetchSize(Integer.MIN_VALUE);
        stmt.setFetchSize(0);
        stmt.setFetchDirection(ResultSet.FETCH_REVERSE);

        int offset = 0;
        while (offset < rowCount) {
            int startRow = offset;

            rs = stmt.executeQuery(String.format("SELECT * FROM `%s` limit %d , %d", tableName, startRow, exportMaxSize));
            int colCount = 0;
            Object colValue = null;
            // 所有数据用","连接
            if (!bulkFlag) {
                while (rs.next()) {
                    colCount = rs.getMetaData().getColumnCount();

                    writer.write(String.format("INSERT INTO `%s` VALUES (", tableName));
                    // 获取表每一列数据
                    for (int j = 0; j < colCount; j++) {
                        if (j > 0) {
                            writer.write(',');
                        }
                        colValue = rs.getObject(j + 1);
                        if (null != colValue) {
                            // MariaDbBlob 单独处理，解决 bolo 类型直接 toString 造成结果有误问题
                            if (colValue instanceof MariaDbBlob) {
                                InputStream binaryStream = ((MariaDbBlob) colValue).getBinaryStream();
                                // 使用 0x 标识十六进制字符串，参照  ResultSet API ，binaryStream 转 十六进制 大写 字符串
                                String blobHexStr = Tool.StrUtil.concat(true, "0x", Tool.HexUtil.encodeHexStr(IoUtil.readBytes(binaryStream), false));
                                writer.write(blobHexStr);
                            } else {
                                writer.write(com.baomidou.mybatisplus.core.toolkit.StringUtils.quotaMark(colValue.toString()));
                            }
                        } else {
                            writer.write("NULL");
                        }
                    }    //end for one record columns
                    writer.write(");");
                    writer.newLine();
                    writer.flush();
                }    //end for table records
            }
            // 每行数据独立分开
            else {
                ResultSetMetaData rsMetaData = null;
                int counter = 0;
                while (rs.next()) {
                    ++counter;
                    rsMetaData = rs.getMetaData();
                    colCount = rsMetaData.getColumnCount();

                    // 第一条记录，则列出列名
                    if (1 == counter) {
                        writer.write(String.format("INSERT INTO `%s` (", tableName));
                        for (int i = 0; i < colCount; i++) {
                            if (i > 0) {
                                writer.write(",");
                            }
                            writer.append('`').append(rsMetaData.getColumnName(i + 1)).append('`');
                        }
                        writer.append(") VALUES ");
                    }
                    // 获取表每一列数据
                    for (int j = 0; j < colCount; j++) {
                        writer.write((0 >= j) ? '(' : ',');
                        colValue = rs.getObject(j + 1);
                        if (null != colValue) {
                            writer.write(com.baomidou.mybatisplus.core.toolkit.StringUtils.quotaMark(colValue.toString()));
                        } else {
                            writer.write("NULL");
                        }
                    }    //end for one record columns
                    // 是否是最后记录
                    if (rowCount > counter) {
                        writer.write("),");
                    } else {
                        writer.write(");");
                    }
                    writer.flush();
                }    //end for table records
            }

            offset += exportMaxSize;
        }
        Tool.IoUtil.close(rs);
        Tool.IoUtil.close(stmt);
    }

    /**
     * secure writer backup file
     * sqlFile -> gzip -> aes -> append remark str -> NZ-date.bak
     *
     * @param tempFilePerfix
     * @param nzBackupFileName
     * @param remark
     * @throws IOException
     */
    private void secureWriterBackupFile(String tempFilePerfix, String nzBackupFileName, String remark) throws IOException {
        log.info("[secureWriterBackupFile] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();

        String tempSqlFilePath = Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, tempFilePerfix, ".sql");
        String tempGzipFilePath = Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, tempFilePerfix, ".gz");
        try {
            // gzip
            File tempGzipFile = FileUtil.file(tempGzipFilePath);
            log.info("[secureWriterBackupFile] [gzip file] [begin]");
            Gzip gzip = null;
            try (FileInputStream fis = new FileInputStream(tempSqlFilePath); FileOutputStream fos = new FileOutputStream(tempGzipFile);) {
                gzip = Gzip.of(fis, fos);
                gzip.gzip();
                log.info("[secureWriterBackupFile] [gzip file] [finshed]");
            } catch (IOException e) {
                log.error(e, "[secureWriterBackupFile] [gzip error]");
                throw e;
            } finally {
                IoUtil.close(gzip);
            }

            // aes encrypt
            String nzBackupFilePath = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);
            File nzBackupFile = FileUtil.file(nzBackupFilePath, nzBackupFileName);

            log.info("[secureWriterBackupFile] [aes encrypt file] [begin]");
            try (FileInputStream fis = new FileInputStream(tempGzipFile); FileOutputStream fos = new FileOutputStream(nzBackupFile);) {
                Tool.AesUtil.encrypt(fis, fos, Constant.AES_SECRET_KEY);
                log.info("[secureWriterBackupFile] [aes encrypt file] [finshed]");
            } catch (Exception e) {
                log.error(e, "[secureWriterBackupFile] [aes encrypt error]");
                throw e;
            }

            // append remark size: 2KB
            remark = StrUtil.emptyToDefault(remark, "");
            int desiredLength = 2 * 1024;
            int currentLength = remark.getBytes().length;
            log.info("[secureWriterBackupFile] [append remark] [begin] [remark length: {}]", currentLength);
            if (currentLength < desiredLength) {
                int spacesToAdd = desiredLength - currentLength;
                StringBuilder paddedString = new StringBuilder(remark);
                for (int i = 0; i < spacesToAdd; i++) {
                    paddedString.append(" ");
                }
                remark = paddedString.toString();
            } else if (currentLength > desiredLength) {
                remark = remark.substring(0, desiredLength);
            }

            try (RandomAccessFile raf = new RandomAccessFile(nzBackupFile, "rw");) {
                raf.seek(raf.length());
                raf.write(remark.getBytes(), 0, remark.getBytes().length);
                log.info("[secureWriterBackupFile] [append remark] [finshed]");
            } catch (IOException e) {
                log.error(e, "[secureWriterBackupFile] [append remark error]");
                throw e;
            }
        } finally {
            // del tmpl file
            FileUtil.del(tempSqlFilePath);
            FileUtil.del(tempGzipFilePath);
        }

        sw.stop();
        log.info("[secureWriterBackupFile] [finshed] [nzBackUpFile: {}] [Run time: {}]", nzBackupFileName, sw.toString());
    }

    /**
     * backup file rotate
     * retentionNum = sys_config.backup_config.retention
     */
    public void backupFileRotate() {
        log.info("[backupFileRotate] [begin]");
        String backupConfigJsonStr = sysConfService.getValue("backup_config");
        JSONObject config = JSON.parseObject(backupConfigJsonStr);
        Integer retention = (Integer) config.get("retention");

        log.info("[backupFileRotate] [retention {}]", retention);
        if (ToolUtil.isNotEmpty(retention) && !retention.equals(-1)) {
            String path = sysConfService.getValueOrDefault("backup_file_path", Constant.BACKUP_ROOT_PATH);
            boolean directory = FileUtil.isDirectory(path);
            if (directory) {
                List<File> loopFiles = FileUtil.loopFiles(path);
                log.info("[backupFileRotate] [current backup file list] [length: {}]", Tool.CollUtil.size(loopFiles));
                if (ToolUtil.isNotEmpty(loopFiles)) {
                    if (loopFiles.size() > retention) {
                        Integer delSize = loopFiles.size() - retention;
                        log.info("[backupFileRotate] [delete expired files] [size: {}]", delSize);
                        for (int i = 0; i < delSize; i++) {
                            File delFile = loopFiles.get(i);
                            String delFileName = delFile.getName();
                            SysBackupLog backupLog = SysBackupLog.buildBasicLogEntity(Constant.SysBackupTypeEnum.DELETE.getValue());
                            backupLog.setFilename(delFileName);
                            try {
                                FileUtil.del(delFile);
                            } catch (Exception e) {
                                log.error(e, "[backupFileRotate] [error] [filename: {}]", delFileName);
                                backupLog.setState(0);
                                backupLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, Constant.MYSQL_TEXT_MAXLENGTH));
                            } finally {
                                try {
                                    // save sys backup log
                                    sysBackupLogService.save(backupLog);
                                } catch (Exception e) {
                                    log.error(e);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            log.info("[backupFileRotate] [no rotate in config]");
        }

        log.info("[backupFileRotate] [finshed]");
    }

    /**
     * secure reader backup file
     *
     * @param secureFile
     * @param sqlFileName
     * @return
     * @throws IOException
     */
    private File secureReaderBackupFile(File secureFile, String sqlFileName) throws IOException {
        log.info("[secureReaderBackupFile] [begin] [sqlFileName: {}]", sqlFileName);
        StopWatch sw = new StopWatch();
        sw.start();

        String tempAesFilePath = Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, IdUtil.fastSimpleUUID(), ".bak");
        String tempGzipFilePath = Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, IdUtil.fastSimpleUUID(), ".gz");
        String tempSqlFilePath = Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, sqlFileName, ".sql");
        try {
            // remove append remark size = 2k
            log.info("[secureReaderBackupFile] [remove append remark] [begin]");
            try (RandomAccessFile raf = new RandomAccessFile(secureFile, "r"); FileOutputStream fos = new FileOutputStream(tempAesFilePath)) {
                int bytesToRemove = 2 * 1024;
                long secureFileSize = raf.length();
                long aesFileSize = secureFileSize - bytesToRemove;
                aesFileSize = aesFileSize < 0 ? 0 : aesFileSize;

                byte[] buffer = new byte[1024];
                int bytesRead;
                long position = 0;
                while ((bytesRead = raf.read(buffer)) != -1) {
                    if (position + bytesRead > aesFileSize) {
                        int remainingBytes = (int) (aesFileSize - position);
                        fos.write(buffer, 0, remainingBytes);
                        break;
                    } else {
                        fos.write(buffer, 0, bytesRead);
                    }
                    position += bytesRead;
                }
                log.info("[secureReaderBackupFile] [remove append remark] [finshed]");
            } catch (IOException e) {
                log.error(e, "[secureReaderBackupFile] [remove append remark] [error]");
                throw e;
            }

            // aes decrypt
            log.info("[secureReaderBackupFile] [aes decrypt file] [begin]");
            try (FileInputStream fis = new FileInputStream(tempAesFilePath); FileOutputStream fos = new FileOutputStream(tempGzipFilePath);) {
                Tool.AesUtil.decrypt(fis, fos, Constant.AES_SECRET_KEY);
                log.info("[secureReaderBackupFile] [aes decrypt file] [finshed]");
            } catch (Exception e) {
                log.error(e, "[secureReaderBackupFile] [aes decrypt file] [error]");
                throw e;
            }

            // unzip
            Gzip gzip = null;
            log.info("[secureReaderBackupFile] [ungzip file] [begin]");
            try (FileInputStream fis = new FileInputStream(tempGzipFilePath); FileOutputStream fos = new FileOutputStream(tempSqlFilePath);) {
                gzip = Gzip.of(fis, fos);
                gzip.unGzip();
                log.info("[secureReaderBackupFile] [ungzip file] [finshed]");
            } catch (Exception e) {
                log.error(e, "[secureReaderBackupFile] [ungzip error]");
                FileUtil.del(tempSqlFilePath);
                throw e;
            } finally {
                IoUtil.close(gzip);
            }
        } finally {
            FileUtil.del(tempGzipFilePath);
            FileUtil.del(tempAesFilePath);
        }

        sw.stop();
        log.info("[secureReaderBackupFile] [finshed] [tempSqlPath: {}] [Run time: {}]", tempSqlFilePath, sw.toString());
        return FileUtil.file(tempSqlFilePath);
    }

    /**
     * perform restore action
     *
     * @param readSqlFile
     * @throws SQLException
     * @throws IOException
     */
    public void performRestoreAction(File readSqlFile) throws SQLException, IOException {
        log.info("[performRestoreAction] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();

        try (Connection connection = dataSource.getConnection();
             BufferedReader reader = FileUtil.getUtf8Reader(readSqlFile);) {

            String line;
            boolean inCreateTable = false;
            StrBuilder strBuilder = StrUtil.strBuilder();
            List<String> insertBatchSqlList = Tool.ListUtil.list(true);

            while ((line = reader.readLine()) != null) {
                line = StrUtil.trim(line);

                if (StrUtil.isEmpty(line)) {
                    continue;
                }

                if (!inCreateTable && StrUtil.endWith(line, ";")) {
                    String execSql = line;
                    // insert 语句批量插入
                    if (StrUtil.startWith(execSql, "INSERT")) {
                        insertBatchSqlList.add(execSql);
                        if (insertBatchSqlList.size() % 1000 == 0) {
                            SqlExecutor.executeBatch(connection, insertBatchSqlList);
                            insertBatchSqlList.clear();
                        }
                    } else {
                        // 其他语句立即执行
                        try {
                            SqlExecutor.execute(connection, execSql);
                        } catch (SQLException e) {
                            log.error(e, "[performRestoreAction] [execute sql error] [sql: {}]", execSql);
                            throw e;
                        }
                    }
                    continue;
                }

                // create table 拼接后执行
                strBuilder.append(line);
                if (StrUtil.startWith(line, "CREATE TABLE")) {
                    inCreateTable = true;
                } else if (inCreateTable && StrUtil.endWith(line, ";")) {
                    String execSql = strBuilder.toString();
                    try {
                        SqlExecutor.execute(connection, execSql);
                    } catch (SQLException e) {
                        log.error(e, "[performRestoreAction] [execute sql error] [sql: {}]", execSql);
                        throw e;
                    }
                    strBuilder.clear();
                    inCreateTable = false;
                }
            }

            // 插入剩余的 insert 语句
            if (Tool.CollUtil.isNotEmpty(insertBatchSqlList)) {
                SqlExecutor.executeBatch(connection, insertBatchSqlList);
            }
        } finally {
            FileUtil.del(readSqlFile);
        }

        sw.stop();
        log.info("[performRestoreAction] [finshed] [Run time: {}]", sw.toString());
    }

}
