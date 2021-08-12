package com.nis.modules.sys.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.MfaUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.entity.AuthTokenEntity;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.Timezone;
import com.nis.modules.sys.redis.SysLoginRedis;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.service.TimezoneService;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.core.util.StrUtil;

@RestController
@RequestMapping("/mfa")
public class MfaController {
	
	@Autowired
	private RedisTemplate<String,String> redisTemplate;
	@Autowired
	private SysConfigService configService;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private SysLoginRedis sysLoginRedis;
	@Autowired
	private TimezoneService timezoneService;
	@Autowired
	private RedisConnectionFactory factory;
	
	@ResponseBody
	@RequestMapping(value = "/reset", method = RequestMethod.POST)
	@SysLog(operation = OperationEnum.RESET,type = TypeEnum.SYSTEM)
	public R reset(@RequestBody SysUserEntity sysUser){
		if(StringUtils.isNotBlank(sysUser.getUserIds())) {
			String[] idArray = sysUser.getUserIds().split(",");
			
			SysUserEntity entity = new SysUserEntity();
			entity.setOtpKey("");
			sysUserService.update(entity, new UpdateWrapper<SysUserEntity>().lambda().in(SysUserEntity::getId, idArray));
		
		}
		return R.ok();
	}
	
	@ResponseBody
	@RequestMapping(value = "/genKey", method = RequestMethod.POST)
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.SYSTEM)
	public R genKey(@RequestBody SysUserEntity sysUser,HttpServletRequest request, HttpServletResponse response){
		
		String requestToken = request.getHeader(Constant.AUTH_TOKEN_CODE);
		String authToken = sysUser.getAuthToken();
		if (StringUtils.isBlank(requestToken) && StringUtils.isBlank(authToken)) {
			return R.error(RCode.SYS_TOKEN_OR_AUTHTOKEN_ISNULL);
		}
		Map<String, Object> obj = new HashMap<>();
		try {
//			Subject subject = ShiroUtils.getSubject();
//			System.out.println(subject.getSession().getId());
//			System.out.println(requestToken); //登录
//			System.out.println(isLogin(requestToken)); //登录
			
			Long userId = -1l;
			String userName = "";
			
			SysUserEntity user = new SysUserEntity();
			if(!isLogin(requestToken)) {//未登录
				//验证authToken是否存在，否，则返回响应失败
				if(StrUtil.isBlank(authToken)) {
					return R.error(RCode.SYS_AUTHTOKEN_ISNULL);
				}else {
					AuthTokenEntity authTokenEntity = sysLoginRedis.get(authToken);
					if(authTokenEntity==null) {
						return R.error(RCode.SYS_AUTHTOKEN_ISVALID);
					}else {
						userId = authTokenEntity.getUserId();
						user = sysUserService.getById(userId);
						if(user!=null) {
							userName = user.getName();
						}
					}
				}
			}else {//已登录
				RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
				redisTemplate.setKeySerializer(new StringRedisSerializer());
				redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
				redisTemplate.setConnectionFactory(factory);

				redisTemplate.afterPropertiesSet();
				
				ValueOperations ops = redisTemplate.opsForValue();
				Object sessionObj = ops.get(ShiroUtils.REDIS_KEYPREFIX + requestToken);
				SimpleSession session = (SimpleSession) sessionObj;
				SimplePrincipalCollection primaryPrincipal = (SimplePrincipalCollection) session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
				SysUserEntity userEntity = (SysUserEntity) primaryPrincipal.getPrimaryPrincipal();

				user = sysUserService.getById(userEntity.getId());
				
				if(user!=null) {
					userId = user.getId();
					userName = user.getName();
				}
			}
			
			//更新authToken，生成authKey并保存
			String newAuthToken = MfaUtils.getAuthToken();
			String authKey = MfaUtils.generateSecretKey();//是否需要存入用户表的otp_key:是
			if(!updateAuthToken(authToken, userId, newAuthToken,authKey,sysUser.getMfaLevel())) {
				return R.error(RCode.SYS_TOKEN_REFRESH_FREQUENT);//超过10 响应操作频繁，请30分钟后重试
			}
			
			SysUserEntity entity = new SysUserEntity();
			entity.setOtpKey(authKey);
			sysUserService.update(entity, new UpdateWrapper<SysUserEntity>().lambda().eq(SysUserEntity::getId, userId));
		
			obj.put("authToken", newAuthToken);
			obj.put("authKey", authKey);
			obj.put("username", userName);
			obj.put("otpauth", MfaUtils.getAuthenticatorBarCode(authKey, userName));
		}catch (UnknownAccountException e) {
			throw new NZException(RCode.SYS_LOGIN_UNKNOWNACCOUNT);
		}catch (IncorrectCredentialsException e) {
			throw new NZException(RCode.SYS_LOGIN_USERPWD);
		}catch (LockedAccountException e) {
			throw new NZException(RCode.SYS_LOGIN_LOCK);
		}catch (AuthenticationException e) {
			throw new NZException(RCode.SYS_LOGIN_ACCOUNTAUTH);
		}
		
		return R.ok(obj);
	}
	
	private boolean updateAuthToken(String oldAuthToken,Long userId,String newAuthToken,String authKey,Integer mfaLevel) {
		//更新authToken
		AuthTokenEntity authTokenEntity = sysLoginRedis.get(oldAuthToken);
		if(authTokenEntity==null) {
			authTokenEntity = new AuthTokenEntity();
			authTokenEntity.setUserId(userId);
			authTokenEntity.setAuthKey(authKey);
			authTokenEntity.setC(1);
			authTokenEntity.setMfaLevel(mfaLevel);
			sysLoginRedis.saveOrUpdate(authTokenEntity,30l*60l,newAuthToken);
		}else {
			int c = authTokenEntity.getC();
			if(c>=10) {
				return false;
			}
			authTokenEntity.setUserId(userId);
			authTokenEntity.setAuthKey(authKey);
			authTokenEntity.setC(c+1);
			authTokenEntity.setMfaLevel(mfaLevel);
			sysLoginRedis.delete(oldAuthToken);
			sysLoginRedis.saveOrUpdate(authTokenEntity,30l*60l,newAuthToken);
		}
		
		return true;
	}
	
	private boolean isLogin(String requestToken) {
		if(StringUtils.isBlank(requestToken)) {
			return false;
		}else {
			Boolean exist = redisTemplate.hasKey(ShiroUtils.REDIS_KEYPREFIX + requestToken);
//			if (exist) {
//	        	SysUserEntity userEntity = ShiroUtils.getUserEntity();
//	        	if(ToolUtil.equals(ShiroUtils.getUserEntity().getId(), 0L)) {
//	        		// 第三方api key登录
//	        		return true;
//	        	}
//	            // 重新查询数据库
//	            SysUserEntity entity = this.sysUserDao.selectById(ShiroUtils.getUserEntity().getId());
//	            if (entity.getStatus() == 0) {
//	                response.getWriter().write(JSON.toJSON(R.error(RCode.SYS_LOGIN_LOCK)).toString());
//	                return false;
//	            }
//	            // key 存在 证明验证成功，刷新过期时间
//	            redisTemplate.expire(requestToken, this.loginExpiration, TimeUnit.MINUTES);
//	            return true;
//	        } else {
//	        	Date now = new Date();
//	        	String time = DateUtil.format(now , "yyyy-MM-dd hh:mm:ss");
//	        	// 查询sys_api_key是否有该token记录
//	        	SysApiKey apiKey = sysApiKeyDao.selectOne(new QueryWrapper<SysApiKey>().lambda().eq(SysApiKey::getToken, requestToken)
//	        			.and(wrapper -> wrapper.ge(SysApiKey::getExpireAt,time).or().isNull(SysApiKey::getExpireAt)));
//	        	// 如果有的话则认证通过  同时将该值存放到redis里
//	        	if(ToolUtil.isEmpty(apiKey)) {
//	        		// 如果没有则 返回响应值
//	                response.getWriter().write(JSON.toJSON(R.error(RCode.SYS_LOGIN_REQUIRED)).toString());
//	                return false;
//	        	}else {
//	        		RedisConnectionFactory factory = SpringContextUtils.getBean("redisConnectionFactory", RedisConnectionFactory.class);
//	        		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//	        		redisTemplate.setKeySerializer(new StringRedisSerializer());
//	        		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
//	        		redisTemplate.setConnectionFactory(factory);
//	        		redisTemplate.afterPropertiesSet();
//	        		
//	        		
//	        		//获取存在缓存中的过期时间
//	        		Date expireAt = apiKey.getExpireAt();
//	        		
//	        		SimpleSession session = new SimpleSession();
//	    			SysUserEntity user = new SysUserEntity();
//	    			user.setApiKeyId(apiKey.getId());
//	    			user.setId(0L);
//	    			user.setName(apiKey.getName());
//	    			SimplePrincipalCollection primaryPrincipal =new SimplePrincipalCollection(user,"");
//	    			session.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY, primaryPrincipal);
//	    			session.setId(apiKey.getToken());
//
//	    			if(!ToolUtil.isEmpty(expireAt)){
//	        			long seconds = DateUtil.between(now, expireAt, DateUnit.SECOND);
//	            		redisTemplate.opsForValue().set(ShiroUtils.REDIS_KEYPREFIX + requestToken, session,seconds,TimeUnit.SECONDS);
//	        		}else{
//	        			redisTemplate.opsForValue().set(ShiroUtils.REDIS_KEYPREFIX + requestToken, session);
//	        		}
//	    			
//	    			DefaultSubjectContext context =new DefaultSubjectContext();
//	    			context.setSession(session);
//	    			SecurityManager securityManager = SecurityUtils.getSecurityManager();
//	    			Subject subject = securityManager.createSubject(context);
//	    			ThreadContext.bind(subject);
//	    			return true;
//	        	}
//	        }
			return exist;
		}
        
	}
	
	@ResponseBody
	@RequestMapping(value = "/bind", method = RequestMethod.POST)
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.SYSTEM)
	public R bind(@RequestBody SysUserEntity sysUser,HttpServletRequest request, HttpServletResponse response){
		String authToken = sysUser.getAuthToken();
		String authCode = sysUser.getAuthCode();//验证码
		if(StringUtils.isBlank(authToken)) {
			return R.error(RCode.SYS_AUTHTOKEN_ISNULL);
		}
		
		if(StringUtils.isBlank(authCode)) {
			return R.error(RCode.SYS_AUTHCODE_ISNULL);
		}
		
		AuthTokenEntity authTokenEntity = sysLoginRedis.get(authToken);
		
		Map<String, Object> obj = new HashMap<>();
		if(authTokenEntity!=null) {
			Long userId = authTokenEntity.getUserId();
			SysUserEntity user = sysUserService.getById(userId);
			String userName = "";
			if(user!=null) {
				userName = user.getName();
				obj.put("username", userName);
			}
			
			String authKey = authTokenEntity.getAuthKey();
			Integer mfaLevel = authTokenEntity.getMfaLevel();
			
			//进行authKey+ authCode 验证
			String otpKey = user.getOtpKey();
			if(isValidAuthCode(authKey,authCode)) {//验证成功
				
				//验证成功后 生成10组recovery code
				List<String> list = MfaUtils.getRecoveryCode();
				obj.put("recoveryCode", list);
				
				//并保存recovery code到 sys_user
				SysUserEntity entity = new SysUserEntity();
				entity.setRecoveryCode(StrUtil.join(",", list));
				entity.setOtpKey(authKey);
				if(mfaLevel!=null) {
					entity.setMfaLevel(mfaLevel);
				}
				sysUserService.update(entity, new UpdateWrapper<SysUserEntity>().lambda().eq(SysUserEntity::getId, userId));
				
			}else {//验证失败
				return R.error(RCode.SYS_AUTHCODE_IS_INVALID)
						.putData("username", userName)
						.putData("authToken", authToken)
						.putData("authKey", authKey)
						.putData("otpauth", MfaUtils.getAuthenticatorBarCode(authKey, userName));
			}
		}else {
			return R.error(RCode.SYS_AUTHTOKEN_ISVALID);
		}
		
		return R.ok(obj);
	}
	
	private boolean isValidAuthCode(String authKey,String authCode) {
		int nowCode = MfaUtils.getNowTOTPCode(authKey);
		int lastCode = MfaUtils.getLastTOTPCode(authKey);
		int nextCode = MfaUtils.getNextTOTPCode(authKey);
		if(StrUtil.equals(nowCode+"", authCode) || StrUtil.equals(lastCode+"", authCode)
				||  StrUtil.equals(nextCode+"", authCode)) {//验证成功
			return true;
		}else {
			return false;
		}
	}
	
	@ResponseBody
	@RequestMapping(value = "/verify", method = RequestMethod.POST)
	@SysLog(operation = OperationEnum.UNKNOWN,type = TypeEnum.SYSTEM)
	public R verify(@RequestBody SysUserEntity sysUser,HttpServletRequest request, HttpServletResponse response){
		String authToken = sysUser.getAuthToken();
		String authCode = sysUser.getAuthCode();//验证码（超过6位是 recovery code）
		if(StringUtils.isBlank(authToken)) {
			return R.error(RCode.SYS_AUTHTOKEN_ISNULL);
		}
		if(StringUtils.isBlank(authCode)) {
			return R.error(RCode.SYS_AUTHCODE_ISNULL);
		}
		Map<String, Object> obj = new HashMap<>();
		AuthTokenEntity authTokenEntity = sysLoginRedis.get(authToken);
		if(authCode.length()>6) {//超过6位是 recovery code,recovery code 一个只能用一次？？
			
			if(authTokenEntity!=null){
				Long userId = authTokenEntity.getUserId();
				SysUserEntity user = sysUserService.getById(userId);
				String recoveryCodes = user.getRecoveryCode();
				List<String> recoveryCodeList = StrUtil.split(recoveryCodes, ',');
				if(recoveryCodeList.contains(authCode)) {
					recoveryCodeList.remove(authCode);
					SysUserEntity entity = new SysUserEntity();
					entity.setRecoveryCode(StrUtil.join(",", recoveryCodeList));
					sysUserService.update(entity, new UpdateWrapper<SysUserEntity>().lambda().eq(SysUserEntity::getId, userId));
					Subject subject = ShiroUtils.getSubject();
					String loginToken=CommonUtils.uuid();
					subject.getSession().setAttribute("token",loginToken);
					obj.put("token", subject.getSession().getId().toString());
			
				}else {
					String newAuthToken =  MfaUtils.getAuthToken();
					String authKey = MfaUtils.generateSecretKey();
					if(!updateAuthToken(authToken, userId, newAuthToken,authKey,null)) {
						return R.error(RCode.SYS_TOKEN_REFRESH_FREQUENT);//超过10 响应操作频繁，请30分钟后重试
					}
					return R.error(RCode.SYS_RECOVERYCODE_IS_INVALID).putData("authToken", newAuthToken);
				}
			}else {
				return R.error(RCode.SYS_AUTHTOKEN_ISVALID);
			}
		}else {//authCode
			if(authTokenEntity!=null && isValidAuthCode(authTokenEntity.getAuthKey(),authCode)) {//authCode验证通过
				Subject subject = ShiroUtils.getSubject();
				String loginToken=CommonUtils.uuid();
				subject.getSession().setAttribute("token",loginToken);
				obj.put("token", subject.getSession().getId().toString());
			}else {//进行验证:当 authCode 验证失败后 ，返回新的 authToken
				if(authTokenEntity==null) {
					return R.error(RCode.SYS_AUTHTOKEN_ISVALID);
				}
				String newAuthToken = MfaUtils.getAuthToken();
				Long userId = authTokenEntity.getUserId();
				String authKey = MfaUtils.generateSecretKey();//是否需要存入用户表的otp_key:是
				if(!updateAuthToken(authToken, userId, newAuthToken,authKey,null)) {
					return R.error(RCode.SYS_TOKEN_REFRESH_FREQUENT);//超过10 响应操作频繁，请30分钟后重试
				}
				return R.error(RCode.SYS_AUTHCODE_IS_INVALID).putData("authToken", newAuthToken);
			}
		}
		
		//组织返回信息
		SysConfigEntity sysConfigEntity = configService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, "mfa_auth_enable"));
		String mfaAuthEnableStr = sysConfigEntity.getParamValue();
		int mfaAuthEnable = StrUtil.isEmpty(mfaAuthEnableStr) ? 0 : Integer.valueOf(mfaAuthEnableStr);
		obj.put("mfaAuthEnable", mfaAuthEnable);
		
		// 查询系统配置信息
		String[] paramkeys = {"system_name", "system_logo", "timezone", "default_cabinet_usize", "map_center_config", "unsaved_change","prometheus_federation_enabled"};
		List<SysConfigEntity> configEntityList = configService.list(new LambdaUpdateWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey, paramkeys));
		for (SysConfigEntity configEntity : configEntityList) {
			if ("timezone".equals(configEntity.getParamKey())) {
				Timezone timezoneEntity = timezoneService.getOne(new LambdaUpdateWrapper<Timezone>().eq(Timezone::getName, configEntity.getParamValue()));
				obj.put("offset", timezoneEntity.getOffset());
			}
			obj.put(ToolUtil.toCamelCase(configEntity.getParamKey()), configEntity.getParamValue());
		}

        return R.ok(obj);
	}
}
