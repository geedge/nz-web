package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
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
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.DATACENTER)
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
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.DATACENTER)
    public R save(@RequestBody Idc idc) {
        ValidateUtils.is(idc.getName()).notNull(RCode.IDC_IDCNAME_ISNULL)
                .and(idc.getY()).min(1, RCode.IDC_Y_MINIMUM)
                .and(idc.getX()).min(1, RCode.IDC_X_MINIMUM);

        // 电话格式校验
        if (StringUtils.isNotEmpty(idc.getTel())) {
            ValidateUtils.is(idc.getTel()).phone(RCode.IDC_IDCPHONE_FORMAT);
        }

        // 校验名称重复
        if (!idcService.checkName(idc.getId(), idc.getName())) {
            // 名称重复
            throw new NZException(RCode.IDC_NAME_DUPLICATE);
        }

        // 默认 行列值
        if(idc.getY() == null){
            idc.setY(1);
        }
        if(idc.getX() == null){
            idc.setX(1);
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
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.DATACENTER)
    public R update(@RequestBody Idc idc) {
        ValidateUtils.is(idc.getId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(idc.getName()).notNull(RCode.IDC_IDCNAME_ISNULL)
                .and(idc.getY()).min(1, RCode.IDC_Y_MINIMUM)
                .and(idc.getX()).min(1, RCode.IDC_X_MINIMUM);

        // 电话格式校验
        if (StringUtils.isNotEmpty(idc.getTel())) {
            ValidateUtils.is(idc.getTel()).phone(RCode.IDC_IDCPHONE_FORMAT);
        }

        // 校验名称重复
        if (!idcService.checkName(idc.getId(), idc.getName())) {
            // 名称重复
            throw new NZException(RCode.IDC_NAME_DUPLICATE);
        }

        // 校验行列是否可以修改
        if(idc.getX() != null || idc.getY() != null){
            idcService.validateIdcSize(idc);
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
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.DATACENTER)
    public R delete(@RequestParam Integer ... ids) {
        ValidateUtils.is(ids).notNull(RCode.IDC_IDCID_ISNULL);

        idcService.delete(ids);
        return R.ok();
    }
}