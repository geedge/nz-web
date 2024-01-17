package com.nis.setup.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.DbUtil;
import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.log.Log;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.setup.entity.DatabaseEntity;
import com.nis.setup.entity.SetupEntity;
import com.nis.setup.entity.SystemEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setup")
public class SetupController  {
    private Log log = Log.get();
    
	@Value("${nezha.inited:0}")
    private int inited;
    @Value("${server.tomcat.basedir}")
    private String tmpDir;
    @Value("${nezha.config.path:./config/nezha.properties}")
    private String nezhaConfigPath;
    @Value("${spring.datasource.druid.driver-class-name}")
    private String driver;
    @Value("${nezha.restart.command:systemctl restart nz-web.service}")
    private String restartCommand;
    @Value("${database.loginTimeout:10}")
    private Integer dbLoginTimeouot;//数据库登录超时时间
    /**
     * 获取 nezha inited 状态
     * @return
     */
    @GetMapping("inited")
    public R state() {
    	return R.ok().put("inited", inited);
    }
    
    /**
     * 最后完成配置
     * @param setup
     * @return
     */
    @PostMapping("/config")
    public R setup(@RequestBody SetupEntity setup){
        R r = doCheckCode(setup.getCode());
        if(r.get("code") != RCode.SUCCESS.getCode()){
            return R.error(RCode.SYS_CONFIG_CODE_INVALID);
        }
        if(inited == 2) {//已经配置过，不需要再配置
        	return R.error(RCode.SYS_CONFIG_HAD_CONFIG);
        }
        // db
        DatabaseEntity database = setup.getDatabase();
        ValidateUtils.is(database.getUrl()).notNull(RCode.SYS_CONFIG_DB_URL_ISNULL)
                .and(database.getUsername()).notNull(RCode.SYS_CONFIG_DB_USERNAME_ISNULL)
                .and(database.getPin()).notNull(RCode.SYS_CONFIG_DB_PASSWORD_ISNULL);
        List<String> dbUrlList = Arrays.asList(database.getUrl());
        if (dbUrlList.size() > 2) {
            throw new NZException(RCode.SYS_CONFIG_DB_URL_EXCEEDED_LIMIT);
        }

        // adminUser
        SysUserEntity adminUser = setup.getAdminUser();
        ValidateUtils.is(adminUser.getUsername()).notNull(RCode.SYS_USER_NAME_ISNULL)
                .and(adminUser.getPin()).notNull(RCode.SYS_USER_PASSWORD_ISNULL);

        // system
        SystemEntity system = setup.getSystem();
        ValidateUtils.is(system.getTimezone()).notNull(RCode.SYS_CONFIG_TIMEZONE_ISNULL);
        if (StrUtil.equals("0", system.getFederationEnabled())) {
            system.setAsset_ping_from("1");
        }
        try{
        	//配置文件
        	boolean absolutePath = Tool.FileUtil.isAbsolutePath(nezhaConfigPath);
    		File configFile = absolutePath ? Tool.FileUtil.file(nezhaConfigPath) : Tool.FileUtil.file(Tool.WebPathUtil.getRootPath(),nezhaConfigPath);
    		log.info("nezhaConfigPath:{}",configFile.getAbsolutePath());
    		//更改 nezha.inited 为 2，下次启动时不再执行
    		//nezha.properties 配置文件路径：./config/nezha.properties
    		if(!configFile.exists()) {
    			//创建父目录&配置文件
    			Tool.FileUtil.mkParentDirs(configFile.getAbsolutePath());
                boolean newFile = configFile.createNewFile();
                log.info("set up config new file {}", newFile);
            }
    		/*
    		 * 数据库密码加密
    		 */
    		// AES aes = Tool.SecureUtil.aes(Constant.AES_SECRET_KEY);
    		// String dbPinHex = aes.encryptHex(setup.getDatabase().getPin());
            String dbPinHex = Tool.AesUtil.encrypt(setup.getDatabase().getPin(), Constant.AES_SECRET_KEY);

            LinkedHashMap<String, String> propsMap = new LinkedHashMap<>();
            propsMap.put("nezha.inited", "1");
            if (Tool.StrUtil.isBlank(propsMap.get("server.port"))) {
                propsMap.put("server.port","80");
            }
            propsMap.put("database.url", dbUrlList.stream().collect(Collectors.joining(",")));
            propsMap.put("database.name", setup.getDatabase().getName());
            propsMap.put("database.user", setup.getDatabase().getUsername());
            propsMap.put("database.pin", dbPinHex);

    		// 保存配置文件
            FileUtil.writeUtf8Map(propsMap, configFile, "=", false);

        	//其它配置保存到临时文件
            File tmpConfigFile = Tool.FileUtil.file(tmpDir, Constant.SETUP_FILE_NAME);
            if(!tmpConfigFile.exists()) {
    			//创建父目录&配置文件
    			Tool.FileUtil.mkParentDirs(tmpConfigFile.getAbsolutePath());
                boolean newFile = tmpConfigFile.createNewFile();
                log.info("set up config new file {}", newFile);
            }
            Tool.FileUtil.writeUtf8String(Tool.JSONUtil.toJsonStr(setup), tmpConfigFile);
            //重启nzweb
            this.restart();
            return R.ok();
        } catch (IOException e) {
            log.error(e);
            return R.error();
        } catch (IORuntimeException e) {
            log.error(e);
            return R.error();
        }
    }
    @GetMapping("/checkCode")
    public R checkCode(String code){
        R r = doCheckCode(code);
        return r;
    }

    @PostMapping("/checkDb")
    public R checkDBConnection(@RequestBody SetupEntity setup) {
        R r = doCheckCode(setup.getCode());
        if (r.get("code") != RCode.SUCCESS.getCode()) {
            return R.error(RCode.SYS_CONFIG_CODE_INVALID);
        }
        ValidateUtils.is(setup.getDatabase().getUrl()).notNull(RCode.SYS_CONFIG_DB_URL_ISNULL)
                .and(setup.getDatabase().getUsername()).notNull(RCode.SYS_CONFIG_DB_USERNAME_ISNULL)
                .and(setup.getDatabase().getPin()).notNull(RCode.SYS_CONFIG_DB_PASSWORD_ISNULL)
                .and(setup.getDatabase().getName()).notNull(RCode.SYS_CONFIG_DB_NAME_ISNULL);
        String dbName = setup.getDatabase().getName();

        log.info("[checkDBConnection] [begin] [checkUrl: {}] [username: {}]", setup.getDatabase().getUrl(), setup.getDatabase().getUsername());

        List<String> dbUrlList = Arrays.asList(setup.getDatabase().getUrl());
        for (String url : dbUrlList) {
            try (SimpleDataSource dataSource = new SimpleDataSource(setup.getDatabase().getConnectionUrl(url), setup.getDatabase().getUsername(), setup.getDatabase().getPin(), driver);) {
                dataSource.setLoginTimeout(dbLoginTimeouot);
                Db db = DbUtil.use(dataSource);
                // 没有自动创建 数据库
                List<String> dbNames = db.query("show databases;", String.class);
                if (dbNames != null) {
                    boolean match = dbNames.stream().anyMatch(dbName::equalsIgnoreCase);
                    if (!match) {
                        int execute = db.execute("create database if not exists `" + dbName + "` default character set utf8mb4 COLLATE utf8mb4_general_ci");
                        log.info("database {} not exists,auto create database,result:{}", dbName, execute);
                    }
                }
                String result = db.queryString("select 1");
                log.info("[checkDBConnection] [url: {}] [result: {}]", url, result);

                if (Tool.StrUtil.isEmpty(result)) {
                    return R.error(RCode.SYS_CONFIG_DB_CONNECTION_FAILED.setParam(url));
                }
            } catch (SQLException | RuntimeException e) {
                log.error(e, "[checkDBConnection] [error] [db: {}]", url);
                return R.error(RCode.SYS_CONFIG_DB_CONNECTION_FAILED.setParam(url));
            }
        }
        log.info("[checkDBConnection] [finshed]");
        return R.ok();
    }

    private R doCheckCode(String code){
        File codeFile = FileUtil.file(this.tmpDir,"nezha.auth");
        log.info("nezha.auth path: {} ",codeFile.getAbsolutePath());
        if(!FileUtil.exist(codeFile)){
            return R.error(RCode.SYS_CONFIG_CODE_FILE_ISNULL);
        }

        String readCode = Tool.FileUtil.readUtf8String(codeFile);
        if(StrUtil.isEmpty(readCode) || !readCode.equals(code)){
            return R.error(RCode.SYS_CONFIG_CODE_INVALID);
        }
        return R.ok();
    }

    public void restart(){
        if (Tool.SystemUtil.getOsInfo().isLinux()){
            RuntimeUtil.exec(restartCommand);
        }
    }
}
