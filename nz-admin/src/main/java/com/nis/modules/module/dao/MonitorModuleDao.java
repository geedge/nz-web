package com.nis.modules.module.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.module.entity.MonitorModule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface MonitorModuleDao extends BaseMapper<MonitorModule> {

    List<MonitorModule> queryPage(IPage page, @Param("params") Map<String, Object> params);

}
