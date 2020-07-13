package com.nis.modules.alert.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author song
 */
@RestController
@RequestMapping("alert/rule")
public class AlertRuleController {
	
    @Autowired
    private AlertRuleService alertRuleService;

    /**
     * 列表
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTRULE)
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = alertRuleService.queryPage(params);

        return R.ok(page);
    }

    /**
     * 保存
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.ALERTRULE)
    public R save(@RequestBody AlertRuleEntity alertRule) {

    	alertRuleService.saveOrUpdateAlertRule(alertRule);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping(value = "", method = RequestMethod.PUT)
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ALERTRULE)
    public R update(@RequestBody AlertRuleEntity alertRule){
        ValidateUtils.is(alertRule.getId()).notNull(RCode.ALERTRULE_ID_ISNULL);

	    alertRuleService.saveOrUpdateAlertRule(alertRule);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping(value = "", method = RequestMethod.DELETE)
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ALERTRULE)
    public R delete(@RequestParam Integer ... ids){

        alertRuleService.delete(ids);
        return R.ok();
    }

    /**
     * 告警规则统计
     *
     * @param id
     * @return
     */
    @GetMapping("/stat")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTRULE)
    public R ruleStat(Integer id){
        ValidateUtils.is(id).notNull(RCode.ALERTRULE_ID_ISNULL);

        Map ruleStat = alertRuleService.getAlertStatById(id);
        return R.ok(ruleStat);
    }
}
