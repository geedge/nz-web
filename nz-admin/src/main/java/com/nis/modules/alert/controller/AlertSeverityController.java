package com.nis.modules.alert.controller;

import cn.hutool.log.Log;
import com.nis.common.annotation.SysLog;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertSeverityConf;
import com.nis.modules.alert.service.AlertSeverityService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alert/severity")
public class AlertSeverityController {
    private Log log = Log.get();

    @Autowired
    private AlertSeverityService alertSeverityService;

    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTSEVERITY)
    public R list(@RequestParam Map<String, Object> params ){
        log.info("query alert severity with parameter: {}", Tool.JSONUtil.toJsonStr(params));

        PageUtils page = alertSeverityService.queryPage(params);

        return R.ok(page);
    }

    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ALERTSEVERITY)
    public R detail(@PathVariable(value = "id",required = true) Integer id){
        AlertSeverityConf alertSeverityConf = alertSeverityService.getSeverityById(id);
        return R.ok(alertSeverityConf);
    }

    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.ALERTSEVERITY)
    @RequiresPermissions({"alert:severity:save"})
    public R save(@RequestBody AlertSeverityConf alertSeverityConf){
        alertSeverityConf.setName(Tool.StrUtil.trim(alertSeverityConf.getName()));
        alertSeverityService.validateSeverity(alertSeverityConf,false);
        alertSeverityService.saveSeverity(alertSeverityConf);
        return R.ok();
    }

    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ALERTSEVERITY)
    @RequiresPermissions({"alert:severity:update"})
    public R update(@RequestBody AlertSeverityConf alertSeverityConf){
        alertSeverityConf.setName(Tool.StrUtil.trim(alertSeverityConf.getName()));
        alertSeverityService.validateSeverity(alertSeverityConf,true);
        alertSeverityService.updateSeverity(alertSeverityConf);
        return R.ok();
    }

    @PutMapping("/weight")
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ALERTSEVERITY)
    public R weight(@RequestBody List<AlertSeverityConf> alertSeverityConfs){
        alertSeverityService.saveWeights(alertSeverityConfs);
        return R.ok();
    }


    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ALERTSEVERITY)
    @RequiresPermissions({"alert:severity:delete"})
    public R delete(@RequestParam(value = "ids",required = true)String ids){
        alertSeverityService.removeByIds(Tool.StrUtil.split(ids, ","));
        return R.ok();
    }

}
