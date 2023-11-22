package com.nis.modules.sys.service.impl;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.service.MonitorEventLogService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.sys.dao.SysConfDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.service.SysService;
import com.nis.modules.sys.shiro.ShiroUtils;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service("sysConfService")
public class SysConfServiceImpl extends ServiceImpl<SysConfDao, SysConfigEntity> implements SysConfService {

    public static final String[] BASIC_KEYS = {"system_name","theme","language","date_format","system_bgImg","system_favicon","system_logo", "default_cabinet_usize", "map_center_config", "timezone", "session_timeout", "snmp_trap_listen_port", "unsaved_change", "mfa_auth_enable","pin_policy"};

    public static final String[] BASIC_REQUIRED = {"system_name", "map_center_config", "timezone", "session_timeout", "snmp_trap_listen_port", "unsaved_change", "mfa_auth_enable","pin_policy"};

    public static final String[] BASIC_INTEGER = {"default_cabinet_usize", "snmp_trap_listen_port", "session_timeout", "mfa_auth_enable"};

    public static final String[][]  BASIC_DEPEND={};

    public static final String[] MONITOR_KEYS = {"alert_api", "prometheus_federation_enabled", "default_scrape_interval", "default_scrape_timeout", "asset_ping_interval", "asset_ping_from", "metrics_storage_retention", "metrics_storage_type", "metrics_storage_s3_endpoint", "metrics_storage_s3_bucket", "metrics_storage_s3_access_key", "metrics_storage_s3_secret_access_key", "metrics_query_max_series", "logs_storage_retention", "logs_storage_type", "logs_storage_s3_endpoint", "logs_storage_s3_bucket", "logs_storage_s3_access_key", "logs_storage_s3_secret_access_key", "logs_query_range_default_limit", "interface_name"};

    public static final String[] MONITOR_REQUIRED = {"prometheus_federation_enabled", "default_scrape_interval", "default_scrape_timeout", "asset_ping_interval", "asset_ping_from", "metrics_storage_retention", "metrics_storage_type", "metrics_query_max_series", "logs_storage_retention", "logs_storage_type", "logs_query_range_default_limit"};

    public static final String[] MONITOR_INTEGER = {"prometheus_federation_enabled", "default_scrape_interval", "default_scrape_timeout", "asset_ping_interval", "asset_ping_from", "metrics_storage_retention", "metrics_storage_type", "metrics_query_max_series", "logs_storage_retention", "logs_storage_type", "logs_query_range_default_limit"};

    public static final String[][] MONITOR_DEPEND = {
            {"metrics_storage_s3_endpoint", "metrics_storage_type", "2"},
            {"metrics_storage_s3_bucket", "metrics_storage_type", "2"},
            {"metrics_storage_s3_access_key", "metrics_storage_type", "2"},
            {"metrics_storage_s3_secret_access_key", "metrics_storage_type", "2"},
            {"logs_storage_s3_endpoint", "logs_storage_type", "2"},
            {"logs_storage_s3_bucket", "logs_storage_type", "2"},
            {"logs_storage_s3_access_key", "logs_storage_type", "2"},
            {"logs_storage_s3_secret_access_key", "logs_storage_type", "2"}
    };

    public static final String[] EMAIL_KEYS={"email_test","email_enable","email_host","email_port","email_timeout","email_auth_account","email_auth_password","email_send_account","email_test_account","email_security_type"};

    public static final String[] EMAIL_REQUIRED={"email_enable","email_host","email_port","email_timeout","email_security_type"};

    public static final String[] EMAIL_INTEGER={"email_port","email_timeout"};

    public static final String[][] EMAIL_DEPEND={{"email_test_account","email_test","true"}};

    public static final String[] SECURITY_TYPES={"NONE","SSL","TLS"};

    public static final String[] SWITCH_TYPES={"on","off"};

    public static final String[] TERMINAL_KEYS={"terminal_timeout","terminal_telnet_user_tip","terminal_telnet_pin_tip","terminal_record_local_retention","terminal_storage_type","terminal_storage_s3_endpoint","terminal_storage_s3_bucket","terminal_storage_s3_access_key","terminal_storage_s3_secret_access_key"};

    public static final String[] TERMINAL_REQUIRED={"terminal_telnet_user_tip","terminal_telnet_pin_tip","terminal_record_local_retention","terminal_storage_type"};

    public static final String[] TERMINAL_S3_REQUIRED={"terminal_storage_s3_endpoint","terminal_storage_s3_bucket","terminal_storage_s3_access_key","terminal_storage_s3_secret_access_key"};

    public static final String[] TERMINAL_INTEGER={"terminal_timeout","terminal_record_local_retention","terminal_storage_type"};

    public static final String[] LDAP_KEYS={"ldap_address","ldap_dn","ldap_password","ldap_ou","ldap_user_filter","ldap_mapping","ldap_timeout","ldap_enable"};

    public static final String[] LDAP_REQUIRED={"ldap_address","ldap_dn","ldap_user_filter","ldap_mapping","ldap_enable","ldap_timeout"};

    private static final Log logger = Log.get();

    @Autowired
    private AgentService agentService;
    @Autowired
    private AlertMessageService alertMessageService;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Value("${nezha.userId.expire}")
    private int expire;

    @Autowired
    private MailUtil mailUtil;

    @Lazy
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private SysService sysService;

    @Autowired
    private MonitorEventLogService eventLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLdap(Map<String, String> params) {
        Map<String, String> result = this.validateKeys(LDAP_KEYS, params);

        String enable = params.get("ldap_enable");
        if(StrUtil.isEmpty(enable)){
            throw new NZException(String.format("%s can not be null","ldap_enable"),573011);
        }
        //过滤非法的key
        if(StrUtil.equals(enable,"on")){
            this.validateRequired(result,LDAP_REQUIRED,null);
            //校验开关值是否合法
            if(!Arrays.asList(SWITCH_TYPES).contains(params.get("ldap_enable"))){
                throw new NZException(RCode.SYS_CONFIG_SWITCHVAUE_INVALIDE);
            }

            this.testConnectionLdapConfig(params);
           	// ldap密码入库前进行加密
            String pin = result.get("ldap_password");
            result.put("ldap_password", StrUtil.str(Base64.getEncoder().encode(pin.getBytes()),"UTF-8"));
              
        }
        this.saveOrUpdateConfsBatch(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTerminal(Map<String, String> params) {
        Map<String, String> result = this.validateKeys(TERMINAL_KEYS, params);
        this.validateRequired(result,TERMINAL_REQUIRED,null);
        //校验integer类型
        this.validateInteger(TERMINAL_INTEGER,result);
        //校验S3存储参数
        if(StrUtil.equals(String.valueOf(params.get("terminal_storage_type")), "2")){
            this.validateRequired(result,TERMINAL_S3_REQUIRED,null);
        }
        this.saveOrUpdateConfsBatch(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEmail(Map<String, String> params) {
        Map<String, String> result = this.validateKeys(EMAIL_KEYS, params);
        this.validateRequired(result,EMAIL_REQUIRED,EMAIL_DEPEND);
        //校验integer类型
        this.validateInteger(EMAIL_INTEGER,result);
        String enable = params.get("email_enable");
        if(!Arrays.asList(SWITCH_TYPES).contains(enable)){
            throw new NZException(RCode.SYS_CONFIG_SWITCHVAUE_INVALIDE);
        }
        if(StrUtil.equals(enable,Constant.SWITCH_OPEN)){
            this.validateRequired(result,EMAIL_REQUIRED,EMAIL_DEPEND);
            if(!Arrays.asList(SECURITY_TYPES).contains(params.get("email_security_type"))){
                throw new NZException(RCode.SYS_CONFIG_EMAILPROTOCOL_INVALIDE);
            }

            String testAccount = params.get("email_test_account");
            if(Tool.StrUtil.equals(params.get("email_test"),"true") && Tool.StrUtil.isNotEmpty(testAccount)){
                ValidateUtils.is(testAccount).notNull(RCode.SYS_CONFIG_EMAILTESTRECIVER_ISNULL).email(RCode.SYS_CONFIG_EMAILTESTRECIVER_FORMAT);
            }
            String sendAccount = params.get("email_send_account");
            if(Tool.StrUtil.isNotEmpty(sendAccount)){
                ValidateUtils.is(sendAccount).email(RCode.SYS_CONFIG_EMAILSMTPACCOUNT_FORMAT);
            }
            int emailPort = CommonUtils.toInt(params.get("email_port"));
            ValidateUtils.is(emailPort).min(1,RCode.SYS_CONFIG_PORT_INVALID).max(65535,RCode.SYS_CONFIG_PORT_INVALID);
            //校验邮箱合法性
            this.testMailServer(params);
        }

        this.saveOrUpdateConfsBatch(result);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBasic(Map<String, String> params) {
        final String SNMP_TRAP_KEY = Constant.SYSCONFIG_SNMP_PORT_KEY;
        SysConfigEntity snmpTrapConf = this.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey,SNMP_TRAP_KEY));

        String timezone = this.getValue("timezone");

        //过滤非法的key
        Map<String, String> result = this.validateKeys(BASIC_KEYS, params);
        //校验是否缺少必填项
        this.validateRequired(result,BASIC_REQUIRED,BASIC_DEPEND);
        //校验integer类型
        this.validateInteger(BASIC_INTEGER,result);
        //校验开关值是否合法
        if(!Arrays.asList(SWITCH_TYPES).contains(params.get("unsaved_change"))){
            throw new NZException(RCode.SYS_CONFIG_SWITCHVAUE_INVALIDE);
        }

        int snmpTrapPort = CommonUtils.toInt(params.get("snmp_trap_listen_port"));
        ValidateUtils.is(snmpTrapPort).min(1,RCode.SYS_CONFIG_PORT_INVALID).max(65535,RCode.SYS_CONFIG_PORT_INVALID);

        Integer defaultCabUIntValue = CommonUtils.toInt(params.get("default_cabinet_usize"));
        ValidateUtils.is(defaultCabUIntValue).min(1, RCode.DC_CABINETUSIZE_MINIMUM).max(Constant.CURRENT_CABINET_MAXU, RCode.DC_CABINET_MAXU);

        Integer sessionTimeoutIntValue = CommonUtils.toInt(params.get("session_timeout"));
        ValidateUtils.is(sessionTimeoutIntValue).min(15, RCode.SYS_CONFIG_SESSION_TIMEOUT_INVALID);
        
        //设置默认值
//        this.setDefaultValue(result,BASIC_DEFAULT);
        //保存
        this.saveOrUpdateConfsBatch(result);

        //发布 snmp trap 或者 session过期时间变化 监听端口
        if(ToolUtil.isEmpty(snmpTrapConf)||!StrUtil.equals(snmpTrapConf.getParamValue(),params.get(SNMP_TRAP_KEY))){
            // notify message
            String message = params.get(Constant.SYSCONFIG_SNMP_PORT_KEY);
            sysService.notifySysMessage(Constant.NZ_WEB_COMPONENT_NAME, Constant.SysTopicNameEnum.SNMP_TRAP_PORT.getName(), message);
        }

        // timezone offset变更，更新 cronJob.timezone
        TimeZone oldTimeZone = TimeZone.getTimeZone(timezone);
        TimeZone newTimeZone = TimeZone.getTimeZone(params.get("timezone"));
        this.updateSystemCronJobTimeZoneConf(oldTimeZone, newTimeZone);
    }

    /**
     * update system cron job time zone conf
     *
     * @param oldTimeZone
     * @param newTimeZone
     */
    private void updateSystemCronJobTimeZoneConf(TimeZone oldTimeZone, TimeZone newTimeZone) {
        logger.info("[updateSystemCronJobTimeZoneConf] [oldTimeZone: {}] [newTimeZone: {}] [change: {}]", oldTimeZone.getID(), newTimeZone.getID(), ObjectUtil.notEqual(oldTimeZone, newTimeZone));
        if (ObjectUtil.notEqual(oldTimeZone, newTimeZone)) {
            logger.info("[updateSystemCronJobTimeZoneConf] [timezone conf change] [update cron job timezone] [begin]");
            try {
                scheduler.pauseAll();
                Set<TriggerKey> triggerKeyList = scheduler.getTriggerKeys(GroupMatcher.anyGroup());
                for (TriggerKey triggerKey : triggerKeyList) {
                    Trigger trigger = scheduler.getTrigger(triggerKey);
                    if (trigger instanceof CronTrigger) {
                        CronTrigger cronTrigger = (CronTrigger) trigger;
                        try {
                            CronTrigger newCronTrigger = cronTrigger.getTriggerBuilder()
                                    .withSchedule(
                                            CronScheduleBuilder.cronSchedule(cronTrigger.getCronExpression()).inTimeZone(newTimeZone)
                                    )
                                    .withIdentity(triggerKey.getName(), triggerKey.getGroup())
                                    .build();
                            scheduler.rescheduleJob(cronTrigger.getKey(), newCronTrigger);
                        } catch (SchedulerException e) {
                            logger.error(e, "[updateSystemCronJobTimeZoneConf] [rescheduleJob error] [jobKey: {}]", JSONUtil.toJsonStr(cronTrigger.getKey()));
                        }
                    }
                }
                logger.info("[updateSystemCronJobTimeZoneConf] [timezone conf change] [update cron job timezone] [finshed]");
            } catch (Exception e) {
                logger.error(e, "[updateSystemCronJobTimeZoneConf] [update cron job timezone] [error]");
            } finally {
                try {
                    scheduler.resumeAll();
                } catch (SchedulerException e) {
                    logger.error(e);
                }
            }
        } else {
            logger.info("[updateSystemCronJobTimeZoneConf] [timezone conf no change]");
        }
    }

    /**
     * 系统设置重置
     * @param param
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetConfig(Map<String, Object> param) throws IOException,RuntimeException{
        String pin = StrUtil.str(param.get(Constant.PIN).toString());
        List<String> types = (List<String>)param.get("type");
        try {
            if(ToolUtil.isEmpty(pin)||ToolUtil.isEmpty(types)){
                throw new NZException(RCode.SYS_CONFIG_PASSWORDORTYPE_INVALID);
            }
            // 当前登录用户
            SysUserEntity userEntity = ShiroUtils.getUserEntity();
            // 原加密密码
            String oriPassword = userEntity.getPin();
            // 加密盐值
            String salt = userEntity.getSalt();
            // 现加密密码
            String currentPassword = ShiroUtils.sha256(pin,salt);
            if(!StrUtil.equals(oriPassword,currentPassword)){
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
        }catch (IOException | RuntimeException e){
            throw e;
        }
    }
    /**
     * 重置metric
     */
    private void resetMetric() throws IOException{
        String seriesUri = "/api/v1/admin/tsdb/delete_series";
        String tombstonesUri = "/api/v1/admin/tsdb/clean_tombstones";
        // 获取正常的监控服务列表
        List<Agent> agentList = agentService.list(
                new LambdaQueryWrapper<Agent>().eq(Agent::getStatus,1));
        if(ToolUtil.isNotEmpty(agentList)){
            final String key= URLEncoder.encode("{__name__=~'.+'}","UTF-8");
            agentList.forEach(agent -> {
            	String result = AgentUtil.request(agent, HttpMethod.POST, seriesUri, String.format("match[]=%s", key), null, String.class);
            	logger.info("request result: {}",result);
            	result = AgentUtil.request(agent, HttpMethod.POST, tombstonesUri, null, null, String.class);
            	logger.info("request result: {}",result);
            });
        }
    }

    /**
     * 重置alert
     */
    private void resetAlert() throws RuntimeException{
        alertMessageService.remove(new LambdaQueryWrapper<AlertMessageEntity>());
    }

    /**
     * 清除用户输入数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetEnterData() {
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
        } catch (RuntimeException e) {
            logger.error("nezha reset fail",e);
            throw new NZException(RCode.SYS_CONFIG_RESET_FAIL);
        }
    }

    @Override
    public String getValue(String key) {
        TimedCache<String, Object> cache = TimedCacheManager.getNeverExpireCache(Constant.SYS_CACHE_PREFIX, Constant.SYS_CONFIG_CACHE_KEY);

        Object value = cache.get(key);
        if (value == null) {
            SysConfigEntity config = this.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, key));
            if (config != null) {
                cache.put(key, config.getParamValue());
            }
        }

        return StrUtil.str(cache.get(key), CharsetUtil.CHARSET_UTF_8);
    }

    @Override
    public String getValueOrDefault(String key, String defaultValue) {
        String value = this.getValue(key);
        return Tool.StrUtil.isEmpty(value) ? defaultValue : value;
    }
    /**
    * @Description 查询返回 所有的key 对应的value
    * @Author  rui
    * @Date   2021/4/19
    */
    public Map<String, String> batchQuery(String... keys) {
        Map<String, String> result = new HashMap<>();
        for (String key : Arrays.asList(keys)) {
            result.put(key, this.getValue(key));
        }
        return result;
    }


    /**
    * @Description 校验参数是否存在，并返回过滤后的map
    * @Date   2021/4/19
    */
    private Map<String,String> validateKeys(String[] keys,Map<String,String> params){
        List<String> keyList = Arrays.asList(keys);
        Map<String,String> result = new HashMap<>();
        for (String key : params.keySet()){
            if(keyList.contains(key)){
               result.put(key,params.get(key));
            }
        }
        return result;
    }

    /**
    * @Description 校验必填项
    * @Date   2021/4/19
    */
    private void validateRequired(Map<String,String> params,String[] requiredKeys,String [][] dependKeys){
        List<String> keyList = Arrays.asList(requiredKeys);

        for (String key : keyList){
            if(Tool.StrUtil.isEmpty(params.get(key))){
                throw new NZException(String.format("%s can not be null",Tool.StrUtil.replace(key,"_"," ")),99999);
            }
        }

        if(dependKeys!=null && dependKeys.length>0){
            for (String[] depend : dependKeys){
                String key = depend[0];
                List<String> dependKey = Tool.StrUtil.split(depend[1],","); //多个依赖用逗号隔开
                List<String> values = Tool.StrUtil.split(depend[2],",");//多个value用逗号隔开
                boolean isDepend = true;
                for (int i = 0; i < dependKey.size(); i++) {
                    String dk = dependKey.get(i);
                    String value = values.get(i);
                    if(!(params.containsKey(dk) && Tool.StrUtil.equals(params.get(dk),value))){ //如果不存在依赖的key 或者依赖的key对应的value 不符合，说明不需要依赖
                        isDepend = false;
                        break;
                    }
                }
                if(isDepend&& Tool.StrUtil.isEmpty(params.get(key))){
                    throw new NZException(String.format("%s can not be null", Tool.StrUtil.replace(key,"_"," ")),99999);
                }
            }
        }
    }

    private void validateInteger(String[] intKeys,Map<String,String> params){
        List<String> keyList = Arrays.asList(intKeys);

        for (String key : keyList){
            if(CommonUtils.toInt(params.get(key) )== null){
                throw new NZException(String.format("%s should be an integer", Tool.StrUtil.replace(key,"_"," ")),573012);
            }

        }
    }

    private void setDefaultValue(Map<String,String> params,String[][] defaultValus){
        for (String[] defalt:defaultValus){
            String key = defalt[0];
            String value = defalt[1];

            if(!params.containsKey(key)||StrUtil.isEmpty(params.get(key))){
                params.put(key,value);
            }

        }
    }

    public Map<String,SysConfigEntity> saveOrUpdateConfsBatch(Map<String,String> params){

        List<SysConfigEntity> exists = this.list(new QueryWrapper<SysConfigEntity>().lambda().in(SysConfigEntity::getParamKey,params.keySet()));

        Map<String,SysConfigEntity> map = new HashMap<>();

        for (SysConfigEntity conf : exists){
            map.put(conf.getParamKey(),conf);
        }

        for (String key : params.keySet()){
            if(map.containsKey(key)){
                map.get(key).setParamValue(params.get(key));
            }else{
                SysConfigEntity sysConfigEntity = new SysConfigEntity();
                sysConfigEntity.setParamKey(key);
                sysConfigEntity.setParamValue(params.get(key));
                map.put(key,sysConfigEntity);
            }
        }

        this.saveOrUpdateBatch(map.values());

        // notify message
        sysService.notifySysMessage(Constant.NZ_WEB_COMPONENT_NAME, Constant.SysTopicNameEnum.SYS_CONFIG_RELOAD.getName(), StrUtil.EMPTY);

        return map;
    }

    private void testMailServer(Map<String, String> map) {
        String emailTest = map.get("email_test");
        map.remove("email_test");
        if (!Tool.StrUtil.equals(emailTest,"true")){
            return ;
        }
        String smtpHost = map.get("email_host");
        String smtpPort = map.get("email_port");
        String smtpTimeout = map.get("email_timeout");
        String securityType = map.get("email_security_type");

        // 登录账户
        String smtpAccount = map.get("email_auth_account");
        String smtpPin = StrUtil.str(map.get("email_auth_password"));

        //测试用收件人
        String receicer = map.get("email_test_account");

        // 发件人
        String sendAccount = map.get("email_send_account");


        //组织properties
        Properties prop = new Properties();
        prop.setProperty("mail.snmp.host", smtpHost);
        prop.setProperty("mail.smtp.port", smtpPort);
        prop.setProperty("mail.transport.protocol", "smtp");
        // 设置超时时间为10秒
        prop.setProperty("mail.smtp.timeout", Tool.StrUtil.isBlank(smtpTimeout) ? Constant.CONNECTION_SMTP_TIMEOUT : smtpTimeout.trim() + "000");
        prop.setProperty("mail.smtp.connectiontimeout", Tool.StrUtil.isBlank(smtpTimeout) ? Constant.CONNECTION_SMTP_TIMEOUT : smtpTimeout.trim() + "000");

        if (Tool.StrUtil.equals(securityType, "SSL")) {
            prop.put("mail.smtp.ssl.trust", "*");
            prop.put("mail.smtp.ssl.enable", "true");
            prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            prop.put("mail.smtp.socketFactory.fallback", "false");
        }

        if (Tool.StrUtil.equals(securityType, "TLS")) {
            prop.put("mail.smtp.starttls.enable", "true");
            prop.put("mail.smtp.ssl.trust", "*");
        }

        //emailTest on 只测试，需要给测试邮箱发邮件
        try {
            mailUtil.sendHTML(smtpHost, smtpPort, prop, smtpAccount, sendAccount, smtpPin, receicer, Constant.TESTEMAIL_SEND_TITLE, this.getSendMailContent());
        } catch (IOException e) {
            logger.error(e, "[testMailServer] [send text email error] [conf: {}]", JSONUtil.toJsonStr(map));
            throw new NZException(RCode.SYS_CONFIG_TESTEMAIL_TEMPLATE_NOTFOUND);
        } catch (TemplateException e) {
            logger.error(e, "[testMailServer] [generate test email context error]");
            throw new NZException(RCode.SYS_CONFIG_TESTEMAIL_TEMPLATE_FAILED);
        }
    }

    // 获取发送邮件内容
    public String getSendMailContent() throws IOException, TemplateException {
        // 加载模板文件
        String templateString = this.getValue(Constant.SYSCONFIG_KEY_TESTEMAIL_SEND_TEMPLATE);
        //得到模板
        Template template = TemplateUtil.stringToTemplate(templateString, "testMail");
        //执行静态化 方式1  获取静态化内容
        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, null);
        //静态化内容;
        return content;
    }

    @Override
    public boolean checkHaMode() {
        Map<String, String> configMap = this.batchQuery("ha_mode", "ha_vip");
        if ("2".equals(configMap.get("ha_mode"))) {
            if (!CommonUtils.getInetIpAddress().contains(configMap.get("ha_vip"))) {
                return false;
            }
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMonitor(Map<String, String> params) {
        // 过滤非法的key
        Map<String, String> result = this.validateKeys(MONITOR_KEYS, params);
        // 校验是否缺少必填项
        this.validateRequired(result, MONITOR_REQUIRED, MONITOR_DEPEND);
        //校验integer类型
        this.validateInteger(MONITOR_INTEGER, result);

        Integer queryMaxSeriesIntValue = CommonUtils.toInt(params.get(Constant.METRICS_QUERY_MAX_SERIES));
        if (queryMaxSeriesIntValue < -1 || queryMaxSeriesIntValue > 1000) {
            throw new NZException(RCode.SYS_CONFIG_QUERYMAXSERIES_INTERVAL);
        }

        Integer promeFedeEnabledIntValue = CommonUtils.toInt(params.get("prometheus_federation_enabled"));
        if (promeFedeEnabledIntValue != 0 && promeFedeEnabledIntValue != 1) {
            throw new NZException(RCode.SYS_CONFIGI_PROMEFEDEENABLED_NCORRECT);
        }

        Integer assetPingFromIntValue = CommonUtils.toInt(params.get("asset_ping_from"));
        if (assetPingFromIntValue != 1 && assetPingFromIntValue != 2) {
            throw new NZException(RCode.SYS_CONFIGI_ASSETPINGFROM_NCORRECT);
        }

        Integer metricsStorageTypeIntValue = CommonUtils.toInt(params.get("metrics_storage_type"));
        if (metricsStorageTypeIntValue != 1 && metricsStorageTypeIntValue != 2) {
            throw new NZException(RCode.SYS_CONFIGI_METRICSSTORAGETYPE_NCORRECT);
        }

        Integer logsStorageTypeIntValue = CommonUtils.toInt(params.get("logs_storage_type"));
        if (logsStorageTypeIntValue != 1 && logsStorageTypeIntValue != 2) {
            throw new NZException(RCode.SYS_CONFIGI_LOGSSTORAGETYPE_NCORRECT);
        }

        // interface_name
        String interfaceName = result.get("interface_name");
        result.put("interface_name", StrUtil.emptyToDefault(interfaceName, "eth0,en0,bond0,ens2f0,enp0s31f6"));

        // 处理 monitor_event_log 数据变动
        this.handleMoniotEventLog(result);
        //保存
        this.saveOrUpdateConfsBatch(result);

        //更新 缓存中的 metrics_query_max_series
        String metricsQueryMaxSeries = params.get(Constant.METRICS_QUERY_MAX_SERIES);
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String queryMaxSeries = valueOperations.get(Constant.METRICS_QUERY_MAX_SERIES);

        if (!StrUtil.equals(metricsQueryMaxSeries, queryMaxSeries)) {
            valueOperations.set(Constant.METRICS_QUERY_MAX_SERIES, metricsQueryMaxSeries, 30, TimeUnit.MINUTES);
            logger.info("Prometheus query_range 查询最大条数限制更改为 " + metricsQueryMaxSeries);
        }
    }

    @Override
    public Map<String, Object> querySysConfig(String configKey) {
        SysConfigEntity sysConfigEntity = this.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, configKey));
        Map<String, Object> map = Tool.BeanUtil.beanToMap(sysConfigEntity);
        return map;
    }

    /**
     * 处理 monitor_event_log 数据变动
     * @param updateMap
     */
    private void handleMoniotEventLog(Map<String, String> updateMap) {
        // 1. 查询未修改前 monitor 数据
        List<String> updateKeys = new ArrayList<>();
        Map<String, String> beforeUpdateMap = this.batchQuery(SysConfServiceImpl.MONITOR_KEYS);
        for (Map.Entry<String, String> entry : beforeUpdateMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // 比对
            if (!StrUtil.equals(updateMap.get(key), value)) {
                updateKeys.add(key);
            }
        }
        logger.info("[handleMoniotEventLog] [Updated monitor config] [key size: {}] [value: {}]", updateKeys.size(), JSONUtil.toJsonStr(updateKeys));
        // 根据 key 更新不同 event_log 值
        if (Tool.CollectionUtil.isNotEmpty(updateKeys)) {
            Set<String> incrementKeys = new HashSet<>();
            // 联邦开关
            if (updateKeys.contains("prometheus_federation_enabled")) {
                incrementKeys.add("prometheus");
                incrementKeys.add("promtail");
                incrementKeys.add("loki");
                incrementKeys.add("cortex");
                // incrementKeys.add("rule");
            }
            // prometheus.yml 改动
            if (updateKeys.contains("asset_ping_from") || updateKeys.contains("asset_ping_interval") || updateKeys.contains("default_scrape_interval") || updateKeys.contains("default_scrape_timeout")) {
                incrementKeys.add("prometheus");
            }
            // prometheus config.conf 启动项改动
            if (updateKeys.contains("metrics_storage_retention")) {
                incrementKeys.add("prometheus");
                incrementKeys.add("cmd");
            }
            // loki.yml 改动
            if (updateKeys.contains("logs_storage_retention") || updateKeys.contains("logs_storage_s3_access_key") || updateKeys.contains("logs_storage_s3_bucket") || updateKeys.contains("logs_storage_s3_endpoint") || updateKeys.contains("logs_storage_s3_secret_access_key")) {
                incrementKeys.add("loki");
            }
            // promtail 下发方式改变
            if (updateKeys.contains("logs_storage_type")) {
                incrementKeys.add("promtail");
                incrementKeys.add("loki");
            }
            // cortex.yml 改动
            if (updateKeys.contains("alert_api") || updateKeys.contains("metrics_storage_s3_access_key") || updateKeys.contains("metrics_storage_s3_bucket") || updateKeys.contains("metrics_storage_s3_endpoint") || updateKeys.contains("metrics_storage_s3_secret_access_key")) {
                incrementKeys.add("cortex");
            }
            // metrics 下发方式改变
            if (updateKeys.contains("metrics_storage_type")) {
                incrementKeys.add("prometheus");
                // incrementKeys.add("rule");
                incrementKeys.add("cortex");
            }
            // interface_name
            if (updateKeys.contains("interface_name")) {
                logger.info("[handleMoniotEventLog] [Updated interface_name]");
                incrementKeys.add("cortex");
                incrementKeys.add("loki");
            }
            eventLogService.increment(ArrayUtil.toArray(incrementKeys, String.class));
        }
    }

    /**
     *测试连接是否可用 ldap
     */
    public void testConnectionLdapConfig(Map<String,String> params) {
        String ldapTest = params.get("ldap_test");
        params.remove("ldap_test");
        if (!Tool.StrUtil.equals(ldapTest,"true")){
            return ;
        }
        String dn = params.get("ldap_dn");
        String pin = params.get("ldap_password");
        String ou = params.get("ldap_ou");
        String address = params.get("ldap_address");
        String timeout = params.get("ldap_timeout");

        InitialDirContext initialDirContext = null;
        try {
            Hashtable<String, String> environment = new Hashtable<>();
            environment.put(Context.SECURITY_AUTHENTICATION, "simple");
            environment.put(Context.SECURITY_PRINCIPAL, dn);
            environment.put(Context.SECURITY_CREDENTIALS, pin);
            environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            // 当连接ldaps时候  会发生异常 无法进行连接
            environment.put("com.sun.jndi.ldap.connect.timeout", timeout+"000");
            environment.put(Context.PROVIDER_URL, address);
            initialDirContext = new InitialDirContext(environment);


            /*// 校验ou属性  该方法有问题  先注释 优先测试能够登陆ldap服务器功能
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
            }*/

        } catch (AuthenticationException e) {
            // 账户类异常 造成原因： 用户名或者密码错误 也就是 dn和password
            logger.error("ldap service config connection failed : username/password wrong",e);
            throw new NZException(RCode.SYS_CONFIG_LDAP_AUTHENTICATIONFAIL);
        } catch (NamingException e) {
            // 连接类异常 造成原因 1. 服务未开启 2. 服务器防火墙等其他原因造成无法连接到服务器
            logger.error("ldap service config connection failed",e);
            throw new NZException(RCode.SYS_CONFIG_LDAP_CONNECTIONFAILED);
        } catch (RuntimeException e) {
            logger.error("ldap service config connection failed",e);
            throw new NZException(RCode.SYS_CONFIG_LDAP_CONNECTIONFAILED);
        } finally {
            if (initialDirContext != null) {
                try {
                    initialDirContext.close();
                } catch (NamingException e) {
                    logger.error("关闭ldapContext失败",e);
                }
            }
        }
    }

    @Override
    public void updateValueByKey(String key, String value) {
        this.updateValueByKey(key, value, StrUtil.EMPTY);
    }

    @Override
    public void updateValueByKey(String key, String value, String remark) {
        String conf = this.getValue(key);
        // save
        if (conf == null) {
            SysConfigEntity entity = new SysConfigEntity();
            entity.setParamKey(key);
            entity.setParamValue(value);
            entity.setRemark(remark);
            this.save(entity);
        } else {
            if (StrUtil.equals(conf, value)) {
                logger.warn("[updateValueByKey] [this configuration has not changed] [key: {}]", key);
                return;
            } else {
                // update
                LambdaUpdateWrapper<SysConfigEntity> updateWrapper = new UpdateWrapper<SysConfigEntity>().lambda()
                        .set(SysConfigEntity::getParamValue, value)
                        .set(SysConfigEntity::getRemark, remark)
                        .eq(SysConfigEntity::getParamKey, key);
                this.update(updateWrapper);
            }
        }

        // notify message
        sysService.notifySysMessage(Constant.NZ_WEB_COMPONENT_NAME, Constant.SysTopicNameEnum.SYS_CONFIG_RELOAD.getName(), StrUtil.EMPTY);
    }

    @Override
    public String getDataBasesVersion() {
        return this.getBaseMapper().getDataBasesVersion();
    }

    @Override
    public synchronized void reloadSysConfigToCache() {
        logger.info("[reloadSysConfigToCache] [begin]");
        try {
            TimedCache<String, Object> cache = TimedCacheManager.getNeverExpireCache(Constant.SYS_CACHE_PREFIX, Constant.SYS_CONFIG_CACHE_KEY);
            // clear
            cache.clear();

            // put
            List<SysConfigEntity> sysConfigList = this.list();
            sysConfigList.parallelStream().forEach(config -> {
                cache.put(config.getParamKey(), config.getParamValue());
            });
        } catch (Exception e) {
            logger.error(e, "[reloadSysConfigToCache] [error]");
        }
        logger.info("[reloadSysConfigToCache] [finshed]");
    }

}
