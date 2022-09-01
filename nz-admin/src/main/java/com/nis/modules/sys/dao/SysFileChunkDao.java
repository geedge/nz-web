package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.SysFileChunkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 文件管理    dao
 */
@Mapper
@Repository
public interface SysFileChunkDao extends BaseMapper<SysFileChunkEntity> {

}
