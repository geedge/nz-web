package com.nis.modules.dashboard.test;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.AdminApplication;
import com.nis.modules.dashboard.entity.DashboardEntity;
import com.nis.modules.dashboard.service.DashboardService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AdminApplication.class)
public class DashboardTest {
	
	@Autowired
	private DashboardService dashboardService;
	
	
	@Test
	public void testDashboardService() {
		List<DashboardEntity> datas = dashboardService.queryAllDashboards();
		System.out.println(JSON.toJSONString(datas));
	}
}
