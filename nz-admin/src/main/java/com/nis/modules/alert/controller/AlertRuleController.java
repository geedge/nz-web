package com.nis.modules.alert.controller;

import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.YamlUtil;

/**
 * @author song
 */
@RestController
@RequestMapping("alert/rule")
public class AlertRuleController {
	
	private final static Logger logger = LoggerFactory.getLogger(AlertRuleController.class);
	
    @Autowired
    private AlertRuleService alertRuleService;

    /**
     * 列表
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = alertRuleService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 保存
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    public R save(@RequestBody AlertRuleEntity alertRule) {
    	ValidateUtils.is(alertRule.getAlertName()).notNull(RCode.ALERTRULE_NAME_ISNULL)
    					.and(alertRule.getExpr()).notNull(RCode.ALERTRULE_EXPR_ISNULL)
    					.and(alertRule.getLast()).notNull(RCode.ALERTRULE_LAST_ISNULL).min(1, RCode.ALERTRULE_LAST_ISNOTPOSITIVE)
    					.and(alertRule.getSeverity()).notNull(RCode.ALERTRULE_SEVERITY_ISNULL);
    	//校验名称重复
    	if (!alertRuleService.checkName(alertRule)) {
    		return R.error(RCode.ALERTRULE_NAME_DUPLICATE);
    	}
    	//校验告警级别合法性
    	if (!Constant.ALERT_LEVEL.contains(alertRule.getSeverity().toLowerCase())) {
    		return R.error(RCode.ALERTRULE_SEVERITY_WRONG);
    	}
    	//校验expr
    	boolean checkRule = false;
    	try {
			checkRule = YamlUtil.checkRule(YamlUtil.alertRuleEntityToYamlMap(alertRule));
		} catch (Exception e) {
			logger.error("expr校验失败", e);
		} finally {
			if (!checkRule) {
				throw new NZException(RCode.ALERTRULE_EXPR_ERROR);
			}
		}
    	
		alertRuleService.save(alertRule);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping(value = "", method = RequestMethod.PUT)
    public R update(@RequestBody AlertRuleEntity alertRule){
    	ValidateUtils.is(alertRule.getAlertName()).notNull(RCode.ALERTRULE_NAME_ISNULL)
						.and(alertRule.getId()).notNull(RCode.ALERTRULE_ID_ISNULL)
    					.and(alertRule.getExpr()).notNull(RCode.ALERTRULE_EXPR_ISNULL)
						.and(alertRule.getLast()).notNull(RCode.ALERTRULE_LAST_ISNULL).min(1, RCode.ALERTRULE_LAST_ISNOTPOSITIVE)
						.and(alertRule.getSeverity()).notNull(RCode.ALERTRULE_SEVERITY_ISNULL);
    	//校验名称重复
    	if (!alertRuleService.checkName(alertRule)) {
    		return R.error(RCode.ALERTRULE_NAME_DUPLICATE);
    	}
    	//校验告警级别合法性
    	if (!Constant.ALERT_LEVEL.contains(alertRule.getSeverity().toLowerCase())) {
    		return R.error(RCode.ALERTRULE_SEVERITY_WRONG);
    	}
    	//校验expr
    	boolean checkRule = false;
    	try {
			checkRule = YamlUtil.checkRule(YamlUtil.alertRuleEntityToYamlMap(alertRule));
		} catch (Exception e) {
			logger.error("expr校验失败", e);
		} finally {
			if (!checkRule) {
				return R.error(RCode.ALERTRULE_EXPR_ERROR);
			}
		}
    	
    	alertRuleService.updateById(alertRule);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public R delete(String ids){
    	ValidateUtils.is(ids).notNull();
    	String[] split = ids.split(",");
		alertRuleService.removeByIds(Arrays.asList(split));

        return R.ok();
    }
    
    /**
     * 校验alertName重复 
     */
    @RequestMapping(value = "checkName", method = RequestMethod.GET)
    public R checkName(@RequestParam Map<String, Object> params){
    	ValidateUtils.is(params.get("alertName")).notNull();
		//校验名称重复
		AlertRuleEntity queryParam = new AlertRuleEntity();
		Object object = params.get("id");
		if (object != null && !StringUtils.isEmpty(object.toString())) {
			queryParam.setId(Integer.valueOf((String)object));
		}
		queryParam.setAlertName((String)params.get("alertName"));
		
		if (alertRuleService.checkName(queryParam)) {
			return R.ok();
		} else {
			return R.error(RCode.ALERTRULE_NAME_DUPLICATE);
		}
    }
}
