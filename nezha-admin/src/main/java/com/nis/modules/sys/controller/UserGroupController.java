package com.nis.modules.sys.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.modules.sys.entity.UserGroupEntity;
import com.nis.modules.sys.entity.UserGroupEntity2;
import com.nis.modules.sys.service.UserGroupService;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

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
    @RequestMapping(value = "list", method = RequestMethod.GET)
    @ApiOperation(httpMethod = "GET", value = "用户组列表查询")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = userGroupService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping(value = "detail", method = RequestMethod.GET)
    @ApiOperation(httpMethod = "GET", value = "用户组详情查询")
    public R detail(@ApiParam @RequestParam Long id){
		UserGroupEntity2 userGroup = userGroupService.queryDetail(id);

        return R.ok(userGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @ApiOperation(httpMethod = "POST", value = "新增用户组")
    public R save(@ApiParam @RequestBody UserGroupEntity userGroup){
		userGroupService.save(userGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @ApiOperation(httpMethod = "PUT", value = "修改用户组")
    public R update(@ApiParam @RequestBody UserGroupEntity userGroup){
		userGroupService.updateById(userGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @ApiOperation(httpMethod = "DELETE", value = "删除用户组")
    public R delete(@RequestBody Long[] ids){
		userGroupService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
