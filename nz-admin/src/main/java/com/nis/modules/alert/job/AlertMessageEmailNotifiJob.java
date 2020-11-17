package com.nis.modules.alert.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.MailUtil;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@DisallowConcurrentExecution
public class AlertMessageEmailNotifiJob extends QuartzJobBean {
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    AlertRuleService alertRuleService;
    @Autowired
    MailUtil mailUtil;
    @Autowired
    SysConfigService configService;
    @Autowired
    private SysUserService sysUserService;

    private Template template;
    
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    	StopWatch sw = new StopWatch();
    	sw.start();
        try {
        	// 加载模板文件
        	template = this.getTemplate();
            this.execAlertMessage();
        }catch (Exception e){
            log.error("邮件发送任务异常", e);
        }finally {
			sw.stop();
		}
        log.info("alert message job  Run Time: {}" ,sw.toString());
    }
    private void execAlertMessage() throws IOException, TemplateException, MessagingException {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        while(true) {
        	Object obj = ops.rightPop(Constant.REDIS_KEY_EMAIL_ALERT_MESSAGE);
            if (ToolUtil.isEmpty(obj)) {
                return;
            }
            // 告警消息集合
            List<AlertMessageEntity> entities = null;
            try {
                entities = JSONObject.parseArray(obj.toString(), AlertMessageEntity.class);
            } catch (Exception e) {
                log.error("从redis中取告警消息转化失败，错误信息：" + e.getMessage() + " 转化参数是：" + obj.toString(), e);
            }
            log.info("取出告警消息，准备邮件通知：" + JSONObject.toJSONString(entities));
            if(CollectionUtils.isEmpty(entities)){
                return;
            }
            // 规则及通知人信息
            List<AlertRuleEntity> ruleList = alertRuleService.list();
            Map<Integer, String> ruleIdAndReceiver = ruleList.stream().filter(entity -> ToolUtil.isNotEmpty(entity.getReceiver())).collect(Collectors.toMap(AlertRuleEntity::getId, AlertRuleEntity::getReceiver));

            // 用户及邮箱信息
            List<SysUserEntity> userList = sysUserService.list();
            Map<Long, String> userIdAndEmail = userList.stream().filter(sysUserEntity -> ToolUtil.isNotEmpty(sysUserEntity.getEmail())).collect(Collectors.toMap(SysUserEntity::getUserId, SysUserEntity::getEmail));

            for (AlertMessageEntity messageEntity : entities) {
                String receiver = ruleIdAndReceiver.get(messageEntity.getRuleId());
                // 通知人不为空 发件
                if(StringUtils.isNotEmpty(receiver)){
                    log.info("邮件关联用户，进行邮件通知");
                    List<String> toSendEmails = new ArrayList<>();
                    Arrays.asList(receiver.split(",")).stream().forEach(receiverId -> {
                        String email = userIdAndEmail.get(Long.valueOf(receiverId));
                        if(StringUtils.isNotEmpty(email)){
                            toSendEmails.add(email);
                        }
                    });
                    log.info("需要发送的邮箱 -> {}",JSON.toJSONString(toSendEmails));
                    String state = "UNKNOWN";
                    switch (messageEntity.getState()) {
    				case 1:state = "Pending";break;
    				case 2:state = "Expired";break;
                    }

                    if(ToolUtil.isNotEmpty(toSendEmails)){
                        mailUtil.sendHTML(toSendEmails, state +" : " +messageEntity.getSummary(), this.sendMail(messageEntity));
                    }
                }else{
                    log.info("邮件没有关联用户，不进行邮件通知");
                }
            }
        }
    }

    public String sendMail(AlertMessageEntity alertMessage) throws IOException, TemplateException {
        //创建模型数据
        Map<String,Object> modelMap = new HashMap<String,Object>();
        modelMap.put("alertMessage",alertMessage);
        //执行静态化 方式1  获取静态化内容
        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, modelMap);
        //静态化内容;
        return content;
    }
    
	private Template getTemplate() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
		String templateString = configService.getValue("email_template");
		// 创建配置类
        Configuration configuration = new Configuration(Configuration.getVersion());
        //创建模板加载器
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        // 存入模板
        templateLoader.putTemplate("template", templateString);  //template  = 虚拟名称, 用来当作获取静态文件的key
        //加载模板加载器
        configuration.setTemplateLoader(templateLoader);
        //得到模板
        Template template = configuration.getTemplate("template", "utf-8");
		return template;
	}
}
