package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.sys.entity.SysFileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 文件管理    dao
 */
@Mapper
@Repository
public interface SysFileDao extends BaseMapper<SysFileEntity> {

    List<SysFileEntity> querySysFileList(IPage page, @Param("params") Map<String, Object> params);
}
