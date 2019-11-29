package com.nis.common.utils;

import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MailUtil {
	
	@Value("${spring.mail.protocol}")
	private String protocol; // SMTP服务器
	@Value("${spring.mail.host}")
    private String host; // SMTP服务器
	@Value("${spring.mail.username}")
	private String user; // 用户名
	@Value("${spring.mail.password}")
	private String pwd; // 密码
	@Value("${spring.mail.port}")
	private String port; // 端口
	@Value("${spring.mail.from}")
    private String from; // 邮箱
    
	private Properties props = null;
	private Session session = null;
	
	public void sendHTML(List<String> to, String subject,String txt) {
		sendHTML(from, to, subject, txt);
	}
	public void sendHTML(String from, List<String> to, String subject,String txt) {
		if (props == null) {
			props = new Properties();
			props.setProperty("mail.transport.protocol", protocol);
			props.setProperty("mail.smtp.host", host);// 服务主机地址
			props.setProperty("mail.smtp.port", port);//端口
			
			//ssl用
			//props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");//端口
			//props.setProperty("mail.smtp.socketFactory.port", "465");//端口
			props.put("mail.smtp.auth", "true");
			// 用刚刚设置好的props对象构建一个session
			session = Session.getDefaultInstance(props);
			session.setDebug(true);
		}
		
		// 用session为参数定义消息对象
		MimeMessage message = new MimeMessage(session);
		try {
			// 加载发件人地址
			message.setFrom(new InternetAddress(from));
			// 加载收件人地址
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
			Transport transport = session.getTransport("smtp");
			// 连接服务器的邮箱
			transport.connect(host, user, pwd);
			// 把邮件发送出去
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
