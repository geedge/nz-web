package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.asset.entity.IssueActivity;
import com.nis.modules.asset.entity.IssueRecord;
import com.nis.modules.asset.service.IssueActivityService;
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
    @Autowired
    private IssueActivityService issueActivityService;

    /**
     * 问题详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ISSUERECORD)
    public R queryIssueInfo(@PathVariable("id") Integer id) {
        return R.ok(issueRecordService.queryIssueInfo(id));
    }

    /**
     * 问题请求列表
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ISSUERECORD)
    public R queryIssuePage(@RequestParam Map<String, Object> params) { return R.ok(issueRecordService.queryIssuePage(params)); }

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
        return R.ok().putData("list", issueRecordService.queryType(name));
    }

    /**
     * 问题动态查询
     * @param params
     * @return
     */
    @GetMapping("/activity")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ISSUEACTIVITY)
    public R queryIssueActivityPage(@RequestParam Map<String, Object> params) { return R.ok(issueActivityService.queryIssueActivityPage(params)); }

    /**
     * 保存
     * @param issueActivity
     * @return
     */
    @PostMapping("/activity")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ISSUEACTIVITY)
    @RequiresPermissions({"issueActivity:save"})
    public R saveIssueActivity(@RequestBody IssueActivity issueActivity) { return R.ok().putData("id", issueActivityService.saveIssueActivity(issueActivity)); }

    /**
     * 修改
     * @param issueActivity
     * @return
     */
    @PutMapping("/activity")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ISSUEACTIVITY)
    @RequiresPermissions({"issueActivity:update"})
    public R updateIssueActivity(@RequestBody IssueActivity issueActivity) { return R.ok().putData("id", issueActivityService.updateIssueActivity(issueActivity)); }

    /**
     * 删除
     * @param ids
     * @return
     */
    @DeleteMapping("/activity")
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ISSUEACTIVITY)
    @RequiresPermissions({"issueActivity:delete"})
    public R deleteIssueActivity(@RequestParam Integer... ids) {
        issueActivityService.deleteIssueActivity(ids);
        return R.ok();
    }
}
