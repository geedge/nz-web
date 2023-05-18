package com.nis.common.utils;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.exception.NZException;
import com.nis.modules.sys.entity.SnmpCredential;
import lombok.Data;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * SnmpUtil
 *
 * @author shizhendong
 */
@Data
public class SnmpUtil {

    private static final Log log = Log.get();

    private String ip;
    private Integer port;
    private String oid;
    private List<String> oidList;
    private SnmpCredential snmpCredential;

    private Snmp snmp;

    private Integer timeout;
    private Integer retries;

    public static PDU createGetPDU(int version, int type, String... oids) {
        PDU pdu = null;
        if (version == SnmpConstants.version3) {
            pdu = new ScopedPDU();
        } else {
            pdu = new PDU();
        }
        pdu.setType(type);
        for (String s : oids) {
            pdu.add(new VariableBinding(new OID(s)));
        }
        return pdu;
    }

    public static PDU createSetPDU(int version, int type, String oid, String value) {
        PDU pdu = null;
        if (version == SnmpConstants.version3) {
            pdu = new ScopedPDU();
        } else {
            pdu = new PDU();
        }
        pdu.setType(type);
        pdu.add(new VariableBinding(new OID(oid), new OctetString(value)));
        return pdu;
    }

    public static boolean checkWalkFinished(String oid, PDU pdu, VariableBinding vb) {
        OID targetOID = new OID(oid);
        boolean finished = false;
        if (pdu.getErrorStatus() != 0) {
            finished = true;
        } else if (vb.getOid() == null) {
            finished = true;
        } else if (vb.getOid().size() < targetOID.size()) {
            finished = true;
        } else if (targetOID.leftMostCompare(targetOID.size(), vb.getOid()) != 0) {
            finished = true;
        } else if (Null.isExceptionSyntax(vb.getVariable().getSyntax())) {
            finished = true;
        } else if (vb.getOid().compareTo(targetOID) <= 0) {
            finished = true;
        }
        return finished;
    }

    public SnmpUtil(String ip, SnmpCredential snmpCredential) {
        this.ip = ip;
        this.port = snmpCredential.getPort();
        this.snmpCredential = snmpCredential;
    }

    public SnmpUtil(String ip, Integer port, String oid, SnmpCredential snmpCredential) {
        this.ip = ip;
        this.port = port;
        this.oid = oid;
        this.snmpCredential = snmpCredential;
    }

    private void initSnmp() throws IOException {
        TransportMapping transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);

        // 开启监听
        snmp.listen();
    }

    private Target createTarget(int pduType) {
        Target target = null;
        Integer type = this.snmpCredential.getType();

        Map<String, String> credentialConfig = JSONObject.parseObject(this.snmpCredential.getConfig(), Map.class);
        if (Tool.ObjectUtil.equal(2, type)) {
            target = new CommunityTarget();
            target.setVersion(SnmpConstants.version2c);

            if (PDU.SET == pduType) {
                String community = credentialConfig.getOrDefault("writeCommunity", "public");
                ((CommunityTarget) target).setCommunity(new OctetString(community));
            } else {
                String community = credentialConfig.getOrDefault("readcommunity", "public");
                ((CommunityTarget) target).setCommunity(new OctetString(community));
            }
        }
        if (Tool.ObjectUtil.equal(3, type)) {
            // 设置安全模式
            USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityModels.getInstance().addSecurityModel(usm);

            // 添加用户
            OID authProtocolOid = null;
            String authProtocol = credentialConfig.get("authProtocol");
            if (Tool.StrUtil.equals("MD5", authProtocol)) {
                authProtocolOid = AuthMD5.ID;
            } else if (Tool.StrUtil.equals("SHA", authProtocol)) {
                authProtocolOid = AuthSHA.ID;
            }

            OID privProtocolOid = null;
            String privProtocol = credentialConfig.get("privProtocol");
            if (Tool.StrUtil.equals("DES", privProtocol)) {
                privProtocolOid = PrivDES.ID;
            } else if (Tool.StrUtil.equals("AES-128", privProtocol)) {
                privProtocolOid = PrivAES128.ID;
            } else if (Tool.StrUtil.equals("AES-192", privProtocol)) {
                privProtocolOid = PrivAES192.ID;
            } else if (Tool.StrUtil.equals("AES-256", privProtocol)) {
                privProtocolOid = PrivAES256.ID;
            }

            String privPassword = credentialConfig.get("privPin");
            OctetString octetPrivPassword = null;
            if (Tool.StrUtil.isNotEmpty(privPassword)) {
                octetPrivPassword = new OctetString(privPassword);
            }

            String pin = credentialConfig.get("authPin");
            OctetString octetpassword = null;
            if (Tool.StrUtil.isNotEmpty(pin)) {
                octetpassword = new OctetString(pin);
            }

            OctetString username = new OctetString(credentialConfig.get("username"));
            UsmUser user = new UsmUser(username, authProtocolOid, octetpassword, privProtocolOid, octetPrivPassword);
            snmp.getUSM().addUser(username, user);

            target = new UserTarget();
            target.setVersion(SnmpConstants.version3);

            String level = credentialConfig.get("securityLevel");
            // 不为空时设置安全级别 如果为空则不处理 默认的安全级别为noAuthNoPriv 不认证不加密
            if (StringUtils.isNotEmpty(level)) {
                // 设置安全级别
                switch (level) {
                    case "authPriv": {
                        ((UserTarget) target).setSecurityLevel(SecurityLevel.AUTH_PRIV);
                        break;
                    }
                    case "noAuthNoPriv": {
                        ((UserTarget) target).setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
                        break;
                    }
                    case "authNoPriv": {
                        ((UserTarget) target).setSecurityLevel(SecurityLevel.AUTH_NOPRIV);
                        break;
                    }
                    default:
                        break;
                }
            }
            ((UserTarget) target).setSecurityName(username);
        }

        target.setTimeout(this.getTimeout());
        target.setRetries(this.getRetries());

        Address address = GenericAddress.parse("udp:" + this.ip + "/" + this.port);
        target.setAddress(address);
        return target;
    }

    public List<Map> snmpGet() {
        List<Map> resultData = Tool.ListUtil.list(true);
        try {
            // initSnmp
            this.initSnmp();

            // createTarget
            Target target = this.createTarget(PDU.GET);

            // 根据版本不同获取不同的pdu对象
            PDU pdu = createGetPDU(this.getSnmpCredential().getType(), PDU.GET, this.oid);

            ResponseEvent respEvent = snmp.send(pdu, target);
            PDU response = respEvent.getResponse();
            if (response == null) {
                if (log.isDebugEnabled()) {
                    log.debug("[snmpGet] [response is null, request time out]");
                }
                return resultData;
            } else {
                VariableBinding vb = null;
                for (int i = 0; i < response.size(); i++) {
                    Map map = Tool.MapUtil.newHashMap();
                    vb = response.get(i);
                    map.put("oid", vb.getOid().toString());
                    map.put("value", vb.getVariable().toString());
                    map.put("type", vb.getVariable().getSyntaxString());
                    map.put("ip", ip);
                    map.put("port", port);
                    resultData.add(map);
                }
            }
        } catch (Exception e) {
            log.error(e, "[snmpGet] [error] [objetc: {}]", this.toString());
        }
        return resultData;
    }

    public List<Map> snmpMulitGet() {
        List<Map> resultData = Tool.ListUtil.list(true);
        try {
            // initSnmp
            this.initSnmp();

            // createTarget
            Target target = this.createTarget(PDU.GET);

            // 根据版本不同获取不同的pdu对象
            String[] array = this.oidList.stream().toArray(n -> new String[n]);
            PDU pdu = createGetPDU(this.getSnmpCredential().getType(), PDU.GET, array);

            ResponseEvent respEvent = snmp.send(pdu, target);
            PDU response = respEvent.getResponse();
            if (response == null) {
                if (log.isDebugEnabled()) {
                    log.debug("[snmpGet] [response is null, request time out]");
                }
                return resultData;
            } else {
                VariableBinding vb = null;
                for (int i = 0; i < response.size(); i++) {
                    Map map = Tool.MapUtil.newHashMap();
                    vb = response.get(i);
                    map.put("oid", vb.getOid().toString());
                    map.put("value", vb.getVariable().toString());
                    map.put("type", vb.getVariable().getSyntaxString());
                    map.put("ip", ip);
                    map.put("port", port);
                    resultData.add(map);
                }
            }
        } catch (Exception e) {
            log.error(e, "[snmpGet] [error] [objetc: {}]", this.toString());
        }
        return resultData;
    }

    public List<Map> snmpGetNext() {
        List<Map> resultData = Tool.ListUtil.list(true);
        try {
            // initSnmp
            this.initSnmp();

            // createTarget
            Target target = this.createTarget(PDU.GETNEXT);

            // 根据版本不同获取不同的pdu对象
            PDU pdu = createGetPDU(this.getSnmpCredential().getType(), PDU.GETNEXT, this.oid);

            ResponseEvent respEvent = snmp.send(pdu, target);
            PDU response = respEvent.getResponse();
            if (response == null) {
                log.error("[snmpGetNext] [response is null, request time out]");
                return resultData;
            } else {
                VariableBinding vb = null;
                for (int i = 0; i < response.size(); i++) {
                    Map map = Tool.MapUtil.newHashMap();
                    vb = response.get(i);
                    map.put("oid", vb.getOid().toString());
                    map.put("value", vb.getVariable().toString());
                    map.put("type", vb.getVariable().getSyntaxString());
                    map.put("ip", ip);
                    map.put("port", port);
                    resultData.add(map);
                }
            }
        } catch (Exception e) {
            log.error(e, "[snmpGetNext] [error] [objetc: {}]", this.toString());
        }
        return resultData;
    }

    public List<Map> snmpWalk() {
        List<Map> resultData = Tool.ListUtil.list(true);
        try {
            // initSnmp
            this.initSnmp();

            // createTarget
            Target target = this.createTarget(PDU.GETNEXT);

            // createPDU
            PDU pdu = createGetPDU(this.getSnmpCredential().getType(), PDU.GETNEXT, this.oid);

            // 结束标识
            boolean finished = false;

            int i = 0;
            while (!finished) {
                i++;
                VariableBinding vb = null;
                ResponseEvent respEvent = snmp.getNext(pdu, target);

                PDU response = respEvent.getResponse();

                // 为空则直接结束
                if (response == null) {
                    finished = true;
                    break;
                } else {
                    vb = response.get(0);
                }

                // 判断是否结束
                finished = checkWalkFinished(this.oid, pdu, vb);
                // 获取 table 类型某一个 标量时 ， 使用 getnext 会获取平行节点数据
                if (i == 1 && finished) {
                    ResponseEvent responseEvent = snmp.get(pdu, target);
                    VariableBinding variableBinding = responseEvent.getResponse().get(0);
                    Map map = Tool.MapUtil.newHashMap();
                    map.put("oid", variableBinding.getOid().toString());
                    map.put("value", variableBinding.getVariable().toString());
                    map.put("type", variableBinding.getVariable().getSyntaxString());
                    map.put("ip", ip);
                    map.put("port", port);
                    resultData.add(map);
                    break;
                } else {
                    if (!finished) {
                        pdu.setRequestID(new Integer32(0));
                        pdu.set(0, vb);

                        Map map = Tool.MapUtil.newHashMap();
                        map.put("oid", vb.getOid().toString());
                        map.put("value", vb.getVariable().toString());
                        map.put("type", vb.getVariable().getSyntaxString());
                        map.put("ip", ip);
                        map.put("port", port);
                        resultData.add(map);
                    } else {
                        // 获取结束
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error(e, "[snmpWalk] [error] [objetc: {}]", this.toString());
        }
        return resultData;
    }

    /**
     * @param value
     * @return 0 success
     * -1 unknow error
     * 1   PDU encoding is too big for the transport used.
     * 2   No such variable binding name.
     * 3   Bad value in variable binding
     * 4   The variable binding is read-only
     * 6   The variable binding is not accessible by the current MIB view,No assess
     * 17  The variable's value cannot be modified
     * For more details see: org.snmp4j.smi.OID.SnmpConstants.SNMP_ERROR_XXX
     */
    public int setPDU(String value) {
        int rv = 0;
        try {
            // initSnmp
            this.initSnmp();

            // createTarget
            Target target = this.createTarget(PDU.SET);

            // createPDU
            PDU pdu = createSetPDU(this.getSnmpCredential().getType(), PDU.SET, this.oid, value);

            ResponseEvent responseEvent = snmp.send(pdu, target);
            PDU response = responseEvent.getResponse();
            if (response == null) {
                log.error("response is null, request time out");
                return -1;
            } else {
                int errorIndex = response.getErrorIndex();
                if (errorIndex != 0) {
                    return response.getErrorStatus();
                }
            }
        } catch (Exception e) {
            log.error(e, "[setPDU] [error] [objetc: {}]", this.toString());
            rv = -1;
        }
        return rv;
    }

    public void close() {
        if (snmp != null) {
            try {
                snmp.close();
            } catch (IOException e) {
                snmp = null;
                log.error(e, "[close] [error]");
            }
        }
    }

    public Integer getTimeout() {
        return Tool.ObjectUtil.defaultIfNull(this.timeout, 1000);
    }

    public Integer getRetries() {
        return Tool.ObjectUtil.defaultIfNull(retries, 3);
    }

    public static void ValidateAuthForSnmpV3(Map<String, String> authInfo) {
        // 默认为noAuthNoPriv。可以是 noAuthNoPriv（不认证不加密），authNoPriv（认证不加密）或 authPriv（认证且加密）
        String securityLevel = authInfo.get("securityLevel");

        String username = authInfo.get("username");

        if (StringUtils.isEmpty(username)) {
            throw new NZException(RCode.MODULE_USERNAME_INVALID);
        }

        // 不为 null 时校验参数是否正确
        if (StringUtils.isNotEmpty(securityLevel)) {
            String pin = authInfo.get("authPin");

            String privPassword = authInfo.get("privPin");
            String privProtocol = authInfo.get("privProtocol");
            String authProtocol = authInfo.get("authProtocol");

            // 认证且加密
            if ("authPriv".equals(securityLevel)) {
                if (StringUtils.isEmpty(privPassword)) {
                    throw new NZException(RCode.MODULE_PRIVPASSWORD_ISNULL);
                }

                if (StringUtils.isEmpty(privProtocol) || !Arrays.asList(new String[]{"DES", "AES-128", "AES-192", "AES-256"}).contains(privProtocol)) {
                    throw new NZException(RCode.MODULE_PRIVPROTOCOL_INVALID);
                }

                if (StringUtils.isEmpty(pin)) {
                    throw new NZException(RCode.MODULE_PASSWORD_INVALID);
                }

                if (StringUtils.isEmpty(authProtocol) || (!"MD5".equals(authProtocol) && !"SHA".equals(authProtocol))) {
                    throw new NZException(RCode.MODULE_AUTHPROTOCOL_INVALID);
                }
                // 认证不加密
            } else if ("authNoPriv".equals(securityLevel)) {
                if (StringUtils.isEmpty(pin)) {
                    throw new NZException(RCode.MODULE_PASSWORD_INVALID);
                }

                if (StringUtils.isEmpty(authProtocol) || (!"MD5".equals(authProtocol) && !"SHA".equals(authProtocol))) {
                    throw new NZException(RCode.MODULE_AUTHPROTOCOL_INVALID);
                }
            } else if ("noAuthNoPriv".equals(securityLevel)) {
                // do nothhing
            } else {
                // 不在枚举范围内
                throw new NZException(RCode.MODULE_AUTHSECURITYLEVEL_INVALID);
            }
        }
    }


    @Override
    public String toString() {
        return "SnmpUtil1{" +
                "ip='" + this.ip + '\'' +
                ", port='" + this.port + '\'' +
                ", oid='" + this.oid + '\'' +
                ", snmpCredential=" + Tool.JSONUtil.toJsonStr(this.snmpCredential) +
                ", timeout=" + this.timeout +
                ", retries=" + this.retries +
                '}';
    }
}
