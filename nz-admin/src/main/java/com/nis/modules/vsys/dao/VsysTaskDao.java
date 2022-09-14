package com.nis.modules.vsys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.vsys.entity.VsysTask;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface VsysTaskDao extends BaseMapper<VsysTask> {

}
