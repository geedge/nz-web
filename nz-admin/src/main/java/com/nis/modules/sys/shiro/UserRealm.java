/**

 *

 *

 */

package com.nis.modules.sys.shiro;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.R;
import com.nis.modules.sys.dao.SysMenuDao;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysMenuEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 认证
 *

 */
@Component
public class UserRealm extends AuthorizingRealm {
    @Autowired
    private SysUserDao sysUserDao;
    @Autowired
    private SysMenuDao sysMenuDao;
    @Autowired
    private SysConfigService sysConfigService;
    /**
     * 授权(验证权限时调用)
     */
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		SysUserEntity user = (SysUserEntity)principals.getPrimaryPrincipal();
		Long userId = user.getUserId();
		
		List<String> permsList;
		
		//系统管理员，拥有最高权限
		if(userId == Constant.SUPER_ADMIN){
			List<SysMenuEntity> menuList = sysMenuDao.selectList(null);
			permsList = new ArrayList<>(menuList.size());
			for(SysMenuEntity menu : menuList){
				permsList.add(menu.getPerms());
			}
		}else{
			permsList = sysUserDao.queryAllPerms(userId);
		}

		//用户权限列表
		Set<String> permsSet = new HashSet<>();
		for(String perms : permsList){
			if(StringUtils.isBlank(perms)){
				continue;
			}
			permsSet.addAll(Arrays.asList(perms.trim().split(",")));
		}
		
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
		info.setStringPermissions(permsSet);
		return info;
	}

	/**
	 * 认证(登录时调用)
	 */
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(
			AuthenticationToken authcToken) throws AuthenticationException {
		UsernamePasswordToken token = (UsernamePasswordToken)authcToken;

		//查询用户信息
		SysUserEntity user = sysUserDao.selectOne(new QueryWrapper<SysUserEntity>().eq("username", token.getUsername()));
		//账号不存在
		if(user == null) {
			R result = getLdapAuthentication(token.getUsername(),new String(token.getPassword()));
			if(ObjectUtils.isNotEmpty(result)) {
				LinkedList resultList = (LinkedList) result.get("data");
				
				// 验证成功 将当前用户信息存入数据库 同时登录成功
				SysUserEntity userInfo =new SysUserEntity();
				userInfo.setCreateTime(DateUtil.getUTCTimeByConfigTimeZone());
				userInfo.setUsername(token.getUsername());
				//sha256加密
				/*String salt = RandomStringUtils.randomAlphanumeric(20);
				userInfo.setSalt(salt);
				userInfo.setPassword(ShiroUtils.sha256(new String(token.getPassword()), userInfo.getSalt()));*/
				userInfo.setSource("ldap");
				userInfo.setStatus(1);
				
				//如果有返回的映射信息 则将ldap中的信息赋值给nezha用户信息中
				if(ObjectUtils.isNotEmpty(resultList)) {
					 Map ldapResult = (Map) resultList.get(0);
					 List<String> fieldNameList=new ArrayList<String>();
					 Map<String,Method> fieldSetMethodMap=new HashMap<String,Method>();
					 
					 Field[] allField = SysUserEntity.class.getDeclaredFields();
					 Method m = null;
					 String methodName;
					 for(Field f: allField){
						 methodName = f.getName();
						 methodName = "set"+methodName.substring(0, 1).toUpperCase()+methodName.substring(1);
						 try {
							 //只返回和当前字段类型相符的set方法，不支持多参数以及不同类型的set方法
							 m = SysUserEntity.class.getDeclaredMethod(methodName, f.getType());
							 if(m != null){
							 	 fieldNameList.add(f.getName());
							 	 fieldSetMethodMap.put(f.getName(), m);
							 }
						 } catch (Exception e) {
							 e.printStackTrace();
						 }
					}
					 // 利用反射 将ldap查询出的数据一一对应赋值给实体类
					Set<String> keySet = ldapResult.keySet();
					for(String set:keySet) {
						if(ObjectUtils.isNotEmpty(ldapResult.get(set))&&ObjectUtils.isNotEmpty(fieldSetMethodMap.get(set))) {
							try {
								fieldSetMethodMap.get(set).invoke(userInfo, ldapResult.get(set));
							} catch (Exception e) {
								e.printStackTrace();
								throw new UnknownAccountException("账号或密码不正确");
							} 
						}
					} 
				}
				sysUserDao.insert(userInfo);
				String salt = RandomStringUtils.randomAlphanumeric(20);
				userInfo.setSalt(salt);
				userInfo.setPassword(ShiroUtils.sha256(new String(token.getPassword()), userInfo.getSalt()));
				SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(userInfo, userInfo.getPassword(), ByteSource.Util.bytes(userInfo.getSalt()), getName());
				return info;
			}else {
				// 验证失败 则登录失败
				throw new IncorrectCredentialsException("账号或密码不正确");
			}
		}else if(user.getSource().equals("ldap")) {
			// 判断用户是否为ldap用户 是则进行ldap验证登录
			R result = getLdapAuthentication(token.getUsername(),new String(token.getPassword()));
			if(ObjectUtils.isNotEmpty(result)) {
				String salt = RandomStringUtils.randomAlphanumeric(20);
				user.setSalt(salt);
				user.setPassword(ShiroUtils.sha256(new String(token.getPassword()), user.getSalt()));
				SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, user.getPassword(), ByteSource.Util.bytes(user.getSalt()), getName());
				return info;
			}else {
				throw new IncorrectCredentialsException("账号或密码不正确");
			}
		}else {
			//账号锁定
			if(user.getStatus() == 0){
				throw new LockedAccountException("账号已被锁定,请联系管理员");
			}
			SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, user.getPassword(), ByteSource.Util.bytes(user.getSalt()), getName());
			return info;
		}
	}

	@Override
	public void setCredentialsMatcher(CredentialsMatcher credentialsMatcher) {
		HashedCredentialsMatcher shaCredentialsMatcher = new HashedCredentialsMatcher();
		shaCredentialsMatcher.setHashAlgorithmName(ShiroUtils.hashAlgorithmName);
		shaCredentialsMatcher.setHashIterations(ShiroUtils.hashIterations);
		super.setCredentialsMatcher(shaCredentialsMatcher);
	}
	
	/**
	 * ldap登录验证
	 * @return
	 */
	public R getLdapAuthentication(String userName,String password) {
		// 获取sys_config中的ldap信息 判断是否支持ldap登录
		Map ldapInfos = sysConfigService.getConfigListByTypes(new String[] {"ldap"}, null);
		Map ldap = (Map) ldapInfos.get("ldap");
		if(ObjectUtils.isEmpty(ldap)) {
			return null;
		}
		Object ldapEnable = ldap.get("ldap_enable");
		// 如果不支持用户ldap登录同时用户信息为空 则登录失败
		if(ObjectUtils.isEmpty(ldapEnable)) {
			return null;
		}else if(ldapEnable.equals("off")){
			return null;
		}else{
			String ldapAddress = (String) ldap.get("ldap_address");
			String ldapDN = (String) ldap.get("ldap_dn");
			String ldapPassword = (String) ldap.get("ldap_password");
			String ldapUserFilter = (String) ldap.get("ldap_user_filter");
			String ldapMapping = (String) ldap.get("ldap_mapping");
			String ldapOu = (String) ldap.get("ldap_ou");
			if(StringUtils.isEmpty(ldapAddress)||StringUtils.isEmpty(ldapDN)||StringUtils.isEmpty(ldapPassword)||StringUtils.isEmpty(ldapUserFilter)) {
				// 参数不正确
				return null;
			}
			
			String[] split = ldapDN.split(",");
			if(split.length<2) {
				// dn格式不正确 返回错误
				return null;
			}
			String base = split[split.length-2]+","+split[split.length-1];
			if(StringUtils.isNotBlank(ldapOu)) {
				base="ou="+ldapOu+","+base;
			}
			// 允许ldap登录 进行ldap验证
			LdapContextSource ldapContextSource = new LdapContextSource();
			ldapContextSource.setUrl(ldapAddress);
			ldapContextSource.setUserDn(ldapDN);
	        ldapContextSource.setPassword(ldapPassword);
	        ldapContextSource.setReferral("follow");
	        ldapContextSource.afterPropertiesSet(); // important
			
			LdapTemplate ldapTemplate =new LdapTemplate(ldapContextSource);
			
			AndFilter filter = new AndFilter();
			filter.and(new EqualsFilter(ldapUserFilter, userName));
			boolean authenticate = ldapTemplate.authenticate(base,filter.toString(),password);
			if(authenticate&&StringUtils.isNotBlank(ldapMapping)) {
				//如果可登录并且有用户设定映射 则查询映射内容补充到系统用户表信息中
				try {
//					ldapTemplate.authenticate(LdapQueryBuilder.query().base("dc=bifang,dc=com").where("uid").is("th"), "th");
//					ldapTemplate.authenticate(LdapQueryBuilder.query().where("uid").is(userName), password);
//					ldapTemplate.authenticate("th","(uid=%s)","th");
					Map<String,String> ldapMap = (Map) JSON.parse(ldapMapping);
					List<Map> search = ldapTemplate.search(LdapQueryBuilder.query().base(base)
			                .where(ldapUserFilter).is(userName), new ContextMapper<Map>() {
	                    @Override
	                    public Map mapFromContext(Object ctx) throws NamingException {
	                        DirContextAdapter  context = (DirContextAdapter) ctx;
	                        Set<String> keySet = ldapMap.keySet();
	                        Map<String,Object> result =new HashMap<String,Object>();
	                        for(String key : keySet) {
	                        	String ldapValue = context.getStringAttribute(ldapMap.get(key));
	                        	if(StringUtils.isNotBlank(ldapValue)) {
	                        		result.put(key, ldapValue);
	                        	}
	                        }
	                        return result;
	                    }
	                });
					return R.ok(search);

				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}else if(authenticate){
				return R.ok();
			}else {
				return null;
			}
			
		}
	}
}
