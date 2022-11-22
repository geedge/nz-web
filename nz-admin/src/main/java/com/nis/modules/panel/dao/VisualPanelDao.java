package com.nis.modules.panel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.panel.entity.VisualPanel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface VisualPanelDao extends BaseMapper<VisualPanel> {
    List<VisualPanel> queryList(IPage page, @Param("params")Map<String, Object> params);

    List<VisualPanel> queryListByType(@Param("type")String type);

    void updateWeightBatch(@Param("panels") List<VisualPanel> updatePanels);
    
    VisualPanel queryInfo(Integer id);

    void delReceivers(List<String> idList);

    List<VisualPanel> queryReportOfEmptyReceivers();
}
