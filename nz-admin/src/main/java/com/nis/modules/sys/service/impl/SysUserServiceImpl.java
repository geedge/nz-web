package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertNotifiScriptDao;
import com.nis.modules.alert.entity.AlertNotificationScript;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.SysUserNotification;
import com.nis.modules.sys.service.SysUserNotificationService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("sysUserService")
public class SysUserServiceImpl extends ServiceImpl<SysUserDao, SysUserEntity> implements SysUserService {

	@Autowired
	private SysUserNotificationService notificationService;

	@Autowired
	private AlertNotifiScriptDao alertNotifiScriptDac;

	@Override
	public List<Long> queryAllMenuId(Long userId) {
		return baseMapper.queryAllMenuId(userId);
	}

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		IPage<SysUserEntity> page = new Query<SysUserEntity>().getPage(params);
		List<SysUserEntity> userList = baseMapper.queryList(page, params);

		// 补充 Notificationscript 信息
		if (CollectionUtils.isNotEmpty(userList)) {
			List<SysUserNotification> sysUserNotifications = notificationService.list();
			Map<Integer, List<SysUserNotification>> userIdAndScriptListMap = sysUserNotifications.stream().collect(Collectors.groupingBy(SysUserNotification::getUserId));
			for (SysUserEntity userEntity : userList) {
				userEntity.setNotifications(userIdAndScriptListMap.get(userEntity.getUserId().intValue()));
			}
		}

		page.setRecords(userList);
		return new PageUtils(page);
	}

    private void validateUserInfo(SysUserEntity user) {
	    Integer status = user.getStatus();
        ValidateUtils.is(user.getUsername()).notNull(RCode.SYS_USER_USERNAME_ISNULL)
                .and(status).notNull(RCode.SYS_USER_STATUS_ISNULL)
                .and(user.getPassword()).notNull(RCode.SYS_USER_PASSWORD_ISNULL);

	    if(status != 0 && status != 1){
	    	throw new NZException(RCode.SYS_USER_STATUS_INVALIDE);
	    }
	    String email = user.getEmail();
	    if(StringUtils.isNotEmpty(email)){
	    	ValidateUtils.is(email).email(RCode.SYS_USER_EMAIL_FORMAT);
	    }

	    List<SysUserNotification> notifications = user.getNotifications();
        if (CollectionUtils.isNotEmpty(notifications)) {
	        List<AlertNotificationScript> scripts = alertNotifiScriptDac.selectList(null);
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
		user.setPassword(ShiroUtils.sha256(user.getPassword(), user.getSalt()));
		this.save(user);

		// 关联 script 不为空则保存
		List<SysUserNotification> notifications = user.getNotifications();
		if (CollectionUtils.isNotEmpty(notifications)) {
			for (SysUserNotification notification : notifications) {
				notification.setUserId(user.getUserId().intValue());
			}
			notificationService.saveBatch(notifications);
		}
	}


	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysUserEntity user) {

		// 校验 user 相关信息
		this.validateUserInfo(user);

		if (StringUtils.isBlank(user.getPassword())) {
			user.setPassword(null);
		} else {
			SysUserEntity userEntity = this.getById(user.getUserId());
			user.setPassword(ShiroUtils.sha256(user.getPassword(), userEntity.getSalt()));
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
	}


	@Override
	public boolean updatePassword(Long userId, String password, String newPassword) {
        SysUserEntity userEntity = new SysUserEntity();
        userEntity.setPassword(newPassword);
        return this.update(userEntity,
        	new LambdaUpdateWrapper<SysUserEntity>()
					.eq(ToolUtil.isNotEmpty(userId),SysUserEntity::getUserId, userId)
					.eq(ToolUtil.isNotEmpty(password),SysUserEntity::getPassword, password));
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
		if(ArrayUtils.contains(split, "1")){
			throw new NZException(RCode.SYS_USER_DELADMIN);
		}

		SysUserEntity userEntity = (SysUserEntity) SecurityUtils.getSubject().getPrincipal();
		if(ArrayUtils.contains(split, userEntity.getUserId().toString())){
			throw new NZException(RCode.SYS_USER_CANNOTDEL);
		}

		// 删除 user
		this.removeByIds(Arrays.asList(split));

		// 删除 关联 script 关系
		notificationService.remove(new QueryWrapper<SysUserNotification>().lambda().in(SysUserNotification::getUserId, Arrays.asList(split)));
	}

}
