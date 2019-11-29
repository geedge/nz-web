package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.service.IdcService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * 数据中心  controller
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@RestController
@RequestMapping("/idc")
public class IdcController {

    @Autowired
    private IdcService idcService;

    /**
     * 数据中心请求列表
     *
     * @param params
     * @return
     */
    @GetMapping
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = idcService.queryPage(params);
        return R.ok(page);
    }

    /**
     * 保存
     *
     * @param idc
     * @return
     */
    @PostMapping
    @SysLog("save idc")
    public R save(@RequestBody Idc idc) {
        ValidateUtils.is(idc.getName()).notNull(RCode.IDC_IDCNAME_ISNULL);
        // 电话格式校验
        if (StringUtils.isNotEmpty(idc.getTel())) {
            ValidateUtils.is(idc.getTel()).phone(RCode.IDC_IDCPHONE_FORMAT);
        }

        idcService.saveOrUpdateIdc(idc);
        return R.ok();
    }

    /**
     * 修改
     *
     * @param idc
     * @return
     */
    @PutMapping
    @SysLog("update idc")
    public R update(@RequestBody Idc idc) {
        ValidateUtils.is(idc.getId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(idc.getName()).notNull(RCode.IDC_IDCNAME_ISNULL);
        // 电话格式校验
        if (StringUtils.isNotEmpty(idc.getTel())) {
            ValidateUtils.is(idc.getTel()).phone(RCode.IDC_IDCPHONE_FORMAT);
        }

        idcService.saveOrUpdateIdc(idc);
        return R.ok();
    }


    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog("delete idc")
    public R delete(String ids) {
        ValidateUtils.is(ids).notNull(RCode.IDC_IDCID_ISNULL);

        idcService.delete(ids);
        return R.ok();
    }
}