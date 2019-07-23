package com.nis.modules.detectType.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.detectType.entity.DeteTypeInfoEntity;

import java.util.Map;

public interface DeteTypeInfoService extends DeteTypeCommonService<DeteTypeInfoEntity>  {
    PageUtils queryPage(Map<String, Object> params);

    DeteTypeInfoEntity queryDetail(Integer id);


    void saveEntity(DeteTypeInfoEntity deteTypeInfo);
}
