package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.AssetFieldGroup;
import com.nis.modules.asset.service.AssetFieldGroupService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/asset/field/group")
public class AssetFieldGroupController {
	@Autowired
	private AssetFieldGroupService assetFieldGroupService;
	
	@GetMapping("/{id}")
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSET_FIELD_GROUP)
	public R queryAssetFieldGroupById(@PathVariable Integer id) {
		ValidateUtils.is(id).notNull(RCode.ASSET_FIELD_GROUP_ID_ISNULL);
		
		AssetFieldGroup assetFieldGroup = assetFieldGroupService.getById(id);
		return R.ok(assetFieldGroup);
	}
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSET_FIELD_GROUP)
	public R queryAssetFieldGroupList(@RequestParam Map<String,Object> params) {
		
		PageUtils page = assetFieldGroupService.queryList(params);
		return R.ok(page);
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ASSET_FIELD_GROUP)
	@RequiresPermissions({"field:group:update"})
	public R modifyAssetFieldGroup(@RequestBody AssetFieldGroup assetFieldGroup) {
		
		ValidateUtils.is(assetFieldGroup.getId()).notNull(RCode.ASSET_FIELD_GROUP_ID_ISNULL)
		.and(assetFieldGroup.getName()).notNull(RCode.ASSET_FIELD_GROUP_NAME_ISNULL).maxLength(64)
		.and(assetFieldGroup.getRemark()).maxLength(256);
		
		Integer id = assetFieldGroupService.saveOrModifyAssetFieldGroup(assetFieldGroup);
		return R.ok().putData("id", id);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.ASSET_FIELD_GROUP)
	@RequiresPermissions({"field:group:save"})
	public R saveAssetFieldGroup(@RequestBody AssetFieldGroup assetFieldGroup) {
		
		ValidateUtils.is(assetFieldGroup.getName()).notNull(RCode.ASSET_FIELD_GROUP_NAME_ISNULL).maxLength(64)
		.and(assetFieldGroup.getRemark()).maxLength(256);
		
		Integer id = assetFieldGroupService.saveOrModifyAssetFieldGroup(assetFieldGroup);
		
		return R.ok().putData("id", id);
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ASSET_FIELD_GROUP)
	@RequiresPermissions({"field:group:delete"})
	public R removeAssetFieldGroup(@RequestParam String ids) {
		ValidateUtils.is(ids).notNull(RCode.ASSET_FIELD_GROUP_ID_ISNULL);
		
		assetFieldGroupService.removeAssetFieldGroups(Arrays.asList(ids.split(",")));
		
		return R.ok();
	}
	
	
}
