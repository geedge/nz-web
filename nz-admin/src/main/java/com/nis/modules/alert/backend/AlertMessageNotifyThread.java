package com.nis.modules.alert.backend;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.nis.common.utils.Constant;
import com.nis.common.utils.MailUtil;
import com.nis.common.utils.TemplateUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertNotifyLog;
import com.nis.modules.alert.entity.AlertNotifyMethod;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertNotifyLogService;
import com.nis.modules.alert.service.AlertNotifyMethodService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * 告警邮件发送线程 1、redis 中读取 需要发送的告警消息 2、检查告警消息发送方式 3、发送 4、记录发送状态
 * 
 * @author ThinkPad
 *
 */
@Component
public class AlertMessageNotifyThread implements Runnable {
	private static final Log logger = Log.get();

	private static final String THREAD_NAME = "Alert Message Notify Handler";
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	@Autowired
	private SysConfigService configService;

	private static Template template;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private AlertNotifyMethodService alertNotifyMethodService;
	@Autowired
	private AlertNotifyLogService alertNotifyLogService;
	@Autowired
	private MailUtil mailUtil;

	@Override
	public void run() {
		Thread.currentThread().setName(THREAD_NAME);
		logger.info("AlertMessageNotifyHandler 告警通知线程启动");
		while (true) {
			try {
				// 检查 haMode
		    	String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
		        if(!StrUtil.equals(serverId, Constant.SERVER_ID)) {
					Thread.sleep(10000);
		        	continue;
		        }
				ListOperations<String, String> opsForList = redisTemplate.opsForList();
				//从队列中取一条 需要通知的 alertmessage
				String messageStr = opsForList.rightPop(Constant.ALERT_MESSAGE_NOTIFY,30,TimeUnit.SECONDS);
				AlertMessageEntity messageEntity = Tool.JSONUtil.toBean(messageStr, AlertMessageEntity.class);
				// 1、rule查询需要 接收通知的人
				AlertRuleEntity alertRule = messageEntity.getAlertRule();
				if(alertRule == null) {
					logger.warn("Message notify error,AlertRule is null ,messageId : {}", messageEntity.getId());
					continue;
				}
				String receiver = alertRule.getReceiver();
				String methods = alertRule.getMethod();
				if(logger.isDebugEnabled()) {
					logger.debug("告警消息内容:{}", Tool.JSONUtil.toJsonStr(messageEntity));
				}
				// 没有接收人或通知方式，无法发送通知
				if (Tool.StrUtil.isBlank(receiver) || Tool.StrUtil.isBlank(methods)) {
					logger.debug("receiver & methods all blank ,messageId:{}",messageEntity.getId());
					continue;
				}
				StopWatch sw = StopWatch.create("notify");
				sw.start("发送通知");
				// 查找所有接收人
				Collection<SysUserEntity> userList = sysUserService.listByIds(Tool.ListUtil.toList(receiver.split(",")));
				// 查找所有通知方式
				Collection<AlertNotifyMethod> methodList = alertNotifyMethodService.listByIds(Tool.ListUtil.toList(methods.split(",")));
				if (Tool.CollUtil.isEmpty(userList) || Tool.CollUtil.isEmpty(methodList)) {
					continue;
				}
				// 内置 email 通知方式
				AlertNotifyMethod buildInEmailMethod = alertNotifyMethodService.getOne(new LambdaQueryWrapper<AlertNotifyMethod>().eq(AlertNotifyMethod::getBuildIn, 1));

				List<AlertNotifyLog> logList = Tool.ListUtil.list(false);
				//遍历用户 & 通知方式 通知
				for (SysUserEntity user : userList) {
					for (AlertNotifyMethod method : methodList) {
						String accountName = method.getAccount();
						Object accountVal = Tool.BeanUtil.getProperty(user, accountName);
						if (Tool.StrUtil.isBlankIfStr(accountVal)) {
							logger.info("用户 {},{} 不存在", user.getName(), accountName);
							continue;
						}
						logger.debug("告警邮件通知人信息:{},通知方式:{}", Tool.JSONUtil.toJsonStr(user),
								Tool.JSONUtil.toJsonStr(method));
						// 只有一条 email 内置通知
						if (Tool.ObjectUtil.equal(method.getBuildIn(), 1)) {
							AlertNotifyLog notifyLog = notifyEmail(user, buildInEmailMethod, messageEntity);
							logList.add(notifyLog);
						}else {
							AlertNotifyLog notifyLog = notifyScript(user, method, messageEntity);
							logList.add(notifyLog);
						}
					}
				}
				sw.stop();
				sw.start("保存通知日志");
				alertNotifyLogService.saveBatch(logList);
				sw.stop();
				if(logger.isDebugEnabled()) {
					logger.debug("耗时统计:{}", sw.prettyPrint());
				}
			} catch (Throwable e) {
				if(!(e instanceof QueryTimeoutException)) {
					logger.error(e, "Alert Message Notify error");
				}
			}
		}
	}

	/**
	 * 
	 * @param user
	 * @param method
	 * @param message
	 * @return
	 */
	public AlertNotifyLog notifyScript(SysUserEntity user, AlertNotifyMethod method, AlertMessageEntity message) {
		AlertNotifyLog log = new AlertNotifyLog();
		log.setMessageId(message.getId());
		log.setMessageState(message.getState());
		log.setMethodId(method.getId());
		log.setUserId(user.getId());
		log.setTime(Tool.DateUtil.date());
		log.setState(1);
		String filePath = method.getFilePath();
		boolean exist = Tool.FileUtil.exist(filePath);
		if (!exist) {
			log.setErrorMsg("file not exist");
			log.setState(0);
		}else {
			Process process = Tool.RuntimeUtil.exec(filePath, Tool.JSONUtil.toJsonStr(message));
			String result = Tool.RuntimeUtil.getErrorResult(process, Charset.forName(Constant.DEFAULT_CHARTSET_NAME));
			Tool.RuntimeUtil.destroy(process);
			if (Tool.StrUtil.isNotBlank(result)) {//错误信息不为空则说明发送报错
				log.setState(0);
				log.setErrorMsg(result);
			}
		}
		logger.debug("告警通知结果:{}", Tool.JSONUtil.toJsonStr(log));
		return log;
	}

	/**
	 * 邮件方式通知
	 * @param user
	 * @param buildInEmailMethod
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws TemplateException
	 */
	public AlertNotifyLog notifyEmail(SysUserEntity user, AlertNotifyMethod buildInEmailMethod, AlertMessageEntity message)
			throws IOException, TemplateException {
		Integer state = message.getState();// 消息状态
		AlertNotifyLog log = new AlertNotifyLog();
		log.setMessageId(message.getId());
		log.setMessageState(state);
		log.setMethodId(buildInEmailMethod != null ? buildInEmailMethod.getId() : 1);
		log.setUserId(user.getId());
		log.setTime(Tool.DateUtil.date());
		Template template = this.getEmailTemplate();
		String content = this.genMailContent(message, template);
		String stateStr = "UNKNOWN";
        switch (message.getState()) {
            case 1:stateStr = "Active";break;
            case 3:stateStr = "Expired";break;
        }
		try {
			mailUtil.sendHTML(Tool.ListUtil.toList(user.getEmail()), Tool.StrUtil.concat(true, stateStr,message.getSummary()), content);
			log.setState(1);
		} catch (MessagingException e) {
			logger.error(e, "邮件通知error,user:{},message:{}", Tool.JSONUtil.toJsonStr(user),Tool.JSONUtil.toJsonStr(message));
			log.setState(0);
			log.setErrorMsg(e.getMessage());
		}
		return log;
	}

	public String genMailContent(AlertMessageEntity alertMessage, Template template)
			throws IOException, TemplateException {
		// 创建模型数据
		Map<String, Object> modelMap = new HashMap<String, Object>();
		modelMap.put("alertMessage", alertMessage);
		// 执行静态化 方式1 获取静态化内容
		String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, modelMap);
		// 静态化内容;
		return content;
	}

	/**
	 * 获取email 模板
	 * 
	 * @return
	 * @throws IOException
	 */
	private Template getEmailTemplate() throws IOException {
		if (template == null) {
			String templateString = configService.getValue("email_template");
			template = TemplateUtil.stringToTemplate(templateString, "emailTemplate");
		}
		return template;
	}

}
