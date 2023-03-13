package com.nis.modules.dashboard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.dashboard.entity.VisualDashboard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface VisualDashboardDao extends BaseMapper<VisualDashboard> {

    List<VisualDashboard> queryList(IPage page, @Param("params")Map<String, Object> params);

    List<VisualDashboard> queryListByType(@Param("type")String type);

    void updateWeightBatch(@Param("dashboards") List<VisualDashboard> updateDashboards);

    VisualDashboard queryInfo(Integer id);

    void delReceivers(List<String> idList);

    List<VisualDashboard> queryReportOfEmptyReceivers();
}
