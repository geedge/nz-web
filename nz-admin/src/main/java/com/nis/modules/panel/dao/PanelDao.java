package com.nis.modules.panel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.panel.entity.Panel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PanelDao extends BaseMapper<Panel> {
    List<Panel> queryPanelsWithPage(IPage<Panel> page, @Param("params")Map<String, Object> params);
}
