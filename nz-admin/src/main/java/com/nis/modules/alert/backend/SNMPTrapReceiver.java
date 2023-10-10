package com.nis.modules.alert.backend;

import cn.hutool.log.Log;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleEntity.ConditionItem;
import com.nis.modules.alert.entity.Snmptrap;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.sys.service.SysConfigService;
import org.snmp4j.*;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * @author
 * 
 */
@Component
@Order(1)
public class SNMPTrapReceiver implements CommandResponder, ApplicationRunner {
	private static Log log = Log.get();
	private MultiThreadedMessageDispatcher dispatcher;
	private Snmp snmp = null;
	private Address listenAddress;
	private int port;
	private ThreadPool threadPool;
	private final static String SYSUPTIME_OID = "1.3.6.1.2.1.1.3.0";
	private final static String SNMPTRAP_OID = "1.3.6.1.6.3.1.1.4.1.0";
	private final static String SNMPTRAPENTERPRISE_OID = "1.3.6.1.6.3.1.1.4.3.0";
	@Autowired
	private AlertMessageService messageService;
	@Autowired
	private SysConfigService configService;
	@Autowired
	private AssetAssetService assetService;

	public SNMPTrapReceiver() {
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String port = configService.getValueOrDefault("snmp_trap_listen_port", "162");
		this.start(Integer.valueOf(port));
	}

	public void start(int port) {
		try {
			this.port = port;
			init();
			snmp.addCommandResponder(this);
			log.info("start success,listen port:{}", port);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void restart(int port) {
		this.port = port;
		this.stop();
		this.start(port);
		log.info("restart success,listen port:{}", port);
	}

	public void stop() {
		if (snmp != null) {
			try {
				snmp.close();
				log.info("stop success,listen port:{}", port);
			} catch (Exception e) {
				log.error(e, "stop snmp error");
			}
		}
	}

	private void init() throws UnknownHostException, IOException {
		threadPool = ThreadPool.create("Trap", 5);
		dispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());

		// TRANSPORT
		listenAddress = GenericAddress.parse(System.getProperty("snmp4j.listenAddress", "udp:0.0.0.0/" + this.port)); // SET
																														// THIS
		TransportMapping<?> transport;
		if (listenAddress instanceof UdpAddress) {
			transport = new DefaultUdpTransportMapping((UdpAddress) listenAddress);
		} else {
			transport = new DefaultTcpTransportMapping((TcpAddress) listenAddress);
		}
		// V3 SECURITY
		USM usm = new USM(SecurityProtocols.getInstance().addDefaultProtocols(),
				new OctetString(MPv3.createLocalEngineID()), 0);
		SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES192());
		SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES256());
		SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());

		usm.setEngineDiscoveryEnabled(true);

		SecurityModels.getInstance().addSecurityModel(usm);

		snmp = new Snmp(dispatcher, transport);
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv3(usm));

		String username = "nezha"; // SET THIS
		String authpassphrase = "nezhaauth"; // SET THIS
		String privacypassphrase = "nezhaprivacy"; // SET THIS

		snmp.getUSM().addUser( // SET THE SECURITY PROTOCOLS HERE
				new OctetString(username), new UsmUser(new OctetString(username), AuthMD5.ID,
						new OctetString(authpassphrase), PrivAES128.ID, new OctetString(privacypassphrase)));

		snmp.listen();
	}

	@Override
	public void processPdu(CommandResponderEvent crEvent) {
		try {
			PDU pdu = crEvent.getPDU();
			int type = pdu.getType();
			if (type != PDU.V1TRAP && type != PDU.TRAP) {
				// 只处理trap数据
				return;
			}
			IpAddress address = (IpAddress) crEvent.getPeerAddress();
			String fromIP = address.getInetAddress().getHostName();
			Snmptrap entity = new Snmptrap();// 重新包装
			entity.setSource(fromIP);
			entity.setFromHostName(fromIP);
			entity.setTimestamp(Tool.DateUtil.current());
			Vector<? extends VariableBinding> varBinds = pdu.getVariableBindings();
			// 处理变量
			if (varBinds != null && !varBinds.isEmpty()) {
				for (VariableBinding vb : varBinds) {
					String oid = vb.getOid().toString();
					String value = vb.getVariable().toString();
					entity.appendVariable(oid, value);
				}
			}

			if (pdu.getType() == PDU.V1TRAP) {
				// snmptrap -v 1 [COMMON OPTIONS] AGENT enterprise-oid agent generic-trap
				// specific-trap uptime [OID TYPE VALUE]...
				PDUv1 pduV1 = (PDUv1) pdu;
				entity.setVersion("v1");
				entity.setSource(pduV1.getAgentAddress().toString());
				entity.setEnterpriseOID(pduV1.getEnterprise().toString());
				long timestamp = pduV1.getTimestamp();
				entity.setSysUptime(new TimeTicks(timestamp).toString());
				entity.setGeneric(pduV1.getGenericTrap());
				entity.setSpecific(pduV1.getSpecificTrap());
				entity.setGeneric(pduV1.getGenericTrap());
				int genericTrap = pduV1.getGenericTrap();
				Map<String, String> map = GENERIC_CONSTANTS.get(genericTrap);
				if (map != null) {
					entity.setTrapOID(map.get("oid"));
					entity.setMessage(map.get("message"));
				} else {
					String trapOID = Tool.StrUtil.concat(true, entity.getEnterpriseOID(), ".0.",
							String.valueOf(entity.getSpecific()));
					String message = entity.getVariable(trapOID);
					if (Tool.StrUtil.isBlank(message)) {
						entity.setTrapOID(entity.getEnterpriseOID());
						entity.setMessage(entity.getVariable(entity.getEnterpriseOID()));
					} else {
						entity.setTrapOID(trapOID);
						entity.setMessage(entity.getVariable(trapOID));
					}
				}
			} else if (pdu.getType() == PDU.TRAP) {
				// snmpinform -v [2c|3] [COMMON OPTIONS] AGENT uptime trap-oid [OID TYPE
				// VALUE]...
				entity.setVersion("v2");
				SecurityModel securityModel = crEvent.getStateReference().getSecurityModel();
				if (securityModel != null) {
					entity.setVersion("v3");
				}
				String trapOID = entity.getVariable(SNMPTRAP_OID);
				String sysUptime = entity.getVariable(SYSUPTIME_OID);
				String enterpriseOID = entity.getVariable(SNMPTRAPENTERPRISE_OID);

				entity.setEnterpriseOID(enterpriseOID);
				entity.setSysUptime(sysUptime);
				entity.setTrapOID(trapOID);
				entity.setMessage(entity.getVariable(trapOID));

				entity.delVariable(SNMPTRAP_OID);
				entity.delVariable(SYSUPTIME_OID);
				entity.delVariable(SNMPTRAPENTERPRISE_OID);
			}
			if (log.isDebugEnabled()){
				log.debug("[SNMPTrapReceiver] [listener from ip: {}] [snmp trap entity: {}]", fromIP, JSONUtil.toJsonStr(entity));
			}
			// 转换为 messagedto
			AlertMessageReceiveDTO dto = this.transSnmptrapToPromMsg(entity);
			if (dto != null) {
				// 发送到 缓存队列
				ArrayList<AlertMessageReceiveDTO> list = Tool.ListUtil.toList(dto);
				messageService.leftPushAlertMessageToRedis(Tool.JSONUtil.toJsonStr(list));
			}
		} catch (Exception e) {
			log.error(e, "snmp trap error");
		}
	}

	/**
	 * 根据 snmptrap 消息 转为 prometheus 格式内容
	 * 
	 * @param trap
	 * @return
	 */
	public AlertMessageReceiveDTO transSnmptrapToPromMsg(Snmptrap trap) {
		if (trap == null) {
			return null;
		}
		String trapOID = trap.getTrapOID();
		String source = trap.getSource();
		// 获取告警规则
		AlertRuleEntity rule = this.getRuleByOID(trapOID);
		if (rule == null) {
			return null;
		}
		if (log.isDebugEnabled()){
			log.debug("[SNMPTrapReceiver] [transSnmptrapToPromMsg] [trap-oid: {}] [alertRule id: {}]", trapOID, rule.getId());
		}

		AlertMessageReceiveDTO dto = new AlertMessageReceiveDTO();
		List<ConditionItem> condition = rule.getConditionItems();
		Integer severityId = null; // 记录是否有满足的条件，没有则不产生告警消息
		for (ConditionItem item : condition) {
			if (item.validate(trap.getMessage())) {
				severityId = item.getId();
				break;
			}
		}
		if (severityId == null) {
			return null;
		}
		LinkedHashMap<String, String> labels = new LinkedHashMap<String, String>();
		labels.put("severity_id", severityId.toString());
		labels.put("rule_type", "snmptrap");
		AssetAsset asset = assetService.getAssetByIP(source);
		if (asset != null) {
			labels.put("asset", asset.getName());
			labels.put("asset_id", asset.getId().toString());
		} else {
			labels.put("source", source);
		}
		labels.put("alertname", rule.getId().toString());
		labels.put("rule_id", rule.getId().toString());
		labels.put("rule_name", rule.getName());
		labels.put("oid", trapOID);
		// 添加自定义标签
		List<Map<String, String>> extLabelList = rule.getExtLabelList();
		extLabelList.forEach(map -> {
			labels.put(map.get("name"), map.get("value"));
		});

		dto.setStartsAt(Tool.AlertUtil.formatMessageDate(trap.getTimestamp()));
		int expiredTimeout = rule.getExpiredTimeout();
		// 用户如果设置了 自动失效 且 有 timeout值
		if (expiredTimeout > 0) {
			dto.setEndsAt(Tool.AlertUtil.formatMessageDate(trap.getTimestamp() + expiredTimeout * 1000));
		} else {
			// 关闭自动 expired 需要确保 每次label 不一致，才不会匹配到
			labels.put("timestamp", String.valueOf(trap.getTimestamp()));
		}
		dto.setLabels(labels);
		Map<String, String> annotations = Tool.MapUtil.newHashMap();
		String summary = rule.getSummary();
		String description = rule.getDescription();
		Map<String, Object> context = Tool.BeanUtil.beanToMap(trap);
		if (Tool.StrUtil.isBlank(summary)) {
			summary = trap.getMessage();
		} else {
			summary = Tool.AlertUtil.renderAlertRuleAnnotations(summary, context);
		}
		annotations.put("summary", summary);
		if (Tool.StrUtil.isBlank(description)) {
			description = trap.toString();
		} else {
			description = Tool.AlertUtil.renderAlertRuleAnnotations(description, context);
		}
		annotations.put("description", description);
		dto.setAnnotations(annotations);
		return dto;
	}

	/**
	 * 根据oid 获取匹配的 alert rule
	 * 
	 * @param oid
	 * @return
	 */
	private AlertRuleEntity getRuleByOID(String oid) {
		Map<String, AlertRuleEntity> allSnmpAlertRule = AlertContext.ALL_SNMP_ALERT_RULE;
		if (allSnmpAlertRule.isEmpty() || Tool.StrUtil.isBlank(oid)) {
			return null;
		}
		while (true) {
			AlertRuleEntity rule = allSnmpAlertRule.get(oid.trim());
			if (rule != null) {
				return rule;
			}
			int indexOf = oid.lastIndexOf(".");
			if (indexOf < 0) {
				return null;
			}
			oid = oid.substring(0, indexOf);
		}
	}

	/**
	 * generic oid 常量
	 */
	public static final Map<Integer, Map<String, String>> GENERIC_CONSTANTS = Tool.MapUtil.newHashMap();
	static {
		GENERIC_CONSTANTS.put(0,
				Tool.MapUtil.builder("oid", "1.3.6.1.6.3.1.1.5.1").put("message", "coldStart").build());
		GENERIC_CONSTANTS.put(1,
				Tool.MapUtil.builder("oid", "1.3.6.1.6.3.1.1.5.2").put("message", "warmStart").build());
		GENERIC_CONSTANTS.put(2, Tool.MapUtil.builder("oid", "1.3.6.1.6.3.1.1.5.3").put("message", "linkDown").build());
		GENERIC_CONSTANTS.put(3, Tool.MapUtil.builder("oid", "1.3.6.1.6.3.1.1.5.4").put("message", "linkUp").build());
		GENERIC_CONSTANTS.put(4,
				Tool.MapUtil.builder("oid", "1.3.6.1.6.3.1.1.5.5").put("message", "authenticationFailure").build());
		GENERIC_CONSTANTS.put(5,
				Tool.MapUtil.builder("oid", "1.3.6.1.6.3.1.1.5.6").put("message", "egpNeighborLoss").build());
//		GENERIC_CONSTANTS.put(6, Tool.MapUtil.builder("oid", "").put("message", "enterpriseSpecific").build());
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
