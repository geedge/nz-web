package com.nis.modules.detect.controller;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.detect.entity.DeteSetInfoEntity;
import com.nis.modules.detect.service.DeteSetInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;



/**
 * 监测设置信息表
 *
 * @author nezha
 * @email 
 * @date 2019-07-25 16:35:48
 */
@Controller
@RequestMapping("/deteSet")
@Api(tags="监测设置")
public class DeteSetController {
    @Autowired
    private DeteSetInfoService deteSetInfoService;

    /**
     * 列表
     */
    @GetMapping(value = "/list")
    @ResponseBody
    @ApiOperation(value = "查询列表", notes = "可使用name字段模糊查询列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = deteSetInfoService.queryPage(params);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @GetMapping(value = "/detail")
    @ResponseBody
    @ApiOperation(value = "查询详细信息")
    public R info(@RequestParam("id") Long id){
		DeteSetInfoEntity deteSetInfo = deteSetInfoService.queryDetail(id);

        return R.ok(deteSetInfo);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    @ApiOperation("新增监测设置")
    @ResponseBody
    public R save(@RequestBody DeteSetInfoEntity deteSetInfo){
        ValidateUtils.is(deteSetInfo.getName()).notNull(RCode.SET_NAME_ISNULL);
        ValidateUtils.is(deteSetInfo.getTypeId()).notNull(RCode.SET_TYPE_ISNULL);
        if(deteSetInfoService.checkNameExists(deteSetInfo.getName(),null)){
            throw new NZException(RCode.SET_NAME_EXISTS);
        }

        deteSetInfoService.saveEntity(deteSetInfo);

        return R.ok();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    @ResponseBody
    @ApiOperation("修改监测设置")
    public R update(@RequestBody DeteSetInfoEntity deteSetInfo){
        ValidateUtils.is(deteSetInfo.getName()).notNull(RCode.SET_NAME_ISNULL);
        ValidateUtils.is(deteSetInfo.getTypeId()).notNull(RCode.SET_TYPE_ISNULL);
        if(deteSetInfoService.checkNameExists(deteSetInfo.getName(),deteSetInfo.getId())){
            throw new NZException(RCode.SET_NAME_EXISTS);
        }

        deteSetInfoService.updateEntity(deteSetInfo);
        return R.ok();
    }

    /**
     * 修改设置状态
     */
    @PutMapping("/changeState")
    @ResponseBody
    @ApiOperation(value="修改设置状态")
    public R changeState(@RequestParam String state,@RequestParam Long... ids){

		deteSetInfoService.changeStateByIds(ids,state);
        return R.ok();
    }



}
