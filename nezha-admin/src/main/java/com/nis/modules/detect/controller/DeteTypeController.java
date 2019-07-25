package com.nis.modules.detect.controller;

import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import com.nis.modules.detect.service.DeteTypeInfoService;
import com.nis.modules.detect.service.DeteTypeMetaService;
import com.nis.modules.detect.service.DeteTypeParamService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/deteType")
@Api(tags = "监测类别")
public class DeteTypeController {
    @Autowired
    private DeteTypeInfoService deteTypeInfoService;
    @Autowired
    private DeteTypeMetaService deteTypeMetaService;
    @Autowired
    private DeteTypeParamService deteTypeParamService;

    private Logger log = LoggerFactory.getLogger(DeteTypeController.class);

    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "查询列表", notes = "可使用name字段模糊查询列表")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = deteTypeInfoService.queryPage(params);
        return R.ok(page);
    }

    @GetMapping(value = "/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "查询详细信息")
    public R detail(@RequestParam Long id) {
        DeteTypeInfoEntity deteTypeInfo = deteTypeInfoService.queryDetail(id);

        return R.ok(deteTypeInfo);
    }

    @PostMapping(value = "/save", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "保存监测类别")
    public R save(@RequestBody DeteTypeInfoEntity deteTypeInfo) {
        deteTypeInfoService.validatePassDate(deteTypeInfo);
        if(deteTypeInfo.getDeteInterval()==null||deteTypeInfo.getDeteInterval()<0){
            deteTypeInfo.setDeteInterval(60);
        }
        deteTypeInfoService.saveEntity(deteTypeInfo);

        return R.ok();
    }


    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "更新监测类别")
    public R update(@RequestBody DeteTypeInfoEntity deteTypeInfo) {
        deteTypeInfoService.validatePassDate(deteTypeInfo);
        deteTypeInfoService.updateEntity(deteTypeInfo);
        return R.ok();
    }

    @DeleteMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "删除监测类别")
    public R delete(@RequestParam Long id) {
        DeteTypeInfoEntity deteTypeInfoEntity = new DeteTypeInfoEntity();
        deteTypeInfoEntity.setId(id);
        deteTypeInfoEntity.setDelFlag("1");

        deteTypeInfoService.deleteEntity(deteTypeInfoEntity);
        return R.ok();
    }

    @GetMapping(value = "/nameCheck", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "监测类别名称校验")
    public R nameCheck( Long id,@RequestParam String name) {
        boolean nameExists = deteTypeInfoService.checkFieldExists(id, "name", name);
        if(nameExists){
            return R.ok("true");
        }else{
            return R.ok("false");
        }
    }
}
