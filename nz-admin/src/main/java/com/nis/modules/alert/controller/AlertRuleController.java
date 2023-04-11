package com.nis.modules.alert.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleEvalLog;
import com.nis.modules.alert.entity.AlertRuleSchedLog;
import com.nis.modules.alert.service.AlertRuleEvalLogService;
import com.nis.modules.alert.service.AlertRuleSchedLogService;
import com.nis.modules.alert.service.AlertRuleService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        ValidateUtils.is(alertRule.getName()).notNull(RCode.ALERTRULE_NAME_ISNULL).maxLength(64)
                .and(alertRule.getExpr()).notNull(RCode.ALERTRULE_EXPR_ISNULL).maxLength(512)
                .and(alertRule.getLast()).notNull(RCode.ALERTRULE_LAST_ISNULL).min(1, RCode.ALERTRULE_LAST_ISNOTPOSITIVE)
                .and(alertRule.getSummary()).notNull(RCode.ALERTRULE_SUMMARY_ISNULL).maxLength(512)
                .and(alertRule.getDescription()).maxLength(512)
                .and(alertRule.getType()).notNull(RCode.ALERTRULE_TYPE_ISNULL);

        return R.ok().putData("id", alertRuleService.saveOrUpdateAlertRule(alertRule));
    }

    /**
     * 修改
     */
    @RequestMapping(value = "", method = RequestMethod.PUT)
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ALERTRULE)
    @RequiresPermissions({"alert:rule:update"})
    public R update(@RequestBody AlertRuleEntity alertRule){
        ValidateUtils.is(alertRule.getId()).notNull(RCode.ALERTRULE_ID_ISNULL)
                .and(alertRule.getName()).notNull(RCode.ALERTRULE_NAME_ISNULL).maxLength(64)
                .and(alertRule.getExpr()).notNull(RCode.ALERTRULE_EXPR_ISNULL).maxLength(512)
                .and(alertRule.getLast()).notNull(RCode.ALERTRULE_LAST_ISNULL).min(1, RCode.ALERTRULE_LAST_ISNOTPOSITIVE)
                .and(alertRule.getSummary()).notNull(RCode.ALERTRULE_SUMMARY_ISNULL).maxLength(512)
                .and(alertRule.getDescription()).maxLength(512)
                .and(alertRule.getType()).notNull(RCode.ALERTRULE_TYPE_ISNULL);

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
    public void getTemplateByLanguage(HttpServletResponse response,
                                      @RequestParam(required = false, defaultValue = "en") String language,
                                      @RequestParam(required = false, defaultValue = "1") String format) throws IOException {

        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(format)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }

        alertRuleService.getTemplateByLanguage(response, format);
    }

    @PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.ALERTRULE)
    @RequiresPermissions({"alert:rule:save"})
    public R importAlertFromExcelFile(@RequestParam(value = "file") MultipartFile excelFile, HttpServletRequest request, @RequestParam Map<String, Object> params) throws IOException {
        String language = request.getHeader(Constant.LANGUAGE);
        ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);
        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }

        return R.ok(alertRuleService.importAlertRuleData(excelFile, params));
    }

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.ALERTRULE)
    public void export(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
        Object format = params.get("format");
        String formatStr = format == null ? "1" : format.toString();
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(formatStr)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }
        params.put("format", formatStr);

        alertRuleService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ALERTRULE)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);

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
