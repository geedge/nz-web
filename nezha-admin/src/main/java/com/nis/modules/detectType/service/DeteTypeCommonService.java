package com.nis.modules.detectType.service;

import com.baomidou.mybatisplus.extension.service.IService;

public interface DeteTypeCommonService<T> extends IService<T>{
    boolean checkFieldExists(String field,String name);
    boolean checkFieldExists(String field,String name,Long typeId);

}
