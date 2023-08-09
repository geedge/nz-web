package com.nis.modules.sys.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.service.SysUserPrefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * System User Preference Controller
 *
 * @author shizhendong
 * @date 2023-08-09
 */
@RestController
@RequestMapping
public class SysUserPrefController {


    @Autowired
    private SysUserPrefService sysUserPrefService;

    /**
     * query current user preference list
     */
    @GetMapping("/sys/user/preference")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYS_USER_PREFERENCE)
    public R queryCurrentUserPrefList(@RequestParam(required = false) String key) {
        return R.ok(sysUserPrefService.queryCurrentUserPrefList(key));
    }


    /**
     * update current user preference
     *
     * @param updateKeyValueMap
     * @return
     */
    @PutMapping("/sys/user/preference")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.SYS_USER_PREFERENCE)
    public R updateCurrentUserPref(@RequestBody(required = false) Map<String, String> updateKeyValueMap) {
        ValidateUtils.is(updateKeyValueMap).notNull();

        sysUserPrefService.updateCurrentUserPref(updateKeyValueMap);
        return R.ok();
    }

    /**
     * delete current user preference
     */
    @DeleteMapping("/sys/user/preference")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.SYS_USER_PREFERENCE)
    public R deleteCurrentUserPref(@RequestParam(required = false) String key) {
        sysUserPrefService.deleteCurrentUserPref(key);
        return R.ok();
    }

}
