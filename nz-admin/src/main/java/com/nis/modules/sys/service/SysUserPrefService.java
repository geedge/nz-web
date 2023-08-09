package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysUserPreference;

import java.util.Map;

/**
 * System User Preference Service
 *
 * @author shizhendong
 * @date 2023-08-09
 */
public interface SysUserPrefService extends IService<SysUserPreference> {

    Map<String, String> queryCurrentUserPrefList(String key);

    void updateCurrentUserPref(Map<String, String> updateKeyValueMap);

    void deleteCurrentUserPref(String key);
}
