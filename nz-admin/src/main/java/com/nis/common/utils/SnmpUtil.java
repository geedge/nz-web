package com.nis.common.utils;

import com.nis.common.exception.NZException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnmpUtil {

    private static Logger logger = LoggerFactory.getLogger(SnmpUtil.class);

    // 默认版本 v2
    public static final int DEFAULT_VERSION = SnmpConstants.version2c;
    public static final String DEFAULT_PROTOCOL = "udp";
    public static final long DEFAULT_TIMEOUT = 1000L;
    public static final int DEFAULT_RETRY = 3;

    // 默认团体名
    public static final String DEFAULT_COMMUNITY = "public";

    /**
     * 获取target
     *
     * @param version
     * @param community
     * @param auth
     * @param ip
     * @param port
     * @param snmp
     * @return
     */
    public static Target getTatget(Integer version, String community, Map<String, String> auth, String ip, Integer port, Snmp snmp) {
        Target target = null;
        if (version == SnmpConstants.version3) {
            // 添加用户
            OID authProtocolOid = null;
            String authProtocol = auth.get("authProtocol");
            if ("MD5".equals(authProtocol)) {
                authProtocolOid = AuthMD5.ID;
            } else if ("SHA".equals(authProtocol)) {
                authProtocolOid = AuthSHA.ID;
            }

            OID privProtocolOid = null;
            String privProtocol = auth.get("privProtocol");
            if ("DES".equals(privProtocol)) {
                privProtocolOid = PrivDES.ID;
            } else if ("AES".equals(privProtocol)) {
                privProtocolOid = PrivAES128.ID;
            }

            String privPassword = auth.get("privPassword");
            OctetString octetPrivPassword = null;
            if(StringUtils.isNotEmpty(privPassword)){
                octetPrivPassword = new OctetString(privPassword);
            }

            String password = auth.get("password");
            OctetString octetpassword = null;
            if(StringUtils.isNotEmpty(password)){
                octetpassword = new OctetString(password);
            }

            OctetString username = new OctetString(auth.get("username"));
            UsmUser user = new UsmUser(username, authProtocolOid, octetpassword, privProtocolOid, octetPrivPassword);
            snmp.getUSM().addUser(username, user);
//            snmp.getUSM().addUser(user);
            target = new UserTarget();

            String level = auth.get("securityLevel");
            // 不为空时设置安全级别 如果为空则不处理 默认的安全级别为noAuthNoPriv 不认证不加密
            if(StringUtils.isNotEmpty(level)){
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
                }
            }
            ((UserTarget) target).setSecurityName(username);
            target.setVersion(SnmpConstants.version3);
        } else {
            target = new CommunityTarget();
            target.setVersion(SnmpConstants.version2c);
            // 如果没有设置团体名 默认使用public
            if (StringUtils.isEmpty(community)) {
                community = DEFAULT_COMMUNITY;
            }
            ((CommunityTarget) target).setCommunity(new OctetString(community));
        }
        // 超时时间
        target.setTimeout(DEFAULT_TIMEOUT);
        // 重试次数
        target.setRetries(DEFAULT_RETRY);

        Address address = GenericAddress.parse(DEFAULT_PROTOCOL + ":" + ip + "/" + port);
        target.setAddress(address);

        return target;
    }

    /**
     * snmpget or getnext
     *
     * @param ip
     * @param port
     * @param version
     * @param community
     * @param oid
     * @param auth
     * @param operation
     * @return
     * @throws IOException
     */
    public static List snmpGetOrGetNext(String ip, Integer port, Integer version, String community, String oid, Map<String, String> auth, String operation) throws IOException {
        List<Map> list = new ArrayList<>();
        Snmp snmp = null;
        DefaultUdpTransportMapping transport = null;
        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            // 如果是v3版本
            if (version == SnmpConstants.version3) {
                // 设置安全模式
                USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
                SecurityModels.getInstance().addSecurityModel(usm);
            }

            Target target = getTatget(version, community, auth, ip, port, snmp);
            snmp.listen();

            // 根据版本不同获取不同的pdu对象
            PDU pdu = getPdu(version);
            pdu.add(new VariableBinding(new OID(oid)));

            if (Constant.SnmpOperationType.GET.getValue().equals(operation.toLowerCase())) {
                pdu.setType(PDU.GET);
            } else if (Constant.SnmpOperationType.GETNEXT.equals(operation.toLowerCase())) {
                pdu.setType(PDU.GETNEXT);
            }

            ResponseEvent respEvent = snmp.send(pdu, target);
            PDU response = respEvent.getResponse();
            if (response == null) {
                logger.error("response is null, request time out,ipAddress is {},oid is {}", ip + ":" + port, oid);
                return null;
            } else {
                VariableBinding vb = null;
                Map map;
                for (int i = 0; i < response.size(); i++) {
                    map = new HashMap();
                    vb = response.get(i);
                    map.put("oid", vb.getOid().toString());
                    map.put("value", vb.getVariable().toString());
                    map.put("type", vb.getVariable().getSyntaxString());
                    map.put("ip", ip);
                    map.put("port", port);
                    list.add(map);
                }
            }
            logger.info("SNMP GET one OID value finished !");
            return list;
        } finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (IOException e) {
                    logger.error("关闭snmp失败", e);
                }
            }
            if (transport != null) {
                try {
                    transport.close();
                } catch (IOException e) {
                    logger.error("关闭transport失败", e);
                }
            }
        }
    }

    private static PDU getPdu(Integer version) {
        if (version == SnmpConstants.version3) {
            ScopedPDU pdu = new ScopedPDU();
            // 如果agent上设定的contextEngineId和snmpEngineId不一致 这里需要设置引擎id
            // pdu.setContextEngineID(new OctetString(""));
            // 如果agent上设定了contextName 这里需要设置contextName
            // pdu.setContextName(new OctetString(""));
            return pdu;
        } else {
            PDU pdu = new PDU();
            return pdu;
        }
    }

    /**
     * snmpwalk
     *
     * @param ip
     * @param port
     * @param version
     * @param community
     * @param oid
     * @param auth
     * @return
     * @throws IOException
     */
    public static List<Map> snmpWalk(String ip, Integer port, Integer version, String community, String oid, Map<String, String> auth) throws IOException {
        List<Map> resultData = new ArrayList<>();
        Snmp snmp = null;
        TransportMapping transport = null;
        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);

            // 如果是v3版本
            if (version == SnmpConstants.version3) {
                // 设置安全模式
                USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
                SecurityModels.getInstance().addSecurityModel(usm);
            }

            Target target = getTatget(version, community, auth, ip, port, snmp);
            snmp.listen();

            PDU pdu = getPdu(version);

            OID targetOID = new OID(oid);
            pdu.add(new VariableBinding(targetOID));

            // 结束标识
            boolean finished = false;

            Map map;
            while (!finished) {
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
                finished = checkWalkFinished(targetOID, pdu, vb);
                if (!finished) {
                    pdu.setRequestID(new Integer32(0));
                    pdu.set(0, vb);

                    map = new HashMap();
                    map.put("oid", vb.getOid().toString());
                    map.put("value", vb.getVariable().toString());
                    map.put("type", vb.getVariable().getSyntaxString());
                    map.put("ip", ip);
                    map.put("port", port);
                    resultData.add(map);
                } else {
                    // 获取结束
                    snmp.close();
                }
            }
        } finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (Exception e) {
                    logger.error("关闭snmp失败", e);
                }
            }
            if (transport != null) {
                try {
                    transport.close();
                } catch (IOException e) {
                    logger.error("关闭transport失败", e);
                }
            }
        }
        return resultData;
    }

    /**
     * 判断walk结束标识
     *
     * @param targetOID
     * @param pdu
     * @param vb
     * @return
     */
    private static boolean checkWalkFinished(OID targetOID, PDU pdu, VariableBinding vb) {
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


    /**
     * snmpSet
     *
     * @param ip
     * @param port
     * @param version
     * @param community
     * @param oid
     * @param auth
     * @param value
     * @return
     * @throws IOException
     */
    public static R setPDU(String ip, Integer port, Integer version, String community, String oid, Map<String, String> auth, String value) throws IOException {
        Snmp snmp = null;
        DefaultUdpTransportMapping transport = null;
        try {
            PDU pdu = getPdu(version);
            pdu.add(new VariableBinding(new OID(oid), new OctetString(value)));
            pdu.setType(PDU.SET);

            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);

            // 如果是v3版本
            if (version == SnmpConstants.version3) {
                // 设置安全模式
                USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
                SecurityModels.getInstance().addSecurityModel(usm);
            }

            Target target = getTatget(version, community, auth, ip, port, snmp);
            snmp.listen();
            ResponseEvent responseEvent = snmp.send(pdu, target);
            PDU response = responseEvent.getResponse();
            if (response == null) {
                logger.error("response is null, request time out,ipAddress is {},oid is {}", ip + ":" + port, oid);
                R.error(RCode.SNMP_MIB_BROWSER_TIMEOUT);
            } else {
                int errorIndex = response.getErrorIndex();
                if(errorIndex != 0){
                    int errorStatus = response.getErrorStatus();
                    switch(errorStatus){
                        case 1:{
                            return R.error(RCode.SNMP_MIB_TOOBIG);
                        }
                        case 2:{
                            return R.error(RCode.SNMP_MIB_NOSUCHNAME);
                        }
                        case 3:{
                            return R.error(RCode.SNMP_MIB_BADVALUE);
                        }
                        case 4:{
                            return R.error(RCode.SNMP_MIB_READONLY);
                        }
                        case 6:{
                            return R.error(RCode.SNMP_MIB_NOACCESS);
                        }
                        case 17:{
                            return R.error(RCode.SNMP_MIB_NOTWRITABLE);
                        }
                    }
                }
                return R.ok();
            }
        } finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (Exception e) {
                    logger.error("关闭snmp失败", e);
                }
            }
            if (transport != null) {
                try {
                    transport.close();
                } catch (IOException e) {
                    logger.error("关闭transport失败", e);
                }
            }
        }
        return R.ok();
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
            String password = authInfo.get("password");

            String privPassword = authInfo.get("privPassword");
            String privProtocol = authInfo.get("privProtocol");
            String authProtocol = authInfo.get("authProtocol");


            if ("authPriv".equals(securityLevel)) { // 认证且加密
                if (StringUtils.isEmpty(privPassword)) {
                    throw new NZException(RCode.MODULE_PRIVPASSWORD_ISNULL);
                }

                if (StringUtils.isEmpty(privProtocol) || (!"DES".equals(privProtocol) && !"AES".equals(privProtocol))) {
                    throw new NZException(RCode.MODULE_PRIVPROTOCOL_INVALID);
                }

                if (StringUtils.isEmpty(password)) {
                    throw new NZException(RCode.MODULE_PASSWORD_INVALID);
                }

                if (StringUtils.isEmpty(authProtocol) || (!"MD5".equals(authProtocol) && !"SHA".equals(authProtocol))) {
                    throw new NZException(RCode.MODULE_AUTHPROTOCOL_INVALID);
                }
            } else if ("authNoPriv".equals(securityLevel)) { // 认证不加密
                if (StringUtils.isEmpty(password)) {
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
}
