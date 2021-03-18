package com.nis.setup.util;

import cn.hutool.core.util.StrUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;

public class Utils {
    public static Resource getResource(String profile){
        if("prod".equals(profile)){
            return new FileSystemResource(System.getProperty("user.dir")+ File.separator+"config"+ File.separator+"application"+ (StrUtil.isEmpty(profile)?"":"-"+profile )+".yml");
        }
        return new ClassPathResource("application"+ (StrUtil.isEmpty(profile)?"":"-"+profile )+".yml");
    }
}
