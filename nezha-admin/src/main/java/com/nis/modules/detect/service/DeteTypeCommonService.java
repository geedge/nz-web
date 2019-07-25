package com.nis.modules.detect.service;

import com.baomidou.mybatisplus.extension.service.IService;

public interface DeteTypeCommonService<T> extends IService<T>{
    boolean checkFieldExists(String field,String name);
    boolean checkFieldExists(Long id,String field,String name);
    boolean checkFieldExists(String field,String name,Long typeId);

}
