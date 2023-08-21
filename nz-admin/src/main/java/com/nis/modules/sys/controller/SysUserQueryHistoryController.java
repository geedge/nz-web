package com.nis.modules.sys.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.entity.SysUserQueryHistoryEntity;
import com.nis.modules.sys.service.SysUserQueryHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sys/user")
public class SysUserQueryHistoryController {

    @Autowired
    private SysUserQueryHistoryService sysUserQueryHistoryService;

    /**
     * 用户查询记录查询
     * @param params
     * @return
     */
    @GetMapping("/queryHistory")
    public R queryHistory(@RequestParam Map<String, Object> params){
        ValidateUtils.is(params.get("key")).notNull(RCode.SYS_USER_QUERY_KEY_ISNULL);
        PageUtils page = sysUserQueryHistoryService.queryHistory(params);

        return R.ok(page);
    }

    /**
     * 用户查询记录保存
     * @param userQueryHistoryEntity
     * @return
     */
    @PostMapping("/queryHistory")
    public R saveQueryHistory(@RequestBody SysUserQueryHistoryEntity userQueryHistoryEntity){
        ValidateUtils.is(userQueryHistoryEntity.getQueryKey()).notNull(RCode.SYS_USER_QUERY_KEY_ISNULL)
                .and(userQueryHistoryEntity.getQueryValue()).notNull(RCode.SYS_USER_QUERY_VALUE_ISNULL);

        Integer id = sysUserQueryHistoryService.saveQueryHistory(userQueryHistoryEntity);

        return R.ok().putData("id", id);
    }

    /**
     * 用户查询记录删除
     * @param params
     * @return
     */
    @DeleteMapping("/queryHistory")
    public R deleteQueryHistory(@RequestParam Map params){

        sysUserQueryHistoryService.deleteQueryHistory(params);

        return R.ok();
    }
}
