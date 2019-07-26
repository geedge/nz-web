package com.nis.modules.sys.controller;

import java.util.Arrays;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.modules.sys.entity.UserGroupEntity;
import com.nis.modules.sys.service.UserGroupService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;



/**
 * 用户组信息表
 *
 * @author song
 * @email 
 * @date 2019-07-26 11:22:14
 */
@RestController
@RequestMapping("sys/usergroup")
public class UserGroupController {
    @Autowired
    private UserGroupService userGroupService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("sys:usergroup:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = userGroupService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("sys:usergroup:info")
    public R info(@PathVariable("id") Long id){
		UserGroupEntity userGroup = userGroupService.getById(id);

        return R.ok().put("userGroup", userGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("sys:usergroup:save")
    public R save(@RequestBody UserGroupEntity userGroup){
		userGroupService.save(userGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("sys:usergroup:update")
    public R update(@RequestBody UserGroupEntity userGroup){
		userGroupService.updateById(userGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("sys:usergroup:delete")
    public R delete(@RequestBody Long[] ids){
		userGroupService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
