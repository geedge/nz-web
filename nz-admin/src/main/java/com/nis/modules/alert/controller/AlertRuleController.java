package com.nis.modules.alert.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
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
    @RequiresPermissions({"alert:rule:list"})
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = alertRuleService.queryPage(params);

        return R.ok(page);
    }

    /**
     * 保存
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.ALERTRULE)
    @RequiresPermissions({"alert:rule:save"})
    public R save(@RequestBody AlertRuleEntity alertRule) {

    	alertRuleService.saveOrUpdateAlertRule(alertRule);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping(value = "", method = RequestMethod.PUT)
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ALERTRULE)
    @RequiresPermissions({"alert:rule:update"})
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
    @RequiresPermissions({"alert:rule:delete"})
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
    @RequiresPermissions({"alert:rule:stat"})
    public R ruleStat(Integer id){
        ValidateUtils.is(id).notNull(RCode.ALERTRULE_ID_ISNULL);

        Map ruleStat = alertRuleService.getAlertStatById(id);
        return R.ok(ruleStat);
    }

    @GetMapping("/template")
    @RequiresPermissions({"alert:rule:template"})
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        if (ToolUtil.isEmpty(language)) throw new NZException(RCode.ALERTRULE_EXPORT_HEADER_LANGUAGE_ISNULL);

        alertRuleService.getExportTemplateByLanguage(response, language);
    }

    @PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.ALERTRULE)
    @RequiresPermissions({"alert:rule:import"})
    public R importAlertFromExcelFile(MultipartFile excelFile,String language) {
        ValidateUtils.is(excelFile).notNull(RCode.ALERTRULE_IMPORT_FILE_ISNULL)
                .and(language).notNull(RCode.ALERTRULE_EXPORT_HEADER_LANGUAGE_ISNULL);

        return R.ok(alertRuleService.importAlertFromExcelFile(excelFile, language));
    }

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.ALERTRULE)
    @RequiresPermissions({"alert:rule:export"})
    public void exportAssetsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        alertRuleService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ALERTRULE)
    @RequiresPermissions({"alert:rule:cancel:import"})
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.ALERTRULE_CANCELIMPORTSEQ_ISNULL);

        alertRuleService.cancelImport(seq);
        return R.ok();
    }
    
    
    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTRULE)
    public R queryAlertRule(@PathVariable Integer id) {
    	ValidateUtils.is(id).notNull(RCode.ALERTRULE_ID_ISNULL);
    	
    	AlertRuleEntity alertRule = alertRuleService.queryAlertRule(id);
    	
    	return R.ok(alertRule);
    }
}
