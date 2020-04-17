package com.nis.modules.alert.job;

import com.alibaba.fastjson.JSON;
import com.nis.common.utils.MailUtil;
import com.nis.common.utils.RedisUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertNameMapUserDTO;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.*;


@Slf4j
public class AlertMessageJob extends QuartzJobBean {
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    AlertRuleService alertRuleService;
    @Autowired
    MailUtil mailUtil;
    @Autowired
    SysConfigService configService;


    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        long start, end;
        start = System.currentTimeMillis();
        try {
            this.execAlertMessage();
        }catch (MessagingException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TemplateException e) {
            e.printStackTrace();
        }
        end = System.currentTimeMillis();

        log.info("alert message job start time:" + start + "; end time:" + end + "; Run Time:" + (end - start) + "(ms)");
    }
    private void execAlertMessage() throws IOException, TemplateException, MessagingException {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        Object obj = ops.rightPop("alert_message_list");
        if(ToolUtil.isEmpty(obj)){
            return;
        }
        List<AlertMessageEntity> entities  = (List<AlertMessageEntity>) obj;
        Set<Integer> ruleIds = new HashSet<>();
        for(AlertMessageEntity alertMessageEntity:entities){
            ruleIds.add(alertMessageEntity.getRuleId());
        }
        List<AlertNameMapUserDTO> alertNameMapEmail = alertRuleService.queryUserByRuleId(ruleIds);
	    		for (AlertMessageEntity entity : entities) {
	        		for (AlertNameMapUserDTO dto : alertNameMapEmail) {
	        			if (entity.getRuleId().intValue()==dto.getRuleId().intValue()) {
	        				List<String> toSendEmails = new ArrayList<>();
	        				List<SysUserEntity> users = dto.getUsers();
	        				for (SysUserEntity user : users) {
	        					if (!StringUtils.isBlank(user.getEmail())) {
	        						toSendEmails.add(user.getEmail());
	        					}
	        				}
                            log.info("需要发送的邮箱 -> {}",JSON.toJSONString(toSendEmails));
	        				if(ToolUtil.isNotEmpty(toSendEmails)){
                                mailUtil.sendHTML(toSendEmails, entity.getSummary(), this.sendMail(entity.getDescription()));
                            }

							break;
	        			}
	        		}
	        	}
    }


    public String sendMail(String description) throws IOException, TemplateException {
        // 创建配置类
        Configuration configuration = new Configuration(Configuration.getVersion());
        // 加载模板文件
        String templateString = configService.getValue("email_template");
        //创建模板加载器
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        // 存入模板
        templateLoader.putTemplate("template", templateString);  //template  = 虚拟名称, 用来当作获取静态文件的key
        //加载模板加载器
        configuration.setTemplateLoader(templateLoader);
        //得到模板
        Template template = configuration.getTemplate("template", "utf-8");
        //创建模型数据
        Map<String,Object> modelMap = new HashMap<String,Object>();
        modelMap.put("description",description);
        //执行静态化 方式1  获取静态化内容
        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, modelMap);
        //静态化内容;
        return content;
    }


}
