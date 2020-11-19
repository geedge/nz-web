package com.nis.modules.alert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.dao.AlertNotifiScriptDao;
import com.nis.modules.alert.entity.AlertNotificationScript;
import com.nis.modules.alert.service.AlertNotifiScriptService;
import com.nis.modules.sys.dao.SysUserNotificationDao;
import com.nis.modules.sys.entity.SysUserNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("alertNotifiScriptService")
public class AlertNotifiScriptServiceImpl extends ServiceImpl<AlertNotifiScriptDao, AlertNotificationScript> implements AlertNotifiScriptService {

    @Autowired
    private SysUserNotificationDao userNotificationDao;

    @Override
    public List<AlertNotificationScript> queryList(Map<String, Object> params) {

        List<AlertNotificationScript> list = this.list(new QueryWrapper<AlertNotificationScript>().lambda()
                .eq(ToolUtil.isNotEmpty(params.get("id")), AlertNotificationScript::getId, params.get("id"))
                .like(ToolUtil.isNotEmpty(params.get("name")), AlertNotificationScript::getName, params.get("name"))
                .like(ToolUtil.isNotEmpty(params.get("filePath")), AlertNotificationScript::getFilePath, params.get("filePath")));

        return list;
    }


    private void validateAssetInfo(AlertNotificationScript alertNotificationScript) {
        // 名称不能为空
        ValidateUtils.is(alertNotificationScript.getName()).notNull(RCode.ALERT_NOTIFISCRIPT_NAME_ISNULL);

        // 校验
        String filePath = alertNotificationScript.getFilePath();
        if (StringUtils.isNotEmpty(filePath)) {
            File file = new File(filePath);
            // 判断文件是否存在
            if (!file.exists()) {
                throw new NZException(RCode.ALERT_NOTIFISCRIPT_FILE_NOTFOUND);
            }
            // 判断该位置是否是文件 而不是文件夹
            if (!file.isFile()) {
                throw new NZException(RCode.ALERT_NOTIFISCRIPT_NOT_FILE);
            }

            if (!file.canExecute()) {
                throw new NZException(RCode.ALERT_NOTIFISCRIPT_NOT_EXECUTABLE);
            }
        }
    }


    @Override
    public void saveOrUpdateNotifiScript(AlertNotificationScript alertNotificationScript) {
        // 校验参数信息
        this.validateAssetInfo(alertNotificationScript);

        this.saveOrUpdate(alertNotificationScript);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScripts(Integer[] ids) {
        List<Integer> idList = Arrays.asList(ids);

        this.removeByIds(idList);

        // 同时删除 USER_NOTIFICATION 表中的数据
        userNotificationDao.delete(new QueryWrapper<SysUserNotification>().lambda().in(SysUserNotification::getScriptId, idList));
    }
}