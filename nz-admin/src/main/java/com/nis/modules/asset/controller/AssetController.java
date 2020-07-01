package com.nis.modules.asset.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.terminal.entity.Account;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * 资产  controller
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@RestController
@RequestMapping("/asset")
public class AssetController {

    @Autowired
    private AssetService assetService;

    /**
     * 资产请求列表
     *
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSET)
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = assetService.queryPage(params);
        return R.ok(page);
    }

    /**
     * 资产详情查询接口
     * @param id
     * @return
     */
    @GetMapping("/info")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSET)
    public R assetInfo(Integer id){
        Map detail = assetService.detail(id);
        return R.ok(detail);
    }

    /**
     * 保存
     *
     * @param asset
     * @return
     */
    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.ASSET)
    public R save(@RequestBody Asset asset) {
        ValidateUtils.is(asset.getSn()).notNull(RCode.ASSET_SN_ISNULL).maxLength(64, RCode.ASSET_SN_TOO_LONG)
                .and(asset.getModelId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(asset.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(asset.getHost()).notNull(RCode.ASSET_HOST_ISNULL).ip(RCode.ASSET_HOST_FORMAT);

        List<Account> accounts = asset.getAccounts();
        if (Constant.ASSET_BUILD_NODE_EXPORTER_FLAG.equals(asset.getExporter()) && CollectionUtils.isEmpty(accounts)) {
            // 指名了需要安装 node exporter 账号信息不能为空
            throw new NZException(RCode.ASSET_ACCOUNT_ISNULL);
        }


        Account accountCli = null;
        if (CollectionUtils.isNotEmpty(accounts)) {
            assetService.validateAccount(asset, accounts);
            // 找到SSH协议的账号
            accountCli = accounts.stream().filter(account -> Constant.ACCOUNT_SSH_PROTOCOL.equals(account.getProtocol()) ? true : false).findAny().orElse(null);
            if(ToolUtil.isNotEmpty(accountCli)){
                accountCli = JSONObject.parseObject(JSON.toJSONString(accountCli), Account.class);
            }
        }

        // 购买时间格式校验
        if (StringUtils.isNotEmpty(asset.getPurchaseDate())) {
            ValidateUtils.is(asset.getPurchaseDate()).date("yyyy-MM-dd", RCode.ASSET_PURCHASEDATE_FORMAT);
        } else {
            asset.setPurchaseDate(null);
        }

        assetService.saveOrUpdateAsset(asset);

        if (Constant.ASSET_BUILD_NODE_EXPORTER_FLAG.equals(asset.getExporter())) {
            R result;
            if (accountCli == null) {
                result = R.error(RCode.ASSET_EXPORTER_UPLOADBYSSH);
            } else {
                result = assetService.uploadExporter(asset, accountCli);
            }
            return result;
        }
        return R.ok();
    }

    /**
     * 修改
     *
     * @param asset
     * @return
     */
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ASSET)
    public R update(@RequestBody Asset asset) {
        ValidateUtils.is(asset.getId()).notNull(RCode.ASSET_ID_ISNULL)
                .and(asset.getSn()).notNull(RCode.ASSET_SN_ISNULL).maxLength(64, RCode.ASSET_SN_TOO_LONG)
                .and(asset.getModelId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(asset.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(asset.getHost()).notNull(RCode.ASSET_HOST_ISNULL).ip(RCode.ASSET_HOST_FORMAT);


        List<Account> accounts = asset.getAccounts();
        if (CollectionUtils.isNotEmpty(accounts)) {
            assetService.validateAccount(asset, accounts);
        }

        // 购买时间格式校验
        if (StringUtils.isNotEmpty(asset.getPurchaseDate())) {
            ValidateUtils.is(asset.getPurchaseDate()).date("yyyy-MM-dd", RCode.ASSET_PURCHASEDATE_FORMAT);
        } else {
            asset.setPurchaseDate(null);
        }

        assetService.saveOrUpdateAsset(asset);
        return R.ok();
    }

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ASSET)
    public R delete(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_ID_ISNULL);

        assetService.delete(ids);
        return R.ok();
    }

    /**
     * 批量导入
     *
     * @param excelFile
     * @return
     */
    @PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT,type = TypeEnum.ASSET)
    public R importAssetsFromExcel(MultipartFile excelFile) {
        ValidateUtils.is(excelFile).notNull(RCode.ASSET_IMPORT_FILE_ISNULL);
        // 本次导入的 uuid
        String seqUUID = UUID.randomUUID().toString();
        Map map = assetService.importAssetsFromExcel(excelFile, seqUUID);
        return R.ok(map);
    }

    /**
     * 导出
     *
     * @param response
     * @param params
     */
    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT,type = TypeEnum.ASSET)
    public void exportAssetsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        assetService.export(response,params);
    }

    /**
     * asset导入撤销
     *
     * @return
     */
    @DeleteMapping("/cancelImport")
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.ASSET_CANCELIMPORTSEQ_ISNULL);

        assetService.cancelImport(seq);
        return R.ok();
    }
    
    
    /**
     * asset更新采集项信息
     */
    @GetMapping("updateFeature")
    public R updateFeature(Integer assetId) {
    	// 采集信息方法
    	try {
			assetService.collectData(assetId);
			return R.ok();
		} catch (Exception e) {
			return R.error(500,e.getMessage());
		}
    }
}