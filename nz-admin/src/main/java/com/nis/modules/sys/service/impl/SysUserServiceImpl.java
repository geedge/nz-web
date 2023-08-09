package com.nis.modules.sys.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.dashboard.dao.VisualDashboardDao;
import com.nis.modules.dashboard.entity.VisualDashboard;
import com.nis.modules.dashboard.service.VisualDashboardService;
import com.nis.modules.sys.dao.SysRoleDao;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.*;
import com.nis.modules.sys.service.*;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("sysUserService")
public class SysUserServiceImpl extends ServiceImpl<SysUserDao, SysUserEntity> implements SysUserService {

	private static Log log = Log.get();

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
	private SysConfigService sysConfigService;

	@Autowired
	private SysApiKeyService sysApiKeyService;

	@Autowired
	private AlertRuleDao alertRuleDao;

	@Autowired
	private VisualDashboardDao dashboardDao;

	@Autowired
	private VisualDashboardService dashboardService;

	@Autowired
	private DcService dcService;

	@Autowired
	private AlertRuleService alertRuleService;

	@Autowired
	private SysUserPrefService sysUserPrefService;

	@Autowired
	private SysUserStarredService sysUserStarredService;

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String ids = (String) params.get("ids");
        if(StrUtil.isNotBlank(ids)) {
        	List<String> listIds = Arrays.asList(ids.split(","));
        	params.put("ids",listIds);
        }
		IPage page = new Query(SysUserEntity.class).getPage(params);
		List<SysUserEntity> userList = baseMapper.queryList(page, params);

		// 补充 Notificationscript roles 信息
		if (CollectionUtils.isNotEmpty(userList)) {
			List<SysUserRoleEntity> userRoleList = sysUserRoleService.list();
			Map<Integer, List<SysUserRoleEntity>> userRoleMap = userRoleList.stream().collect(Collectors.groupingBy(SysUserRoleEntity::getUserId));
			List<SysRoleEntity> roleList = sysRoleService.list();
			Map<Integer, SysRoleEntity> roleIdAndEntityMap = new HashMap<>();
			for (SysRoleEntity sysRoleEntity : roleList) {
				roleIdAndEntityMap.put(sysRoleEntity.getId(), sysRoleEntity);
			}
			// 获取缓存中登录的用户
			Map<String, SysUserEntity> shiroSessionInfoMap = ShiroUtils.getShiroSessionInfoMap();
			List<Long> loginUserIds = Tool.CollUtil.newArrayList();
			shiroSessionInfoMap.values().forEach( u -> {
				loginUserIds.add(u.getId());
			});
			for (SysUserEntity userEntity : userList) {
				if (loginUserIds.contains(userEntity.getId())){
					//在线
					userEntity.setOnline(1);
				}else {
					//离线
					userEntity.setOnline(0);
				}
				List<SysUserRoleEntity> userRoleEntities = userRoleMap.get(userEntity.getId().intValue());
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
        ValidateUtils.is(user.getUsername()).notNull(RCode.SYS_USER_USERNAME_ISNULL).maxLength(64)
        		.and(user.getMobile()).maxLength(64)
                .and(status).notNull(RCode.SYS_USER_STATUS_ISNULL).and(user.getName()).notNull(RCode.SYS_USER_NAME_ISNULL).maxLength(64);

        List<SysUserEntity> list = this.list(new QueryWrapper<SysUserEntity>().lambda().eq(SysUserEntity::getName, user.getName()).ne(ObjectUtil.isNotEmpty(user.getId()),SysUserEntity::getId, user.getId()));
        if(ObjectUtil.isNotEmpty(list)) {
        	throw new NZException(RCode.SYS_USER_NAME_DUPLICATE);
        }

	    if(status != 0 && status != 1){
	    	throw new NZException(RCode.SYS_USER_STATUS_INVALIDE);
	    }
	    String email = user.getEmail();
	    if(StringUtils.isNotEmpty(email)){
	    	ValidateUtils.is(email).email(RCode.SYS_USER_EMAIL_FORMAT).maxLength(64);
	    }

	    // 校验角色
	    String roleIds = user.getRoleIds();
	    if (StringUtils.isEmpty(roleIds)) throw new NZException(RCode.SYS_ROLE_ID_ISNULL);

	    List<SysRoleEntity> roleList = sysRoleService.list();
	    List<Integer> roleIdList = roleList.stream().map(SysRoleEntity::getId).collect(Collectors.toList());

	    for (String roleId : Arrays.asList(roleIds.split(","))) {
		    if (!roleIdList.contains(Integer.valueOf(roleId)))
			    throw new NZException(RCode.SYS_ROLE_ISNULL);
	    }

	    if(ObjectUtil.isEmpty(user.getTheme())) {
        	user.setTheme(1);
        }
	    this.checkPin(user,false);
    }

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer saveUser(SysUserEntity user) {

		ValidateUtils.is(user.getPin()).notNull(RCode.SYS_USER_PASSWORD_ISNULL).maxLength(64);
		user.setPinTime(new Date());
		// 校验 user 相关信息
		user.setName(StrUtil.trim(user.getName()));
		this.validateUserInfo(user);
		if(Tool.StrUtil.isBlank(user.getLang())) {
			user.setLang("en");
		}
		user.setCreateAt(new Date());
		//sha256加密
		String salt = RandomStringUtils.randomAlphanumeric(20);
		user.setSalt(salt);
		user.setPin(ShiroUtils.sha256(user.getPin(), user.getSalt()));
		user.setCreateBy(ShiroUtils.getUserId().intValue());
		this.save(user);

		// 保存用户 和 角色 关联关系
		sysUserRoleService.saveOrUpdate(user.getId(), user.getRoleIds());

		return user.getId().intValue();
	}


	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer update(SysUserEntity user) {

		// 校验 user 相关信息
		user.setName(StrUtil.trim(user.getName()));
		this.validateUserInfo(user);
		if(Tool.StrUtil.isBlank(user.getLang())) {
			user.setLang("en");
		}
		if (StringUtils.isBlank(user.getPin())) {
			user.setPin(null);
		} else {
			// 如果不是本人更改 自己密码的话，删除 redis 中用户session
			Long loginUserId = ShiroUtils.getUserId();
			if (!loginUserId.equals(user.getId())) {
				// 通过 userId 删除 redis 中记录的 登录信息
				this.removeUserInfoInRedis(user.getId());
			}
			SysUserEntity userEntity = this.getById(user.getId());
			user.setPin(ShiroUtils.sha256(user.getPin(), userEntity.getSalt()));
		}

		this.updateById(user);

		// 处理用户角色关系
		this.handleUserRoleRelation(user.getId(), user.getRoleIds());

		return user.getId().intValue();
	}

	/**
	 * 处理用户 和 角色 关联关系
	 * @param userId
	 * @param roleIds
	 */
	private void handleUserRoleRelation(Long userId, String roleIds) {
		// 当前用户关联的角色
		List<SysUserRoleEntity> roles = sysUserRoleService.list(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId.intValue()));
		List<Integer> idList = roles.stream().map(SysUserRoleEntity::getRoleId).distinct().collect(Collectors.toList());

		// 角色信息未改变 不进行处理
		List<Integer> roleIdList = new ArrayList<>();
		for (String s : Arrays.asList(roleIds.split(","))) {
			roleIdList.add(Integer.valueOf(s));
		}
		if (CommonUtils.checkIdListDiffrent(idList, roleIdList))
			return;

		// 角色变更
		sysUserRoleService.saveOrUpdate(userId, roleIds);

		// 通过 userId 删除 redis 中记录的 登录信息
		this.removeUserInfoInRedis(userId);

		// 变更 sys_api_key role_id  注：当前 用户只能关联一个角色
		String updateRoleId = Arrays.asList(roleIds.split(",")).get(0);
		sysApiKeyService.updateRoleIdByUser(userId.intValue(), Integer.valueOf(updateRoleId));
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
				if (userId.equals(userEntity.getId())) {
					removeKeys.add(entry.getKey());
				}
			}
		} catch (Exception e) {
            log.error(e, "[removeUserInfoInRedis] [error] [userId: {}]", userId);
		} finally {
			// 删除 关联key
			if (CollectionUtils.isNotEmpty(removeKeys)) {
				ShiroUtils.deleteSession(removeKeys);
			}
		}
	}


	@Override
	public boolean updatePassword(Long userId, String pin, String newPin) {
        SysUserEntity userEntity = this.baseMapper.selectById(userId);
        userEntity.setPin(newPin);
        userEntity.setPinTime(new Date());
        return this.update(userEntity,
        	new LambdaUpdateWrapper<SysUserEntity>()
					.eq(ToolUtil.isNotEmpty(userId),SysUserEntity::getId, userId)
					.eq(ToolUtil.isNotEmpty(pin),SysUserEntity::getPin, pin));
    }

	@Override
	public boolean checkName(SysUserEntity user) {
		List<SysUserEntity> list = this.list(new LambdaUpdateWrapper<SysUserEntity>()
				.eq(ToolUtil.isNotEmpty(user.getUsername()), SysUserEntity::getUsername, user.getUsername())
				.eq(ToolUtil.isNotEmpty(user.getStatus()), SysUserEntity::getStatus, 1));
		boolean dumpFlag = true; //是否名称重复，true为重复

		//当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
		if ((list == null || list.size() == 0) ||
					(list.size() == 1 && list.get(0).getId().longValue() == user.getId().longValue())) {
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
		if (ArrayUtils.contains(split, userEntity.getId().toString())) {
			throw new NZException(RCode.SYS_USER_CANNOTDEL);
		}

		List<String> idList = Arrays.asList(split);
		// 删除 user
		this.removeByIds(idList);

		// 删除 关联 role 关系
		sysUserRoleService.remove(new LambdaQueryWrapper<SysUserRoleEntity>().in(SysUserRoleEntity::getUserId, idList));
		// 删除 alert_rule 引用
		alertRuleDao.delReceiver(idList);
		// 若告警规则通知人为空，则关闭主动通知
		alertRuleService.update(new LambdaUpdateWrapper<AlertRuleEntity>().eq(AlertRuleEntity::getReceiver, "")
				.set(AlertRuleEntity::getNotifyActive, 0).set(AlertRuleEntity::getNotifyExpired, 0));
		// 删除 dc 引用
		dcService.update(new LambdaUpdateWrapper<Dc>().in(Dc::getPrincipal, idList).set(Dc::getPrincipal, null));
		// 删除 dashboard 引用
		dashboardDao.delReceivers(idList);
		// 关闭 dashboard 接收人为空的报表
		List<VisualDashboard> visualDashboardList = dashboardDao.queryReportOfEmptyReceivers();
		for (VisualDashboard dashboard : visualDashboardList) {
			Map newParamMap = JSONObject.parseObject(dashboard.getParam().toString(), LinkedHashMap.class);
			Map publishMap = new HashMap(4);
			publishMap.put("dashboardId", dashboard.getId());
			publishMap.putAll(newParamMap);

			redisTemplate.convertAndSend(Constant.REPORT_TOPIC_NAME, JSONObject.toJSONString(publishMap));
		}
		if(Tool.CollUtil.isNotEmpty(visualDashboardList)) dashboardService.updateBatchById(visualDashboardList);

        // 先查询，redis 中的 api key 同样需要删除
        List<SysApiKey> apiKeyList = sysApiKeyService.list(new LambdaUpdateWrapper<SysApiKey>().in(SysApiKey::getCreateBy, idList));
        // 删除相关 sys_api_key
        sysApiKeyService.remove(new LambdaUpdateWrapper<SysApiKey>().in(SysApiKey::getCreateBy, idList));

		// sys user starred
		sysUserStarredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>().in(SysUserStarredEntity::getUid, idList));

		// sys user preference
		sysUserPrefService.remove(new LambdaQueryWrapper<SysUserPreference>().in(SysUserPreference::getUid, idList));

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
				if (userIdList.contains(userInfo.getId())) {
					removeKeys.add(entry.getKey());
				}
			}
            for (SysApiKey apiKey : apiKeyList) {
                removeKeys.add(apiKey.getToken());
            }
		} catch (Exception e) {
            log.error(e, "[delete user error] [userIds: {}]", userIds);
		} finally {
			// 删除 关联key
			if (CollectionUtils.isNotEmpty(removeKeys)) {
				ShiroUtils.deleteSession(removeKeys);
			}
		}
	}

	@Override
	public Map<String, Object> getUserPermissions(HttpServletRequest request) {
		String requestToken = request.getHeader(Constant.AUTH_TOKEN_CODE);
		requestToken = StringUtils.isEmpty(requestToken) ? request.getParameter(Constant.AUTH_TOKEN_CODE) : requestToken;
		if (StringUtils.isEmpty(requestToken)) {
			throw new NZException(RCode.SYS_TOKEN_ISNULL);
		}
		Session session = ShiroUtils.getSessionById(requestToken);
		if(Tool.ObjectUtil.isNull(session)) {
			throw new NZException(RCode.SYS_LOGIN_REQUIRED);
		}
		PrincipalCollection primaryPrincipal = (PrincipalCollection) session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
		SysUserEntity userEntity = (SysUserEntity) primaryPrincipal.getPrimaryPrincipal();

		SysUserEntity user = this.getById(userEntity.getId());
		if (user == null)
			throw new NZException(RCode.SYS_USER_NOT_EXIST);

		Map<String, Object> resultMap = new HashMap<>(4);
		List<SysRoleEntity> rolesByUesr = sysRoleDao.getRolesByUesrId(user.getId().intValue());
		resultMap.put("roles", rolesByUesr);

		List<Integer> roleIdList = rolesByUesr.stream().map(SysRoleEntity::getId).collect(Collectors.toList());
		Integer menuState = 1;
		Map<String, Object> menuInfoByRoles = sysMenuService.getMenuInfoByRoles(roleIdList, menuState);
		resultMap.put("menus", menuInfoByRoles.get("menus"));
		resultMap.put("buttons", menuInfoByRoles.get("buttons"));
		return resultMap;
	}

	@Override
	public List<SysUserEntity> getUserListByRoleId(Integer roleId) {
		List<SysUserEntity> userList = this.getBaseMapper().getUserListByRoleId(roleId);
		return userList;
	}

	@Override
	public SysUserEntity queryUserInfo(Long userId) {
		SysUserEntity user = this.getById(userId);
		if (Tool.ObjectUtil.isEmpty(user)) {
			throw new NZException(RCode.SYS_USER_NOT_EXIST);
		}
		//获取用户所属的角色列表
		List<Integer> roleIdList = sysUserRoleService.queryRoleIdList(userId.intValue());
		List<SysRoleEntity> roles = sysRoleService.list(new QueryWrapper<SysRoleEntity>().lambda().in(SysRoleEntity::getId, roleIdList));
		user.setRoles(roles);
		// 获取缓存中登录的用户
		Map<String, SysUserEntity> shiroSessionInfoMap = ShiroUtils.getShiroSessionInfoMap();
		List<SysUserEntity> users = new ArrayList<SysUserEntity>(shiroSessionInfoMap.values());
		for (SysUserEntity userEntity : users) {
			if (userEntity.getId().equals(user.getId())){
				//在线
				user.setOnline(1);
				break;
			}else {
				//离线
				user.setOnline(0);
			}
		}
		return user;
	}


	@SuppressWarnings("unchecked")
	@Override
	public void checkPin(SysUserEntity user,boolean isLogin) {
		String pin = user.getPin();
		Date pinTime = user.getPinTime();
		String pinPolicy = sysConfigService.getValue("pin_policy");
		if(ToolUtil.isNotEmpty(pinPolicy) && StrUtil.isNotBlank(pin)) {
			Map<String,Object> pinPolicyInfo = (Map<String, Object>) JSON.parse(pinPolicy);
			Integer enable = (Integer) pinPolicyInfo.get("enable");
			Integer length = (Integer) pinPolicyInfo.get("length");
			Integer num = (Integer) pinPolicyInfo.get("num");
			Integer lower = (Integer) pinPolicyInfo.get("lower");
			Integer upper = (Integer) pinPolicyInfo.get("upper");
			Integer spec = (Integer) pinPolicyInfo.get("spec");
			Integer valid = (Integer) pinPolicyInfo.get("valid");
			Integer retry = (Integer) pinPolicyInfo.get("retry");
			Integer lockTime = (Integer) pinPolicyInfo.get("lockTime");
			if(ToolUtil.isNotEmpty(enable) && enable.equals(1)) {
				 // ldap用户不进行密码策略认证 但是会进行错误认证校验
				if(!StrUtil.equals("ldap", user.getSource())) {
					// 校验密码长度是否合法
					if(ToolUtil.isNotEmpty(length)&&length>0&&(length > pin.length())&&!isLogin) {
						throw new NZException(RCode.SYS_USER_PIN_TOO_SHORT.setParam(length));
					}
					// 校验密码是否包含字母
					if(ToolUtil.isNotEmpty(num)&&num.equals(1)&&!CommonUtils.checkPinCorrect(pin,1)&&!isLogin) {
						throw new NZException(RCode.SYS_USER_PIN_NOT_CONTAIN_NUMBER);
					}
					// 校验密码是否包含小写字母
					if(ToolUtil.isNotEmpty(lower)&&lower.equals(1)&&!CommonUtils.checkPinCorrect(pin,2)&&!isLogin) {
						throw new NZException(RCode.SYS_USER_PIN_NOT_CONTAIN_LOWERCASE_LETTERS);
					}
					// 校验密码是否包含大写字母
					if(ToolUtil.isNotEmpty(upper)&&upper.equals(1)&&!CommonUtils.checkPinCorrect(pin,3)&&!isLogin) {
						throw new NZException(RCode.SYS_USER_PIN_NOT_CONTAIN_UPPERCASE_LETTERS);
					}
					// 校验密码包含特殊字符是否合法
					if(ToolUtil.isNotEmpty(spec)&&spec.equals(1)&&!CommonUtils.checkPinCorrect(pin,4)&&!isLogin) {
						throw new NZException(RCode.SYS_USER_PIN_NOT_CONTAIN_SPECIAL_LETTERS);
					}
					// 校验密码修改天数是否合法
					if(ToolUtil.isNotEmpty(valid)&&valid>0&&Tool.DateUtil.betweenDay(pinTime, new Date(), false)>valid) {
						throw new NZException(RCode.SYS_USER_PIN_LONGTIME_NO_MODIFY.setParam(valid));
					}
				}
				if(retry>0 && lockTime>0) {
					// 校验用户登录次数以及封锁时间
					ValueOperations<String,String> opsForValue = redisTemplate.opsForValue();
					String redisKey = String.format("sysUserLogin_%s", user.getUsername());
					String redisValue = opsForValue.get(redisKey);
					if(StrUtil.isNotBlank(redisValue)) {
						Integer redisRetry = Integer.valueOf(redisValue);
						Long time = redisTemplate.getExpire(redisKey)/60;
						if(redisRetry >= retry) {
							throw new NZException(RCode.SYS_USER_PIN_RETRY_ERROR.setParam(retry,time));
						}
					}
				}
			}
		}
	}

	@Override
	public void checkRedisRetry(SysUserEntity user) {
		Integer retry = 0;
		Integer lockTime = 30;
		String pinPolicy = sysConfigService.getValue("pin_policy");
		if(ToolUtil.isNotEmpty(pinPolicy)) {
			Map<String,Object> pinPolicyInfo = (Map<String, Object>) JSON.parse(pinPolicy);
			retry = (Integer) pinPolicyInfo.get("retry");
			lockTime = (Integer) pinPolicyInfo.get("lockTime");
		}
		ValueOperations<String,String> opsForValue = redisTemplate.opsForValue();
		String redisKey = String.format("sysUserLogin_%s", user.getUsername());
		String redisValue = opsForValue.get(redisKey);
		if(ToolUtil.isNotEmpty(redisValue)&&retry!=0) {
			Integer redisRetry = Integer.valueOf(redisValue);
			if(retry<=redisRetry) {
				return;
			}
			redisRetry+=1;
			opsForValue.set(redisKey, redisRetry.toString());
			if(retry.equals(redisRetry)) {
				redisTemplate.expire(redisKey, lockTime, TimeUnit.MINUTES);
			}else{
				redisTemplate.expire(redisKey, 5, TimeUnit.MINUTES);
			}
		}else {
			opsForValue.set(redisKey, "1");
			redisTemplate.expire(redisKey, 5, TimeUnit.MINUTES);
		}
	}
}
