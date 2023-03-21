package com.nis.modules.alert.controller;

import cn.hutool.core.util.StrUtil;
import com.nis.common.annotation.SysLog;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertNotifyMethod;
import com.nis.modules.alert.service.AlertNotifyMethodService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/alert/notify/method")
public class AlertNotifyMethodController {

    @Autowired
    private AlertNotifyMethodService alertNotifyMethodService;

    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTNOTIFYMETHOD)
    public R detail(@PathVariable(value = "id",required = true) Integer id){
        AlertNotifyMethod alertNotifyMethod = alertNotifyMethodService.getById(id);

        return R.ok(alertNotifyMethod);
    }

    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTNOTIFYMETHOD)
    public R list(@RequestParam Map<String, Object> params ){
        PageUtils page = alertNotifyMethodService.queryPage(params);

        return R.ok(page);
    }

    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.ALERTNOTIFYMETHOD)
    @RequiresPermissions({"alert:notify:save"})
    public R save(@RequestBody AlertNotifyMethod alertNotifyMethod){
        alertNotifyMethod.setName(StrUtil.trim(alertNotifyMethod.getName()));
        alertNotifyMethodService.validateNotifyMethod(alertNotifyMethod,false);
        alertNotifyMethod.setBuildIn(0);
        alertNotifyMethodService.save(alertNotifyMethod);
        return R.ok();
    }
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ALERTNOTIFYMETHOD)
    @RequiresPermissions({"alert:notify:update"})
    public R update(@RequestBody AlertNotifyMethod alertNotifyMethod){
        alertNotifyMethod.setName(StrUtil.trim(alertNotifyMethod.getName()));
        alertNotifyMethodService.validateNotifyMethod(alertNotifyMethod,true);
        alertNotifyMethodService.updateById(alertNotifyMethod);
        return R.ok();
    }

    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ALERTNOTIFYMETHOD)
    @RequiresPermissions({"alert:notify:delete"})
    public R delete(@RequestParam(value = "ids",required = true)String ids){
        alertNotifyMethodService.deleteByIds(Tool.StrUtil.split(ids, ","));
        return R.ok();
    }
}
