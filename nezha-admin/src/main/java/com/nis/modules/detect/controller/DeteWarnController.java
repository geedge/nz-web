package com.nis.modules.detect.controller;

import com.alibaba.fastjson.JSON;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.detect.entity.DeteWarnInfoEntity;
import com.nis.modules.detect.service.DeteSetInfoService;
import com.nis.modules.detect.service.DeteWarnInfoService;
import com.nis.modules.sys.service.PermissionInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@Api(tags = "监测告警")
@RequestMapping("/deteWarn")
public class DeteWarnController {
    @Autowired
    private DeteWarnInfoService deteWarnInfoService;
    @Autowired
    private DeteSetInfoService deteSetInfoService;
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
    public R save(@RequestParam(value="deteWarnInfo",required = true) String jsonWarnInfo,@RequestParam(value="file",required = false) MultipartFile file) {
        DeteWarnInfoEntity deteWarnInfoEntity= JSON.parseObject(jsonWarnInfo,DeteWarnInfoEntity.class);

        ValidateUtils.is(deteWarnInfoEntity.getName()).notNull(RCode.WARN_NAME_ISNULL);
        ValidateUtils.is(deteWarnInfoEntity.getDeteTypeId()).notNull(RCode.WARN_TYPE_ISNULL);
        if(deteWarnInfoService.checkNameExists(deteWarnInfoEntity.getName(),null)){
            throw new NZException(RCode.WARN_NAME_EXISTS);
        }
        if(!deteWarnInfoService.checkTriggerTypeIsValide(deteWarnInfoEntity)){
            throw new NZException(RCode.WARN_TRIGGERTYPE_INVALID);
        }
        if(!deteWarnInfoService.checkWarnModeIsValide(deteWarnInfoEntity)){
            throw new NZException(RCode.WARN_WARNMODE_INVALID);
        }
        //告警模式为汇总时校验统计方式
        if("2".equals(deteWarnInfoEntity.getWarnMode())&& !deteWarnInfoService.checkStatTypeIsValide(deteWarnInfoEntity)){
            throw new NZException(RCode.WARN_STATTYPE_INVALID);
        }
        //触发方式为阈值触发时校验比较符是否合法
        if("1".equals(deteWarnInfoEntity.getTriggerType()) && !deteWarnInfoService.checkSymbolIsValide(deteWarnInfoEntity)){
            throw new NZException(RCode.WARN_SYMBOL_INVALID);
        }
        if("1".equals(deteWarnInfoEntity.getTriggerType()) && !deteWarnInfoService.checkThresholdIsValide(deteWarnInfoEntity)){
            throw new NZException(RCode.WARN_SYMBOL_INVALID);
        }
        if(!deteWarnInfoService.checkWarnLevelIsValide(deteWarnInfoEntity)){
            throw new NZException(RCode.WARN_WARNLEVEL_INVALID);
        }
        if(!deteWarnInfoService.checkCallbackTypeIsValide(deteWarnInfoEntity)){
            throw new NZException(RCode.WARN_CALLBACKTYPE_INVALID);
        }
        if(!deteWarnInfoService.checkCallbackContentIsValide(deteWarnInfoEntity)){
            throw new NZException(RCode.WARN_CALLBACKCONTENT_ISNULL);
        }
        if(!deteWarnInfoService.checkCallbackScirptFileIsValide(deteWarnInfoEntity,file)){
            throw new NZException(RCode.DETE_UPLOAD_FILE_ISNULL);
        }
//        if(!deteWarnInfoService.checkViewLevelValide(deteWarnInfoEntity.getViewLevel())){
//            throw new NZException(RCode.VIEWLEVEL_INVALID);
//        }

        if(!deteWarnInfoService.checkTypeExists(deteWarnInfoEntity.getDeteTypeId())){
            throw new NZException(RCode.WARN_TYPE_NOTEXISTS);
        }

        if(!deteSetInfoService.checkDeteSetExsits(deteWarnInfoEntity.getDeteSetIds())){
            throw new NZException(RCode.WARN_SET_NOTEXISTS);
        }


        deteWarnInfoService.saveEntity(deteWarnInfoEntity,file);
        return R.ok();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    @ResponseBody
    @ApiOperation("修改告警配置")
    public R update(@RequestParam(value="deteWarnInfo",required = true) String jsonWarnInfo,@RequestParam(value="file",required = false) MultipartFile file) {
        DeteWarnInfoEntity deteWarnInfoEntity=JSON.parseObject(jsonWarnInfo,DeteWarnInfoEntity.class);
        ValidateUtils.is(deteWarnInfoEntity.getName()).notNull(RCode.WARN_NAME_ISNULL);
        if(deteWarnInfoService.checkNameExists(deteWarnInfoEntity.getName(),deteWarnInfoEntity.getId())){
            throw new NZException(RCode.WARN_NAME_EXISTS);
        }
        deteWarnInfoService.updateEntity(deteWarnInfoEntity,file);
        return R.ok();
    }

    @DeleteMapping("/delete")
    @ResponseBody
    @ApiOperation("删除告警配置")
    public R delete(@RequestBody Long ... ids){

        deteWarnInfoService.deleteEntity(ids);
        return R.ok();
    }
    @GetMapping("nameCheck")
    @ResponseBody
    @ApiOperation(value="名称校验")
    public R nameCheck(@RequestParam String name,Long id){
        if(deteWarnInfoService.checkNameExists(name,id)){
            return R.error(RCode.WARN_NAME_EXISTS);
        }else{
            return R.ok("true");
        }
    }
}
