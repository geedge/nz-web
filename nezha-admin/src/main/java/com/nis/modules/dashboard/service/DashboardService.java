package com.nis.modules.dashboard.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.dashboard.entity.DashboardEntity;

public interface DashboardService extends IService<DashboardEntity>{

	List<DashboardEntity> queryAllDashboards();
	
	void removeDashboardById(Long id);
	
	void addDashboard(DashboardEntity dashboard);
	
	void modifyDashboard(DashboardEntity dashboard);
}
