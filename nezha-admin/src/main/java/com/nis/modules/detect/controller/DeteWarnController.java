package com.nis.modules.detect.controller;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.detect.entity.DeteWarnInfoEntity;
import com.nis.modules.detect.service.DeteWarnInfoService;
import com.nis.modules.sys.service.PermissionInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@Api(tags = "监测告警")
@RequestMapping("/deteWarn")
public class DeteWarnController {
    @Autowired
    private DeteWarnInfoService deteWarnInfoService;
    @Autowired
    private PermissionInfoService permissionInfoService;

    /**
     * 列表
     */
    @GetMapping(value = "/list")
    @ResponseBody
    @ApiOperation(value = "查询列表", notes = "可使用name字段模糊查询列表")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page=deteWarnInfoService.queryPage(params);
        return R.ok(page);
    }


    /**
     * 信息
     */
    @GetMapping(value = "/detail")
    @ResponseBody
    @ApiOperation(value = "查询详细信息")
    public R info(@RequestParam("id") Long id) {
        DeteWarnInfoEntity deteWarnInfoEntity=deteWarnInfoService.queryDetail(id);
        return R.ok(deteWarnInfoEntity);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    @ApiOperation("新增告警配置")
    @ResponseBody
    public R save(@RequestBody DeteWarnInfoEntity deteWarnInfoEntity) {
        ValidateUtils.is(deteWarnInfoEntity.getName()).notNull(RCode.WARN_NAME_ISNULL);
        ValidateUtils.is(deteWarnInfoEntity.getDeteTypeId()).notNull(RCode.WARN_TYPE_ISNULL);
        if(deteWarnInfoService.checkNameExists(deteWarnInfoEntity.getName(),null)){
            throw new NZException(RCode.WARN_NAME_EXISTS);
        }
        deteWarnInfoService.saveEntity(deteWarnInfoEntity);
        return R.ok();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    @ResponseBody
    @ApiOperation("修改告警配置")
    public R update(@RequestBody DeteWarnInfoEntity deteWarnInfoEntity) {
        ValidateUtils.is(deteWarnInfoEntity.getName()).notNull(RCode.WARN_NAME_ISNULL);
        if(deteWarnInfoService.checkNameExists(deteWarnInfoEntity.getName(),deteWarnInfoEntity.getId())){
            throw new NZException(RCode.WARN_NAME_EXISTS);
        }
        deteWarnInfoService.updateEntity(deteWarnInfoEntity);
        return R.ok();
    }

    @DeleteMapping("/delete")
    @ResponseBody
    @ApiOperation("删除告警配置")
    public R delete(@RequestParam Long ... ids){

        deteWarnInfoService.deleteEntity(ids);
        return R.ok();
    }
    @GetMapping("nameCheck")
    @ResponseBody
    @ApiOperation(value="名称校验",notes="true 表示名称存在，不可用；false表示名称不存在，可用")
    public R nameCheck(@RequestParam String name,Long id){
        if(deteWarnInfoService.checkNameExists(name,id)){
            return R.ok("true");
        }else{
            return R.ok("false");
        }
    }
}
