package com.nis.common.config;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.TrapMessage;
import com.nis.modules.sys.service.SysConfigService;

import org.apache.commons.lang3.StringUtils;
import org.snmp4j.*;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 本类用于监听代理进程的Trap信息 1.加载alert_rule表内置规则（有且只有一条） 2.启动snmp_server 3.接收trap信息
 * 4.组装prometheus_alert格式 5.发送到web端接口处理
 * 
 * @author YJS
 * 
 */

@Component
@Order(1)
public class SNMPTrapServer implements CommandResponder, ApplicationRunner {// implements CommandResponder, extends
																			// Service
	private static Log logger = Log.get();
	@Autowired
	private AssetAssetService assetAssetService;
	@Autowired
	private AlertRuleService alertRuleService;
	@Autowired
	private SysConfigService sysConfigService;;
	private String alertApi;
	private String alertPathPrefix;
	private Integer snmpTrapThredPoolSize = 2;

	private static Snmp snmp;
	
	@Override
	public void run(ApplicationArguments args) throws IOException {
		initServer();
	}

	public static Snmp getSnmp() {
		return snmp;
	}

	public void initServer() throws IOException {
		logger.info("snmpTrapThredPoolSize is {}", snmpTrapThredPoolSize);
		ThreadPool threadPool = ThreadPool.create("Trap", Integer.valueOf(snmpTrapThredPoolSize));
		logger.info("snmpTrapThredPoolSize is {}", snmpTrapThredPoolSize);
		MultiThreadedMessageDispatcher dispatcher = new MultiThreadedMessageDispatcher(threadPool,
				new MessageDispatcherImpl());
		SysConfigEntity sysConfigEntity = sysConfigService.getOne(
				new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, "snmp_trap_listen_port"));
		String paramValue = sysConfigEntity.getParamValue();
		Integer snmpTrapPort = StrUtil.isEmpty(paramValue) ? 162 : Integer.valueOf(paramValue);
		Address listenAddress = GenericAddress
				.parse(System.getProperty("snmp4j.listenAddress", "udp:0.0.0.0/" + snmpTrapPort)); // 本地IP与监听端口
		logger.info("snmpTrapPort is {}", snmpTrapPort);
		TransportMapping transport = null;

//	    			 对TCP与UDP协议进行处理
		if (listenAddress instanceof UdpAddress) {
			transport = new DefaultUdpTransportMapping((UdpAddress) listenAddress);
			logger.debug("this listenAddress type is UDP");
		} else {
			transport = new DefaultTcpTransportMapping((TcpAddress) listenAddress);
			logger.debug("this listenAddress type is TCP");
		}

		snmp = new Snmp(dispatcher, transport);
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv3());
		USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
		SecurityModels.getInstance().addSecurityModel(usm);
		snmp.listen();
		snmp.addCommandResponder(this);

	}

	/**
	 * 实现CommandResponder的processPdu方法, 用于处理传入的请求、PDU等信息 当接收到trap时，会自动进入这个方法
	 * 
	 * @param respEvnt
	 */

	public void processPdu(CommandResponderEvent commandresponderevent) {
		try {
			//考虑数据库数据可能存在变化 由仅查询一次更改为每次都确认一下数据信息
			//加载alert_rule表内置规则
			AlertRuleEntity alertRule = alertRuleService
					.getOne(new QueryWrapper<AlertRuleEntity>().lambda().eq(AlertRuleEntity::getId, 1));
			List<SysConfigEntity> sysConfigs = sysConfigService.list();
			if (!sysConfigs.isEmpty()) {
				for (SysConfigEntity sysconfig : sysConfigs) {
					if (StrUtil.equals(sysconfig.getParamKey(), Constant.ALERT_API)) {
						alertApi = sysconfig.getParamValue();
					} else if (StrUtil.equals(sysconfig.getParamKey(), Constant.ALERT_PATH_PREFIX)) {
						alertPathPrefix = sysconfig.getParamValue();
					}
				}
				logger.info("sysconfig alert api info {}", alertApi);
				logger.info("sysconfig alert path prefix info {}", alertPathPrefix);
			} else {
				logger.error("Sysconfig missing alert_api or alert_path_prefix data");
				return;
			}
			logger.info("processPdu start");
			logger.info("alertRule info is {}", JSON.toJSON(alertRule));
			if (alertRule == null) {
				logger.error(" alert rule info is null ");
				return;
			}
			logger.info((new StringBuilder("receive snmp event：")).append(commandresponderevent.getPDU()).toString());
			UdpAddress udpaddress = (UdpAddress) commandresponderevent.getPeerAddress();
			String s = udpaddress.getInetAddress().toString();
			if(StringUtils.isNotEmpty(s)){
				s = s.substring(s.lastIndexOf("/") + 1, s.length());
			}
			PDU pdu = commandresponderevent.getPDU();
			logger.info((new StringBuilder("receive TRap:")).append(pdu).toString());
			TrapMessage trapmessageinfo = new TrapMessage();
			trapmessageinfo.setAgentSendIP(s);
			setOIDList(trapmessageinfo, pdu);
			setValueList(trapmessageinfo, pdu);
			trapmessageinfo.setOriginalPDU(pdu);
			if (pdu instanceof PDUv1) {
				PDUv1 pduv1 = (PDUv1) pdu;
				trapmessageinfo.setTrapVersion(1);
				String s1 = pduv1.getEnterprise().toString();
				trapmessageinfo.setTrapOID(s1);
				trapmessageinfo.setPduAgentIP(s);
				trapmessageinfo.setTrapV1SpecificType(pduv1.getSpecificTrap());
				trapmessageinfo.setTrapV1GenericType(pduv1.getGenericTrap());
				trapmessageinfo.setTrapName(trapmessageinfo.getTrapOID());
				switch (pduv1.getGenericTrap()) {
				case 0: // '\0'
//					trapmessageinfo.setTrapName("设备冷启动");
					trapmessageinfo.setTrapName("Device Cold Start");
					break;

				case 1: // '\001'
//					trapmessageinfo.setTrapName("设备热启动");
					trapmessageinfo.setTrapName("Device Hot Start");
					break;

				case 2: // '\002'
//					trapmessageinfo.setTrapName("接口关闭");
					trapmessageinfo.setTrapName("The Interface Is Closed");
					break;

				case 3: // '\003'
//					trapmessageinfo.setTrapName("接口启用");
					trapmessageinfo.setTrapName("Interface Enabled");
					break;

				case 4: // '\004'
//					trapmessageinfo.setTrapName("SNMP认证失败");
					trapmessageinfo.setTrapName("SNMP Authentication Failed");
					break;

				case 5: // '\005'
//					trapmessageinfo.setTrapName("EGP邻居丢失");
					trapmessageinfo.setTrapName("EGP Neighbor Lost");
					break;
				}
			} else {
				trapmessageinfo.setTrapVersion(2);
				Variable variable = trapmessageinfo.getOIDValue(SnmpConstants.snmpTrapAddress.toString());
				logger.info("pdu.snmpTrapAddress() =-=-= " + variable);
				if (variable != null)
					trapmessageinfo.setPduAgentIP(variable.toString());
				Variable variable1 = trapmessageinfo.getOIDValue(SnmpConstants.snmpTrapOID.toString());
				if (variable1 != null)
					trapmessageinfo.setTrapOID(variable1.toString());
				trapmessageinfo.setTrapName(trapmessageinfo.getTrapOID());
				if (variable1 != null)
					if (variable1.equals(SnmpConstants.coldStart))
//						trapmessageinfo.setTrapName("设备冷启动");
						trapmessageinfo.setTrapName("Device Cold Start");
					else if (variable1.equals(SnmpConstants.warmStart))
//						trapmessageinfo.setTrapName("设备热启动");
						trapmessageinfo.setTrapName("Device Hot Start");
					else if (variable1.equals(SnmpConstants.linkDown))
//						trapmessageinfo.setTrapName("接口关闭");
						trapmessageinfo.setTrapName("The Interface Is Closed");
					else if (variable1.equals(SnmpConstants.linkUp))
//						trapmessageinfo.setTrapName("接口启用");
						trapmessageinfo.setTrapName("Interface Enabled");
					else if (variable1.equals(SnmpConstants.authenticationFailure))
//						trapmessageinfo.setTrapName("SNMP认证失败");
						trapmessageinfo.setTrapName("SNMP Authentication Failed");
					/*else // 系统默认的类别不能匹配时，再使用自定义的trap信息库。是否需要分别自定义snmpV1和snmpValue2的trap信息库（定义到文件或者定义到数据库，必须体统重新加载的界面接口）
					if (getTrapName(variable1) != null && !variable1.equals("")) {
						trapmessageinfo.setTrapName(getTrapName(variable1));
					} else {
//						throw new Exception("未被定义的SNMPTRAP类型！");
						throw new Exception("Undefined SNMPTRAP type!");
					}*/
			}
			StringBuffer trapInfo = new StringBuffer();
			trapInfo.append("trapmessageinfo.getTrapName()：" + trapmessageinfo.getTrapName());
			trapInfo.append("; trapmessageinfo.getAgentSendIP()：" + trapmessageinfo.getAgentSendIP());
			trapInfo.append("; trapmessageinfo.getOriginalPDU()：" + trapmessageinfo.getOriginalPDU());
			trapInfo.append("; trapmessageinfo.getTrapVersion()：" + trapmessageinfo.getTrapVersion());
			trapInfo.append("; trapmessageinfo.getTrapOID()：" + trapmessageinfo.getTrapOID());
			trapInfo.append("; trapmessageinfo.getPduAgentIP()：" + trapmessageinfo.getPduAgentIP());
			trapInfo.append("; trapmessageinfo.getTrapV1GenericType()：" + trapmessageinfo.getTrapV1GenericType());
			trapInfo.append("; trapmessageinfo.getTrapV1SpecificType()：" + trapmessageinfo.getTrapV1SpecificType());
			logger.info("agent IP：" + s + "  trap message：" + trapInfo.toString());

// 组装prometheus_alert格式
			Integer alertName = alertRule.getId();
			Integer severity = alertRule.getSeverityId();
			String asset = trapmessageinfo.getAgentSendIP();
			AssetAsset assetData = assetAssetService.getOne(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getManageIp, asset));
			String oid = trapmessageinfo.getTrapOID();
			String value = trapmessageinfo.getOriginalPDU().getVariableBindings().toString();

			StringBuilder description = new StringBuilder();
			description.append("version:" + trapmessageinfo.getTrapVersion());
			description.append(",agentip:" + trapmessageinfo.getAgentSendIP());
			description.append(",oid:" + trapmessageinfo.getTrapOID());
			description.append(",value:" + value);
			String trapName = trapmessageinfo.getTrapName();
			if (trapName != null && trapName.length() > 0) {
				description.append(",trapName:" + trapName);
			}
			StringBuilder summary = new StringBuilder("snmptrap:");
			summary.append(trapmessageinfo.getAgentSendIP() + "(");
			summary.append(trapmessageinfo.getTrapOID() + ":" + value + ")");
			boolean snmptrap = true;
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			String startsAt = sdf.format(new Date());

			JSONObject labels = new JSONObject();
			labels.put("alertname", alertName.toString());
			labels.put("severity", severity);
			if(ToolUtil.isNotEmpty(assetData)) {
				labels.put("asset", assetData.getId());
			}
			labels.put("trapOID", oid);

			JSONObject annotations = new JSONObject();
			annotations.put("description", description.toString());
			annotations.put("summary", summary.toString());

			JSONObject result = new JSONObject();
			result.put("labels", labels);
			result.put("annotations", annotations);
			result.put("snmptrap", snmptrap);
			result.put("startsAt", startsAt);

			JSONArray mess = new JSONArray();
			mess.add(result);

			logger.info("send alert info is {}", JSON.toJSON(mess));

// 将告警信息发送到web端处理
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity entity = new HttpEntity(mess, headers);
//			String url="http://"+alertApi+alertPathPrefix+"/api/v1/alerts";
			if (StringUtils.isNotBlank(alertApi)) {
				String[] alertApis = alertApi.split(":");
				alertApi = alertApis[0];
				Integer alertPort = 80; 
				if(alertApis.length>1) {
					alertPort = Integer.valueOf(alertApis[1]);;
				}
				String path = String.format("%s%s", alertPathPrefix, "/api/v1/alerts");
				String url = UrlBuilder.create().setScheme("http").setHost(alertApi).setPort(alertPort)
						.setPath(UrlPath.of(path, Charset.forName("UTF-8")))
						.toString();
				Object postForEntity = restTemplate.postForEntity(url, entity, Object.class);				
				logger.info("post success info {}", JSON.toJSON(postForEntity));
			}
		} catch (NumberFormatException e) {
			logger.error("Error parsing UDP trap: {}", ExceptionUtil.getMessage(e));
		} catch (RestClientException e) {
			logger.error("Error parsing UDP trap: {}", ExceptionUtil.getMessage(e));
		}
	}

	public String getTrapName(Variable variable1) {
		String trapName = "";
		Map allTrapTypeDefine = new HashMap();
		trapName = allTrapTypeDefine.get(variable1.toString()).toString();
		return trapName;
	}

	void setOIDList(TrapMessage trapmessageinfo, PDU pdu) {
		Vector vector = pdu.getVariableBindings();
		String s;

		for (Iterator iterator = vector.iterator(); iterator.hasNext(); trapmessageinfo.getTrapPDUOIDs().add(s)) {
			VariableBinding variablebinding = (VariableBinding) iterator.next();
			s = variablebinding.getOid().toString();
		}

	}

	void setValueList(TrapMessage trapmessageinfo, PDU pdu) {
		Vector vector = pdu.getVariableBindings();
		Variable variable;
		for (Iterator iterator = vector.iterator(); iterator.hasNext(); trapmessageinfo.getTrapPDUOIDValues()
				.add(variable)) {
			VariableBinding variablebinding = (VariableBinding) iterator.next();
			variable = variablebinding.getVariable();
		}

	}

	/**
	 * 将十进六制转换成为中文
	 */
	private static String hexString = "0123456789ABCDEF";

}
