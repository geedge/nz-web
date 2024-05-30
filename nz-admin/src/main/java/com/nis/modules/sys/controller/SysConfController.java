package com.nis.modules.sys.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.service.impl.SysConfServiceImpl;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/sys/config")
public class SysConfController {
    @Autowired
    private SysConfService sysConfService;

    @GetMapping("/basic")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SYS_CONF)
    public R basic(){
        Map<String,String> result = sysConfService.batchQuery(SysConfServiceImpl.BASIC_KEYS);
        return R.ok(result);
    }

    @PutMapping("/basic")
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.SYS_CONF)
    @RequiresPermissions({"sys:config:update"})
    public R saveBasic(@RequestBody Map<String,String> params){

        sysConfService.saveBasic(params);
        return R.ok();
    }

    @GetMapping("/monitor")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SYS_CONF)
    public R monitor(){
        Map<String,String> result = sysConfService.batchQuery(SysConfServiceImpl.MONITOR_KEYS);
        result = new TreeMap<>(result);
        return R.ok(result);
    }

    @PutMapping("/monitor")
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.SYS_CONF)
    @RequiresPermissions({"sys:config:update"})
    public R saveMonitor(@RequestBody Map<String,String> params){

        sysConfService.saveMonitor(params);
        return R.ok();
    }

    @GetMapping("/email")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SYS_CONF)
    public R email(){
        Map<String,String> result = sysConfService.batchQuery(SysConfServiceImpl.EMAIL_KEYS);

        return R.ok(result);
    }

    @PutMapping("/email")
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.SYS_CONF)
    @RequiresPermissions({"sys:config:update"})
    public R saveEmail(@RequestBody Map<String,String> params){
        sysConfService.saveEmail(params);
        return R.ok();
    }

    @GetMapping("/terminal")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SYS_CONF)
    public R terminal(){
        Map<String,String> result = sysConfService.batchQuery(SysConfServiceImpl.TERMINAL_KEYS);

        return R.ok(result);
    }

    @PutMapping("/terminal")
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.SYS_CONF)
    @RequiresPermissions({"sys:config:update"})
    public R saveTerminal(@RequestBody Map<String,String> params){
        sysConfService.saveTerminal(params);
        return R.ok();
    }
    @GetMapping("/ldap")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SYS_CONF)
    public R ldap(){
        Map<String,String> result = sysConfService.batchQuery(SysConfServiceImpl.LDAP_KEYS);
        //不将密码传递到前端
        result.remove("ldap_password");
        return R.ok(result);
    }

    @PutMapping("/ldap")
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.SYS_CONF)
    @RequiresPermissions({"sys:config:update"})
    public R saveLdap(@RequestBody Map<String,String> params){
        sysConfService.saveLdap(params);
        return R.ok();
    }

    @GetMapping("/key/{configKey}")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SYS_CONF)
    public R querySysConfig(@PathVariable("configKey") String configKey){
        Map<String ,Object> result = sysConfService.querySysConfig(configKey);
        return R.ok(result);
    }

    @GetMapping("/auditlog")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYS_CONF)
    public R auditlog() {
        Map<String, String> result = sysConfService.batchQuery(SysConfServiceImpl.AUDITLOG_KEYS);
        return R.ok(result);
    }

    @PutMapping("/auditlog")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.SYS_CONF)
    @RequiresPermissions({"sys:config:update"})
    public R auditlog(@RequestBody Map<String, String> params) {
        sysConfService.saveAuditlog(params);
        return R.ok();
    }

}
