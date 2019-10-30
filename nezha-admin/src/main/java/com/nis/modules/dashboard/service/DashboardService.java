package com.nis.modules.dashboard.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.modules.dashboard.entity.ChartEntity;
import com.nis.modules.dashboard.entity.ChartSeriesEntity;
import com.nis.modules.dashboard.entity.DashboardEntity;

public interface DashboardService extends IService<DashboardEntity>{

	List<DashboardEntity> queryAllDashboards();
	
	void removeDashboardById(Long id);
	
	void addDashboard(DashboardEntity dashboard);
	
	void modifyDashboard(DashboardEntity dashboard);
	
	void validateDashboardData(DashboardEntity dashboard);
	
	List<ChartSeriesEntity> queryData(String sql,String timeFormat,Integer chartType);
}
