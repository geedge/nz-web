package com.nis.common.utils;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.IOException;

public class TemplateUtil {

    public static Template stringToTemplate(String templateStr,String templateKey) throws IOException {

        // 创建配置类
        Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        //创建模板加载器
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        // 存入模板
        templateLoader.putTemplate(templateKey, templateStr);  //template  = 虚拟名称, 用来当作获取静态文件的key
        //加载模板加载器
        configuration.setTemplateLoader(templateLoader);
        //得到模板
        Template template = configuration.getTemplate(templateKey, "utf-8");

        return  template;

    }

}
