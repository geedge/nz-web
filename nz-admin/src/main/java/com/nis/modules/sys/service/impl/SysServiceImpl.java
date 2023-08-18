package com.nis.modules.sys.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.sys.dao.SysDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysServiceImpl extends ServiceImpl<SysDao, SysConfigEntity> implements SysService {

    public static final String[] BASIC_KEYS = {"system_name", "theme", "language", "date_format", "system_bgImg", "system_favicon", "system_logo", "timezone", "copyright"};

    @Override
    public Map<String, String> queryAppearance() {

        List<String> keys = Arrays.asList(BASIC_KEYS);
        List<SysConfigEntity> sysConfigList = this.list();

        Map<String, String> sysConfigMap = sysConfigList.stream()
                .filter(config -> keys.contains(config.getParamKey()))
                .collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

        String copyright = StrUtil.nullToDefault(sysConfigMap.get("copyright"), "0");
        sysConfigMap.put("copyright", copyright);
        sysConfigMap.put("dateFormat", sysConfigMap.remove("date_format"));

        return sysConfigMap;
    }
}
