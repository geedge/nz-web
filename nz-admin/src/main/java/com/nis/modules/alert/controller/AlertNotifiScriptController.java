package com.nis.modules.alert.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.alert.entity.AlertNotificationScript;
import com.nis.modules.alert.service.AlertNotifiScriptService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("alert/script")
public class AlertNotifiScriptController {

    @Autowired
    private AlertNotifiScriptService notifiScriptService;

    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SCRIPT)
    @RequiresPermissions({"alert:script:list"})
    public R list(@RequestParam Map<String, Object> params) {
        List<AlertNotificationScript> list = notifiScriptService.queryList(params);
        Map map = new HashMap(2);
        map.put("list", list);
        return R.ok(map);
    }


    @PostMapping
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.SCRIPT)
    @RequiresPermissions({"alert:script:save"})
    public R saveScript(@RequestBody AlertNotificationScript alertNotificationScript) {
        notifiScriptService.saveOrUpdateNotifiScript(alertNotificationScript);
        return R.ok();
    }

    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.SCRIPT)
    @RequiresPermissions({"alert:script:update"})
    public R updateScript(@RequestBody AlertNotificationScript alertNotificationScript) {
        ValidateUtils.is(alertNotificationScript.getId()).notNull(RCode.ALERT_NOTIFISCRIPT_ID_ISNULL);

        notifiScriptService.saveOrUpdateNotifiScript(alertNotificationScript);
        return R.ok();
    }

    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.SCRIPT)
    @RequiresPermissions({"alert:script:delete"})
    public R delete(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ALERT_NOTIFISCRIPT_ID_ISNULL);

        notifiScriptService.deleteScripts(ids);
        return R.ok();
    }
}