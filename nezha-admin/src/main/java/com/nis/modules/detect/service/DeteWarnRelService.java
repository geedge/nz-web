package com.nis.modules.detect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.detect.entity.DeteWarnRelEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeteWarnRelService extends IService<DeteWarnRelEntity> {
    List<DeteWarnRelEntity> queryByTypeId(@Param("typeId")Long typeId);
}
