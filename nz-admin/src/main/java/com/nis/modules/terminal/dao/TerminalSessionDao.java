package com.nis.modules.terminal.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.terminal.entity.TerminalSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TerminalSessionDao extends BaseMapper<TerminalSessionEntity>{

    List<TerminalSessionEntity> queryList(IPage page, @Param("params") Map<String, Object> params);

}
