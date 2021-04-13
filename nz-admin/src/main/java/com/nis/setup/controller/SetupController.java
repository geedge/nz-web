package com.nis.setup.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.druid.pool.DruidDataSource;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.setup.entity.DatabaseEntity;
import com.nis.setup.entity.RedisEntity;
import com.nis.setup.entity.SetupEntity;
import com.nis.setup.util.Utils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.Yaml;
import redis.clients.jedis.Jedis;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/setup")
@ConditionalOnProperty(prefix = "nezha",name = "inited",havingValue = "0")
public class SetupController implements ApplicationContextAware, EnvironmentAware {
    private Log log = Log.get();


    @Value("${spring.profiles.active}")
    private String profile;

    @Value("${server.tomcat.basedir}")
    private String tmpDir;


    private Environment environment;

    private ConfigurableApplicationContext context;

    @PostMapping("/config")
    public R setup(@RequestBody SetupEntity setup){
        ValidateUtils.is(setup.getDatabase().getHost()).notNull(RCode.SYS_CONFIG_DB_URL_ISNULL)
                .and(setup.getDatabase().getUsername()).notNull(RCode.SYS_CONFIG_DB_USERNAME_ISNULL)
                .and(setup.getDatabase().getPin()).notNull(RCode.SYS_CONFIG_DB_PASSWORD_ISNULL)
                .and(setup.getRedis().getHost()).notNull(RCode.SYS_CONFIG_REDIS_HOST_ISNULL);

        R r = doCheckCode(setup.getCode());
        if(r.get("code") != RCode.SUCCESS.getCode()){
            return R.error(RCode.SYS_CONFIG_CODE_INVALID);
        }
        try{
            Resource applicationResource = Utils.getResource(profile,null);
            modifyApplication(applicationResource, setup.getRedis());

            Resource applicationProfileResource = Utils.getResource(profile,profile);
            modifyApplicationProfile(applicationProfileResource,setup.getDatabase());

            writeTmpFile(setup);

            restart();
            return R.ok();
        }catch (IOException e){
            return R.error();
        }
    }
    @GetMapping("/checkCode")
    public R checkCode(String code){
        R r = doCheckCode(code);
        return r;
    }

    @PostMapping("/checkDb")
    public R checkDBConnection(@RequestBody SetupEntity setup){
        R r = doCheckCode(setup.getCode());
        if(r.get("code") != RCode.SUCCESS.getCode()){
            return R.error(RCode.SYS_CONFIG_CODE_INVALID);
        }
        ValidateUtils.is(setup.getDatabase().getHost()).notNull(RCode.SYS_CONFIG_DB_PARAM_INVALID)
                .and(setup.getDatabase().getUsername()).notNull(RCode.SYS_CONFIG_DB_USERNAME_ISNULL)
                .and(setup.getDatabase().getPin()).notNull(RCode.SYS_CONFIG_DB_PASSWORD_ISNULL);
        Connection connection=null;
        DruidDataSource druidDataSource = null;
        String driver = environment.getProperty("spring.datasource.druid.driver-class-name");
        try{

            Class.forName(driver);
//            connection = DriverManager.getConnection(setup.getDatabase().getUrl(),setup.getDatabase().getUsername(),setup.getDatabase().getPassword());
            druidDataSource = new DruidDataSource();
            druidDataSource.setUrl(setup.getDatabase().getUrl());
            druidDataSource.setUsername(setup.getDatabase().getUsername());
            druidDataSource.setPassword(setup.getDatabase().getPin());
            druidDataSource.setValidationQuery("SELECT 1");
            druidDataSource.setTestOnBorrow(true);
            druidDataSource.setMaxActive(2);
            druidDataSource.setInitialSize(1);
            druidDataSource.init();
            connection = druidDataSource.getConnection(5000);
            if(connection != null){
                return R.ok();
            }
            return R.error(RCode.SYS_CONFIG_DB_PARAM_INVALID);
        }catch (SQLException | ReflectiveOperationException e){
            log.error(e);
            return R.error(RCode.SYS_CONFIG_DB_PARAM_INVALID);
        }finally {
            IoUtil.close(connection);
            IoUtil.close(druidDataSource);
        }
    }

    @PostMapping("/checkRedis")
    public R checkRedisConnection(@RequestBody SetupEntity setup){
        R r = doCheckCode(setup.getCode());
        if(r.get("code") != RCode.SUCCESS.getCode()){
            return R.error(RCode.SYS_CONFIG_CODE_INVALID);
        }
        ValidateUtils.is(setup.getRedis().getHost()).notNull(RCode.SYS_CONFIG_REDIS_HOST_ISNULL);

        Jedis jedis = null;
        try{
            jedis = new Jedis(setup.getRedis().getHost(),setup.getRedis().getPort(),5000);
            if(!StrUtil.isEmpty(setup.getRedis().getPin())){
                jedis.auth(setup.getRedis().getPin());
            }

            if("pong".equalsIgnoreCase(jedis.ping())){
                return R.ok();
            }
            return R.error(RCode.SYS_CONFIG_REDIS_PARAM_INVALID);
        }catch (RuntimeException e){
            log.error(e);
            if(e.getMessage().indexOf("NOAUTH") != -1){
                return R.error(RCode.SYS_CONFIG_REDIS_PASSWORD_REQUIRED);
            }else if(e.getMessage().indexOf("invalid password") != -1){
                return R.error(RCode.SYS_CONFIG_REDIS_PASSWORD_INVALID);
            }else{
                return R.error(RCode.SYS_CONFIG_REDIS_PARAM_INVALID);
            }
        }finally {
            IoUtil.close(jedis);
        }
    }
    private R doCheckCode(String code){
        File codeFile = FileUtil.file(this.tmpDir,"nezha.auth");
        if(!FileUtil.exist(codeFile)){
            return R.error(RCode.SYS_CONFIG_CODE_FILE_ISNULL);
        }

        String readCode = Tool.FileUtil.readUtf8String(codeFile);
        if(StrUtil.isEmpty(readCode) || !readCode.equals(code)){
            return R.error(RCode.SYS_CONFIG_CODE_INVALID);
        }
        return R.ok();
    }

    private void modifyApplication(Resource resource,RedisEntity redisEntity) throws IOException{

        try{
            Yaml yaml = new Yaml();
            Map<String,Object> application = yaml.load(resource.getInputStream());
            Map<String,Object> spring =(Map<String,Object>) application.get("spring");

            Map<String,Object> autoconfigure =(Map<String,Object>) spring.get("autoconfigure");
            autoconfigure.put("exclude",new Class[]{});
            spring.put("autoconfigure",autoconfigure);

            Map<String,Object> redis =(Map<String,Object>) spring.get("redis");
            redis.put("host",redisEntity.getHost());
            redis.put("port",redisEntity.getPort());
            redis.put("password",redisEntity.getPin());
            spring.put("redis",redis);

            application.put("spring",spring);

            Map<String,Object> nezha =(Map<String,Object>) application.get("nezha");
            nezha.put("inited",1);

            BufferedOutputStream outputStream = FileUtil.getOutputStream(resource.getURL().toString());
            IoUtil.write(outputStream,true,yaml.dumpAsMap(application).getBytes());
        }catch (IOException e){
            log.error(e);
            throw e;
        }
    }

    private void modifyApplicationProfile(Resource resource, DatabaseEntity database) throws IOException {
        try{
            Yaml yaml = new Yaml();
            Map<String,Object> profile = yaml.load(resource.getInputStream());
            Map<String,Object> spring =(Map<String,Object>) profile.get("spring");

            Map<String,Object> datasource = (Map<String,Object>) spring.get("datasource");
            Map<String,Object> druid = (Map<String,Object>) datasource.get("druid");

            druid.put("url",database.getUrl());
            druid.put("username",database.getUsername());
            druid.put("password",database.getPin());

            datasource.put("druid",druid);
            profile.put("datasource",datasource);

            BufferedOutputStream outputStream = FileUtil.getOutputStream(resource.getURL().toString());
            IoUtil.write(outputStream,true,yaml.dumpAsMap(profile).getBytes());

        }catch (IOException e){
            log.error(e);
            throw e;
        }
    }
    public void writeTmpFile(SetupEntity setup) throws IOException{
        File file = FileUtil.file(this.tmpDir,"setup.obj");
        if (FileUtil.exist(file)){
            boolean delete = file.delete();
            if(delete) {
            	log.debug("文件删除成功");
            }else {
            	log.debug("文件删除失败");
            }
        }
        IoUtil.writeObj(FileUtil.getOutputStream(file),true,setup);
    }

    public void restart(){
        if (Tool.SystemUtil.getOsInfo().isLinux()){
            RuntimeUtil.exec("systemctl restart nzweb.service");
        }
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context =(ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
