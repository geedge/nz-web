package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysConfigEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SysConfService extends IService<SysConfigEntity> {

    Map<String,SysConfigEntity> saveOrUpdateConfsBatch(Map<String,String> params);

    void saveBasic(Map<String, String> params);


    void saveEmail(Map<String, String> params);


    void saveTerminal(Map<String, String> params);

    String getValue(String key);

    Map<String,String> batchQuery(String ... keys);

    void saveLdap(Map<String, String> params);

    void resetConfig(Map<String, Object> param) throws IOException;

    boolean checkHaMode();

    void saveMonitor(Map<String, String> params);

}
