package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.asset.service.AssetFieldMetaService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/asset/field/meta")
public class AssetFieldMetaController {
	
	@Autowired
	private AssetFieldMetaService assetFieldMetaService;
	
	@GetMapping("/{id}")
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSET_FIELD_META)
	public R queryAssetFieldMetaById(@PathVariable Integer id) {
		ValidateUtils.is(id).notNull(RCode.ASSET_FIELD_META_ID_ISNULL);
		
		AssetFieldMeta assetFieldMeta=assetFieldMetaService.queryAssetFieldMeta(id);
		return R.ok(assetFieldMeta);
	}
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSET_FIELD_META)
	public R queryAssetFieldMetaList(@RequestParam Map<String,Object> params) {
		
		PageUtils page = assetFieldMetaService.queryList(params);
		return R.ok(page);
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ASSET_FIELD_META)
	@RequiresPermissions({"field:meta:update"})
	public R modifyAssetFieldMeta(@RequestBody AssetFieldMeta assetFieldMeta) {
		ValidateUtils.is(assetFieldMeta.getId()).notNull(RCode.ASSET_FIELD_META_ID_ISNULL)
		.and(assetFieldMeta.getName()).notNull(RCode.ASSET_FIELD_META_NAME_ISNULL).maxLength(64)
		.and(assetFieldMeta.getMetaKey()).notNull(RCode.ASSET_FIELD_META_KEY_ISNULL).maxLength(128)
		.and(assetFieldMeta.getGroupId()).notNull(RCode.ASSET_FIELD_META_GROUPID_ISNULL)
		.and(assetFieldMeta.getSearch()).notNull(RCode.ASSET_FIELD_META_SEARCH_ISNULL)
		.and(assetFieldMeta.getDisplay()).notNull(RCode.ASSET_FIELD_META_DISPLAY_ISNULL)
		.and(assetFieldMeta.getType()).notNull(RCode.ASSET_FIELD_META_TYPE_ISNULL)
		.and(assetFieldMeta.getRemark()).maxLength(256);
		
		Integer id = assetFieldMetaService.saveOrModify(assetFieldMeta);
		return R.ok().putData("id",id);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.ASSET_FIELD_META)
	@RequiresPermissions({"field:meta:save"})
	public R saveAssetFieldMeta(@RequestBody AssetFieldMeta assetFieldMeta) {
		ValidateUtils.is(assetFieldMeta.getName()).notNull(RCode.ASSET_FIELD_META_NAME_ISNULL).maxLength(64)
		.and(assetFieldMeta.getMetaKey()).notNull(RCode.ASSET_FIELD_META_KEY_ISNULL).maxLength(128)
		.and(assetFieldMeta.getGroupId()).notNull(RCode.ASSET_FIELD_META_GROUPID_ISNULL)
		.and(assetFieldMeta.getSearch()).notNull(RCode.ASSET_FIELD_META_SEARCH_ISNULL)
		.and(assetFieldMeta.getDisplay()).notNull(RCode.ASSET_FIELD_META_DISPLAY_ISNULL)
		.and(assetFieldMeta.getType()).notNull(RCode.ASSET_FIELD_META_TYPE_ISNULL)
		.and(assetFieldMeta.getRemark()).maxLength(256);
		
		Integer id = assetFieldMetaService.saveOrModify(assetFieldMeta);
		return R.ok().putData("id",id);
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ASSET_FIELD_META)
	@RequiresPermissions({"field:meta:delete"})
	public R removeAssetFieldMeta(@RequestParam Map requestParam) {
		String ids = (String)requestParam.get("ids");
		Integer force = Integer.valueOf(Tool.StrUtil.emptyToDefault((String)requestParam.get("force"),"0"));
		ValidateUtils.is(ids).notNull(RCode.ASSET_FIELD_META_ID_ISNULL);
		
		assetFieldMetaService.removeAssetFieldMetas(Arrays.asList(ids.split(",")),force);
		return R.ok();
	}
	
	
}
