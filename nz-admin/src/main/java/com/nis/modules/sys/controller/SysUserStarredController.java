package com.nis.modules.sys.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.SysUserStarredService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/user")
public class SysUserStarredController {

    @Autowired
    private SysUserStarredService sysUserStarredService;

    @GetMapping("/starred")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
    public R queryStarred(String type) {
        List<SysUserStarredEntity> sysUserStarredEntityList = sysUserStarredService.queryStarred(type);
        Map<String, Object> starredMap = Tool.MapUtil.newConcurrentHashMap();
        sysUserStarredEntityList = ToolUtil.isEmpty(sysUserStarredEntityList) ? Tool.ListUtil.empty() : sysUserStarredEntityList;
        starredMap.put("list",sysUserStarredEntityList);
        return R.ok(starredMap);
    }

    @PostMapping("/starred")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.SYSTEM)
    @RequiresPermissions("sys:user:save")
    public R addStarred(@RequestBody SysUserStarredEntity sysUserStarredEntity) {
        sysUserStarredService.addStarred(sysUserStarredEntity);
        return R.ok();
    }

    @DeleteMapping("/starred")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.SYSTEM)
    @RequiresPermissions("sys:user:delete")
    public R delete(@RequestParam Map<String, Object> params) {
        sysUserStarredService.deleteStarred(params);
        return R.ok();
    }
}
