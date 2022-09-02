package com.nis.modules.alert.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.RecordRuleEntity;
import com.nis.modules.alert.entity.RecordRuleEvalLog;
import com.nis.modules.alert.service.RecordRuleEvalLogService;
import com.nis.modules.alert.service.RecordRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/record/rule")
public class RecordRuleController {

    @Autowired
    private RecordRuleService recordRuleService;

    @Autowired
    private RecordRuleEvalLogService recordRuleEvalLogService;

    /**
     * 详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.RECORD_RULE)
    public R queryRecordRule(@PathVariable Integer id) {
        ValidateUtils.is(id).notNull(RCode.RECORD_RULE_ID_ISNULL);
        RecordRuleEntity recordRule = recordRuleService.queryRecordRule(id);
        return R.ok(recordRule);
    }

    /**
     * 列表
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.RECORD_RULE)
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page =  recordRuleService.list(params);
        return R.ok(page);
    }

    /**
     *
     * 新增
     * @param recordRule
     * @return
     */
    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.RECORD_RULE)
    public R saveRecordRule(@RequestBody RecordRuleEntity recordRule) {
        int id =  recordRuleService.saveRecordRule(recordRule);
        return R.ok().putData("id", id);
    }

    /**
     * 修改
     * @param recordRule
     * @return
     */
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.RECORD_RULE)
    public R updateRecordRule(@RequestBody RecordRuleEntity recordRule) {
        ValidateUtils.is(recordRule.getId()).notNull(RCode.RECORD_RULE_ID_ISNULL);
        int id =  recordRuleService.updateRecordRule(recordRule);
        return R.ok().putData("id", id);
    }

    /**
     * 删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.RECORD_RULE)
    public R deleteRecordRule(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.RECORD_RULE_ID_ISNULL);
        recordRuleService.deleteRecordRule(ids);
        return R.ok();
    }

    /**
     * 执行记录查询
     * @param id
     * @return
     */
    @GetMapping("/{id}/evalLog")
    public R queryEvalLog(@PathVariable Integer id) {
        ValidateUtils.is(id).notNull(RCode.RECORD_RULE_ID_ISNULL);
        List<RecordRuleEvalLog> recordRuleEvalLogs = recordRuleEvalLogService.queryListByRuleId(id);
        return R.ok().putData("list",recordRuleEvalLogs);
    }


}
