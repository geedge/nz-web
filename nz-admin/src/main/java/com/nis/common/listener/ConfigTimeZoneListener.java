package com.nis.common.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.DateUtil;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时获取sys_config表中的timezone 时区设置
 */
@Component
@Slf4j
public class ConfigTimeZoneListener implements CommandLineRunner {

    @Autowired
    private SysConfigDao sysConfigDao;

    @Override
    public void run(String... args) throws Exception {
        log.info("...................timezone listener start......................");
        SysConfigEntity timeZoneConfig = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, "timezone"));
        if (timeZoneConfig != null) {
            DateUtil.configTimeZone = Integer.valueOf(timeZoneConfig.getParamValue());
            log.debug(String.format("当前时区是UTC %s%s", DateUtil.configTimeZone, ":00"));
        }
        log.info("...................timezone listener end ......................");
    }
}
