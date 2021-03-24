package com.nis.common.utils;

import cn.hutool.log.Log;
import com.nis.common.exception.NZException;
import com.nis.modules.sys.service.SysConfigService;
import com.sun.mail.util.MailConnectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.List;
import java.util.Properties;

@Component
@Slf4j
public class MailUtil {

	private static Log logger = Log.get();

	@Autowired
	SysConfigService configService;
    
	private Properties props = null;
	private Session session = null;

	String from,host,port,code,protocol,user;

	
	public void sendHTML(List<String> to, String subject,String txt) throws MessagingException {
		/*from = configService.getValue("email_smtp_account");
		sendHTML(from, to, subject, txt);*/
		// 发送邮箱地址
		from = configService.getValue("email_send_account");
		if(StringUtils.isEmpty(from)){
			// 如果发送邮箱地址为空 则默认使用登录邮箱
			from = configService.getValue("email_smtp_account");
		}
		sendHTML(from, to, subject, txt);
	}
	public void sendHTML(String from, List<String> to, String subject,String txt) throws MessagingException {
		host = configService.getValue("email_smtp_host");
		port = configService.getValue("email_smtp_port");
		code = configService.getValue("email_smtp_password");
		protocol = configService.getValue("email_smtp_protocol");
		user = configService.getValue("email_send_account");

		if (props == null) {
			props = new Properties();
			//props.setProperty("mail.transport.protocol", protocol);
			props.setProperty("mail.smtp.host", host);// 服务主机地址
			props.setProperty("mail.smtp.port",port);//端口
			
			//ssl用
			//props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");//端口
			//props.setProperty("mail.smtp.socketFactory.port", "465");//端口
			props.put("mail.smtp.auth", "true");
			// 用刚刚设置好的props对象构建一个session
			session = Session.getInstance(props);
			session.setDebug(true);
		}
		
		// 用session为参数定义消息对象
		MimeMessage message = new MimeMessage(session);
		try {
			// 加载发件人地址
			message.setFrom(new InternetAddress(from+"<"+user+">"));
			// 加载收件人地址
			to.add(user);
			Address[] addresses = new Address[to.size()];
			for (int i = 0; i < to.size(); i++) {
				Address address = new InternetAddress(to.get(i));
				addresses[i] = address;
			}
			message.addRecipients(Message.RecipientType.TO, addresses);
			// 加载标题
			message.setSubject(subject);
			// 向multipart对象中添加邮件的各个部分内容，包括文本内容和附件
			Multipart multipart = new MimeMultipart();

			// 设置邮件的文本内容
			BodyPart contentPart = new MimeBodyPart();
			
			contentPart.setHeader("heard1", "header2");
			contentPart.setContent(txt, "text/html;charset=utf-8");
			multipart.addBodyPart(contentPart);

			// 将multipart对象放到message中
			message.setContent(multipart);
			// 保存邮件
			message.saveChanges();
			// 发送邮件
			Transport transport = session.getTransport(protocol);
			// 连接服务器的邮箱
			transport.connect(host, user, code);
			// 把邮件发送出去
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
		} catch (RuntimeException e) {
			log.error("send to {} subject {} from {} faild with exception ",to.toString(),from,subject,e);
			throw e;
		}
	}


	/**
	 * 发送邮件
	 *
	 * @param smtpHost    smtp服务地址
	 * @param prop        配置信息
	 * @param smtpAccount 登录邮箱账号
	 * @param sendAccount 发送邮箱
	 * @param password    密码
	 * @param to          接收邮箱
	 * @param subject     主题
	 * @param txt         邮件内容
	 * @return
	 */
	public void sendHTML(String smtpHost, Properties prop, String smtpAccount, String sendAccount, String pin, String to, String subject, String txt) {
		session = Session.getInstance(prop);
		session.setDebug(true);

		// 用session为参数定义消息对象
		MimeMessage message = new MimeMessage(session);
		Transport ts = null;
		try {
			// 如果发件人为空，默认使用登录账号作为发送账号
			if (StringUtils.isEmpty(sendAccount)) {
				sendAccount = smtpAccount;
			}
			// 加载发件人地址
			message.setFrom(new InternetAddress(sendAccount));
			// 加载收件人地址
			message.addRecipients(Message.RecipientType.TO, to);
			// 加载标题
			message.setSubject(subject);

			// 向multipart对象中添加邮件的各个部分内容，包括文本内容和附件
			Multipart multipart = new MimeMultipart();

			// 设置邮件的文本内容
			BodyPart contentPart = new MimeBodyPart();

			contentPart.setHeader("heard1", "header2");
			contentPart.setContent(txt, "text/html;charset=utf-8");
			multipart.addBodyPart(contentPart);

			// 将multipart对象放到message中
			message.setContent(multipart);
			// 保存邮件
			message.saveChanges();
			// 发送邮件
			ts = session.getTransport();
			// 连接服务器的邮箱
			ts.connect(smtpHost, smtpAccount, pin);
			// 把邮件发送出去
			ts.sendMessage(message, message.getAllRecipients());
		} catch (AuthenticationFailedException e) {
			// for authentication failures
			logger.error("Authentication failed to connect to SMTP server",e);
			throw new NZException(RCode.SYS_CONFIG_EMAIL_AUTHENTICATIONFAIL);
		} catch (MailConnectException e) {
			// 连接smtp服务异常 造成此类问题的原因：  1. 服务未开启 2. 服务器防火墙等其他原因造成无法连接到服务器
			logger.error("Failed to connect to SMTP server",e);
			throw new NZException(RCode.SYS_CONFIG_EMAIL_CONNECTFAIL);
		} catch (MessagingException e) {
			// 其他关于登陆的错误 比如 连接超时
			logger.error("Failed to connect to SMTP server",e);
			throw new NZException(RCode.SYS_CONFIG_EMAIL_OTHERFAILURES);
		} catch (RuntimeException e) {
			logger.error("Failed to connect to SMTP server",e);
			throw new NZException(RCode.SYS_CONFIG_EMAIL_CONNECTFAIL);
		} finally {
			if (ts != null) {
				try {
					ts.close();
				} catch (MessagingException e) {
					logger.error("关闭Transport失败",e);
				}
			}
		}
	}

}
