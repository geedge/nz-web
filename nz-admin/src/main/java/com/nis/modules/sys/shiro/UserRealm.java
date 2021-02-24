package com.nis.modules.sys.shiro;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.R;
import com.nis.modules.sys.dao.SysRoleDao;
import com.nis.modules.sys.dao.SysRoleMenuDao;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.dao.SysUserRoleDao;
import com.nis.modules.sys.entity.*;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysMenuService;
import org.apache.commons.collections.CollectionUtils;
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
import java.util.stream.Collectors;

/**
 * 认证
 */
@Component
public class UserRealm extends AuthorizingRealm {

    @Autowired
    private SysUserDao sysUserDao;

	@Autowired
    private SysRoleDao sysRoleDao;

	@Autowired
    private SysRoleMenuDao sysRoleMenuDao;

	@Autowired
	private SysMenuService sysMenuService;

	@Autowired
    private SysConfigService sysConfigService;

	@Autowired
	private SysUserRoleDao sysUserRoleDao;

    /**
     * 授权(验证权限时调用)
     */
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		SysUserEntity user = (SysUserEntity)principals.getPrimaryPrincipal();

		// 查看用户所有角色
		List<SysRoleEntity> rolesByUesr = sysRoleDao.getRolesByUesrId(user.getUserId().intValue());

		List<String> permsList;
		if (CollectionUtils.isEmpty(rolesByUesr)) {
			permsList = Collections.emptyList();
		} else {
			SysRoleEntity buildRole = rolesByUesr.stream().filter(sysRoleEntity -> sysRoleEntity.getId().equals(Constant.SUPER_ADMIN)).findAny().orElse(null);
			// admin 角色 拥有所有接口权限
			List<SysMenuEntity> menuList;
			if (buildRole != null) {
				menuList = sysMenuService.list();
			} else {
				Set<Integer> roleIds = rolesByUesr.stream().map(SysRoleEntity::getId).collect(Collectors.toSet());
				List<SysRoleMenuEntity> sysRoleMenuEntities = sysRoleMenuDao.selectList(new LambdaQueryWrapper<SysRoleMenuEntity>().in(SysRoleMenuEntity::getRoleId, roleIds));
				if (CollectionUtils.isNotEmpty(sysRoleMenuEntities)) {
					// 所有叶子节点
					Set<Integer> menuIds = sysRoleMenuEntities.stream().map(SysRoleMenuEntity::getMenuId).collect(Collectors.toSet());
					menuList = sysMenuService.list(new LambdaQueryWrapper<SysMenuEntity>().in(SysMenuEntity::getId, menuIds));
					// 根据叶子节点得到完整的权限
					menuList = sysMenuService.getMenuByLeafMenu(menuList);
				} else {
					menuList = Collections.emptyList();
				}
			}

			permsList = new ArrayList<>();
			for (SysMenuEntity menu : menuList) {
				permsList.add(menu.getPerms());
			}
		}

		// 用户权限列表
		Set<String> permsSet = new HashSet<>();
		for (String perms : permsList) {
			if (StringUtils.isEmpty(perms)) continue;
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

				// ldap 登录用户默认分配 common 角色
				SysUserRoleEntity sysUserRole = new SysUserRoleEntity();
				sysUserRole.setUserId(userInfo.getUserId().intValue());
				sysUserRole.setRoleId(Constant.SYSTEM_COMMON_ROLE);
				sysUserRoleDao.insert(sysUserRole);

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
			ldapPassword = new String(Base64.getDecoder().decode(ldapPassword));
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
				base=ldapOu;
			}
			// 允许ldap登录 进行ldap验证 判断连接是ldap还是ldaps
			LdapContextSource ldapContextSource=null;
			if(ldapAddress.startsWith("ldaps")) {
				ldapContextSource = new SSLLdapContextSource();
			}else {
				ldapContextSource = new LdapContextSource();
			}
			ldapContextSource.setUrl(ldapAddress);
			ldapContextSource.setUserDn(ldapDN);
	        ldapContextSource.setPassword(ldapPassword);
	        ldapContextSource.setReferral("follow");
	        ldapContextSource.setPooled(false);  
	        ldapContextSource.afterPropertiesSet(); // important
			
			LdapTemplate ldapTemplate =new LdapTemplate(ldapContextSource);

			// 设置 ldap 超时时间 0 表示不限制, 单位 milliseconds 毫秒
			SysConfigEntity ldapQueryTimeOutConfig = sysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_LDAP_QUERY_TIMEOUT));
			int ldapQueryTimeOut = StringUtils.isEmpty(ldapQueryTimeOutConfig.getParamValue()) ? 0 : Integer.valueOf(ldapQueryTimeOutConfig.getParamValue());
			ldapTemplate.setDefaultTimeLimit(ldapQueryTimeOut);

			AndFilter filter = new AndFilter();
			filter.and(new EqualsFilter("uid", userName));
			// 将用户设置的filter以及用户的名称uid组装
			if(ldapUserFilter.contains("&")) {
				ldapUserFilter=ldapUserFilter.substring(0, ldapUserFilter.length()-1)+filter.encode()+")";
			}else {
				ldapUserFilter="(&{}{})".format(ldapUserFilter, filter.encode());
			}
			boolean authenticate = ldapTemplate.authenticate(base,filter.toString(),password);
			if(authenticate&&StringUtils.isNotBlank(ldapMapping)) {
				//如果可登录并且有用户设定映射 则查询映射内容补充到系统用户表信息中
				try {
//					ldapTemplate.authenticate(LdapQueryBuilder.query().base("dc=bifang,dc=com").where("uid").is("th"), "th");
//					ldapTemplate.authenticate(LdapQueryBuilder.query().where("uid").is(userName), password);
//					ldapTemplate.authenticate("th","(uid=%s)","th");
					Map<String,String> ldapMap = (Map) JSON.parse(ldapMapping);
					List<Map> search = ldapTemplate.search(LdapQueryBuilder.query().base(base)
			                .filter(filter.toString()), new ContextMapper<Map>() {
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
