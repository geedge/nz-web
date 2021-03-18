package com.nis.modules.asset.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.AssetFieldGroup;

@Mapper
@Repository
public interface AssetFieldGroupDao extends BaseMapper<AssetFieldGroup>{

}
