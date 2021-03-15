package com.nis.modules.asset.controller;

import java.util.Arrays;
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
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.asset.entity.AssetAccount;
import com.nis.modules.asset.service.AssetAccountService;

@RestController
@RequestMapping("/asset/account")
public class AssetAccountController {

	@Autowired
	private AssetAccountService assetAccountService;
	
	@GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETACCOUNT)
    @RequiresPermissions({"asset:account:list"})
    public R list(@RequestParam Map<String, Object> params) {
		PageUtils page = assetAccountService.queryPage(params);
        return R.ok(page);
    }
	
	@GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETACCOUNT)
    @RequiresPermissions({"asset:account:id"})
    public R queryById(@PathVariable Integer id) {
		
		ValidateUtils.is(id).notNull(RCode.ASSET_ACCOUNT_ID_ISNULL);
		
		AssetAccount AssetAccount  = assetAccountService.queryAssetAccountById(id);
        return R.ok(AssetAccount);
    }
	
	@GetMapping("/asset/{id}")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSETACCOUNT)
    @RequiresPermissions({"asset:account:asset:id"})
    public R queryByAssetId(@PathVariable Integer id) {
		ValidateUtils.is(id).notNull(RCode.ASSET_ID_ISNULL);
		
		PageUtils page = assetAccountService.queryPageByAssetId(id);
		
        return R.ok(page);
    }
	
	
	@PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.ASSETACCOUNT)
    @RequiresPermissions({"asset:account:save"})
    public R save(@RequestBody AssetAccount assetAccount) {
		ValidateUtils.is(assetAccount.getName()).notNull(RCode.ASSET_ACCOUNT_NAME_ISNULL)
		.and(assetAccount.getProtocol()).notNull(RCode.ASSET_ACCOUNT_PROTOCOL_ISNULL)
		.and(assetAccount.getAuthType()).notNull(RCode.ASSET_ACCOUNT_AUTHTYPE_ISNULL)
		.and(assetAccount.getUsername()).notNull(RCode.ASSET_ACCOUNT_USERNAME_ISNULL)
		.and(assetAccount.getPassword()).notNull(RCode.ASSET_ACCOUNT_PASSWORD_ISNULL)
		.and(assetAccount.getPriKey()).notNull(RCode.ASSET_ACCOUNT_PRIKEY_ISNULL)
		.and(assetAccount.getWeight()).notNull(RCode.ASSET_ACCOUNT_WEIGHT_ISNULL)
		.and(assetAccount.getState()).notNull(RCode.ASSET_ACCOUNT_STATE_ISNULL);
		
		if(assetAccount.getProtocol().equals(2)) {
			ValidateUtils.is(assetAccount.getUserTip()).notNull(RCode.ASSET_ACCOUNT_USERTIP_ISNULL)
			.and(assetAccount.getPasswordTip()).notNull(RCode.ASSET_ACCOUNT_PASSWORDTIP_ISNULL);
		}
		
		assetAccountService.saveOrUpdateAssetAccount(assetAccount);
        return R.ok();
    }
	
	
	@PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ASSETACCOUNT)
    @RequiresPermissions({"asset:account:modify"})
    public R modify(@RequestBody AssetAccount assetAccount) {
		ValidateUtils.is(assetAccount.getId()).notNull(RCode.ASSET_ACCOUNT_ID_ISNULL)
		.and(assetAccount.getName()).notNull(RCode.ASSET_ACCOUNT_NAME_ISNULL)
		.and(assetAccount.getProtocol()).notNull(RCode.ASSET_ACCOUNT_PROTOCOL_ISNULL)
		.and(assetAccount.getAuthType()).notNull(RCode.ASSET_ACCOUNT_AUTHTYPE_ISNULL)
		.and(assetAccount.getUsername()).notNull(RCode.ASSET_ACCOUNT_USERNAME_ISNULL)
		.and(assetAccount.getPassword()).notNull(RCode.ASSET_ACCOUNT_PASSWORD_ISNULL)
		.and(assetAccount.getPriKey()).notNull(RCode.ASSET_ACCOUNT_PRIKEY_ISNULL)
		.and(assetAccount.getWeight()).notNull(RCode.ASSET_ACCOUNT_WEIGHT_ISNULL)
		.and(assetAccount.getState()).notNull(RCode.ASSET_ACCOUNT_STATE_ISNULL);
		
		if(assetAccount.getProtocol().equals(2)) {
			ValidateUtils.is(assetAccount.getUserTip()).notNull(RCode.ASSET_ACCOUNT_USERTIP_ISNULL)
			.and(assetAccount.getPasswordTip()).notNull(RCode.ASSET_ACCOUNT_PASSWORDTIP_ISNULL);
		}
		
		assetAccountService.saveOrUpdateAssetAccount(assetAccount);
        return R.ok();
    }
	
	@DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ASSETACCOUNT)
    @RequiresPermissions({"asset:account:delete"})
    public R delete(@RequestParam Integer... ids) {
		ValidateUtils.is(ids).notNull(RCode.ASSET_ACCOUNT_ID_ISNULL);

        assetAccountService.removeByIds(Arrays.asList(ids));
        return R.ok();
    }
	
	@PutMapping("/rel")
    @RequiresPermissions({"asset:account:rel:modify"})
    public R accountAssociation(@RequestParam Integer id , @RequestParam Integer[] ids) {
		ValidateUtils.is(ids).notNull(RCode.ASSET_ID_ISNULL)
		.and(id).notNull(RCode.ASSET_ACCOUNT_ID_ISNULL);
		
		assetAccountService.saveAssetAccountRel(id,Arrays.asList(ids));
		
        return R.ok();
    }
}
