package com.nis.modules.sys.controller;

import java.util.Arrays;
import java.util.List;
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
import com.nis.modules.sys.shiro.ShiroUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;



/**
 * 用户组
 *
 * @author song
 * @email 
 * @date 2019-07-26 11:22:14
 */
@Api(tags="用户组")
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
    public R save(@RequestBody UserGroupEntity userGroup){
    	ValidateUtils.is(userGroup.getName()).notNull();
    	if (userGroupService.checkName(userGroup)) {
    		return R.error(RCode.SYS_USERGROUP_DUPLICATENAME);
    	}
		userGroupService.save(userGroup);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @ApiOperation(httpMethod = "PUT", value = "修改用户组")
    public R update(@ApiParam @RequestBody UserGroupEntity userGroup){
    	ValidateUtils.is(userGroup.getName()).notNull()
    				 .and(userGroup.getId()).notNull();
    	if (userGroupService.checkName(userGroup)) {
    		return R.error(RCode.SYS_USERGROUP_DUPLICATENAME);
    	}
		userGroupService.update(userGroup);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @ApiOperation(httpMethod = "DELETE", value = "删除用户组")
    public R delete(@RequestBody Long[] ids){
    	ValidateUtils.is(ids).notNull();
    	List<Long> asList = Arrays.asList(ids);
		if (!ShiroUtils.isGlobal()) {//global限制
			return R.error(RCode.NO_PERMISSION_ERROR);
		}
		userGroupService.delete(asList);
        return R.ok();
    }

    /**
	 * 业务系统名称校验
	 * @param params
	 * @return
	 */
	@RequestMapping(value = "nameCheck", method = RequestMethod.GET)
	@ApiOperation(httpMethod = "GET", response = R.class, value = "业务系统名称校验")
	public R nameCheck(@RequestParam Map<String, Object> params){
		ValidateUtils.is(params.get("name")).notNull();
		//校验名称重复
		UserGroupEntity queryParam = new UserGroupEntity();
		Object object = params.get("id");
		if (object != null && !StringUtils.isEmpty(object.toString())) {
			queryParam.setId(Long.valueOf((String)object));
		}
		queryParam.setName((String)params.get("name"));
		
		if (userGroupService.checkName(queryParam)) {
			return R.error(RCode.SYS_USERGROUP_DUPLICATENAME);
		} else {
			return R.ok();
		}
	}
}
