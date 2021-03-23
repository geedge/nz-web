package com.nis.setup.register;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.log.Log;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.setup.entity.SetupEntity;
import com.nis.setup.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "nezha",name = "inited",havingValue = "1")
public class SetupDatabase {
    private Log log = Log.get();
    @Value("${nezha.inited}")
    private String inited;

    @Value("${nezha.adminId:1}")
    private String adminId;

    @Value("${spring.profiles.active}")
    private String profile;

    @Value("${server.tomcat.basedir}")
    private String tmpDir;

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    SysUserService sysUserService;

    @PostConstruct
    public void initDatabase() throws IOException{
        if (!"1".equals(inited)) {
            return;
        }
        SetupEntity setupEntity = readObject();
        sysConfigService.updateValueByKey("alert_api",setupEntity.getSystem().getAlertPath());
        sysConfigService.updateValueByKey("alert_path_prefix",setupEntity.getSystem().getAlertPrefix());
        sysConfigService.updateValueByKey("ha_mode",setupEntity.getSystem().getHaMode()+"");
        sysConfigService.updateValueByKey("ha_vip",setupEntity.getSystem().getHaVip());

        SysUserEntity sysUser =  sysUserService.getById(adminId);
        sysUser.setUserId(Long.parseLong(adminId));
        sysUser.setUsername(setupEntity.getSystem().getUsername());
        sysUser.setPassword(ShiroUtils.sha256(setupEntity.getSystem().getPassword(), sysUser.getSalt()));

        sysUserService.updateById(sysUser);

        Resource applicationResource = Utils.getResource(profile,null);
        modifyApplication(applicationResource);
    }

    private SetupEntity readObject(){
        File file = FileUtil.file(tmpDir,"setup.obj");
        if (!FileUtil.exist(file))
            return null;
        try {
            SetupEntity setup = IoUtil.readObj(new FileInputStream(file));
            FileUtil.del(file);
            return setup;
        }catch (IOException e){
            log.error(e);
            return null;
        }
    }

    private void modifyApplication(Resource resource) throws IOException{
        try{
            Yaml yaml = new Yaml();
            Map<String,Object> application = yaml.load(resource.getInputStream());

            Map<String,Object> nezha =(Map<String,Object>) application.get("nezha");
            nezha.put("inited",2);

            BufferedOutputStream outputStream = FileUtil.getOutputStream(resource.getURL().toString());
            IoUtil.write(outputStream,true,yaml.dumpAsMap(application).getBytes());
        }catch (IOException e){
            log.error(e);
            throw e;
        }
    }
}
