package com.nis.modules.detectSet.controller;

import java.util.Arrays;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.modules.detectSet.entity.DeteSetInfoEntity;
import com.nis.modules.detectSet.service.DeteSetInfoService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;



/**
 * 监测设置信息表
 *
 * @author nezha
 * @email 
 * @date 2019-07-25 16:35:48
 */
@RestController
@RequestMapping("deteSet")
public class DeteSetController {
    @Autowired
    private DeteSetInfoService deteSetInfoService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("generator:detesetinfo:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = deteSetInfoService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("generator:detesetinfo:info")
    public R info(@PathVariable("id") Long id){
		DeteSetInfoEntity deteSetInfo = deteSetInfoService.getById(id);

        return R.ok().put("deteSetInfo", deteSetInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("generator:detesetinfo:save")
    public R save(@RequestBody DeteSetInfoEntity deteSetInfo){
		deteSetInfoService.save(deteSetInfo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("generator:detesetinfo:update")
    public R update(@RequestBody DeteSetInfoEntity deteSetInfo){
		deteSetInfoService.updateById(deteSetInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("generator:detesetinfo:delete")
    public R delete(@RequestBody Long[] ids){
		deteSetInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
