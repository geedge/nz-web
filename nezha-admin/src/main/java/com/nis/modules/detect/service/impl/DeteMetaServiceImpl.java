package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.modules.detect.dao.DeteTypeMetaDao;
import com.nis.modules.detect.entity.DeteTypeMetaEntity;
import com.nis.modules.detect.service.DeteMetaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("deteTypeMetaService")
public class DeteMetaServiceImpl extends DeteCommonServiceImpl<DeteTypeMetaDao, DeteTypeMetaEntity> implements DeteMetaService {
    public boolean checkFieldTypeIsValide(DeteTypeMetaEntity deteTypeMetaEntity){
        return checkFieldIsValide(DIC_FIELDTYPE,deteTypeMetaEntity.getFieldType());
    }
    public boolean checkDataTypeIsValide(DeteTypeMetaEntity deteTypeMetaEntity){
        return checkFieldIsValide(DIC_DATATYPE,deteTypeMetaEntity.getDataType());
    }

    public boolean checkNameExists(DeteTypeMetaEntity deteTypeMetaEntity){
        QueryWrapper<DeteTypeMetaEntity> deteTypeMetaQueryWrapper = new QueryWrapper<>();
        deteTypeMetaQueryWrapper.ne(deteTypeMetaEntity.getId()!=null,"id",deteTypeMetaEntity.getId());
        deteTypeMetaQueryWrapper.eq("type_id",deteTypeMetaEntity.getTypeId());
        deteTypeMetaQueryWrapper.eq("name",deteTypeMetaEntity.getName());
        List<DeteTypeMetaEntity> list = this.list(deteTypeMetaQueryWrapper);
        return list!=null&&list.size()>0;
    }

    public boolean checkFieldNameExists(DeteTypeMetaEntity deteTypeMetaEntity){
        QueryWrapper<DeteTypeMetaEntity> deteTypeMetaQueryWrapper = new QueryWrapper<>();
        deteTypeMetaQueryWrapper.ne(deteTypeMetaEntity.getId()!=null,"id",deteTypeMetaEntity.getId());
        deteTypeMetaQueryWrapper.eq("type_id",deteTypeMetaEntity.getTypeId());
        deteTypeMetaQueryWrapper.eq("field_name",deteTypeMetaEntity.getFieldName());
        List<DeteTypeMetaEntity> list = this.list(deteTypeMetaQueryWrapper);
        return list!=null&&list.size()>0;
    }
}
