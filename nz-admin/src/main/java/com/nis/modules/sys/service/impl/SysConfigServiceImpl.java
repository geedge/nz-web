/**

 *

 *

 */

package com.nis.modules.sys.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.service.impl.PromserverProxyServiceImpl;
import com.nis.modules.promserver.utils.HttpClientUtils;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.redis.SysConfigRedis;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.shiro.ShiroUtils;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service("sysConfigService")
public class SysConfigServiceImpl extends ServiceImpl<SysConfigDao, SysConfigEntity> implements SysConfigService {

    private static Logger logger = LoggerFactory.getLogger(SysConfigServiceImpl.class);

    @Autowired
    private SysConfigRedis sysConfigRedis;
    @Autowired
    private PromserverProxyServiceImpl promserverProxyService;
	@Autowired
    private PromserverService promserverService;
	@Autowired
    private AlertMessageService alertMessageService;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
	@Value("${nezha.userId.expire}")
    private int expire;

    @Autowired
    private MailUtil mailUtil;

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String paramKey = (String)params.get("paramKey");

		IPage<SysConfigEntity> page = this.page(
			new Query<SysConfigEntity>().getPage(params),
			new QueryWrapper<SysConfigEntity>()
				.like(StringUtils.isNotBlank(paramKey),"param_key", paramKey)
				.eq("status", 1)
		);

		return new PageUtils(page);
	}
	
	@Override
	public void saveConfig(SysConfigEntity config) {
		this.save(config);
		sysConfigRedis.saveOrUpdate(config);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysConfigEntity config) {
		this.updateById(config);
		sysConfigRedis.saveOrUpdate(config);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateValueByKey(String key, String value) {
		baseMapper.updateValueByKey(key, value);
		sysConfigRedis.delete(key);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteBatch(Long[] ids) {
		for(Long id : ids){
			SysConfigEntity config = this.getById(id);
			sysConfigRedis.delete(config.getParamKey());
		}

		this.removeByIds(Arrays.asList(ids));
	}

	@Override
	public String getValue(String key) {
		SysConfigEntity config = sysConfigRedis.get(key);
		if(config == null){
			config = baseMapper.queryByKey(key);
			sysConfigRedis.saveOrUpdate(config);
		}

		return config == null ? null : config.getParamValue();
	}
	
	@Override
	public <T> T getConfigObject(String key, Class<T> clazz) {
		String value = getValue(key);
		if(StringUtils.isNotBlank(value)){
			return new Gson().fromJson(value, clazz);
		}

		try {
			return clazz.newInstance();
		} catch (Exception e) {
			throw new NZException("获取参数失败");
		}
	}
    @Override
    public Map getConfigListByTypes(String[] types, String[] paramKey) {

        List<String> paramKeys = paramKey == null || paramKey.length == 0 ? new ArrayList<>() : new ArrayList<>(Arrays.asList(paramKey));

        boolean typeFlag = false;
        if(types != null && types.length > 0){
            typeFlag = true;
            for (String type : types) {
                paramKeys.addAll(Constant.SYSCONFIG_TYPEWITHKEYS_MAP.get(type));
            }
        }

        List<SysConfigEntity> list = this.list(new QueryWrapper<SysConfigEntity>()
                .eq("status", 1)
                .in(CollectionUtils.isNotEmpty(paramKeys), "param_key", paramKeys));
        
        Map<String, String> paramKeyWithValueMap = new HashMap<>();
        for (SysConfigEntity entity: list) {
            paramKeyWithValueMap.put(entity.getParamKey(), entity.getParamValue());
        }


        // 组织返回数据
        Map resultMap = new HashMap(), values;

        // 如果type存在 则返回此type包含的所有内容
        if(typeFlag){
            List<String> keys;
            String value;
            for (String typeName : types) {
                values = new HashMap();

                keys = Constant.SYSCONFIG_TYPEWITHKEYS_MAP.get(typeName);
                for (String str : keys) {
                    value = paramKeyWithValueMap.get(str);
                    if(StringUtils.isNotEmpty(value)){
                        values.put(str, value);
                    }
                }
               
                resultMap.put(typeName, values);
            }
        }

        // 处理 paramKey参数查询
        if(paramKey != null && paramKey.length > 0){
            String value;
            Map paramValues = new HashMap();
            for (String key : paramKey) {
                value = paramKeyWithValueMap.get(key);
                if(StringUtils.isNotEmpty(value)){
                    paramValues.put(key, value);
                }
            }
            resultMap.put("paramKey",paramValues);
        }

        return resultMap;
    }


    @Override
    public R updateConfigs(Map<String, Object> param) {
        // 根据 类型 校验 需要添加/修改的 key , 不在常量中定义的不入库
        Set<String> keySet = Constant.SYSCONFIG_TYPEWITHKEYS_MAP.keySet();

        String saveType = "";
        Map<String, String> saveMap = null;
        List<String> removeKeys = new ArrayList<>();
        for (String type : keySet) {
            saveMap = (Map) param.get(type);

            if (ToolUtil.isNotEmpty(saveMap)) {
                saveType = type;
                List<String> keys = Constant.SYSCONFIG_TYPEWITHKEYS_MAP.get(type);

                for (String key : saveMap.keySet()) {
                    if (!keys.contains(key)) {
                        removeKeys.add(key);
                    }
                }
                break;
            }
        }

        if (ToolUtil.isNotEmpty(saveMap)) {

            if (CollectionUtils.isNotEmpty(removeKeys)) {
                // 去掉不在 定义范围内的数据
                for (String str : removeKeys) {
                    saveMap.remove(str);
                }
            }

            // 根据类别校验具体的参数
            switch (saveType) {
                case "basic":
                    this.validateBasicParams(saveMap);
                    this.updateEntity(saveMap);
                    break;
                case "email":
                    // 开启 email 开关
                    boolean emailEnableFlag = this.validateEmailParams(saveMap);
                    // 判断 test字段是否为 true
                    String emailTest = saveMap.get("test");
                    if (StringUtils.isEmpty(emailTest)) {
                        throw new NZException(RCode.SYS_CONFIG_TEST_ISNULL);
                    } else if ("true".equals(emailTest)) {
                        // 只做测试连接  不进行入库操作
                        this.testConnectionSmtpServer(saveMap);
                        return R.ok("Test connection to SMTP server succeeded");
                    } else {
                        if (emailEnableFlag) {
                            this.updateEntity(saveMap);
                        } else {
                            // email 开关关闭 只更新此开关
                            SysConfigEntity emailEnable = this.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, "email_enable"));
                            // 数据库目前没有此项
                            if (emailEnable == null) {
                                emailEnable = new SysConfigEntity();
                                emailEnable.setParamKey("email_enable");
                                emailEnable.setParamValue(Constant.SWITCH_CLOSE);
                            } else {
                                emailEnable.setParamValue(Constant.SWITCH_CLOSE);
                            }
                            this.saveOrUpdate(emailEnable);
                        }
                    }
                    break;
                case "terminal":
                    this.validateTerminalParams(saveMap);
                    this.updateEntity(saveMap);
                    break;
                case "ldap":
                    // 开启 ldap 开关
                    boolean ldapEnableFlag = this.validateLdapParams(saveMap);
                    // 判断 test字段是否为 true
                    String ldapTest = saveMap.get("test");
                    if (StringUtils.isEmpty(ldapTest)) {
                        throw new NZException(RCode.SYS_CONFIG_TEST_ISNULL);
                    } else if ("true".equals(ldapTest)) {
                        // 只做测试连接  不进行入库操作
                        R r = this.testConnectionLdapConfig(saveMap.get("ldap_dn"), saveMap.get("ldap_password"), saveMap.get("ldap_address"), saveMap.get("ldap_ou"));
                        return r;
                    } else {
                        if (ldapEnableFlag) {
                            this.updateEntity(saveMap);
                        } else {
                            SysConfigEntity ldapEnable = this.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", "ldap_enable"));
                            // 数据库目前没有此项
                            if (ldapEnable == null) {
                                ldapEnable = new SysConfigEntity();
                                ldapEnable.setParamKey("ldap_enable");
                                ldapEnable.setParamValue(Constant.SWITCH_CLOSE);
                            } else {
                                ldapEnable.setParamValue(Constant.SWITCH_CLOSE);
                            }
                            this.saveOrUpdate(ldapEnable);
                        }
                    }
                    break;
            }
        }
        return R.ok();
    }


    /**
     * 系统设置重置
     * @param param
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetConfig(Map<String, Object> param) throws Exception{
        String password = param.get("password").toString();
        List<String> types = (List<String>)param.get("type");
        try {
            if(ToolUtil.isEmpty(password)||ToolUtil.isEmpty(types)){
                throw new NZException(RCode.SYS_CONFIG_PASSWORDORTYPE_INVALID);
            }
            // 当前登录用户
            SysUserEntity userEntity = ShiroUtils.getUserEntity();
            // 原加密密码
            String oriPassword = userEntity.getPassword();
            // 加密盐值
            String salt = userEntity.getSalt();
            // 现加密密码
            String currentPassword = ShiroUtils.sha256(password,salt);
            if(!oriPassword.equals(currentPassword)){
                // 密码不对，禁止重置
                throw new NZException(RCode.SYS_CONFIG_PASSWORD_ERROR);
            }
            for (String type: types){
                if(!ArrayUtils.contains(Constant.SYSCONFIG_RESET_TYPE,type)){
                    throw new NZException(RCode.SYS_CONFIG_PASSWORDORTYPE_INVALID);
                }
                if("metric".equals(type)){
                    resetMetric();
                }else if("alert".equals(type)){
                    resetAlert();
                }else {
                    resetEnterData();
                }
            }
        }catch (Exception e){
            throw e;
        }
    }

    /**
     * 重置metric
     */
    private void resetMetric() throws Exception{
        String seriesUri = "/api/v1/admin/tsdb/delete_series";
        String tombstonesUri = "/api/v1/admin/tsdb/clean_tombstones";
        // 获取正常的监控服务列表
        List<Promserver> promserverList = promserverService.list(
                new LambdaQueryWrapper<Promserver>().eq(Promserver::getStatus,1));
        if(promserverList.size()>0){
        	final String key=URLEncoder.encode("{__name__=~'.+'}","UTF-8");
            promserverList.forEach(promserver -> {
                HttpClientUtils.httpPost("http://"+promserver.getHost()+":"+promserver.getPort()+seriesUri,"match[]="+key);
                HttpClientUtils.httpPost("http://"+promserver.getHost()+":"+promserver.getPort()+tombstonesUri,"");
            });
        }
    }

    /**
     * 重置alert
     */
    private void resetAlert() throws Exception{
        alertMessageService.remove(new LambdaQueryWrapper<AlertMessageEntity>());
    }

    /**
     * 清除用户输入数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetEnterData() throws Exception{
        try {
			// redis写入当前用户id
			Long userId = ShiroUtils.getUserId();
			redisTemplate.opsForValue().set("reset",String.valueOf(userId),expire, TimeUnit.SECONDS);
			// 删除用户输入的数据
			String jsonTables = this.getValue(Constant.RESET_TABLE_INFOS);
			if(StringUtils.isNotEmpty(jsonTables)) {
				List<String> tables = (List<String>) JSONArray.parse(jsonTables);
			    Arrays.stream(tables.toArray()).forEach(table->{
			    	this.baseMapper.clearData((String)table);
			    });
			}
			// 删除redis中key=reset的数据
			redisTemplate.delete("reset");
			logger.info("nezha reset success");
		} catch (Exception e) {
			logger.error("nezha reset fail",e);
			throw new NZException(RCode.SYS_CONFIG_RESET_FAIL);
		}
    }


    private void updateEntity(Map<String, String> saveMap) {
        // test字段不进行保存入库
        saveMap.remove("test");
        // 当前记录
        List<SysConfigEntity> configs = this.list(new QueryWrapper<SysConfigEntity>().lambda().in(SysConfigEntity::getParamKey, saveMap.keySet()));

        // saveMap 中存在需要新增的数据
        if (configs.size() != saveMap.size()) {
            List<String> paramKeys = new ArrayList<>();
            for (SysConfigEntity entity : configs) {
                paramKeys.add(entity.getParamKey());
            }
            SysConfigEntity sysConfigEntity;
            for (String key : saveMap.keySet()) {
                if (!paramKeys.contains(key)) {
                    sysConfigEntity = new SysConfigEntity();
                    sysConfigEntity.setParamKey(key);
                    configs.add(sysConfigEntity);
                }
            }
        }
        // 更改param_value 值
        for (SysConfigEntity entity : configs) {
            entity.setParamValue(saveMap.get(entity.getParamKey()));
        }

        this.saveOrUpdateBatch(configs);

        // 更新配置市区值
        SysConfigEntity timezone = configs.stream().filter(config -> config.getParamKey().equals("timezone") ? true : false).findAny().orElse(null);
        if (timezone != null && !timezone.getParamValue().equals(DateUtil.configTimeZone + "")) {
            DateUtil.configTimeZone = Integer.valueOf(timezone.getParamValue());
        }
    }


    /**
     * 校验 Basic 更新的参数
     * @param saveMap
     */
    private void validateBasicParams(Map<String, String> saveMap) {
        String assetPingSwitch = saveMap.get("asset_ping_switch");
        String assetPingInterval = saveMap.get("asset_ping_interval");
        String scrapeInterval = saveMap.get("scrape_interval");
        String storageLocalRetention = saveMap.get("storage_local_retention");
        String timezone = saveMap.get("timezone");

        ValidateUtils.is(saveMap.get("alert_api")).notNull(RCode.SYS_CONFIG_ALERTAPI_ISNULL)
                .and(saveMap.get("node_exporter_target_path")).notNull(RCode.SYS_CONFIG_NODETARGETPATH_ISNULL)
                .and(assetPingSwitch).notNull(RCode.SYS_CONFIG_ASSETPINGSWITCH_ISNULL)
                //.and(assetPingInterval).notNull(RCode.SYS_CONFIG_ASSETPINGINTERVAL_ISNULL)
                .and(scrapeInterval).notNull(RCode.SYS_CONFIG_SCRAPEINTERVAL_ISNULL)
                .and(storageLocalRetention).notNull(RCode.SYS_CONFIG_LOCALRETENTION_ISNULL)
                .and(saveMap.get("system_name")).notNull(RCode.SYS_CONFIG_SYSTEMNAME_ISNULL)
                .and(timezone).notNull(RCode.SYS_CONFIG_TIMEZONE_ISNULL);

        // 校验开关类型值
        this.validateSwitchValue(assetPingSwitch);

        // 校验数值类型参数是否正确
        /*if (ToolUtil.isNotEmpty(assetPingInterval) && CommonUtils.toInt(assetPingInterval) == null) {
            throw new NZException(RCode.SYS_CONFIG_ASSETPINGINTERVAL_INVALIDE);
        }*/

        if (CommonUtils.toInt(scrapeInterval) == null) {
            throw new NZException(RCode.SYS_CONFIG_SCRAPEINTERVAL_INVALIDE);
        }

        if (CommonUtils.toInt(storageLocalRetention) == null) {
            throw new NZException(RCode.SYS_CONFIG_LOCALRETENTION_INVALIDE);
        }

        Integer timezoneIntValue = CommonUtils.toInt(timezone);
        if (timezoneIntValue == null) {
            throw new NZException(RCode.SYS_CONFIG_TIMEZONE_INVALIDE);
        }

        if (timezoneIntValue < -12 || timezoneIntValue > 12) {
            throw new NZException(RCode.SYS_CONFIG_TIMEZONE_INTERVAL);
        }
    }

    private boolean validateEmailParams(Map<String, String> saveMap) {
        // 是否启用邮件
        String emailEnable = saveMap.get("email_enable");

        ValidateUtils.is(emailEnable).notNull(RCode.SYS_CONFIG_EMAILENABLE_ISNULL);

        this.validateSwitchValue(emailEnable);

        // 启用邮件 校验相关参数
        if (emailEnable.equals(Constant.SWITCH_OPEN)) {
            String emailSmtpHost = saveMap.get("email_smtp_host");
            String emailSmtpPort = saveMap.get("email_smtp_port");
            String emailSslFlag = saveMap.get("email_ssl_flag");
            String emailTlsFlag = saveMap.get("email_tls_flag");

            ValidateUtils.is(emailSmtpHost).notNull(RCode.SYS_CONFIG_EMAILSMTPHOST_ISNULL) // .regex(Constant.REGEX_DOMAIN, RCode.SYS_CONFIG_EMAILSMTPHOST_FORMAT)
                    .and(emailSmtpPort).notNull(RCode.SYS_CONFIG_EMAILSMTPPORT_ISNULL)
                    .and(saveMap.get("email_smtp_account")).notNull(RCode.SYS_CONFIG_EMAILSMTPACCOUNT_ISNULL).email(RCode.SYS_CONFIG_EMAILSMTPACCOUNT_FORMAT)
                    .and(saveMap.get("email_smtp_password")).notNull(RCode.SYS_CONFIG_EMAILSMTPPASSWORD_ISNULL)
                    //.and(saveMap.get("email_send_account")).notNull(RCode.SYS_CONFIG_EMAILSENDACCOUNT_ISNULL)
                    //.and(saveMap.get("email_test_reciver")).notNull(RCode.SYS_CONFIG_EMAILTESTRECIVER_ISNULL).email(RCode.SYS_CONFIG_EMAILTESTRECIVER_FORMAT)
                    .and(emailSslFlag).notNull(RCode.SYS_CONFIG_EMAILSSLFLAG_FORMAT)
                    .and(emailTlsFlag).notNull(RCode.SYS_CONFIG_EMAILTLSFLAG_FORMAT);

            String emailTest = saveMap.get("test");
            if (StringUtils.isEmpty(emailTest)) {
                throw new NZException(RCode.SYS_CONFIG_TEST_ISNULL);
            } else if ("true".equals(emailTest)) {
                // 测试连接时 text_reciver为必填
                ValidateUtils.is(saveMap.get("email_test_reciver")).notNull(RCode.SYS_CONFIG_EMAILTESTRECIVER_ISNULL).email(RCode.SYS_CONFIG_EMAILTESTRECIVER_FORMAT);
            }

            String emailSendAccount = saveMap.get("email_send_account");
            if (StringUtils.isNotEmpty(emailSendAccount)) {
                ValidateUtils.is(emailSendAccount).email(RCode.SYS_CONFIG_EMAILSENDCCOUNT_FORMAT);
            }

            if (!CommonUtils.checkIp(emailSmtpHost) && !CommonUtils.checkDomain(emailSmtpHost)) {
                throw new NZException(RCode.SYS_CONFIG_EMAILSMTPHOST_FORMAT);
            }

            if (CommonUtils.toInt(emailSmtpPort) == null) {
                throw new NZException(RCode.SYS_CONFIG_EMAILSMTPPORT_INVALIDE);
            }

            if (!CommonUtils.checkPort(emailSmtpPort)) {
                throw new NZException(RCode.SYS_CONFIG_EMAILSMTPPORT_ERROR);
            }

            this.validateSwitchValue(emailSslFlag);
            this.validateSwitchValue(emailTlsFlag);
            return true;
        }
        return false;
    }


    /**
     * 校验 Terminal 更新的参数
     * @param saveMap
     */
    private void validateTerminalParams(Map<String, String> saveMap) {
        String maxTerminalNum = saveMap.get("max_terminal_num");

        if (maxTerminalNum == null) {
            throw new NZException(RCode.SYS_CONFIG_MAXTERMINALNUM_ISNULL);
        }

        Integer anInt = CommonUtils.toInt(maxTerminalNum);
        if (anInt == null || anInt < 1) {
            throw new NZException(RCode.SYS_CONFIG_MAXTERMINALNUM_INVALIDE);
        }
    }

    /**
     * 校验 Ldap 更新的参数
     * @param saveMap
     * @return
     */
    private boolean validateLdapParams(Map<String, String> saveMap) {
        String ldapEnable = saveMap.get("ldap_enable");
        ValidateUtils.is(ldapEnable).notNull(RCode.SYS_CONFIG_LDAPENABLE_ISNULL);

        this.validateSwitchValue(ldapEnable);

        // 启用ldap 校验相关参数
        if (ldapEnable.equals(Constant.SWITCH_OPEN)) {
            ValidateUtils.is(saveMap.get("ldap_address")).notNull(RCode.SYS_CONFIG_LDAPADDRESS_ISNULL)
                    .and(saveMap.get("ldap_dn")).notNull(RCode.SYS_CONFIG_LDAPDN_ISNULL)
                    //.and(saveMap.get("ldap_password")).notNull(RCode.SYS_CONFIG_LDAPPASSWORD_ISNULL)
                    //.and(saveMap.get("ldap_ou")).notNull(RCode.SYS_CONFIG_LDAPOU_ISNULL)
                    .and(saveMap.get("ldap_user_filter")).notNull(RCode.SYS_CONFIG_LDAPUSERFILTER_ISNULL)
                    .and(saveMap.get("ldap_mapping")).notNull(RCode.SYS_CONFIG_LDAPMAPPING_ISNULL);
            String ldapMapping = saveMap.get("ldap_mapping");
            try {
				JSONObject.parseObject(ldapMapping);
			} catch (Exception e) {
				throw new NZException(RCode.SYS_CONFIG_LDAPMAPPING_FORMAT_ERROR);
			}
            return true;
        }
        return false;
    }


    // 校验开关类型值是否符合规范 on or off
    private void validateSwitchValue(String switchValue) {
        if (!switchValue.equals(Constant.SWITCH_OPEN) && !switchValue.equals(Constant.SWITCH_CLOSE)) {
            throw new NZException(RCode.SYS_CONFIG_SWITCHVAUE_INVALIDE);
        }
    }

    public void testConnectionSmtpServer(Map<String, String> map) {
        String smtpHost = map.get("email_smtp_host");
        String smtpPort = map.get("email_smtp_port");

        Properties prop = new Properties();
        prop.setProperty("mail.snmp.host", smtpHost);
        prop.setProperty("mail.smtp.port", smtpPort);
        prop.setProperty("mail.transport.protocol", "smtp");
        prop.setProperty("mail.smtp.auth", "true");
        // 设置超时时间为10秒
        prop.setProperty("mail.smtp.timeout", Constant.CONNECTION_SMTP_TIMEOUT);

        boolean sslFlag = Constant.SWITCH_OPEN.equals(map.get("email_ssl_flag"));
        boolean tlsFlag = Constant.SWITCH_OPEN.equalsIgnoreCase(map.get("email_tls_flag"));
        if(sslFlag || tlsFlag){
            prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            prop.put("mail.smtp.socketFactory.fallback", "false");
            prop.put("mail.smtp.starttls.enable", "true");
        }

        if (sslFlag) {
            prop.put("mail.smtp.socketFactory.port", "465");
            prop.remove("mail.smtp.port");
        }

        if (tlsFlag) {
            prop.put("mail.smtp.starttls.enable", "true");
            prop.put("mail.smtp.port", "587");
        }

        // 登录账户
        String smtpAccount = map.get("email_smtp_account");
        // 发件人
        String sendAccount = map.get("email_send_account");
        if (StringUtils.isEmpty(sendAccount)) {
            sendAccount = smtpAccount;
        }

        try {
            String receicer = map.get("email_test_reciver");
            String password = map.get("email_smtp_password");

            mailUtil.sendHTML(smtpHost, prop, smtpAccount, sendAccount, password, receicer, Constant.TESTEMAIL_SEND_TITLE, this.getSendMailContent(sendAccount));
        } catch (IOException e) {
            logger.error("获取测试发送邮件模板失败", e);
            throw new NZException(RCode.SYS_CONFIG_TESTEMAIL_TEMPLATE_NOTFOUND);
        } catch (TemplateException e) {
            logger.error("静态化模板内容失败", e);
            throw new NZException(RCode.SYS_CONFIG_TESTEMAIL_TEMPLATE_FAILED);
        }
    }

    /**
     *测试连接是否可用 ldap
     * @param dn
     * @param password
     * @param address
     */
    public R testConnectionLdapConfig(String dn, String password, String address, String ou) {
        InitialDirContext initialDirContext = null;
        try {
            Hashtable<String, String> environment = new Hashtable<>();
            environment.put(Context.SECURITY_AUTHENTICATION, "simple");
            environment.put(Context.SECURITY_PRINCIPAL, dn);
            environment.put(Context.SECURITY_CREDENTIALS, password);
            environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            environment.put(Context.PROVIDER_URL, address);
            initialDirContext = new InitialDirContext(environment);
            
            // 校验ou属性
            if (StringUtils.isNotBlank(ou)) {
                String[] split = dn.split(",");
                String base = split[split.length - 2] + "," + split[split.length - 1];
                base = "ou=" + ou + "," + base;

                boolean ouFlag = true;
                Attributes attributes = initialDirContext.getAttributes(base);
                NamingEnumeration<String> nEnum = attributes.getIDs();
                for (; nEnum.hasMore(); ) {
                    String attrID = nEnum.next();
                    Attribute attribute = attributes.get(attrID);
                    if (attribute.toString().startsWith("ou:")) {
                        if (attribute.toString().contains(ou)) {
                            ouFlag = false;
                        }
                    }
                }
                if (ouFlag) {
                    return R.error(RCode.SYS_CONFIG_LDAP_OU_NOT_EXIST);
                }
            }
            
        } catch (AuthenticationException e) {
            // 账户类异常 造成原因： 用户名或者密码错误 也就是 dn和password
            logger.error("ldap service config connection failed : username/password wrong", e);
            return R.error(RCode.SYS_CONFIG_LDAP_AUTHENTICATIONFAIL);
        } catch (NamingException e) {
            // 连接类异常 造成原因 1. 服务未开启 2. 服务器防火墙等其他原因造成无法连接到服务器
            logger.error("ldap service config connection failed", e);
            return R.error(RCode.SYS_CONFIG_LDAP_CONNECTIONFAILED);
        } catch (Exception e) {
            logger.error("ldap service config connection failed", e);
            return R.error(RCode.SYS_CONFIG_LDAP_CONNECTIONFAILED);
        } finally {
            if (initialDirContext != null) {
                try {
                    initialDirContext.close();
                } catch (NamingException e) {
                    logger.error("关闭ldapContext失败",e);
                }
            }
        }
        return R.ok("Test connection to LDAP server succeeded");
    }

    // 获取发送邮件内容
    public String getSendMailContent(String sendEmail) throws IOException, TemplateException {
        // 创建配置类
        Configuration configuration = new Configuration(Configuration.getVersion());
        // 加载模板文件
        String templateString = this.getValue(Constant.SYSCONFIG_KEY_TESTEMAIL_SEND_TEMPLATE);
        //创建模板加载器
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        // 存入模板
        templateLoader.putTemplate("template", templateString);  //template  = 虚拟名称, 用来当作获取静态文件的key
        //加载模板加载器
        configuration.setTemplateLoader(templateLoader);
        //得到模板
        Template template = configuration.getTemplate("template", "utf-8");
        //创建模型数据
       /* Map<String, Object> modelMap = new HashMap<String, Object>();
        modelMap.put("sendEmail", sendEmail);*/
        //执行静态化 方式1  获取静态化内容
        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, null);
        //静态化内容;
        return content;
    }
}
