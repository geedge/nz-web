package com.nis.common.config;

import com.nis.modules.sys.dao.SysConfigDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

@Slf4j
//@Component
public class SettingConfig implements CommandLineRunner {

    @Autowired
    private SysConfigDao sysConfigDao;

    @Override
    public void run(String... args) throws Exception {
       /* SysConfigEntity sysConfigEntity = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, "query_max_series"));
        PromConstant.queryMaxSeries = Integer.valueOf(sysConfigEntity.getParamValue());
        log.info("当前 Prometheus query_range 查询最大条数限制是 " + sysConfigEntity.getParamValue());*/
    }
}