/**

 *

 *

 */

package com.nis.modules.sys.controller;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.validator.ValidatorUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 数据字典
 *

 */
@Api(tags="系统字典")
@RestController
@RequestMapping("sys/dict")
public class SysDictController extends AbstractController {
    @Autowired
    private SysDictService sysDictService;

    /**
     * 列表
     */
    @GetMapping("/list")
//    @RequiresPermissions("sys:dict:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = sysDictService.queryPage(params);

        R re = R.ok(page);
        return re;
    }

    /**
     * 不分页
     * @return
     */
    @GetMapping("/all")
    @ApiOperation(httpMethod = "GET", value = "业务系统列表查询")
    @ApiImplicitParams({
        @ApiImplicitParam(paramType="query", name = "name", value = "名称", required = false),
        @ApiImplicitParam(paramType="query", name = "type", value = "类别", required = false)
    })
    public R all(@ApiParam(hidden=true) @RequestParam Map<String, Object> params) {
    	String type = (String) params.get("type");
    	ValidateUtils.is(type).notNull(RCode.NOT_NULL_ERROR);
    	List<SysDictEntity> queryList = sysDictService.queryList(params);
    	
    	return R.ok(queryList);
    }

    /**
     * 信息
     */
//    @RequestMapping("/info/{id}")
//    @RequiresPermissions("sys:dict:info")
    public R info(@PathVariable("id") Long id){
        SysDictEntity dict = sysDictService.getById(id);

        return R.ok(dict);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
//    @RequiresPermissions("sys:dict:save")
    public R save(@RequestBody SysDictEntity dict){

        ValidateUtils.is(dict.getType()).notNull(RCode.SYS_DICT_TYPE_ISNULL);
        ValidateUtils.is(dict.getValue()).notNull(RCode.SYS_DICT_VALUE_ISNULL);
        //字典名称，非必填项，为空后台自动添加value值
        if(StringUtils.isBlank(dict.getName())){
            dict.setName(dict.getValue());
        }
        //字典码，当字典类型为：vendor,assetType时，可以不填，后台根据已有最大code值 +1 填写
        if( StringUtils.isBlank(dict.getCode()) && Constant.SPECIAL_SYS_DICT.contains(dict.getType())){
            Integer maxCode=sysDictService.queryMaxTypeCode(dict.getType());
            if(maxCode != null){
                maxCode+=1;
            }else{
                maxCode=1;
            }

            dict.setCode(maxCode+"");
        }else{
            throw new NZException(RCode.SYS_DICT_CODE_ISNULL);
        }
        dict.setOperator(getUserId());
        dict.setOpTime(new Date());
        sysDictService.save(dict);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
//    @RequiresPermissions("sys:dict:update")
    public R update(@RequestBody SysDictEntity dict){
        //校验类型
        ValidateUtils.is(dict.getType()).notNull(RCode.SYS_DICT_TYPE_ISNULL);
        ValidateUtils.is(dict.getValue()).notNull(RCode.SYS_DICT_VALUE_ISNULL);
        ValidateUtils.is(dict.getCode()).notNull(RCode.SYS_DICT_CODE_ISNULL);
        //字典名称，非必填项，为空后台自动添加value值
        if(StringUtils.isBlank(dict.getName())){
            dict.setName(dict.getValue());
        }
        dict.setOperator(getUserId());
        dict.setOpTime(new Date());
        
        sysDictService.update(dict);

        return R.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
//    @RequiresPermissions("sys:dict:delete")
    public R delete(@RequestParam String ids){
    	ValidateUtils.is(ids).notNull(RCode.SYS_DICT_ID_ISNULL);
        sysDictService.delete(ids);

        return R.ok();
    }


    /**
     *  name type 相同下 value 值不能重复
     * @param id     修改使用
     * @param name   字典名称
     * @param type   字典类型
     * @param value  字典值
     * @return
     */
//    @GetMapping("/dictValueCheck")
    public R dictValueCheck(Long id, String name, String type, String value){
        ValidateUtils.is(name).notNull(RCode.SYS_DICT_NAME_ISNULL)
                .and(type).notNull(RCode.SYS_DICT_TYPE_ISNULL)
                .and(value).notNull(RCode.SYS_DICT_VALUE_ISNULL);
        if (sysDictService.dictValueCheck(id,name,type,value)) {
            return R.ok();
        }
        // 字典值重复
        throw new NZException(RCode.SYS_DICT_VALUE_DUPLICATE);
    }

}
