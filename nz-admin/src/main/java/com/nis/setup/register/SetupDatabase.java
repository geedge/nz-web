package com.nis.setup.register;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.log.Log;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.setup.entity.SetupEntity;
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

    @Value("${nezha.resource.main:config/application.yml}")
    private String mainConfPath;

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
        sysUser.setId(Long.parseLong(adminId));
        sysUser.setUsername(setupEntity.getSystem().getUsername());
        sysUser.setPin(ShiroUtils.sha256(setupEntity.getSystem().getPin(), sysUser.getSalt()));

        sysUserService.updateById(sysUser);

        File file = CommonUtils.getResourceFile(mainConfPath);
        modifyApplication(file);
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

    private void modifyApplication(File file) throws IOException{
        Yaml yaml = new Yaml();
        Map<String,Object> application = yaml.load(Tool.FileUtil.getInputStream(file));

        Map<String,Object> nezha =(Map<String,Object>) application.get("nezha");
        nezha.put("inited",2);

        BufferedOutputStream outputStream = FileUtil.getOutputStream(file);
        IoUtil.write(outputStream,true,yaml.dumpAsMap(application).getBytes());
    }
}
