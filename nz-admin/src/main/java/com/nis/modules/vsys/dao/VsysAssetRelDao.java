package com.nis.modules.vsys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.vsys.entity.VsysAssetRel;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface VsysAssetRelDao extends BaseMapper<VsysAssetRel> {

}
