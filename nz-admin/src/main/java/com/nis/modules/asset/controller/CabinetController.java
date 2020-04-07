package com.nis.modules.asset.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * 机柜  controller
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@RestController
@RequestMapping("/cabinet")
public class CabinetController {

    private static Logger logger = LoggerFactory.getLogger(CabinetController.class);

    @Autowired
    private CabinetService cabinetService;

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 机柜请求列表
     *
     * @param params
     * @return
     */
    @GetMapping
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = cabinetService.queryPage(params);
        return R.ok(page);
    }

    /**
     * 保存
     *
     * @param cabinet
     * @return
     */
    @PostMapping
    @SysLog("save cabinet")
    public R save(@RequestBody Cabinet cabinet) {
        ValidateUtils.is(cabinet.getName()).notNull(RCode.IDC_CABINETNAME_ISNULL)
                .and(cabinet.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(cabinet.getY()).min(1, RCode.CABINET_Y_MINIMUM)
                .and(cabinet.getX()).min(1, RCode.CABINET_X_MINIMUM);

        if (cabinet.getuSize() == null) {
            // 从配置表中获取到机柜默认大小
            SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", Constant.SYSCONFIG_KEY_DEFAULT_CABINETU).eq("status", 1));
            if (configEntity != null) {
                Integer defaultSize = null;
                try {
                    defaultSize = Integer.parseInt(configEntity.getParamValue());
                } catch (NumberFormatException e) {
                    logger.error("配置表中机柜默认值有误 param_key 为 {}", Constant.SYSCONFIG_KEY_DEFAULT_CABINETU);
                }

                if (defaultSize == null) {
                    throw new NZException(RCode.SYS_CONFIG_DEFALUTCABINETU_INTERVAL);
                }
                cabinet.setuSize(defaultSize);
            }
        } else {
            ValidateUtils.is(cabinet.getuSize()).min(1, RCode.IDC_CABINETUSIZE_MINIMUM)
                    .max(Constant.CURRENT_CABINET_MAXU, RCode.IDC_CABINET_MAXU);
        }

        // 校验名称重复
        if (!cabinetService.cabinetNameCheck(cabinet.getId(), cabinet.getName(), cabinet.getIdcId())) {
            // 名称重复
            throw new NZException(RCode.IDC_CABINETNAME_DUPLICATE);
        }

        cabinetService.saveOrUpdateCabinet(cabinet);
        return R.ok();
    }

    /**
     * 修改
     *
     * @param cabinet
     * @return
     */
    @PutMapping
    @SysLog("update cabinet")
    public R update(@RequestBody Cabinet cabinet) {
        ValidateUtils.is(cabinet.getId()).notNull(RCode.IDC_CABINETID_ISNULL)
                .and(cabinet.getName()).notNull(RCode.IDC_CABINETNAME_ISNULL)
                .and(cabinet.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(cabinet.getY()).min(1, RCode.CABINET_Y_MINIMUM)
                .and(cabinet.getX()).min(1, RCode.CABINET_X_MINIMUM);

        if (cabinet.getuSize() != null) {
            ValidateUtils.is(cabinet.getuSize()).min(1, RCode.IDC_CABINETUSIZE_MINIMUM)
                    .max(Constant.CURRENT_CABINET_MAXU, RCode.IDC_CABINET_MAXU);
        }

        // 校验名称重复
        if (!cabinetService.cabinetNameCheck(cabinet.getId(), cabinet.getName(), cabinet.getIdcId())) {
            // 名称重复
            throw new NZException(RCode.IDC_CABINETNAME_DUPLICATE);
        }

        cabinetService.saveOrUpdateCabinet(cabinet);
        return R.ok();
    }

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog("delete cabinet")
    public R delete(@RequestParam Integer ... ids) {
        ValidateUtils.is(ids).notNull(RCode.IDC_CABINETID_ISNULL);

        cabinetService.delete(ids);
        return R.ok();
    }

    /**
     * 机柜u位查询
     * @param id
     * @return
     */
    @GetMapping("/u")
    public R getUInfo(Integer id) {
        ValidateUtils.is(id).notNull(RCode.IDC_CABINETID_ISNULL);

        Map uInfo = cabinetService.getUInfo(id);
        return R.ok(uInfo);
    }
}