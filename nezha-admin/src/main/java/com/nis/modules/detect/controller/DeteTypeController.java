package com.nis.modules.detect.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import com.nis.modules.detect.entity.DeteTypeMetaMappingEntity;
import com.nis.modules.detect.entity.DeteTypeMetaMapppingVO;
import com.nis.modules.detect.service.DeteTypeInfoService;
import com.nis.modules.detect.service.DeteTypeMetaMappingService;
import com.nis.modules.detect.service.DeteTypeMetaService;
import com.nis.modules.detect.service.DeteTypeParamService;
import com.nis.modules.sys.shiro.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
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
    @Autowired
    private DeteTypeMetaMappingService deteTypeMetaMappingService;

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

    @GetMapping(value = "/dicList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "监测类别元数据字典查询")
    public R dicList(@RequestParam Map<String, Object> params ) {
        PageUtils page = deteTypeMetaMappingService.queryMetaMappingPage(params);
        return R.ok(page);
    }

    @PostMapping(value = "/saveDic", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "保存监测类别字典数据")
    public R saveDic(@RequestBody DeteTypeMetaMapppingVO deteTypeMetaMapppingVO) {
        ValidateUtils.is(deteTypeMetaMapppingVO.getType()).notNull(RCode.DICT_TYPE_ISNULL);
        ValidateUtils.is(deteTypeMetaMapppingVO.getMappings()).notNull(RCode.DICT_MAPPINGS_ISNULL);

        Long userId = ShiroUtils.getUserId();
        Date date=new Date();


        List<DeteTypeMetaMappingEntity> mappings = deteTypeMetaMapppingVO.getMappings();

        for(DeteTypeMetaMappingEntity mapping:mappings){
            mapping.setType(deteTypeMetaMapppingVO.getType());
            mapping.setOperator(userId);
            mapping.setOpTime(date);
        }

        deteTypeMetaMappingService.saveDicts(mappings);
        return R.ok();
    }

    @PutMapping(value = "/updateDic", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "更新监测类别字典数据")
    public R updateDic(@RequestBody DeteTypeMetaMapppingVO deteTypeMetaMapppingVO) {
        ValidateUtils.is(deteTypeMetaMapppingVO.getType()).notNull(RCode.DICT_TYPE_ISNULL);
        ValidateUtils.is(deteTypeMetaMapppingVO.getMappings()).notNull(RCode.DICT_MAPPINGS_ISNULL);
        int count=0;
        for(DeteTypeMetaMappingEntity mapping:deteTypeMetaMapppingVO.getMappings()){
            if(mapping.getDelFlag()!=null&& !Constant.DELFLAG_FALSE.equals(mapping.getDelFlag())){
                count++;
            }
        }
        if(count==deteTypeMetaMapppingVO.getMappings().size()){
            return deteTypeMetaMappingService.deleteDic(deteTypeMetaMapppingVO.getType());
        }
        deteTypeMetaMappingService.updateDict(deteTypeMetaMapppingVO);
        return R.ok();
    }

    @DeleteMapping(value = "/deleteDic", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "删除监测类别字典")
    public R deleteDic(@RequestParam String type) {
        return deteTypeMetaMappingService.deleteDic(type);
    }
}
