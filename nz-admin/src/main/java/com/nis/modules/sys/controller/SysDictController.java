/**

 *

 *

 */

package com.nis.modules.sys.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    @RequiresPermissions("sys:dict:list")
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
    @RequiresPermissions({"sys:dict:all"})
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
    @RequiresPermissions("sys:dict:save")
    public R save(@RequestBody SysDictEntity dict){

        ValidateUtils.is(dict.getType()).notNull(RCode.SYS_DICT_TYPE_ISNULL);
        ValidateUtils.is(dict.getValue()).notNull(RCode.SYS_DICT_VALUE_ISNULL);

        sysDictService.saveSysDict(dict);
        Map<String, SysDictEntity> resultEntity = new HashMap<>(2);
        resultEntity.put("dictEntity", dict);
        return R.ok(resultEntity);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("sys:dict:update")
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
        dict.setOpTime(DateUtil.getUTCTimeByConfigTimeZone());
        
        sysDictService.update(dict);

        return R.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    @RequiresPermissions("sys:dict:delete")
    public R delete(@RequestParam String ids){
    	ValidateUtils.is(ids).notNull(RCode.SYS_DICT_ID_ISNULL);
        sysDictService.delete(ids);

        return R.ok();
    }
}
