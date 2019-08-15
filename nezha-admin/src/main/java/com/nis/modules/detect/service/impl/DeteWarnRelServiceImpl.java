package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.detect.dao.DeteWarnInfoDao;
import com.nis.modules.detect.dao.DeteWarnRelDao;
import com.nis.modules.detect.entity.DeteWarnInfoEntity;
import com.nis.modules.detect.entity.DeteWarnRelEntity;
import com.nis.modules.detect.service.DeteWarnInfoService;
import com.nis.modules.detect.service.DeteWarnRelService;
import org.springframework.stereotype.Service;

import java.util.List;


@Service("deteWarnRelService")
public class DeteWarnRelServiceImpl extends ServiceImpl<DeteWarnRelDao, DeteWarnRelEntity> implements DeteWarnRelService {
    @Override
    public List<DeteWarnRelEntity> queryByTypeId(Long typeId) {
        return  this.getBaseMapper().queryByTypeId(typeId);
    }
}