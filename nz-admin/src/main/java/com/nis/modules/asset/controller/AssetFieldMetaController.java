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
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.asset.service.AssetFieldMetaService;

@RestController
@RequestMapping("/asset/field/meta")
public class AssetFieldMetaController {
	
	@Autowired
	private AssetFieldMetaService assetFieldMetaService;
	
	@GetMapping("/{id}")
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETFIELDMETA)
	@RequiresPermissions({"asset:fieldmeta:info"})
	public R queryAssetFieldMetaById(@PathVariable Integer id) {
		ValidateUtils.is(id).notNull(RCode.ASSET_FIELD_META_ID_ISNULL);
		
		AssetFieldMeta assetFieldMeta=assetFieldMetaService.queryAssetFieldMeta(id);
		return R.ok(assetFieldMeta);
	}
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETFIELDMETA)
	@RequiresPermissions({"asset:fieldmeta:list"})
	public R queryAssetFieldMetaList(@RequestParam Map<String,Object> params) {
		
		PageUtils page = assetFieldMetaService.queryList(params);
		return R.ok(page);
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETFIELDMETA)
	@RequiresPermissions({"asset:fieldmeta:update"})
	public R modifyAssetFieldMeta(@RequestBody AssetFieldMeta assetFieldMeta) {
		ValidateUtils.is(assetFieldMeta.getId()).notNull(RCode.ASSET_FIELD_META_ID_ISNULL)
		.and(assetFieldMeta.getName()).notNull(RCode.ASSET_FIELD_META_NAME_ISNULL)
		.and(assetFieldMeta.getMetaKey()).notNull(RCode.ASSET_FIELD_META_KEY_ISNULL)
		.and(assetFieldMeta.getGroupId()).notNull(RCode.ASSET_FIELD_META_GROUPID_ISNULL)
		.and(assetFieldMeta.getSearch()).notNull(RCode.ASSET_FIELD_META_SEARCH_ISNULL)
		.and(assetFieldMeta.getDisplay()).notNull(RCode.ASSET_FIELD_META_DISPLAY_ISNULL)
		.and(assetFieldMeta.getType()).notNull(RCode.ASSET_FIELD_META_TYPE_ISNULL);
		
		Integer id = assetFieldMetaService.saveOrModify(assetFieldMeta);
		return R.ok().putData("id",id);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETFIELDMETA)
	@RequiresPermissions({"asset:fieldmeta:add"})
	public R saveAssetFieldMeta(@RequestBody AssetFieldMeta assetFieldMeta) {
		ValidateUtils.is(assetFieldMeta.getName()).notNull(RCode.ASSET_FIELD_META_NAME_ISNULL)
		.and(assetFieldMeta.getMetaKey()).notNull(RCode.ASSET_FIELD_META_KEY_ISNULL)
		.and(assetFieldMeta.getGroupId()).notNull(RCode.ASSET_FIELD_META_GROUPID_ISNULL)
		.and(assetFieldMeta.getSearch()).notNull(RCode.ASSET_FIELD_META_SEARCH_ISNULL)
		.and(assetFieldMeta.getDisplay()).notNull(RCode.ASSET_FIELD_META_DISPLAY_ISNULL)
		.and(assetFieldMeta.getType()).notNull(RCode.ASSET_FIELD_META_TYPE_ISNULL);
		
		Integer id = assetFieldMetaService.saveOrModify(assetFieldMeta);
		return R.ok().putData("id",id);
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETFIELDMETA)
	@RequiresPermissions({"asset:fieldmeta:delete"})
	public R removeAssetFieldMeta(@RequestParam String ids) {
		ValidateUtils.is(ids).notNull(RCode.ASSET_FIELD_META_ID_ISNULL);
		
		assetFieldMetaService.removeAssetFieldMetas(Arrays.asList(ids.split(",")));
		return R.ok();
	}
	
	
}
