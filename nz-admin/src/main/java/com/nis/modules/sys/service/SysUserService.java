/**

 *

 *

 */

package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.SysUserEntity;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;


/**
 * 系统用户
 *

 */
public interface SysUserService extends IService<SysUserEntity> {
	
	SysUserEntity queryUserInfo(Long userId);

	PageUtils queryPage(Map<String, Object> params);
	
	/**
	 * 保存用户
	 */
	Integer saveUser(SysUserEntity user);
	
	/**
	 * 修改用户
	 */
	Integer update(SysUserEntity user);

	/**
	 * 修改密码
	 * @param userId       用户ID
	 * @param password     原密码
	 * @param newPassword  新密码
	 */
	boolean updatePassword(Long userId, String password, String newPassword);

	void deleteUsers(String userIds);

    Map<String, Object> getUserPermissions(HttpServletRequest request);

	// 根据角色ID 获取所属用户
    List<SysUserEntity> getUserListByRoleId(Integer roleId);
    
    void checkPin(SysUserEntity user,boolean isLogin);
    
    void checkCacheRetry(SysUserEntity user);

	void validateUsername(String username);

}
