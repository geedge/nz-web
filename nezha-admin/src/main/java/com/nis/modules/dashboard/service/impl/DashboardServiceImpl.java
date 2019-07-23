package com.nis.modules.dashboard.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.dashboard.dao.DashboardDao;
import com.nis.modules.dashboard.entity.DashboardEntity;
import com.nis.modules.dashboard.service.DashboardService;
import com.nis.modules.sys.shiro.ShiroUtils;

@Service("dashboardService")
public class DashboardServiceImpl extends ServiceImpl<DashboardDao, DashboardEntity> implements DashboardService{

	@Override
	public List<DashboardEntity> queryAllDashboards() {
		// 获取当前用户的所在系统信息
		Long currSystemId = ShiroUtils.getCurrSystemId();
		// 根据查询出来的信息 获取用户sql语句
		// 根据sql语句查询数据信息
		// 组装数据返回前端
		QueryWrapper<DashboardEntity> s=new QueryWrapper<DashboardEntity>();
		s.eq("system_id",currSystemId);
		return baseMapper.selectList(s);
	}

	@Override
	public void removeDashboardById(Long id) {
		baseMapper.deleteById(id);
	}

	@Override
	public void addDashboard(DashboardEntity dashboard) {
		dashboard.setOperator();
		dashboard.setOptime();
		baseMapper.insert(dashboard);
	}

	@Override
	public void modifyDashboard(DashboardEntity dashboard) {
		dashboard.setOperator();
		dashboard.setOptime();
		baseMapper.updateById(dashboard);
	}
}
