package com.nis.modules.asset.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.asset.entity.AssetFieldGroup;
import com.nis.modules.asset.service.AssetFieldGroupService;

@RestController
@RequestMapping("/asset/field/group")
public class AssetFieldGroupController {
	@Autowired
	private AssetFieldGroupService assetFieldGroupService;
	
	@GetMapping("/{id}")
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETFIELDGROUP)
	@RequiresPermissions({"asset:fieldgroup:info"})
	public R queryAssetFieldGroupById(@PathVariable Integer id) {
		ValidateUtils.is(id).notNull(RCode.ASSET_FIELD_GROUP_ID_ISNULL);
		
		AssetFieldGroup assetFieldGroup = assetFieldGroupService.getById(id);
		return R.ok(assetFieldGroup);
	}
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETFIELDGROUP)
	@RequiresPermissions({"asset:fieldgroup:list"})
	public R queryAssetFieldGroupList(@RequestParam Map<String,Object> params) {
		
		PageUtils page = assetFieldGroupService.queryList(params);
		return R.ok(page);
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETFIELDGROUP)
	@RequiresPermissions({"asset:fieldgroup:update"})
	public R modifyAssetFieldGroup(@RequestBody AssetFieldGroup assetFieldGroup) {
		
		ValidateUtils.is(assetFieldGroup.getId()).notNull(RCode.ASSET_FIELD_GROUP_ID_ISNULL)
		.and(assetFieldGroup.getName()).notNull(RCode.ASSET_FIELD_GROUP_NAME_ISNULL);
		
		Integer id = assetFieldGroupService.saveOrModifyAssetFieldGroup(assetFieldGroup);
		return R.ok().putData("id", id);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETFIELDGROUP)
	@RequiresPermissions({"asset:fieldgroup:add"})
	public R saveAssetFieldGroup(@RequestBody AssetFieldGroup assetFieldGroup) {
		
		ValidateUtils.is(assetFieldGroup.getName()).notNull(RCode.ASSET_FIELD_GROUP_NAME_ISNULL);
		
		Integer id = assetFieldGroupService.saveOrModifyAssetFieldGroup(assetFieldGroup);
		
		return R.ok().putData("id", id);
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETFIELDGROUP)
	@RequiresPermissions({"asset:fieldgroup:delete"})
	public R removeAssetFieldGroup(@RequestParam String ids) {
		ValidateUtils.is(ids).notNull(RCode.ASSET_FIELD_GROUP_ID_ISNULL);
		
		assetFieldGroupService.removeAssetFieldGroups(Arrays.asList(ids.split(",")));
		
		return R.ok();
	}
	
	
}
