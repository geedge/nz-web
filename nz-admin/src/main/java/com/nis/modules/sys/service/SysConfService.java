package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysConfigEntity;

import java.io.IOException;
import java.util.Map;

public interface SysConfService extends IService<SysConfigEntity> {

    Map<String,SysConfigEntity> saveOrUpdateConfsBatch(Map<String,String> params);

    void saveBasic(Map<String, String> params);


    void saveEmail(Map<String, String> params);


    void saveTerminal(Map<String, String> params);

    String getValue(String key);

    /**
     * 根据key 获取配置值，为空返回默认值
     *
     * @param key
     * @param defaultValue
     * @return
     */
    String getValueOrDefault(String key, String defaultValue);

    Map<String,String> batchQuery(String ... keys);

    void saveLdap(Map<String, String> params);

    void resetConfig(Map<String, Object> param) throws IOException;

    boolean checkHaMode();

    void saveMonitor(Map<String, String> params);

    Map<String, Object> querySysConfig(String configKey);

    /**
     * 根据key，更新value
     */
    void updateValueByKey(String key, String value);

    void updateValueByKey(String key, String value, String remark);

    String getDataBasesVersion();

    void reloadSysConfigToCache();

}
