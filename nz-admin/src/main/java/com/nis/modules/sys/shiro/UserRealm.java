package com.nis.modules.sys.shiro;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.ToolUtil;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextMapper;
import org.springframework.ldap.core.LdapEntryIdentification;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.HardcodedFilter;
import org.springframework.ldap.filter.OrFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapName;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 认证
 */
@Component
public class UserRealm extends AuthorizingRealm {

	private static final Log log = Log.get();
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

	@Value("${ldap.default.mapping:{\"email\":\"mail\",\"name\":\"uid|sAMAccountName|cn\",\"mobile\":\"mobile\",\"username\":\"uid|sAMAccountName|cn\"}}")
	private String defaultMappingStr;

	private static final String LDAP_MAPPING_USERNAME_KEY = "username";

	/**
	 * 授权(验证权限时调用)
	 */
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		SysUserEntity user = (SysUserEntity) principals.getPrimaryPrincipal();

		// 查看用户所有角色
		List<SysRoleEntity> rolesByUesr = null;
		
		if(ToolUtil.isNotEmpty(user.getApiKeyId())) {
			rolesByUesr = sysRoleDao.getRolesBySysApiKeyId(user.getApiKeyId());
		}else {
			rolesByUesr = sysRoleDao.getRolesByUesrId(user.getId().intValue());
		}

		List<String> permsList;
		if (CollectionUtils.isEmpty(rolesByUesr)) {
			permsList = Collections.emptyList();
		} else {
			SysRoleEntity buildRole = rolesByUesr.stream()
					.filter(sysRoleEntity -> sysRoleEntity.getId().equals(Constant.SUPER_ADMIN)).findAny().orElse(null);
			// admin 角色 拥有所有接口权限
			List<SysMenuEntity> menuList;
			if (buildRole != null) {
				menuList = sysMenuService.list();
			} else {
				Set<Integer> roleIds = rolesByUesr.stream().map(SysRoleEntity::getId).collect(Collectors.toSet());
				List<SysRoleMenuEntity> sysRoleMenuEntities = sysRoleMenuDao.selectList(
						new LambdaQueryWrapper<SysRoleMenuEntity>().in(SysRoleMenuEntity::getRoleId, roleIds));
				if (CollectionUtils.isNotEmpty(sysRoleMenuEntities)) {
					// 所有叶子节点
					Set<Integer> menuIds = sysRoleMenuEntities.stream().map(SysRoleMenuEntity::getMenuId)
							.collect(Collectors.toSet());
					menuList = sysMenuService
							.list(new LambdaQueryWrapper<SysMenuEntity>().in(SysMenuEntity::getId, menuIds));
					// 根据叶子节点得到完整的权限
					menuList = sysMenuService.getMenuByLeafMenu(menuList, null);
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
			if (StringUtils.isEmpty(perms))
				continue;
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
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken)
			throws AuthenticationException {
		UsernamePasswordToken token = (UsernamePasswordToken) authcToken;

		// 查询用户信息
		SysUserEntity user = sysUserDao.selectOne(new QueryWrapper<SysUserEntity>().eq("username", token.getUsername()));
		// 账号不存在，使用ldap验证
		if (user == null) {
			StringBuilder sb = new StringBuilder();
			sb.append(token.getPassword());
			user = getLdapAuthentication(token.getUsername(), sb.toString());
			if (user == null) {
				throw new IncorrectCredentialsException("账号或密码不正确");
			}
			user.setSource("ldap");
			user.setStatus(1);
			user.setPinTime(new Date());
			sysUserDao.insert(user);

			// ldap 登录用户默认分配 common 角色
			SysUserRoleEntity sysUserRole = new SysUserRoleEntity();
			sysUserRole.setUserId(user.getId().intValue());
			sysUserRole.setRoleId(Constant.SYSTEM_COMMON_ROLE);
			sysUserRoleDao.insert(sysUserRole);
			String salt = RandomStringUtils.randomAlphanumeric(20);
			user.setSalt(salt);
			user.setPin(ShiroUtils.sha256(sb.toString(), salt));
		} else if(user.getSource().equals("ldap")){
			//ldap非首次登陆，更新同步内容
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(token.getPassword());
			SysUserEntity tmpUser = getLdapAuthentication(token.getUsername(), stringBuilder.toString());
			if(tmpUser == null) {
				// 验证失败 则登录失败
				throw new IncorrectCredentialsException("账号或密码不正确");
			}
			@SuppressWarnings("unchecked")
			Map<String,String> defaultMapping = JSONUtil.toBean(defaultMappingStr, HashMap.class);
			for(Entry<String,String> entry : defaultMapping.entrySet()) {
				String key = entry.getKey();
				String value = BeanUtil.getProperty(tmpUser, key);
				BeanUtil.setProperty(user, key, value);
			}
			sysUserDao.updateById(user);
			String salt = RandomStringUtils.randomAlphanumeric(20);
			user.setSalt(salt);
			user.setPin(ShiroUtils.sha256(stringBuilder.toString(), salt));
		} else {
			// 判断系统内用户账号是否锁定
			if (user.getStatus() == 0) {
				throw new LockedAccountException("账号已被锁定,请联系管理员");
			}
		}
		SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, user.getPin(),ByteSource.Util.bytes(user.getSalt()), getName());
		return info;
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
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public SysUserEntity getLdapAuthentication(String userName, String pin) {
		// 获取sys_config中的ldap信息 判断是否支持ldap登录
		Map<String, Object> ldapInfos = sysConfigService.getConfigListByTypes(new String[] { "ldap" }, null);
		Map<String, String> ldap = (Map<String, String>) ldapInfos.get("ldap");
		// 判断是否开启 ldap 认证
		if (ObjectUtils.isEmpty(ldap) || StrUtil.isBlank(ldap.get("ldap_enable"))
				|| "off".equalsIgnoreCase(ldap.get("ldap_enable"))) {
			return null;
		}
		String ldapAddress = ldap.get("ldap_address");
		String ldapDN = ldap.get("ldap_dn");
		String ldapPassword = ldap.get("ldap_password");
		ldapPassword = StrUtil.str(Base64.getDecoder().decode(ldapPassword), "UTF-8");
		String ldapUserFilter = ldap.get("ldap_user_filter");
		String ldapMapping = ldap.get("ldap_mapping");
		String ldapOu = ldap.get("ldap_ou");

		if (StrUtil.isBlank(ldapAddress) || StrUtil.isBlank(ldapDN) || StrUtil.isBlank(ldapPassword)
				|| StrUtil.isBlank(ldapUserFilter)) {
			// 参数不正确
			return null;
		}
		//用户配置 mapping
		Map<String, String> configMapping = JSONUtil.toBean(ldapMapping, HashMap.class);
		Map<String,String> defaultMapping = JSONUtil.toBean(defaultMappingStr, HashMap.class);
		LdapContextSource ldapContextSource = new LdapContextSource();
		ldapContextSource.setUrl(ldapAddress);
		ldapContextSource.setUserDn(ldapDN);
		ldapContextSource.setPassword(ldapPassword);
		ldapContextSource.setReferral("follow");
		ldapContextSource.afterPropertiesSet(); // important

		LdapTemplate ldapTemplate = new LdapTemplate(ldapContextSource);
		// 组织filter
		AndFilter queryFilter = new AndFilter();
		// username filter
		String uname = StrUtil.isNotBlank(configMapping.get(LDAP_MAPPING_USERNAME_KEY))
				? configMapping.get(LDAP_MAPPING_USERNAME_KEY)
				: defaultMapping.get(LDAP_MAPPING_USERNAME_KEY);
		List<String> unameList = StrUtil.splitTrim(uname, "|");
		OrFilter orFilter = new OrFilter();
		for (String n : unameList) {
			orFilter.append(new EqualsFilter(n, userName));
		}
		queryFilter.append(orFilter);
		if (StrUtil.isNotEmpty(ldapUserFilter)) {
			queryFilter.append(new HardcodedFilter(ldapUserFilter));
		}
		// 支持配置多个 ou 搜索用户
		List<String> baseList = StrUtil.splitTrim(ldapOu, "|");
		SysUserEntity entity = null;
		for (String base : baseList) {
			LdapQuery query = LdapQueryBuilder.query().base(base).timeLimit(20000).countLimit(1000).filter(queryFilter);
			try {
				// 开始认证
				entity = ldapTemplate.authenticate(query, pin,new AuthenticatedLdapEntryContextMapper<SysUserEntity>() {
							@Override
							public SysUserEntity mapWithContext(DirContext ctx,LdapEntryIdentification ldapEntryIdentification) {
								LdapName ldapName = ldapEntryIdentification.getAbsoluteName();
								Attributes attributes;
								SysUserEntity user = null;
								try {
									attributes = ctx.getAttributes(ldapName);
									Map<String, String> attrMap = new HashMap<String, String>();
									// 获取用户属性导入 系统
									for (Entry<String, String> entry : defaultMapping.entrySet()) {
										String key = entry.getKey();
										String attrKey = StrUtil.isNotBlank(configMapping.get(key)) ? configMapping.get(key) : entry.getValue();
										List<String> attrKeyList = StrUtil.splitTrim(attrKey, "|");
										for (String ak : attrKeyList) {
											Object obj = attributes.get(ak) != null ? attributes.get(ak).get() : null;
											if (obj != null) {
												attrMap.put(key, obj.toString());
												break;
											}
										}
									}
									// 赋值 sysuser
									user = new SysUserEntity();
									BeanUtil.fillBeanWithMap(attrMap, user, true);
								} catch (NamingException e) {
									log.debug(e);
								}
								LdapUtils.closeContext(ctx);
								return user;
							}
						});
			} catch (RuntimeException e) {
				log.warn(e,"ldap Authentication failed,base: {}",base);
			}
			if (entity != null) {
				break;
			}
		}
		return entity;
	}

}
