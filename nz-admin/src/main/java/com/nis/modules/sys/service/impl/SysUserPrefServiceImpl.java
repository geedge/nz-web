package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.dao.SysUserPrefDao;
import com.nis.modules.sys.entity.SysUserPreference;
import com.nis.modules.sys.service.SysUserPrefService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * System User Preference Service Implement
 *
 * @author shizhendong
 * @date 2023-08-09
 */
@Service
public class SysUserPrefServiceImpl extends ServiceImpl<SysUserPrefDao, SysUserPreference> implements SysUserPrefService {

    @Override
    public Map<String, String> queryCurrentUserPrefList(String key) {
        // current login user
        LambdaQueryWrapper<SysUserPreference> queryWrapper = new LambdaQueryWrapper<SysUserPreference>()
                .eq(SysUserPreference::getUid, ShiroUtils.getUserId().intValue());

        if (Tool.StrUtil.isNotEmpty(key)) {
            queryWrapper.in(SysUserPreference::getPrefKey, Tool.StrUtil.split(key, ","));
        }

        // query list
        List<SysUserPreference> userPrefList = this.list(queryWrapper);

        // return Map format
        Map<String, String> userPrefMap = userPrefList.stream()
                .collect(Collectors.toMap(SysUserPreference::getPrefKey, SysUserPreference::getPrefValue));
        return userPrefMap;
    }

    @Override
    public void updateCurrentUserPref(Map<String, String> updateKeyValueMap) {
        // remove
        this.remove(new LambdaQueryWrapper<SysUserPreference>()
                .eq(SysUserPreference::getUid, ShiroUtils.getUserId().intValue())
                .in(SysUserPreference::getPrefKey, updateKeyValueMap.keySet()));

        // insert
        List<SysUserPreference> saveBatchList = Tool.ListUtil.list(true);
        for (Map.Entry<String, String> entry : updateKeyValueMap.entrySet()) {
            SysUserPreference sysUserPreference = new SysUserPreference();
            sysUserPreference.setUid(ShiroUtils.getUserId().intValue());
            sysUserPreference.setPrefKey(entry.getKey());
            sysUserPreference.setPrefValue(entry.getValue());
            sysUserPreference.setUts(System.currentTimeMillis());
            saveBatchList.add(sysUserPreference);
        }
        this.saveBatch(saveBatchList);
    }

    @Override
    public void deleteCurrentUserPref(String key) {
        // current login user
        LambdaQueryWrapper<SysUserPreference> queryWrapper = new LambdaQueryWrapper<SysUserPreference>()
                .eq(SysUserPreference::getUid, ShiroUtils.getUserId().intValue());

        if (Tool.StrUtil.isNotEmpty(key)) {
            queryWrapper.in(SysUserPreference::getPrefKey, Tool.StrUtil.split(key, ","));
        }

        // remove
        this.remove(queryWrapper);
    }
}
