package com.nis.modules.asset.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.AssetStateConf;
import com.nis.modules.asset.entity.AssetTypeConf;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.AssetStateConfService;
import com.nis.modules.asset.service.AssetTypeConfService;
import com.nis.modules.terminal.entity.Account;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
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

    @Autowired
    private AssetStateConfService stateConfService;

    @Autowired
    private AssetTypeConfService typeConfService;

    /**
     * 资产请求列表
     *
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSET)
    @RequiresPermissions({"asset:list"})
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
    @RequiresPermissions({"asset:info"})
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
    @RequiresPermissions({"asset:save"})
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
            assetService.validateAccount(accounts);
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
                result = assetService.uploadBuildExporter(null, asset, accountCli);
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
    @RequiresPermissions({"asset:update"})
    public R update(@RequestBody Asset asset) {
        ValidateUtils.is(asset.getId()).notNull(RCode.ASSET_ID_ISNULL)
                .and(asset.getSn()).notNull(RCode.ASSET_SN_ISNULL).maxLength(64, RCode.ASSET_SN_TOO_LONG)
                .and(asset.getModelId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(asset.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(asset.getHost()).notNull(RCode.ASSET_HOST_ISNULL).ip(RCode.ASSET_HOST_FORMAT);


        List<Account> accounts = asset.getAccounts();
        if (CollectionUtils.isNotEmpty(accounts)) {
            assetService.validateAccount(accounts);
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
    @RequiresPermissions({"asset:delete"})
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
    @RequiresPermissions({"asset:import"})
    public R importAssetsFromExcel(MultipartFile excelFile,String language) {
        ValidateUtils.is(excelFile).notNull(RCode.ASSET_IMPORT_FILE_ISNULL)
                .and(language).notNull(RCode.ASSET_EXPORT_HEADER_LANGUAGE_ISNULL);
        // 本次导入的 uuid
        String seqUUID = UUID.randomUUID().toString();
        Map map = assetService.importAssetsFromExcel(excelFile, seqUUID, language);
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
    @RequiresPermissions({"asset:export"})
    public void exportAssetsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        assetService.export(response,params);
    }

    /**
     * asset导入撤销
     *
     * @return
     */
    @DeleteMapping("/cancelImport")
    @RequiresPermissions({"asset:cancel:import"})
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ASSET)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.ASSET_CANCELIMPORTSEQ_ISNULL);

        assetService.cancelImport(seq);
        return R.ok();
    }
    
    
    /**
     * asset更新采集项信息
     */
    @GetMapping("updateFeature")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET)
    @RequiresPermissions({"asset:feature"})
    public R updateFeature(Integer assetId) {
    	// 采集信息方法
    	try {
			assetService.collectData(assetId);
			return R.ok();
		} catch (Exception e) {
			return R.error(500,e.getMessage());
		}
    }

    /**
     * asset筛选条件接口, 按照dc assetType vendor pingStatus 组织统计与筛选条件
     *
     * @return
     */
    @GetMapping("/filter")
    @RequiresPermissions({"asset:filter"})
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET)
    public R updateFeature() {
        return R.ok(assetService.filterStat());
    }

    /**
     * 获取导入模板
     * @param response
     * @param language
     */
    @GetMapping("/template")
    @RequiresPermissions({"asset:import:template"})
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        if (ToolUtil.isEmpty(language)) throw new NZException(RCode.ASSET_EXPORT_HEADER_LANGUAGE_ISNULL);

        assetService.getExportTemplateByLanguage(response, language);
    }

    @GetMapping("/tagKey")
    @RequiresPermissions({"asset:tag:key"})
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET)
    public R tagKey(String key) {
        List<String> keys = assetService.getTagsBykeyOrderCounts(key);
        Map result = new HashMap(2);
        result.put("list", keys);
        return R.ok(result);
    }

    @GetMapping("/tagValue")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET)
    @RequiresPermissions({"asset:tag:value"})
    public R tagValue(String key, String value) {
        ValidateUtils.is(key).notNull(RCode.ASSET_TAGKEY_ISNULL);

        List<String> values = assetService.getTagValsByKeyOrderCounts(key, value);
        Map result = new HashMap(2);
        result.put("list", values);
        return R.ok(result);
    }

    @PutMapping("/batch")
    @RequiresPermissions({"asset:batch:update"})
    public R updateBatchAccountAndTagByAssetIds(@RequestBody Map<String, Object> param){
        ValidateUtils.is(param.get("assetIds")).notNull(RCode.ASSET_ID_ISNULL);

        assetService.updateBatchAccountAndTagByAssetIds(param);
        return R.ok();
    }

    @GetMapping("/stateConf")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSETSTATECONF)
    public R queryStateConfList(@RequestParam Map<String, Object> params) {
        PageUtils page = stateConfService.queryPage(params);
        return R.ok(page);
    }

    @GetMapping("/stateConf/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSETSTATECONF)
    public R queryStateConfInfo(@PathVariable("id") Integer id) {
        AssetStateConf assetStateConf = stateConfService.queryConfInfo(id);
        return R.ok(assetStateConf);
    }

    @PostMapping("/stateConf")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ASSETSTATECONF)
    public R saveAssetStateConf(@RequestBody AssetStateConf conf) {
        ValidateUtils.is(conf.getName()).notNull(RCode.STATECONF_NAME_ISNULL)
                .and(conf.getPing()).notNull(RCode.STATECONF_PING_ISNULL)
                .and(conf.getAlert()).notNull(RCode.STATECONF_ALERT_ISNULL)
                .and(conf.getMonitor()).notNull(RCode.STATECONF_MONITOR_ISNULL);

        Map map = new HashMap(2);
        map.put("id", stateConfService.saveConf(conf));
        return R.ok(map);
    }

    @PutMapping("/stateConf")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ASSETSTATECONF)
    public R editAssetStateConf(@RequestBody AssetStateConf conf) {
        ValidateUtils.is(conf.getId()).notNull(RCode.STATECONF_ID_ISNULL)
                .and(conf.getName()).notNull(RCode.STATECONF_NAME_ISNULL)
                .and(conf.getPing()).notNull(RCode.STATECONF_PING_ISNULL)
                .and(conf.getAlert()).notNull(RCode.STATECONF_ALERT_ISNULL)
                .and(conf.getMonitor()).notNull(RCode.STATECONF_MONITOR_ISNULL);

        Map map = new HashMap(2);
        map.put("id", stateConfService.updateConf(conf));
        return R.ok(map);
    }

    @DeleteMapping("/stateConf")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ASSETSTATECONF)
    public R deleteAssetStateConf(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.STATECONF_ID_ISNULL);

        stateConfService.delete(ids);
        return R.ok();
    }


    @GetMapping("/typeConf/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSETTYPECONF)
    public R queryTypeConfInfo(@PathVariable("id") Integer id) {
        AssetTypeConf assetTypeConf = typeConfService.queryConfInfo(id);
        return R.ok(assetTypeConf);
    }

    @GetMapping("/typeConf")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSETTYPECONF)
    public R queryTypeConfList(@RequestParam Map<String, Object> params) {
        PageUtils page = typeConfService.queryPage(params);
        return R.ok(page);
    }

    @GetMapping("/typeConf/tree")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSETTYPECONF)
    public R queryTypeConfTree(Integer pid) {
        List<AssetTypeConf> typeConfTreeList = typeConfService.queryTypeConfTree(pid);
        Map map = new HashMap(2);
        map.put("list", typeConfTreeList);
        return R.ok(map);
    }

    @PostMapping("/typeConf")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ASSETTYPECONF)
    public R saveAssetTypeConf(@RequestBody AssetTypeConf conf) {
        ValidateUtils.is(conf.getName()).notNull(RCode.TYPECONF_NAME_ISNULL)
                .and(conf.getVm()).notNull(RCode.TYPECONF_VM_ISNULL)
                .and(conf.getVmh()).notNull(RCode.TYPECONF_VMH_ISNULL)
                .and(conf.getSsh()).notNull(RCode.TYPECONF_SSH_ISNULL)
                .and(conf.getTelnet()).notNull(RCode.TYPECONF_TELNET_ISNULL);

        Map map = new HashMap(2);
        map.put("id", typeConfService.saveConf(conf));
        return R.ok(map);
    }

    @PutMapping("/typeConf")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ASSETTYPECONF)
    public R editAssetTypeConf(@RequestBody AssetTypeConf conf) {
        ValidateUtils.is(conf.getId()).notNull(RCode.TYPECONF_ID_ISNULL)
                .and(conf.getName()).notNull(RCode.TYPECONF_NAME_ISNULL)
                .and(conf.getVm()).notNull(RCode.TYPECONF_VM_ISNULL)
                .and(conf.getVmh()).notNull(RCode.TYPECONF_VMH_ISNULL)
                .and(conf.getSsh()).notNull(RCode.TYPECONF_SSH_ISNULL)
                .and(conf.getTelnet()).notNull(RCode.TYPECONF_TELNET_ISNULL);

        Map map = new HashMap(2);
        map.put("id", typeConfService.updateConf(conf));
        return R.ok(map);
    }

    @DeleteMapping("/typeConf")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ASSETTYPECONF)
    public R deleteAssetTypeConf(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.TYPECONF_ID_ISNULL);

        typeConfService.delete(ids);
        return R.ok();
    }

}
