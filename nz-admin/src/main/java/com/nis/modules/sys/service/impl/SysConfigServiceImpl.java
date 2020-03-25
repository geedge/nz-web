/**

 *

 *

 */

package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
import com.nis.modules.promserver.service.PromserverProxyService;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.service.impl.PromserverProxyServiceImpl;
import com.nis.modules.promserver.utils.HttpClientUtils;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.redis.SysConfigRedis;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.shiro.ShiroUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service("sysConfigService")
public class SysConfigServiceImpl extends ServiceImpl<SysConfigDao, SysConfigEntity> implements SysConfigService {
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
    public Map getConfigListByTypes(String[] types) {

        List<String> paramKeys = new ArrayList<>();
        for (String type : types) {
            paramKeys.addAll(Constant.SYSCONFIG_TYPEWITHKEYS_MAP.get(type));
        }

        List<SysConfigEntity> list = this.list(new QueryWrapper<SysConfigEntity>()
                .eq("status", 1)
                .in(CollectionUtils.isNotEmpty(paramKeys), "param_key", paramKeys));


        // 组织返回数据
        Map typeMap = new HashMap(), values;

        List<String> keys;
        for (String typeName : types) {
            values = new HashMap();

            keys = Constant.SYSCONFIG_TYPEWITHKEYS_MAP.get(typeName);
            for (SysConfigEntity entity : list) {
                if (keys.contains(entity.getParamKey())) {
                    values.put(entity.getParamKey(), entity.getParamValue());
                }
            }

            typeMap.put(typeName, values);
        }

        return typeMap;
    }


    @Override
    public void updateConfigs(Map<String, Object> param) {
        // 根据 类型 校验 需要添加/修改的 key , 不在常量中定义的不入库
        Set<String> keySet = Constant.SYSCONFIG_TYPEWITHKEYS_MAP.keySet();

        String saveType = "";
        Map<String, Object> saveMap = null;
        List<String> removeKeys = new ArrayList<>();
        for (String type : keySet) {
            saveMap = (Map) param.get(type);

            if (saveMap != null && saveMap.size() > 0) {
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
        // 去掉不在 定义范围内的数据
        for (String str : removeKeys) {
            saveMap.remove(str);
        }

        // 根据类别校验具体的参数
        switch (saveType) {
            case "basic":
                this.validateBasicParams(saveMap);
                this.updateEntity(saveMap);
                break;
            case "email":
                // 开启 email 开关
                if (this.validateEmailParams(saveMap)) {
                    this.updateEntity(saveMap);
                } else {
                    // email 开关关闭 只更新此开关
                    SysConfigEntity emailEnable = this.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", "email_enable"));
                    // 数据库目前没有此项
                    if (emailEnable == null) {
                        emailEnable = new SysConfigEntity();
                        emailEnable.setParamKey("email_enable");
                        emailEnable.setParamValue(Constant.SWITCH_CLOSE.toString());
                    }else{
                        emailEnable.setParamValue(Constant.SWITCH_CLOSE.toString());
                    }
                    this.saveOrUpdate(emailEnable);
                }
                break;
            case "terminal":
                this.validateTerminalParams(saveMap);
                this.updateEntity(saveMap);
                break;
            case "ldap":
                // 开启 ldap 开关
                if (this.validateLdapParams(saveMap)) {
                    this.updateEntity(saveMap);
                } else {
                    SysConfigEntity ldapEnable = this.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", "ldap_enable"));
                    // 数据库目前没有此项
                    if (ldapEnable == null) {
                        ldapEnable = new SysConfigEntity();
                        ldapEnable.setParamKey("ldap_enable");
                        ldapEnable.setParamValue(Constant.SWITCH_CLOSE.toString());
                    }else{
                        ldapEnable.setParamValue(Constant.SWITCH_CLOSE.toString());
                    }
                    this.saveOrUpdate(ldapEnable);
                }
                break;
        }
    }


    /**
     * 系统设置重置
     * @param password
     * @param type
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetConfig(String password, String[] type) throws Exception{
        try {
            if(StringUtils.isEmpty(password)||ArrayUtils.isEmpty(type)
                    || !ArrayUtils.contains(Constant.SYSCONFIG_RESET_TYPE,type[0])){
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
            if("metric".equals(type[0])){
                resetMetric();
            }else if("alert".equals(type[0])){
                resetAlert();
            }else {
                resetEnterData();
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
            promserverList.forEach(promserver -> {
                HttpClientUtils.httpPost("http://"+promserver.getHost()+":"+promserver.getPort()+seriesUri,"match[]={_name_=~'.+'}");
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
    private void resetEnterData() throws Exception{
        // redis写入当前用户id
        Long userId = ShiroUtils.getUserId();
        redisTemplate.opsForValue().set("reset",String.valueOf(userId),expire, TimeUnit.SECONDS);
        // 删除用户输入的数据
        Arrays.stream(Constant.TABLESWITHCLEAR).forEach(table->{
            this.baseMapper.clearByTableName(table);
        });
        // 删除redis中key=reset的数据
        redisTemplate.delete("reset");

    }


    private void updateEntity(Map<String, Object> saveMap) {
        // 当前记录
        List<SysConfigEntity> configs = this.list(new QueryWrapper<SysConfigEntity>().in("param_key", saveMap.keySet()));

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
            entity.setParamValue(saveMap.get(entity.getParamKey()).toString());
        }

        this.saveOrUpdateBatch(configs);
    }


    /**
     * 校验 Basic 更新的参数
     * @param saveMap
     */
    private void validateBasicParams(Map<String, Object> saveMap) {
        Integer assetPingSwitch = (Integer) saveMap.get("asset_ping_switch");
        Integer assetPingInterval = (Integer) saveMap.get("asset_ping_interval");

        ValidateUtils.is(saveMap.get("alert_api")).notNull(RCode.SYS_CONFIG_ALERTAPI_ISNULL)
                .and(saveMap.get("node_exporter_target_path")).notNull(RCode.SYS_CONFIG_NODETARGETPATH_ISNULL)
                .and(assetPingSwitch).notNull(RCode.SYS_CONFIG_ASSETPINGSWITCH_ISNULL)
                .and(assetPingInterval).notNull(RCode.SYS_CONFIG_ASSETPINGINTERVAL_ISNULL);

        // 校验开关类型值
        this.validateSwitchValue(assetPingSwitch);
    }

    private boolean validateEmailParams(Map<String, Object> saveMap) {
        // 是否启用邮件
        Integer emailEnable = (Integer) saveMap.get("email_enable");

        ValidateUtils.is(emailEnable).notNull(RCode.SYS_CONFIG_EMAILENABLE_ISNULL);

        this.validateSwitchValue(emailEnable);

        // 启用邮件 校验相关参数
        if (emailEnable.equals(1)) {
            String emailSmtpHost = (String) saveMap.get("email_smtp_host");
            Integer emailSmtpPort = (Integer) saveMap.get("email_smtp_port");
            Integer emailSslFlag = (Integer) saveMap.get("email_ssl_flag");
            Integer emailTlsFlag = (Integer) saveMap.get("email_tls_flag");

            ValidateUtils.is(emailSmtpHost).notNull(RCode.SYS_CONFIG_EMAILSMTPHOST_ISNULL).ip(RCode.SYS_CONFIG_EMAILSMTPHOST_FORMAT)
                    .and(emailSmtpPort).notNull(RCode.SYS_CONFIG_EMAILSMTPPORT_ISNULL)
                    .and(saveMap.get("email_smtp_account")).notNull(RCode.SYS_CONFIG_EMAILSMTPACCOUNT_ISNULL)
                    .and(saveMap.get("email_smtp_password")).notNull(RCode.SYS_CONFIG_EMAILSMTPPASSWORD_ISNULL)
                    .and(saveMap.get("email_send_account")).notNull(RCode.SYS_CONFIG_EMAILSENDACCOUNT_ISNULL)
                    .and(saveMap.get("email_test_reciver")).notNull(RCode.SYS_CONFIG_EMAILTESTRECIVER_ISNULL).email(RCode.SYS_CONFIG_EMAILTESTRECIVER_FORMAT)
                    .and(emailSslFlag).notNull(RCode.SYS_CONFIG_EMAILSSLFLAG_FORMAT)
                    .and(emailTlsFlag).notNull(RCode.SYS_CONFIG_EMAILTLSFLAG_FORMAT);

            if (!CommonUtils.checkPort(emailSmtpPort.toString())) {
                throw new NZException(RCode.SYS_CONFIG_EMAILSMTPPORT_ERROR);
            }
            return true;
        }
        return false;
    }


    /**
     * 校验 Terminal 更新的参数
     * @param saveMap
     */
    private void validateTerminalParams(Map<String, Object> saveMap) {
        Integer maxTerminalNum = (Integer) saveMap.get("max_terminal_num");

        if (maxTerminalNum == null) {
            throw new NZException(RCode.SYS_CONFIG_MAXTERMINALNUM_ISNULL);
        }

        if (maxTerminalNum < 1) {
            throw new NZException(RCode.SYS_CONFIG_MAXTERMINALNUM_INVALIDE);
        }
    }

    /**
     * 校验 Ldap 更新的参数
     * @param saveMap
     * @return
     */
    private boolean validateLdapParams(Map<String, Object> saveMap) {
        Integer ldapEnable = (Integer) saveMap.get("ldap_enable");
        ValidateUtils.is(ldapEnable).notNull(RCode.SYS_CONFIG_LDAPENABLE_ISNULL);

        this.validateSwitchValue(ldapEnable);

        // 启用ldap 校验相关参数
        if (ldapEnable.equals(1)) {
            ValidateUtils.is(saveMap.get("ldap_address")).notNull(RCode.SYS_CONFIG_LDAPADDRESS_ISNULL)
                    .and(saveMap.get("ldap_dn")).notNull(RCode.SYS_CONFIG_LDAPDN_ISNULL)
                    .and(saveMap.get("ldap_password")).notNull(RCode.SYS_CONFIG_LDAPPASSWORD_ISNULL)
                    .and(saveMap.get("ldap_user_filter")).notNull(RCode.SYS_CONFIG_LDAPUSERFILTER_ISNULL)
                    .and(saveMap.get("ldap_mapping")).notNull(RCode.SYS_CONFIG_LDAPMAPPING_ISNULL);
            return true;
        }
        return false;
    }


    // 校验开关类型值是否符合规范 0 or 1
    private void validateSwitchValue(Integer switchValue) {
        if (!switchValue.equals(Constant.SWITCH_OPEN) && !switchValue.equals(Constant.SWITCH_CLOSE)) {
            throw new NZException(RCode.SYS_CONFIG_SWITCHVAUE_INVALIDE);
        }
    }
}
