package com.nis.modules.alert.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;

import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;

import freemarker.template.utility.StringUtil;


@Service("alertRuleService")
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleDao, AlertRuleEntity> implements AlertRuleService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
    	boolean searchId = false;
    	Object object = params.get("id");
    	if (object != null && !StringUtils.isEmpty(object.toString())) {
    		searchId = true;
    	}
    	IPage<AlertRuleEntity> page = this.page(
                new Query<AlertRuleEntity>().getPage(params),
                new QueryWrapper<AlertRuleEntity>()
                	.like(!StringUtils.isEmpty((String)params.get("alertName")), "alert_name", (String)params.get("alertName"))
                	.like(!StringUtils.isEmpty((String)params.get("expr")), "expr", (String)params.get("expr"))
                	.eq(!StringUtils.isEmpty((String)params.get("severity")), "severity", (String)params.get("severity"))
                	.eq(searchId, "id", searchId ? Integer.parseInt(params.get("id").toString()) : "")
        );

        return new PageUtils(page);
    }

	@Override
	public boolean checkName(AlertRuleEntity alertRule) {
		List<AlertRuleEntity> list = this.list(new QueryWrapper<AlertRuleEntity>()
				.eq("alert_name", alertRule.getAlertName()));
		
		//当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
		if ((list == null || list.size() == 0) ||
				(list.size() == 1 && alertRule.getId() != null && list.get(0).getId().intValue() == alertRule.getId().intValue())) {
			return true;
		}
		return false;
	}

}