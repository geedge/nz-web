/*
package com.nis.modules.terminal.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.terminal.entity.Account;
import com.nis.modules.terminal.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ACCOUNT)
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = accountService.queryPage(params);
        return R.ok(page);
    }


    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.ACCOUNT)
    public R save(@RequestBody Account account) {
        // 校验参数
        accountService.validateAccount(account);

        accountService.saveOrUpdateAccount(account);
        return R.ok();
    }

    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ACCOUNT)
    public R update(@RequestBody Account account) {
        // 校验参数
        ValidateUtils.is(account.getId()).notNull(RCode.ACCOUNT_ID_ISNULL);
        accountService.validateAccount(account);

        accountService.saveOrUpdateAccount(account);
        return R.ok();
    }

    @DeleteMapping
    @ResponseBody
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ACCOUNT)
    public R delete(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ACCOUNT_ID_ISNULL);

        accountService.delete(ids);
        return R.ok();
    }

}*/
