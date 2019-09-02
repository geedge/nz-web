package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.modules.detect.dao.DeteTypeParamDao;
import com.nis.modules.detect.entity.DeteTypeMetaEntity;
import com.nis.modules.detect.entity.DeteTypeParamEntity;
import com.nis.modules.detect.service.DeteParamService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("deteTypeParamService")
public class DeteParamServiceImpl extends DeteCommonServiceImpl<DeteTypeParamDao, DeteTypeParamEntity> implements DeteParamService {
    public boolean checkNameExists(DeteTypeParamEntity deteTypeParamEntity){
        QueryWrapper<DeteTypeParamEntity> deteParamQueryWrapper = new QueryWrapper<DeteTypeParamEntity>();
        deteParamQueryWrapper.ne(deteTypeParamEntity.getId()!=null,"id",deteTypeParamEntity.getId());
        deteParamQueryWrapper.eq("type_id",deteTypeParamEntity.getTypeId());
        deteParamQueryWrapper.eq("name",deteTypeParamEntity.getName());
        List<DeteTypeParamEntity> list = this.list(deteParamQueryWrapper);
        return list!=null&&list.size()>0;
    }

}
