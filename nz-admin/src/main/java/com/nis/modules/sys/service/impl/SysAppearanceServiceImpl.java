package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.sys.dao.SysAppearanceDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysAppearanceService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysAppearanceServiceImpl extends ServiceImpl<SysAppearanceDao, SysConfigEntity> implements SysAppearanceService {

    public static final String[] BASIC_KEYS = {"system_name","theme","language","date_format","system_bgImg","system_favicon","system_logo", "timezone"};

    @Override
    public Map<String, String> queryAppearance() {
        List<SysConfigEntity> list = this.baseMapper.selectList(new LambdaQueryWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey, BASIC_KEYS));
        Map<String,String> map = new HashMap<>();
        for (SysConfigEntity entity : list){
            if (entity.getParamKey().equals("date_format")){
                map.put("dateFormat",entity.getParamValue());
            }else {
                map.put(entity.getParamKey(),entity.getParamValue());
            }
        }
        return map;
    }
}
