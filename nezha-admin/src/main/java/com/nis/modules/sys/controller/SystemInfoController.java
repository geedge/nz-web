package com.nis.modules.sys.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.node.service.NodeInfoService;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.service.SystemInfoService;
import com.nis.modules.sys.shiro.ShiroUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags="业务系统")
@RestController
@RequestMapping("/system")
public class SystemInfoController extends AbstractController {
	
	@Autowired
	private SystemInfoService systemInfoService;
	@Autowired
	private NodeInfoService nodeInfoService;
	
	@RequestMapping(value = "list", method = RequestMethod.GET)
	@ApiOperation(httpMethod = "GET", value = "业务系统列表查询")
	public R list(@RequestParam Map<String, Object> params){
		PageUtils queryPage = systemInfoService.queryPage(params);
		return R.ok(queryPage);
	}
	
	@RequestMapping(value = "detail", method = RequestMethod.GET)
	@ApiOperation(httpMethod = "GET", value = "业务系统详情查询")
	public R detail(Long id) {
		if (id == null) {
			return R.ok(ShiroUtils.getCurrSystem());
		}
		SystemInfoEntity queryDetail = systemInfoService.queryDetail(id);
		return R.ok(queryDetail);
	}
	
	@RequestMapping(value = "save", method = RequestMethod.POST)
	@ApiOperation(httpMethod = "POST", response = R.class, value = "保存业务系统")
	public R save(@RequestBody SystemInfoEntity systemInfo){
		//global限制
		if (!ShiroUtils.isGlobal()) {
			return R.error(RCode.NO_PERMISSION_ERROR);
		}
		
		ValidateUtils.is(systemInfo.getName()).notNull(RCode.SYS_SYS_NULLNAME);
		//校验名称重复
		if (systemInfoService.checkName(systemInfo)) {
			return R.error(RCode.SYS_SYS_DUPLICATENAME);
		}
		
		Date now = new Date();
		
		systemInfo.setCreateBy(getUserId());
		systemInfo.setUpdateBy(getUserId());
		systemInfo.setCreateDate(now);
		systemInfo.setUpdateDate(now);
		
		systemInfoService.save(systemInfo);
		
		return R.ok();
	}
	
	@RequestMapping(value = "update", method = RequestMethod.PUT)
	@ApiOperation(httpMethod = "PUT", response = R.class, value = "修改业务系统")
	public R update(@RequestBody SystemInfoEntity systemInfo){
		//global限制
		if (!ShiroUtils.isGlobal()) {
			return R.error(RCode.NO_PERMISSION_ERROR);
		}
		
		ValidateUtils.is(systemInfo.getName()).notNull(RCode.SYS_SYS_NULLNAME)
				.and(systemInfo.getId()).notNull(RCode.NOT_NULL_ERROR);
		//校验是否是内置系统，内置系统不让改
		SystemInfoEntity old = systemInfoService.queryDetail(systemInfo.getId());
		if (Constant.SYSTEM_BUILDIN_YES == old.getBuiltIn()) {
			return R.error(RCode.SYS_SYS_BUILTIN);
		}
		//校验名称重复
		if (systemInfoService.checkName(systemInfo)) {
			return R.error(RCode.SYS_SYS_DUPLICATENAME);
		}
		
		Date now = new Date();
		
		systemInfo.setUpdateBy(getUserId());
		systemInfo.setUpdateDate(now);
		
		systemInfoService.updateById(systemInfo);
		
		return R.ok();
	}
	
	@RequestMapping(value = "delete", method = RequestMethod.DELETE)
	@ApiOperation(httpMethod = "DELETE", response = R.class, value = "删除业务系统")
	public R delete(@ApiParam @RequestBody Long[] ids){
		ValidateUtils.is(ids).notNull(RCode.NOT_NULL_ERROR);
		List<Long> asList = Arrays.asList(ids);
		if (!ShiroUtils.isGlobal()) {//global限制
			return R.error(RCode.NO_PERMISSION_ERROR);
		}
		if (asList.contains(Constant.GLOBAL_ID)) {//不能删除global
			return R.error(RCode.SYS_SYS_DELGLOBAL);
		}
		systemInfoService.delete(asList);
		return R.ok();
	}
	
	/**
	 * 查询业务系统下的节点
	 */
	@RequestMapping(value = "nodes", method = RequestMethod.GET)
	@ApiOperation(httpMethod = "GET", response = R.class, value = "查询业务系统下的节点")
	public R nodes(@RequestParam Map<String, Object> params){
		ValidateUtils.is(params.get("id")).notNull(RCode.NOT_NULL_ERROR);
		params.put("id", Long.valueOf((String)params.get("id")));
		
		PageUtils queryPageBySystemId = nodeInfoService.queryPageBySystemId(params);
		return R.ok(queryPageBySystemId);
	}
	
	/**
	 * 分配节点
	 */
	@RequestMapping(value = "saveNodeRel", method = RequestMethod.POST)
	@ApiOperation(httpMethod = "POST", response = R.class, value = "分配节点")
	public R saveNodeRel(@ApiParam @RequestBody SystemInfoEntity entity){
		if (!ShiroUtils.isGlobal()) {
			entity.setId(ShiroUtils.getCurrSystemId());
		}
		ValidateUtils.is(entity.getId()).notNull(RCode.NOT_NULL_ERROR)
					 .and(entity.getUuids()).notNull(RCode.NOT_NULL_ERROR);
		
		systemInfoService.saveNodeRel(entity);
		return R.ok();
	}
	
	/**
	 * 移除节点
	 */
	@RequestMapping(value = "deleteNodeRel", method = RequestMethod.DELETE)
	@ApiOperation(httpMethod = "DELETE", response = R.class, value = "移除节点")
	public R deleteNodeRel(@ApiParam @RequestBody SystemInfoEntity entity){
		if (!ShiroUtils.isGlobal()) {
			entity.setId(ShiroUtils.getCurrSystemId());
		}
		ValidateUtils.is(entity.getId()).notNull(RCode.NOT_NULL_ERROR)
					 .and(entity.getUuids()).notNull(RCode.NOT_NULL_ERROR);
		
		systemInfoService.deleteNodeRel(entity);
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
		SystemInfoEntity queryParam = new SystemInfoEntity();
		Object object = params.get("id");
		if (object != null && !StringUtils.isEmpty(object.toString())) {
			queryParam.setId(Long.valueOf((String)object));
		}
		queryParam.setName((String)params.get("name"));
		
		if (systemInfoService.checkName(queryParam)) {
			return R.error(RCode.SYS_SYS_DUPLICATENAME);
		} else {
			return R.ok();
		}
	}
	
	/**
	 * 用户所属业务系统的列表
	 * @return
	 */
	@RequestMapping(value = "listByUser", method = RequestMethod.GET)
	@ApiOperation(httpMethod = "GET", response = R.class, value = "用户所属业务系统的列表")
	public R listByUser(){
		Long userId = getUserId();
		//检查是否有可用的业务系统
		List<SystemInfoEntity> systemInfosByUserId = systemInfoService.querySystemInfosByUserId(userId);
		if (systemInfosByUserId == null || systemInfosByUserId.size() == 0) {
			return R.error(RCode.SYS_LOGIN_SELECTSYS);
		} else {
			return R.ok(systemInfosByUserId);
		}
	}
	
	/**
	 * 切换业务系统
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "changeSystem", method = RequestMethod.GET)
	@ApiOperation(httpMethod = "GET", response = R.class, value = "切换业务系统")
	public R changeSystem(@ApiParam @RequestParam Long id){
		ValidateUtils.is(id).notNull();
		Long userId = getUserId();
		SystemInfoEntity newSystem = null;
		//检查是否有可用的业务系统
		List<SystemInfoEntity> systemInfosByUserId = systemInfoService.querySystemInfosByUserId(userId);
		if (CommonUtil.isNull(systemInfosByUserId)) {
			return R.error(RCode.SYS_LOGIN_SELECTSYS);
		} else {
			boolean flag = false;
			for (SystemInfoEntity si : systemInfosByUserId) {
				if (si.getId() == id) {
					newSystem = si;
					flag = true;
					break;
				}
			}
			if (flag) {
				ShiroUtils.getUserEntity().setCurrSystem(newSystem);
				return R.ok(newSystem);
			} else {
				return R.error(RCode.SYS_LOGIN_SELECTSYS);
			}
		}
	}
	
	
}