package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.asset.entity.IssueRecord;
import com.nis.modules.asset.service.IssueRecordService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * 问题管理  controller
 */
@RestController
@RequestMapping("/issue")
public class IssueController {

    @Autowired
    private IssueRecordService issueRecordService;

    /**
     * 问题详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ISSUERECORD)
    public R queryAssetInfo(@PathVariable("id") Integer id) {
        return R.ok(issueRecordService.queryIssueInfo(id));
    }

    /**
     * 问题请求列表
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ISSUERECORD)
    public R queryAssetPage(@RequestParam Map<String, Object> params) { return R.ok(issueRecordService.queryIssuePage(params)); }

    /**
     * 保存
     * @param issueRecord
     * @return
     */
    @PostMapping
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ISSUERECORD)
    @RequiresPermissions({"issueRecord:save"})
    public R saveIssue(@RequestBody IssueRecord issueRecord) { return R.ok().putData("id", issueRecordService.saveIssue(issueRecord)); }

    /**
     * 修改
     * @param issueRecord
     * @return
     */
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ISSUERECORD)
    @RequiresPermissions({"issueRecord:update"})
    public R updateIssue(@RequestBody IssueRecord issueRecord) {
        ValidateUtils.is(issueRecord.getId()).notNull(RCode.ISSUE_ID_ISNULL);
        return R.ok().putData("id", issueRecordService.updateIssue(issueRecord));
    }

    /**
     * 删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ISSUERECORD)
    @RequiresPermissions({"issueRecord:delete"})
    public R deleteIssue(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ISSUE_ID_ISNULL);
        issueRecordService.deleteIssue(ids);
        return R.ok();
    }

    /**
     * 问题类型查询
     * @param name
     * @return
     */
    @GetMapping("/type")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ISSUERECORD)
    public R queryType(String name) {
        return R.ok(issueRecordService.queryType(name));
    }

}
