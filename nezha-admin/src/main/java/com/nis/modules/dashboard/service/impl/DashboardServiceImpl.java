package com.nis.modules.dashboard.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.modules.dashboard.utils.DateUtils;
import com.nis.modules.dashboard.utils.TimeUtils;
import com.nis.common.utils.RCode;
import com.nis.modules.dashboard.dao.DashboardDao;
import com.nis.modules.dashboard.entity.ChartEntity;
import com.nis.modules.dashboard.entity.ChartSeriesEntity;
import com.nis.modules.dashboard.entity.ChartSingleEntity;
import com.nis.modules.dashboard.entity.ChartStatisticalEntity;
import com.nis.modules.dashboard.entity.ChartTableEntity;
import com.nis.modules.dashboard.entity.ChartType;
import com.nis.modules.dashboard.entity.DashboardEntity;
import com.nis.modules.dashboard.entity.UserSql;
import com.nis.modules.dashboard.service.DashboardService;
import com.nis.modules.sys.shiro.ShiroUtils;

@Service("dashboardService")
public class DashboardServiceImpl extends ServiceImpl<DashboardDao, DashboardEntity> implements DashboardService{
	private final static Logger logger=LoggerFactory.getLogger(DashboardServiceImpl.class);
	@Override
	public List<DashboardEntity> queryAllDashboards() {
		// 获取当前用户的所在系统信息
		Long currSystemId = ShiroUtils.getCurrSystemId();
		logger.info("systemID is "+currSystemId);
		// 根据查询出来的信息 获取用户sql语句
		// 根据sql语句查询数据信息
		// 组装数据返回前端
		QueryWrapper<DashboardEntity> s=new QueryWrapper<DashboardEntity>();
		s.eq("system_id",currSystemId);
		List<DashboardEntity> dashboardEntitys = baseMapper.selectList(s);
		for (DashboardEntity dashboardEntity : dashboardEntitys) {
			List<UserSql> userSqls=null;
			switch(ChartType.getChartNameByTypeId(Integer.valueOf(dashboardEntity.getChartType()))) {
			case STATISTICAL:
				ChartStatisticalEntity chartStatisticalEntity = JSON.parseObject(dashboardEntity.getData(), ChartStatisticalEntity.class);
				userSqls = chartStatisticalEntity.getUserSqls();
				List<List<ChartSeriesEntity>> seriess=new ArrayList<List<ChartSeriesEntity>>();
				for (UserSql userSql : userSqls) {
					// 验证用户输入sql是否有问题
					List<ChartSeriesEntity> chartStatisticalSeries = baseMapper.selectResultData(userSql.getSql());
					seriess.add(chartStatisticalSeries);
				}
				String series = JSON.toJSONString(seriess);
				chartStatisticalEntity.setSeries(series);
				String chartStatisticalEntityData = JSON.toJSONString(chartStatisticalEntity);
				dashboardEntity.setData(chartStatisticalEntityData);
				logger.info("dashboardEntity data info "+JSON.toJSONString(dashboardEntity));
				break;
			case SINGLE:
				ChartSingleEntity chartSingleEntity = JSON.parseObject(dashboardEntity.getData(), ChartSingleEntity.class);
				userSqls = chartSingleEntity.getUserSqls();
				List<ChartSeriesEntity> chartSingleSeries = baseMapper.selectResultData(userSqls.get(0).getSql());
				String singleResult = JSON.toJSONString(chartSingleSeries);
				chartSingleEntity.setSeries(singleResult);
				String chartSingleEntityData = JSON.toJSONString(chartSingleEntity);
				dashboardEntity.setData(chartSingleEntityData);
				logger.info("dashboardEntity data info "+JSON.toJSONString(dashboardEntity));
				break;
			case TABLE:
				ChartTableEntity chartTableEntity = JSON.parseObject(dashboardEntity.getData(), ChartTableEntity.class);
				userSqls = chartTableEntity.getUserSqls();
				List<ChartSeriesEntity> chartTableSeries = baseMapper.selectResultData(userSqls.get(0).getSql());
				String tableResult = JSON.toJSONString(chartTableSeries);
				chartTableEntity.setSeries(tableResult);
				String chartTableEntityData = JSON.toJSONString(chartTableEntity);
				dashboardEntity.setData(chartTableEntityData);
				logger.info("dashboardEntity data info "+JSON.toJSONString(dashboardEntity));
				break;
			default:
				break;
			}
		}
		return dashboardEntitys;
	}

	public DashboardEntity queryDashboardById(Long id) {
		DashboardEntity dashboardEntity = baseMapper.selectById(id);
		List<UserSql> userSqls=null;
		switch(ChartType.getChartNameByTypeId(Integer.valueOf(dashboardEntity.getChartType()))) {
		case STATISTICAL:
			ChartStatisticalEntity chartStatisticalEntity = JSON.parseObject(dashboardEntity.getData(), ChartStatisticalEntity.class);
			userSqls = chartStatisticalEntity.getUserSqls();
			List<List<ChartSeriesEntity>> seriess=new ArrayList<List<ChartSeriesEntity>>();
			for (UserSql userSql : userSqls) {
				// 验证用户输入sql是否有问题
				List<ChartSeriesEntity> chartStatisticalSeries = baseMapper.selectResultData(userSql.getSql());
				seriess.add(chartStatisticalSeries);
			}
			String series = JSON.toJSONString(seriess);
			chartStatisticalEntity.setSeries(series);
			String chartStatisticalEntityData = JSON.toJSONString(chartStatisticalEntity);
			dashboardEntity.setData(chartStatisticalEntityData);
			logger.info("dashboardEntity data info "+JSON.toJSONString(dashboardEntity));
			break;
		case SINGLE:
			ChartSingleEntity chartSingleEntity = JSON.parseObject(dashboardEntity.getData(), ChartSingleEntity.class);
			userSqls = chartSingleEntity.getUserSqls();
			List<ChartSeriesEntity> chartSingleSeries = baseMapper.selectResultData(userSqls.get(0).getSql());
			String singleResult = JSON.toJSONString(chartSingleSeries);
			chartSingleEntity.setSeries(singleResult);
			String chartSingleEntityData = JSON.toJSONString(chartSingleEntity);
			dashboardEntity.setData(chartSingleEntityData);
			logger.info("dashboardEntity data info "+JSON.toJSONString(dashboardEntity));
			break;
		case TABLE:
			ChartTableEntity chartTableEntity = JSON.parseObject(dashboardEntity.getData(), ChartTableEntity.class);
			userSqls = chartTableEntity.getUserSqls();
			List<ChartSeriesEntity> chartTableSeries = baseMapper.selectResultData(userSqls.get(0).getSql());
			String tableResult = JSON.toJSONString(chartTableSeries);
			chartTableEntity.setSeries(tableResult);
			String chartTableEntityData = JSON.toJSONString(chartTableEntity);
			dashboardEntity.setData(chartTableEntityData);
			logger.info("dashboardEntity data info "+JSON.toJSONString(dashboardEntity));
			break;
		default:
			break;
		}
		return dashboardEntity;
	}
	
	@Override
	@Transactional(readOnly = false)
	public void removeDashboardById(Long id) {
		baseMapper.deleteById(id);
	}

	@Override
	@Transactional(readOnly = false)
	public void addDashboard(DashboardEntity dashboard) {
		dashboard.setX(1);
		dashboard.setY(1);
		dashboard.setHeight(300);
		dashboard.setWidth(300);
		dashboard.setSystemId(ShiroUtils.getCurrSystemId());
		dashboard.setOperator();
		dashboard.setOptime();
		baseMapper.insert(dashboard);
	}

	@Override
	@Transactional(readOnly = false)
	public void modifyDashboard(DashboardEntity dashboard) {
		dashboard.setOperator();
		dashboard.setOptime();
		baseMapper.updateById(dashboard);
	}

	@Override
	@Transactional(readOnly = false)
	public void modifyDashboards(List<DashboardEntity> dashboards) {
		for(DashboardEntity dashboard:dashboards) {
			dashboard.setOperator();
			dashboard.setOptime();
			baseMapper.updateById(dashboard);
		}
	}
	
	@Override
	public void validateDashboardData(DashboardEntity dashboard) {
		List<UserSql> userSqls = null;
		String data = dashboard.getData();
		switch(ChartType.getChartNameByTypeId(Integer.valueOf(dashboard.getChartType()))) {
		case STATISTICAL:
			ChartStatisticalEntity chartStatisticalEntity = JSON.parseObject(data,ChartStatisticalEntity.class);
			userSqls = chartStatisticalEntity.getUserSqls();
			break;
		case SINGLE:
			ChartSingleEntity chartSingleEntity = JSON.parseObject(data,ChartSingleEntity.class);
			userSqls = chartSingleEntity.getUserSqls();
			break;
		case TABLE:
			ChartTableEntity chartTableEntity = JSON.parseObject(data,ChartTableEntity.class);
			userSqls = chartTableEntity.getUserSqls();
			break;
		default:
			break;
		}
		logger.info("userSql content is"+ userSqls);
		logger.info("start validate  userSql");
		ValidateUtils.is(userSqls).notNull(RCode.DASHBOARD_USERSQL_NULL);
		try {
			for (UserSql userSql : userSqls) {
				// 验证用户输入sql是否有问题
				baseMapper.selectResultData(userSql.getSql());
			}
		}catch(Exception e) {
			throw new NZException(RCode.DASHBOARD_USERSQL_ERROR);
		}
	}

	@Override
	public List<ChartSeriesEntity> queryData(String sql, String timeFormat, Integer chartType) {
		if(sql != null && !sql.isEmpty()) {
			sql=sql.replace("&lt;","<").replace("&gt;", ">");
		}else {
			return new ArrayList<ChartSeriesEntity>();
		}
		List<ChartSeriesEntity> resultDatas = baseMapper.selectResultData(sql);
		if(timeFormat != null && !timeFormat.isEmpty()&& chartType==3) {
			for(ChartSeriesEntity resultData : resultDatas) {
				resultData.setFormatTime(DateUtils.formatDate(resultData.getTime(),timeFormat));
			}
		}
		return resultDatas;
	}
}
