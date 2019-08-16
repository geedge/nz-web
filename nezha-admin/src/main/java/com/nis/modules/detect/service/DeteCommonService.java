package com.nis.modules.detect.service;

import com.baomidou.mybatisplus.extension.service.IService;

public interface DeteCommonService<T> extends IService<T>{
    boolean checkFieldExists(Long id,String field,String value);
    String formatString(String tableName);

    boolean checkNameExists(String name,Long id);



}
