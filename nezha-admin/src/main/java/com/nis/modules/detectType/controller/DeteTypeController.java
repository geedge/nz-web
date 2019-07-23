package com.nis.modules.detectType.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.detectType.entity.DeteTypeInfoEntity;
import com.nis.modules.detectType.service.DeteTypeInfoService;
import com.nis.modules.detectType.service.DeteTypeMetaService;
import com.nis.modules.sys.shiro.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@Controller
@RequestMapping("/deteType")
@Api(tags = "监测类别")
public class DeteTypeController {
    @Autowired
    private DeteTypeInfoService deteTypeInfoService;
    @Autowired
    private DeteTypeMetaService deteTypeMetaService;

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
    public R detail(@RequestParam Integer id) {
        DeteTypeInfoEntity deteTypeInfo = deteTypeInfoService.queryDetail(id);

        return R.ok(deteTypeInfo);
    }

    @PostMapping(value = "/save", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "保存监测类别")
    public R save(@RequestBody DeteTypeInfoEntity deteTypeInfo) {

        deteTypeInfoService.saveEntity(deteTypeInfo);
        return R.ok();
    }
}
