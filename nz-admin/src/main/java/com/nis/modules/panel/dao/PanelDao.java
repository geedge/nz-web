package com.nis.modules.panel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.panel.entity.Panel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PanelDao extends BaseMapper<Panel> {
    List<Panel> queryList(@Param("params")Map<String, Object> params);

    List<Panel> queryListByType(@Param("type")String type);

    void updateWeightBatch(@Param("panels") List<Panel> updatePanels);
}
