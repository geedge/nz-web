package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.IdcTraffic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface IdcTrafficDao extends BaseMapper<IdcTraffic> {

    List<IdcTraffic> queryTrafficSetting(IPage<IdcTraffic> page, @Param("params")Map<String, Object> params);
}
