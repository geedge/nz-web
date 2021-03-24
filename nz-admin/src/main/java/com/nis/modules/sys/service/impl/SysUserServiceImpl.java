package com.nis.modules.sys.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertNotifiScriptDao;
import com.nis.modules.alert.entity.AlertNotificationScript;
import com.nis.modules.sys.dao.SysRoleDao;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysRoleEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.SysUserNotification;
import com.nis.modules.sys.entity.SysUserRoleEntity;
import com.nis.modules.sys.service.*;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Service("sysUserService")
public class SysUserServiceImpl extends ServiceImpl<SysUserDao, SysUserEntity> implements SysUserService {

	private static Log log = Log.get();

	@Autowired
	private SysUserNotificationService notificationService;

	@Autowired
	private AlertNotifiScriptDao alertNotifiScriptDao;

	@Autowired
	private SysRoleService sysRoleService;

	@Autowired
	private SysUserRoleService sysUserRoleService;

	@Autowired
	private SysRoleDao sysRoleDao;

	@Autowired
	private SysMenuService sysMenuService;

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private RedisConnectionFactory factory;

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		IPage<SysUserEntity> page = new Query<SysUserEntity>().getPage(params);
		List<SysUserEntity> userList = baseMapper.queryList(page, params);

		// 补充 Notificationscript roles 信息
		if (CollectionUtils.isNotEmpty(userList)) {
			List<SysUserNotification> sysUserNotifications = notificationService.list();
			Map<Integer, List<SysUserNotification>> userIdAndScriptListMap = sysUserNotifications.stream().collect(Collectors.groupingBy(SysUserNotification::getUserId));

			List<SysUserRoleEntity> userRoleList = sysUserRoleService.list();
			Map<Integer, List<SysUserRoleEntity>> userRoleMap = userRoleList.stream().collect(Collectors.groupingBy(SysUserRoleEntity::getUserId));
			List<SysRoleEntity> roleList = sysRoleService.list();
			Map<Integer, SysRoleEntity> roleIdAndEntityMap = new HashMap<>();
			for (SysRoleEntity sysRoleEntity : roleList) {
				roleIdAndEntityMap.put(sysRoleEntity.getId(), sysRoleEntity);
			}

			for (SysUserEntity userEntity : userList) {
				userEntity.setNotifications(userIdAndScriptListMap.get(userEntity.getUserId().intValue()));

				List<SysUserRoleEntity> userRoleEntities = userRoleMap.get(userEntity.getUserId().intValue());
				if (CollectionUtils.isEmpty(userRoleEntities)) {
					userEntity.setRoles(Collections.emptyList());
				} else {
					List<SysRoleEntity> roles = new ArrayList<>();
					for (SysUserRoleEntity userRoleEntity : userRoleEntities) {
						roles.add(roleIdAndEntityMap.get(userRoleEntity.getRoleId()));
					}
					userEntity.setRoles(roles);
				}
			}
		}

		page.setRecords(userList);
		return new PageUtils(page);
	}

    private void validateUserInfo(SysUserEntity user) {
	    Integer status = user.getStatus();
        ValidateUtils.is(user.getUsername()).notNull(RCode.SYS_USER_USERNAME_ISNULL)
                .and(status).notNull(RCode.SYS_USER_STATUS_ISNULL);

	    if(status != 0 && status != 1){
	    	throw new NZException(RCode.SYS_USER_STATUS_INVALIDE);
	    }
	    String email = user.getEmail();
	    if(StringUtils.isNotEmpty(email)){
	    	ValidateUtils.is(email).email(RCode.SYS_USER_EMAIL_FORMAT);
	    }

	    List<SysUserNotification> notifications = user.getNotifications();
        if (CollectionUtils.isNotEmpty(notifications)) {
	        List<AlertNotificationScript> scripts = alertNotifiScriptDao.selectList(null);
	        List<Integer> scriptIds = scripts.stream().map(AlertNotificationScript::getId).distinct().collect(Collectors.toList());

	        int size = notifications.size();
            Map<String, Integer> map = new HashMap();
            // 新增 校验 notifications scriptId accout 是否重复
	        for (SysUserNotification notification : notifications) {
		        if (ToolUtil.isEmpty(notification.getScriptId())) {
			        throw new NZException(RCode.ALERT_NOTIFISCRIPT_ID_ISNULL);
		        }

		        if (!scriptIds.contains(notification.getScriptId())) {
			        throw new NZException(RCode.ALERT_NOTIFISCRIPT_SCRIPT_NOTFOUND);
		        }

		        if (StringUtils.isEmpty(notification.getAccount())) {
			        throw new NZException(RCode.ALERT_NOTIFISCRIPT_ACCOUNT_ISNULL);
		        }
		        map.put(notification.getScriptId() + notification.getAccount(), 1);
	        }

            if (size != map.size()) {
                throw new NZException(RCode.SYS_USER_SCRIPT_ACCOUNT_REPEATED);
            }
        }

	    // 校验角色
	    List<Integer> roleIds = user.getRoleIds();
	    if (CollectionUtils.isEmpty(roleIds)) throw new NZException(RCode.SYS_ROLE_ID_ISNULL);

	    List<SysRoleEntity> roleList = sysRoleService.list();
	    List<Integer> roleIdList = roleList.stream().map(SysRoleEntity::getId).collect(Collectors.toList());

	    for (Integer roleId : roleIds) {
		    if (!roleIdList.contains(roleId))
			    throw new NZException(RCode.SYS_ROLE_ISNULL);
	    }
    }

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveUser(SysUserEntity user) {

		// 校验 user 相关信息
		this.validateUserInfo(user);

		user.setCreateTime(DateUtil.getUTCTimeByConfigTimeZone());
		//sha256加密
		String salt = RandomStringUtils.randomAlphanumeric(20);
		user.setSalt(salt);
		user.setPin(ShiroUtils.sha256(user.getPin(), user.getSalt()));
		this.save(user);

		// 关联 script 不为空则保存
		List<SysUserNotification> notifications = user.getNotifications();
		if (CollectionUtils.isNotEmpty(notifications)) {
			for (SysUserNotification notification : notifications) {
				notification.setUserId(user.getUserId().intValue());
			}
			notificationService.saveBatch(notifications);
		}

		// 保存用户 和 角色 关联关系
		sysUserRoleService.saveOrUpdate(user.getUserId(), user.getRoleIds());
	}


	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysUserEntity user) {

		// 校验 user 相关信息
		this.validateUserInfo(user);

		if (StringUtils.isBlank(user.getPin())) {
			user.setPin(null);
		} else {
			// 如果不是本人更改 自己密码的话，删除 redis 中用户session
			Long loginUserId = ShiroUtils.getUserId();
			if (!loginUserId.equals(user.getUserId())) {
				// 通过 userId 删除 redis 中记录的 登录信息
				this.removeUserInfoInRedis(user.getUserId());
			}
			SysUserEntity userEntity = this.getById(user.getUserId());
			user.setPin(ShiroUtils.sha256(user.getPin(), userEntity.getSalt()));
		}

		this.updateById(user);

		// 关联 script 不为空则保存
		List<SysUserNotification> notifications = user.getNotifications();
		if (CollectionUtils.isNotEmpty(notifications)) {
			// 先删除
			notificationService.remove(new QueryWrapper<SysUserNotification>().lambda().eq(SysUserNotification::getUserId,user.getUserId()));
			for (SysUserNotification notification : notifications) {
				notification.setUserId(user.getUserId().intValue());
			}
			notificationService.saveBatch(notifications);
		}

		// 处理用户角色关系
		this.handleUserRoleRelation(user.getUserId(), user.getRoleIds());
	}

	/**
	 * 处理用户 和 角色 关联关系
	 * @param userId
	 * @param roleIds
	 */
	private void handleUserRoleRelation(Long userId, List<Integer> roleIds) {
		// 当前用户关联的角色
		List<SysUserRoleEntity> roles = sysUserRoleService.list(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId.intValue()));
		List<Integer> idList = roles.stream().map(SysUserRoleEntity::getRoleId).distinct().collect(Collectors.toList());

		// 角色信息未改变 不进行处理
		if (CommonUtils.checkIdListDiffrent(idList, roleIds))
			return;

		// 角色变更
		sysUserRoleService.saveOrUpdate(userId, roleIds);

		// 通过 userId 删除 redis 中记录的 登录信息
		this.removeUserInfoInRedis(userId);
	}

	/**
	 * 通过 userId 删除 redis 中记录的 登录信息
	 *
	 * @param userId
	 */
	private void removeUserInfoInRedis(Long userId) {
		// 删除 redis 中的 token
		List<String> removeKeys = new ArrayList<>();
		try {
			Map<String, SysUserEntity> shiroSessionInfoMap = ShiroUtils.getShiroSessionInfoMap();
			for (Map.Entry<String, SysUserEntity> entry : shiroSessionInfoMap.entrySet()) {
				SysUserEntity userEntity = entry.getValue();
				if (userId.equals(userEntity.getUserId())) {
					removeKeys.add(entry.getKey());
				}
			}
		} catch (RuntimeException e) {
			log.error(String.format("从redis中获取用户登录信息失败,错误信息是：%s,当前用户是:%s", e.getMessage(), userId), e);
		} finally {
			// 删除 关联key
			if (CollectionUtils.isNotEmpty(removeKeys)) {
				redisTemplate.delete(removeKeys);
			}
		}
	}


	@Override
	public boolean updatePassword(Long userId, String mima, String newPassword) {
        SysUserEntity userEntity = new SysUserEntity();
        userEntity.setPin(newPassword);
        return this.update(userEntity,
        	new LambdaUpdateWrapper<SysUserEntity>()
					.eq(ToolUtil.isNotEmpty(userId),SysUserEntity::getUserId, userId)
					.eq(ToolUtil.isNotEmpty(mima),SysUserEntity::getPin, mima));
    }

	@Override
	public boolean checkName(SysUserEntity user) {
		List<SysUserEntity> list = this.list(new LambdaUpdateWrapper<SysUserEntity>()
				.eq(ToolUtil.isNotEmpty(user.getUsername()), SysUserEntity::getUsername, user.getUsername())
				.eq(ToolUtil.isNotEmpty(user.getStatus()), SysUserEntity::getStatus, 1));
		boolean dumpFlag = true; //是否名称重复，true为重复

		//当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
		if ((list == null || list.size() == 0) ||
					(list.size() == 1 && list.get(0).getUserId().longValue() == user.getUserId().longValue())) {
			dumpFlag = false;
		}
		return dumpFlag;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteUsers(String userIds) {
		ValidateUtils.is(userIds).notNull(RCode.SYS_USER_ID_ISNULL);
		String[] split = userIds.split(",");
		if (ArrayUtils.contains(split, "1")) {
			throw new NZException(RCode.SYS_USER_DELADMIN);
		}

		SysUserEntity userEntity = (SysUserEntity) SecurityUtils.getSubject().getPrincipal();
		if (ArrayUtils.contains(split, userEntity.getUserId().toString())) {
			throw new NZException(RCode.SYS_USER_CANNOTDEL);
		}

		// 删除 user
		this.removeByIds(Arrays.asList(split));

		// 删除 关联 script 关系
		notificationService.remove(new QueryWrapper<SysUserNotification>().lambda().in(SysUserNotification::getUserId, Arrays.asList(split)));

		// 删除 关联 role 关系
		sysUserRoleService.remove(new LambdaQueryWrapper<SysUserRoleEntity>().in(SysUserRoleEntity::getUserId, Arrays.asList(split)));

		// 删除 redis 中 用户token 信息
		List<String> removeKeys = new ArrayList<>();
		List<Long> userIdList = new ArrayList<>(split.length);
		for (int i = 0; i < split.length; i++) {
			userIdList.add(Long.valueOf(split[i]));
		}
		try {
			Map<String, SysUserEntity> shiroSessionInfoMap = ShiroUtils.getShiroSessionInfoMap();
			for (Map.Entry<String, SysUserEntity> entry : shiroSessionInfoMap.entrySet()) {
				SysUserEntity userInfo = entry.getValue();
				if (userIdList.contains(userInfo.getUserId())) {
					removeKeys.add(entry.getKey());
				}
			}
		} catch (RuntimeException e) {
			log.error(String.format("从redis中获取用户登录信息失败,错误信息是：%s,删除用户ID:%s", e.getMessage(), userIdList.toString()), e);
		} finally {
			// 删除 关联key
			if (CollectionUtils.isNotEmpty(removeKeys)) {
				redisTemplate.delete(removeKeys);
			}
		}
	}

	@Override
	public Map<String, Object> getUserPermissions(HttpServletRequest request) {
		String requestToken = request.getHeader(Constant.AUTH_TOKEN_CODE);
		requestToken = StringUtils.isEmpty(requestToken) ? request.getParameter(Constant.AUTH_TOKEN_CODE) : requestToken;
		if (StringUtils.isEmpty(requestToken))
			throw new NZException(RCode.SYS_TOKEN_ISNULL);

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.setConnectionFactory(factory);

		redisTemplate.afterPropertiesSet();

		Boolean exist = redisTemplate.hasKey(ShiroUtils.REDIS_KEYPREFIX + requestToken);
		if (!exist)
			throw new NZException(RCode.SYS_LOGIN_REQUIRED);

		ValueOperations ops = redisTemplate.opsForValue();
		Object obj = ops.get(ShiroUtils.REDIS_KEYPREFIX + requestToken);
		SimpleSession session = (SimpleSession) obj;
		SimplePrincipalCollection primaryPrincipal = (SimplePrincipalCollection) session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
		SysUserEntity userEntity = (SysUserEntity) primaryPrincipal.getPrimaryPrincipal();

		SysUserEntity user = this.getById(userEntity.getUserId());
		if (user == null)
			throw new NZException(RCode.SYS_USER_NOT_EXIST);

		Map<String, Object> resultMap = new HashMap<>(4);
		List<SysRoleEntity> rolesByUesr = sysRoleDao.getRolesByUesrId(user.getUserId().intValue());
		resultMap.put("roles", rolesByUesr);

		List<Integer> roleIdList = rolesByUesr.stream().map(SysRoleEntity::getId).collect(Collectors.toList());
		Map<String, Object> menuInfoByRoles = sysMenuService.getMenuInfoByRoles(roleIdList);
		resultMap.put("menus", menuInfoByRoles.get("menus"));
		resultMap.put("buttons", menuInfoByRoles.get("buttons"));
		return resultMap;
	}

	@Override
	public List<SysUserEntity> getUserListByRoleId(Integer roleId) {
		List<SysUserEntity> userList = this.getBaseMapper().getUserListByRoleId(roleId);
		return userList;
	}
}