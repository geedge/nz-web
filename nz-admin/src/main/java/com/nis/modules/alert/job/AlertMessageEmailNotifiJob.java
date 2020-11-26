package com.nis.modules.alert.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.MailUtil;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.dao.AlertNotifiScriptDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertNotificationScript;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.sys.dao.SysUserNotificationDao;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.SysUserNotification;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import freemarker.cache.StringTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@DisallowConcurrentExecution
public class AlertMessageEmailNotifiJob extends QuartzJobBean {

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    // 告警通知脚本超时时间单位 s秒
    @Value("${nezha.alertNotifiTimeOut:3}")
    private Integer alertNotifiTimeOut;

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

    @Autowired
    private SysUserNotificationDao userNotificationDao;

    @Autowired
    private AlertNotifiScriptDao alertNotifiScriptDao;

    private Template template;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 检查 haMode
        boolean haMode = configService.checkHaMode();
        if (!haMode) return;

    	StopWatch sw = new StopWatch();
    	sw.start();
        try {
        	// 加载模板文件
        	template = this.getTemplate();
            this.execAlertMessage();
        }catch (Exception e){
            log.error("告警通知异常", e);
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

            // 用户和关联脚本信息
            List<SysUserNotification> userNotificationList = userNotificationDao.selectList(null);
            Map<Integer, List<SysUserNotification>> userIdAndNotificationMap = userNotificationList.stream().collect(Collectors.groupingBy(SysUserNotification::getUserId));

            // 脚本信息
            List<AlertNotificationScript> scriptList = alertNotifiScriptDao.selectList(null);
            Map<Integer, String> scriptIdAndFilePathMap = scriptList.stream().collect(Collectors.toMap(AlertNotificationScript::getId, AlertNotificationScript::getFilePath));


            for (AlertMessageEntity messageEntity : entities) {
                String receiver = ruleIdAndReceiver.get(messageEntity.getRuleId());
                // 通知人不为空 发件
                if(StringUtils.isNotEmpty(receiver)){
                    log.info("邮件关联用户，进行邮件通知");
                    List<String> toSendEmails = new ArrayList<>();

                    // 用户关联脚本
                    List<SysUserNotification> userNotifications = new ArrayList<>();

                    Arrays.asList(receiver.split(",")).stream().forEach(receiverId -> {
                        String email = userIdAndEmail.get(Long.valueOf(receiverId));
                        if(StringUtils.isNotEmpty(email)){
                            toSendEmails.add(email);
                        }

                        // 通过关联用户
                        List<SysUserNotification> sysUserNotifications = userIdAndNotificationMap.get(Integer.valueOf(receiverId));
                        if(CollectionUtils.isNotEmpty(sysUserNotifications)){
                            userNotifications.addAll(sysUserNotifications);
                        }
                    });

                    log.info("需要发送的邮箱 -> {}",JSON.toJSONString(toSendEmails));
                    String state = "UNKNOWN";
                    switch (messageEntity.getState()) {
    				case 1:state = "Pending";break;
    				case 2:state = "Expired";break;
                    }

                    if (ToolUtil.isNotEmpty(toSendEmails)) {
                        try {
                            mailUtil.sendHTML(toSendEmails, state + " : " + messageEntity.getSummary(), this.sendMail(messageEntity));
                        } catch (Exception e) {
                            log.error("邮件发送任务异常", e);
                        }
                    }

                    // 通知脚本信息不为空
                    if (CollectionUtils.isNotEmpty(userNotifications)) {
                        log.info("邮件进行脚本通知");
                        executorService.submit(() -> this.notifiScript(userNotifications, scriptIdAndFilePathMap, JSONObject.toJSONString(messageEntity)));
                    } else {
                        log.info("邮件没有关联用户脚本，不进行脚本通知");
                    }
                }else{
                    log.info("邮件没有关联用户，不进行邮件通知");
                }
            }
        }
    }

    private void notifiScript(List<SysUserNotification> userNotifications, Map<Integer, String> scriptIdAndFilePathMap, String alertMsg) {
        String filePath, account;
        for (SysUserNotification userNotification : userNotifications) {
            filePath = scriptIdAndFilePathMap.get(userNotification.getScriptId());
            account = userNotification.getAccount();
            String[] commands = new String[]{filePath, account, alertMsg};
            try {
                Process exec = Runtime.getRuntime().exec(commands);
                boolean b = exec.waitFor(alertNotifiTimeOut, TimeUnit.SECONDS);
                if (log.isDebugEnabled())
                    log.debug(String.format("通知命令执行结束，执行命令是: %s , 结果是: %s", Arrays.asList(commands).toString(), b ? "成功" : "失败"));
            } catch (Exception e) {
                log.debug(String.format("通知命令执行结束，执行命令是: %s , 结果是: %s", Arrays.asList(commands).toString(), "失败"));
                log.error("alert通知脚本异常,异常消息是:" + e.getMessage(), e);
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
