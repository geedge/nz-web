package com.nis.setup.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.setup.entity.DatabaseEntity;
import com.nis.setup.entity.RedisEntity;
import com.nis.setup.entity.SetupEntity;
import com.nis.setup.util.Utils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.Yaml;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/setup")
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
                .and(setup.getDatabase().getPassword()).notNull(RCode.SYS_CONFIG_DB_PASSWORD_ISNULL)
                .and(setup.getRedis().getHost()).notNull(RCode.SYS_CONFIG_REDIS_HOST_ISNULL);

        R r = doCheckCode(setup.getCode());
        if(r.get("code") != RCode.SUCCESS.getCode()){
            return R.error(RCode.SYS_CONFIG_CODE_INVALID);
        }
        try{
            Resource applicationResource = Utils.getResource(null);
            modifyApplication(applicationResource, setup.getRedis());

            Resource applicationProfileResource = Utils.getResource(profile);
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
                .and(setup.getDatabase().getPassword()).notNull(RCode.SYS_CONFIG_DB_PASSWORD_ISNULL);
        Connection connection=null;
        String driver = environment.getProperty("spring.datasource.druid.driver-class-name");
        try{

            Class.forName(driver);
            connection = DriverManager.getConnection(setup.getDatabase().getUrl(),setup.getDatabase().getUsername(),setup.getDatabase().getPassword());
            if(connection.isValid(5)){
                return R.ok();
            }
            return R.error(RCode.SYS_CONFIG_DB_PARAM_INVALID);
        }catch (Exception e){
            log.error(e);
            return R.error(RCode.SYS_CONFIG_DB_PARAM_INVALID);
        }finally {
            IoUtil.close(connection);
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
            if(!StrUtil.isEmpty(setup.getRedis().getPassword())){
                jedis.auth(setup.getRedis().getPassword());
            }

            if("pong".equalsIgnoreCase(jedis.ping())){
                return R.ok();
            }
            return R.error(RCode.SYS_CONFIG_REDIS_PARAM_INVALID);
        }catch (Exception e){
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
        try{
            FileReader reader  = new FileReader(codeFile);
            BufferedReader br = new BufferedReader(reader);
            String readCode = br.readLine();
            if(StrUtil.isEmpty(readCode) || !readCode.equals(code)){
                return R.error(RCode.SYS_CONFIG_CODE_INVALID);
            }
            return R.ok();
        }catch (IOException e){
            return R.error(RCode.SYS_CONFIG_CODE_INVALID);
        }
    }

    private void modifyApplication(Resource resource,RedisEntity redisEntity) throws IOException{
        FileWriter writer=null;
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
            redis.put("password",redisEntity.getPassword());
            spring.put("redis",redis);

            application.put("spring",spring);

            Map<String,Object> nezha =(Map<String,Object>) application.get("nezha");
            nezha.put("inited",1);

            writer = new FileWriter(resource.getFile());
            writer.write(yaml.dumpAsMap(application));
            writer.flush();
            writer.close();
        }catch (IOException e){
            log.error(e);
            throw e;
        }finally {
            IoUtil.close(writer);
        }
    }

    private void modifyApplicationProfile(Resource resource, DatabaseEntity database) throws IOException {
        FileWriter writer=null;
        try{
            Yaml yaml = new Yaml();
            Map<String,Object> profile = yaml.load(resource.getInputStream());
            Map<String,Object> spring =(Map<String,Object>) profile.get("spring");

            Map<String,Object> datasource = (Map<String,Object>) spring.get("datasource");
            Map<String,Object> druid = (Map<String,Object>) datasource.get("druid");

            druid.put("url",database.getUrl());
            druid.put("username",database.getUsername());
            druid.put("password",database.getPassword());

            datasource.put("druid",druid);
            profile.put("datasource",datasource);

            writer = new FileWriter(resource.getFile());
            writer.write(yaml.dumpAsMap(profile));
            writer.flush();

        }catch (IOException e){
            log.error(e);
            throw e;
        }finally {
            IoUtil.close(writer);
        }
    }
    public void writeTmpFile(SetupEntity setup) throws IOException{
        File file = FileUtil.file(this.tmpDir,"setup.obj");
        if (FileUtil.exist(file)){
            file.delete();
        }
        ObjectOutputStream oos=null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(setup);
        } catch (IOException e) {
            log.error(e);
            throw e ;
        }finally {
            IoUtil.close(oos);
        }
    }

    public void restart(){
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("linux") != -1){
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
