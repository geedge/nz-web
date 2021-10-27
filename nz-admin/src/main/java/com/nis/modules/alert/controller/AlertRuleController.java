package com.nis.modules.alert.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleEvalLog;
import com.nis.modules.alert.entity.AlertRuleSchedLog;
import com.nis.modules.alert.service.AlertRuleEvalLogService;
import com.nis.modules.alert.service.AlertRuleSchedLogService;
import com.nis.modules.alert.service.AlertRuleService;

/**
 * @author song
 */
@RestController
@RequestMapping("alert/rule")
public class AlertRuleController {
	
    @Autowired
    private AlertRuleService alertRuleService;
    
    @Autowired
    private AlertRuleEvalLogService alertRuleEvalLogService;
    
    @Autowired
    private AlertRuleSchedLogService alertRuleSchedLogService;
    /**
     * 详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTRULE)
    public R queryAlertRule(@PathVariable Integer id) {
        ValidateUtils.is(id).notNull(RCode.ALERTRULE_ID_ISNULL);

        AlertRuleEntity alertRule = alertRuleService.queryAlertRule(id);

        return R.ok(alertRule);
    }

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
    @RequiresPermissions({"alert:rule:save"})
    public R save(@RequestBody AlertRuleEntity alertRule) {

        return R.ok().putData("id", alertRuleService.saveOrUpdateAlertRule(alertRule));
    }

    /**
     * 修改
     */
    @RequestMapping(value = "", method = RequestMethod.PUT)
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ALERTRULE)
    @RequiresPermissions({"alert:rule:update"})
    public R update(@RequestBody AlertRuleEntity alertRule){
        ValidateUtils.is(alertRule.getId()).notNull(RCode.ALERTRULE_ID_ISNULL);

        return R.ok().putData("id", alertRuleService.saveOrUpdateAlertRule(alertRule));
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


    @GetMapping("/template")
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        if (ToolUtil.isEmpty(language)) throw new NZException(RCode.ALERTRULE_EXPORT_HEADER_LANGUAGE_ISNULL);

        alertRuleService.getExportTemplateByLanguage(response, language);
    }

    @PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.ALERTRULE)
    @RequiresPermissions({"alert:rule:save"})
    public R importAlertFromExcelFile(MultipartFile excelFile,String language) {
        ValidateUtils.is(excelFile).notNull(RCode.ALERTRULE_IMPORT_FILE_ISNULL)
                .and(language).notNull(RCode.ALERTRULE_EXPORT_HEADER_LANGUAGE_ISNULL);

        return R.ok(alertRuleService.importAlertFromExcelFile(excelFile, language));
    }

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.ALERTRULE)
    public void exportAssetsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        alertRuleService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ALERTRULE)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.ALERTRULE_CANCELIMPORTSEQ_ISNULL);

        alertRuleService.cancelImport(seq);
        return R.ok();
    }
    
    @GetMapping("/{id}/evalLog")
    public R queryEvalLog(@PathVariable Integer id) {
    	ValidateUtils.is(id).notNull(RCode.ALERTRULE_ID_ISNULL);
    	List<AlertRuleEvalLog> alertRuleEvalLogs = alertRuleEvalLogService.queryListByRuleId(id);
    	return R.ok(alertRuleEvalLogs);
    }
    
    @GetMapping("/{id}/schedLog")
    public R querySchedLog(@PathVariable Integer id) {
    	ValidateUtils.is(id).notNull(RCode.ALERTRULE_ID_ISNULL);
    	List<AlertRuleSchedLog> alertRuleSchedLogs = alertRuleSchedLogService.queryListByRuleId(id);
    	return R.ok(alertRuleSchedLogs);
    }
}
