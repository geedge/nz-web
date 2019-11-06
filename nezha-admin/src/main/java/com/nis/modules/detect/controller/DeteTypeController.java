package com.nis.modules.detect.controller;

import com.alibaba.fastjson.JSON;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import com.nis.modules.detect.entity.DeteTypeMetaMappingEntity;
import com.nis.modules.detect.entity.DeteTypeMetaMapppingVO;
import com.nis.modules.detect.service.DeteTypeInfoService;
import com.nis.modules.detect.service.DeteMetaMappingService;
import com.nis.modules.detect.service.DeteMetaService;
import com.nis.modules.detect.service.DeteParamService;
import com.nis.modules.sys.shiro.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
    private DeteMetaService deteTypeMetaService;
    @Autowired
    private DeteParamService deteTypeParamService;
    @Autowired
    private DeteMetaMappingService deteTypeMetaMappingService;

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

//    @PostMapping(value = "/save", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/save")
    @ResponseBody
    @ApiOperation(value = "保存监测类别")
    public R save(@RequestParam(value="deteTypeInfo",required = true) String jsonTypeInfo,@RequestParam(value="file",required = false) MultipartFile file) {
        DeteTypeInfoEntity deteTypeInfo= JSON.parseObject(jsonTypeInfo,DeteTypeInfoEntity.class);
        deteTypeInfoService.validatePassDate(deteTypeInfo,file);
        if(deteTypeInfo.getDeteInterval()==null||deteTypeInfo.getDeteInterval()<0){
            deteTypeInfo.setDeteInterval(60);
        }
        deteTypeInfoService.saveEntity(deteTypeInfo,file);

        return R.ok();
    }


    @PutMapping(value = "/update")
//    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "更新监测类别")
    public R update(@RequestParam(value="deteTypeInfo",required = true) String jsonTypeInfo,@RequestParam(value="file",required = false) MultipartFile file) {
        DeteTypeInfoEntity deteTypeInfo= JSON.parseObject(jsonTypeInfo,DeteTypeInfoEntity.class);
        if(deteTypeInfoService.isBuildIn(deteTypeInfo)){ //内置配置，不允许修改
            throw new NZException(RCode.BUILDIN_CONFIG);
        }
        deteTypeInfoService.validatePassDate(deteTypeInfo,file);
        deteTypeInfoService.updateEntity(deteTypeInfo,file);
        return R.ok();
    }

    @DeleteMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "删除监测类别")
    public R delete(@RequestBody Long... ids) {

        deteTypeInfoService.deleteEntity(ids);
        return R.ok();
    }

    @GetMapping(value = "/nameCheck", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiOperation(value = "监测类别名称校验")
    public R nameCheck( Long id,@RequestParam String name) {
        boolean nameExists = deteTypeInfoService.checkNameExists(name,id);
        if(nameExists){ //名称存在
            return R.error(RCode.TYPE_NAME_EXISTS);
        }else{
            return R.ok("true");
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
        if(deteTypeMetaMappingService.checkMetaMappingTypeExists(deteTypeMetaMapppingVO.getType())){
            throw new NZException(RCode.METAMAPPING_TYPE_EXISTS);
        }
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
    public R deleteDic(@RequestBody String ... types) {
        return deteTypeMetaMappingService.deleteDic(types);
    }

    @GetMapping(value="/dicNameCheck")
    @ResponseBody
    @ApiOperation(value="元数据字典type校验")
    public R dicNameCheck(String type){
        if(deteTypeMetaMappingService.checkMetaMappingTypeExists(type)){
            return R.error(RCode.METAMAPPING_TYPE_EXISTS);
        }else{
            return R.ok("true");
        }
    }

}
