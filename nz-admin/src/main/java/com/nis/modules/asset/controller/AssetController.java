package com.nis.modules.asset.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.web.multipart.MultipartFile;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.asset.entity.AssetStateConf;
import com.nis.modules.asset.entity.AssetTypeConf;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetStateConfService;
import com.nis.modules.asset.service.AssetTypeConfService;

import cn.hutool.core.util.ObjectUtil;


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
    private AssetStateConfService stateConfService;

    @Autowired
    private AssetTypeConfService typeConfService;

    @Autowired
    private AssetAssetService assetAssetService;

    /**
     * 资产详情
     * @param id
     * @return
     */
    @GetMapping("/asset/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET)
    public R queryAssetInfo(@PathVariable("id") Integer id) {
        return R.ok(assetAssetService.queryAssetInfo(id));
    }

    /**
     * 资产请求列表
     * @param params
     * @return
     */
    @GetMapping("/asset")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSET)
    public R queryAssetPage(@RequestParam Map<String, Object> params) {
        PageUtils page = assetAssetService.queryAssetPage(params);
        Object statFlag = params.getOrDefault("statistics", 0);
        R result = R.ok(page);
        if(Tool.ObjectUtil.equal("1", String.valueOf(statFlag))) {
        	Map<String, List> assetGroupBy = assetAssetService.queryAssetGroupBy(params);
        	result.put("statistics", assetGroupBy);
        }
        return result;
    }

    /**
     * 保存
     * @param asset
     * @return
     */
    @PostMapping("/asset")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ASSET)
    @RequiresPermissions({"asset:save"})
    public R saveAsset(@RequestBody AssetAsset asset) {
        ValidateUtils.is(asset.getName()).notNull(RCode.ASSET_NAME_ISNULL).maxLength(128)
                .and(asset.getSn()).notNull(RCode.ASSET_SN_ISNULL).maxLength(128)
                .and(asset.getManageIp()).notNull(RCode.ASSET_MANAGEIP_ISNULL).ip(RCode.ASSET_MANAGEIP_FORMAT).maxLength(64)
                .and(asset.getModelId()).notNull(RCode.ASSET_MODEL_ID_ISNULL)
                .and(asset.getDcId()).notNull(RCode.ASSET_DC_ID_ISNULL)
                .and(asset.getStateId()).notNull(RCode.ASSET_STATE_ID_ISNULL)
                .and(asset.getTypeId()).notNull(RCode.ASSET_TYPE_ID_ISNULL)
                .and(asset.getNumber()).maxLength(128)
                .and(asset.getClientAddress()).maxLength(64)
                .and(asset.getClientPin()).maxLength(64)
                .and(asset.getClientToken()).maxLength(64)
                .and(asset.getClientUser()).maxLength(64);

        if(ObjectUtil.equal(asset.getAuthType(), 1)) {
        	ValidateUtils.is(asset.getAuthUsername()).notNull(RCode.ASSET_AUTHUSERNAME_ISNULL).maxLength(64)
        	.and(asset.getAuthPin()).notNull(RCode.ASSET_AUTHPIN_ISNULL).maxLength(256);
        }else if(ObjectUtil.equal(asset.getAuthType(), 2)){
        	ValidateUtils.is(asset.getAuthPriKey()).notNull(RCode.ASSET_AUTHPRIKEY_ISNULL);
        }
        // client enable =1 开启talon ，token，port不能为空
        if(Tool.ObjectUtil.equal(asset.getClientEnable(),1)) {
        	ValidateUtils.is(asset.getClientPort()).notNull(RCode.ASSET_CLIENT_PROT_ISNULL)
        		.and(asset.getClientToken()).notNull(RCode.ASSET_CLIENT_TOKEN_ISNULL);
        }
        
        Integer id = assetAssetService.saveAsset(asset);
        return R.ok().putData("id", id);
    }


    /**
     * 修改
     * @param asset
     * @return
     */
    @PutMapping("/asset")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ASSET)
    @RequiresPermissions({"asset:update"})
    public R updateAsset(@RequestBody AssetAsset asset) {
        ValidateUtils.is(asset.getId()).notNull(RCode.ASSET_ID_ISNULL)
                .and(asset.getName()).notNull(RCode.ASSET_NAME_ISNULL).maxLength(128)
                .and(asset.getSn()).notNull(RCode.ASSET_SN_ISNULL).maxLength(128)
                .and(asset.getManageIp()).notNull(RCode.ASSET_MANAGEIP_ISNULL).ip(RCode.ASSET_MANAGEIP_FORMAT).maxLength(64)
                .and(asset.getBrandId()).notNull(RCode.ASSET_BRAND_ID_ISNULL)
                .and(asset.getModelId()).notNull(RCode.ASSET_MODEL_ID_ISNULL)
                .and(asset.getDcId()).notNull(RCode.ASSET_DC_ID_ISNULL)
                .and(asset.getStateId()).notNull(RCode.ASSET_STATE_ID_ISNULL)
                .and(asset.getTypeId()).notNull(RCode.ASSET_TYPE_ID_ISNULL)
                .and(asset.getNumber()).maxLength(128)
                .and(asset.getNumber()).maxLength(128)
                .and(asset.getClientAddress()).maxLength(64)
                .and(asset.getClientPin()).maxLength(64)
                .and(asset.getClientToken()).maxLength(64)
                .and(asset.getClientUser()).maxLength(64);
        
        /*if(ObjectUtil.equal(asset.getAuthType(), 1)) {
        	ValidateUtils.is(asset.getAuthUsername()).notNull(RCode.ASSET_AUTHUSERNAME_ISNULL).maxLength(64)
        	.and(asset.getAuthPin()).notNull(RCode.ASSET_AUTHPIN_ISNULL).maxLength(256);
        }else if(ObjectUtil.equal(asset.getAuthType(), 2)){
        	ValidateUtils.is(asset.getAuthPriKey()).notNull(RCode.ASSET_AUTHPRIKEY_ISNULL);
        }*/
        
        // client enable =1 开启talon ，token，port不能为空
        if(Tool.ObjectUtil.equal(asset.getClientEnable(),1)) {
        	ValidateUtils.is(asset.getClientPort()).notNull(RCode.ASSET_CLIENT_PROT_ISNULL)
        		.and(asset.getClientToken()).notNull(RCode.ASSET_CLIENT_TOKEN_ISNULL);
        }
        
        Integer id = assetAssetService.updateAsset(asset);
        return R.ok().putData("id", id);
    }


    /**
     * 删除
     * @param ids
     * @return
     */
    @DeleteMapping("/asset")
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ASSET)
    @RequiresPermissions({"asset:delete"})
    public R deleteAsset(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_ID_ISNULL);

        assetAssetService.deleteAsset(ids);
        return R.ok();
    }


    /**
     * alert 统计
     * @return
     */
    /*@GetMapping("/asset/stat/alert/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET)
    public R alertStatByAsset(@PathVariable Integer id) {
        return R.ok(assetAssetService.alertStatByAsset(id));
    }*/

    /**
     * endpoint 统计
     * @return
     */
    @GetMapping("/asset/stat/endpoint/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET)
    public R endpointStatByAsset(@PathVariable Integer id) {
        return R.ok(assetAssetService.endpointStatByAsset(id));
    }


    /**
     * feature 统计
     * @return
     */
    @GetMapping("/asset/feature/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET)
    public R featureStatByAsset(@PathVariable Integer id) {
        return R.ok(assetAssetService.featureStatByAsset(id));
    }


    /**
     * Asset field value查询接口
     * @return
     */
    @GetMapping("/asset/field/value")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET)
    public R queryAssetFieldValue(Integer assetId,Integer metaId) {
        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL)
                .and(metaId).notNull(RCode.ASSET_FIELD_META_ID_ISNULL);

        Map map = new HashMap(2);
        List<AssetFieldMeta> metaList = assetAssetService.queryAssetFieldValue(assetId, metaId);
        map.put("list", metaList);
        return R.ok(map);
    }

    /**
     * asset 查询条件字段接口（asset左侧菜单使用）
     *
     * @return
     */
    @GetMapping("/asset/queryParams")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET)
    public R queryParams() {
        return R.ok(assetAssetService.queryParams());
    }


    /**
     * 获取导入模板
     * @param response
     * @param language
     */
    @GetMapping("/asset/template")
    public void getAssetExcelTemplateByLanguage(HttpServletResponse response, HttpServletRequest request) {
        String language = request.getHeader(Constant.LANGUAGE);
        assetAssetService.getExportTemplateByLanguage(response, language);
    }

    /**
     * 批量导入
     * @param excelFile
     * @return
     */
    @PostMapping("/asset/import")
    @SysLog(operation = OperationEnum.IMPORT,type = TypeEnum.ASSET)
    @RequiresPermissions({"asset:save"})
    public R importAssets(MultipartFile excelFile,HttpServletRequest request) {
        ValidateUtils.is(excelFile).notNull(RCode.ASSET_IMPORT_FILE_ISNULL);
        String language = request.getHeader(Constant.LANGUAGE);
        Map map = assetAssetService.importAssetsFromExcel(excelFile, language);
        return R.ok(map);
    }

    /**
     * asset导入撤销
     *
     * @return
     */
    @DeleteMapping("/asset/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ASSET)
    public R cancelImportAssetData(String seq) {
        ValidateUtils.is(seq).notNull(RCode.ASSET_CANCELIMPORTSEQ_ISNULL);

        assetAssetService.cancelImport(seq);
        return R.ok();
    }


    /**
     * 导出
     * @param response
     * @param params
     */
    @GetMapping("/asset/export")
    @SysLog(operation = OperationEnum.EXPORT,type = TypeEnum.ASSET)
    public void exportAssets(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        assetAssetService.export(response,params);
    }


    /**
     * asset资产批量修改
     */
    @PutMapping("/asset/bulkEdit")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ASSET)
    @RequiresPermissions({"asset:update"})
    public R bulkEdit(@RequestBody Map<String,Object> params) {
    	assetAssetService.bulkEdit(params);
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
    @RequiresPermissions({"asset:state:save"})
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
    @RequiresPermissions({"asset:state:update"})
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
    @RequiresPermissions({"asset:state:delete"})
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
    @RequiresPermissions({"asset:type:save"})
    public R saveAssetTypeConf(@RequestBody AssetTypeConf conf) {
        ValidateUtils.is(conf.getName()).notNull(RCode.TYPECONF_NAME_ISNULL)
                .and(conf.getVm()).notNull(RCode.TYPECONF_VM_ISNULL)
                .and(conf.getVmh()).notNull(RCode.TYPECONF_VMH_ISNULL)
                .and(conf.getAuthProtocol()).notNull(RCode.TYPECONF_AUTHPROTOCOL_ISNULL)
                .and(conf.getSnmpEnable()).notNull(RCode.TYPECONF_SNMPENABLE_ISNULL)
                .and(conf.getSnmpCollect()).notNull(RCode.TYPECONF_SNMPCOLLECT_ISNULL)
                .and(conf.getSshCollect()).notNull(RCode.TYPECONF_SSHCOLLECT_ISNULL)
                .and(conf.getClientEnable()).notNull(RCode.TYPECONF_CLIENTENABLE_ISNULL);
        if(ToolUtil.equals(conf.getSshCollect(), 1)) {
        	ValidateUtils.is(conf.getSshCollectScript()).notNull(RCode.TYPECONF_SSHCOLLECTSCRIPT_ISNULL);
        }
        Map map = new HashMap(2);
        map.put("id", typeConfService.saveConf(conf));
        return R.ok(map);
    }

    @PutMapping("/typeConf")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ASSETTYPECONF)
    @RequiresPermissions({"asset:type:update"})
    public R editAssetTypeConf(@RequestBody AssetTypeConf conf) {
        ValidateUtils.is(conf.getId()).notNull(RCode.TYPECONF_ID_ISNULL)
                .and(conf.getName()).notNull(RCode.TYPECONF_NAME_ISNULL)
                .and(conf.getVm()).notNull(RCode.TYPECONF_VM_ISNULL)
                .and(conf.getVmh()).notNull(RCode.TYPECONF_VMH_ISNULL)
                .and(conf.getAuthProtocol()).notNull(RCode.TYPECONF_AUTHPROTOCOL_ISNULL)
                .and(conf.getSnmpEnable()).notNull(RCode.TYPECONF_SNMPENABLE_ISNULL)
                .and(conf.getSnmpCollect()).notNull(RCode.TYPECONF_SNMPCOLLECT_ISNULL)
                .and(conf.getSshCollect()).notNull(RCode.TYPECONF_SSHCOLLECT_ISNULL)
                .and(conf.getClientEnable()).notNull(RCode.TYPECONF_CLIENTENABLE_ISNULL);
        if(ToolUtil.equals(conf.getSshCollect(), 1)) {
        	ValidateUtils.is(conf.getSshCollectScript()).notNull(RCode.TYPECONF_SSHCOLLECTSCRIPT_ISNULL);
        }

        Map map = new HashMap(2);
        map.put("id", typeConfService.updateConf(conf));
        return R.ok(map);
    }

    @DeleteMapping("/typeConf")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ASSETTYPECONF)
    @RequiresPermissions({"asset:type:delete"})
    public R deleteAssetTypeConf(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.TYPECONF_ID_ISNULL);

        typeConfService.delete(ids);
        return R.ok();
    }

}
