package com.nis.modules.asset.controller;

import cn.hutool.core.util.ObjectUtil;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.*;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetCommentService;
import com.nis.modules.asset.service.AssetStateConfService;
import com.nis.modules.asset.service.AssetTypeConfService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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

    @Autowired
    private AssetCommentService assetCommentService;

    /**
     * 资产详情
     * @param id
     * @return
     */
    @GetMapping("/asset/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.HARDWARE_ASSET)
    public R queryAssetInfo(@PathVariable("id") Integer id, @RequestParam(value = "feature", required = false) Integer feature) {
        return R.ok(assetAssetService.queryAssetWithFeatureInfo(id, feature));
    }

    /**
     * 资产请求列表
     * @param params
     * @return
     */
    @GetMapping("/asset")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.HARDWARE_ASSET)
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
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.HARDWARE_ASSET)
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
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.HARDWARE_ASSET)
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
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.HARDWARE_ASSET)
    @RequiresPermissions({"asset:delete"})
    public R deleteAsset(@RequestParam(required = false, defaultValue = "0") Integer force, Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_ID_ISNULL);

        return assetAssetService.deleteAsset(force, ids);
    }

    /**
     * endpoint 统计
     * @return
     */
    @GetMapping("/asset/stat/endpoint/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.HARDWARE_ASSET)
    public R endpointStatByAsset(@PathVariable Integer id) {
        return R.ok(assetAssetService.endpointStatByAsset(id));
    }


    /**
     * feature 统计
     * @return
     */
    @GetMapping("/asset/feature/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.HARDWARE_ASSET)
    public R featureStatByAsset(@PathVariable Integer id) {
        return R.ok(assetAssetService.featureStatByAsset(id));
    }


    /**
     * Asset field value查询接口
     * @return
     */
    @GetMapping("/asset/field/value")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.HARDWARE_ASSET)
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
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.HARDWARE_ASSET)
    public R queryParams() {
        return R.ok(assetAssetService.queryParams());
    }


    /**
     * 获取导入模板
     * @param response
     */
    @GetMapping("/asset/template")
    @SysLog(operation = OperationEnum.DOWNLOAD, type = TypeEnum.HARDWARE_ASSET)
    public void getTemplateByLanguage(HttpServletResponse response,
                                      @RequestParam(required = false, defaultValue = "en") String language,
                                      @RequestParam(required = false, defaultValue = "1") String format) throws IOException {

        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(format)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }

        assetAssetService.getTemplateByLanguage(response, format);
    }

    /**
     * 批量导入
     * @param excelFile
     * @return
     */
    @PostMapping("/asset/import")
    @SysLog(operation = OperationEnum.IMPORT,type = TypeEnum.HARDWARE_ASSET)
    @RequiresPermissions({"asset:save"})
    public R importAssetsData(@RequestParam(value = "file") MultipartFile excelFile, HttpServletRequest request, @RequestParam Map<String, Object> params) throws IOException {
        String language = request.getHeader(Constant.LANGUAGE);
        ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);
        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }

        Map info = assetAssetService.importAssetsData(excelFile, params);
        return R.ok(info);
    }

    /**
     * asset导入撤销
     *
     * @return
     */
    @DeleteMapping("/asset/cancelImport")
    @SysLog(operation = OperationEnum.CANCEL, type = TypeEnum.HARDWARE_ASSET)
    public R cancelImportAssetData(String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);

        assetAssetService.cancelImport(seq);
        return R.ok();
    }


    /**
     * 导出
     * @param response
     * @param params
     */
    @GetMapping("/asset/export")
    @SysLog(operation = OperationEnum.EXPORT,type = TypeEnum.HARDWARE_ASSET)
    public void exportAssets(HttpServletResponse response, @RequestParam Map<String, Object> params)  throws IOException {
        Object format = params.get("format");
        String formatStr = format == null ? "1" : format.toString();
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(formatStr)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }
        params.put("format", formatStr);

        assetAssetService.export(response, params);
    }


    /**
     * asset资产批量修改
     */
    @PutMapping("/asset/bulkEdit")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.HARDWARE_ASSET)
    @RequiresPermissions({"asset:update"})
    public R bulkEdit(@RequestBody Map<String,Object> params) {
    	assetAssetService.bulkEdit(params);
        return R.ok();
    }


    /**
     * 资产配置同步更新 dashboard，endpoint
     *
     * @return
     */
    @RequestMapping(value = "/asset/sync", method = {RequestMethod.POST, RequestMethod.PUT})
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.HARDWARE_ASSET)
    public R syncAssetEndpointAndDashboard(@RequestBody(required = false) Map<String, Object> requestParam) {
        // validate param
        requestParam = Tool.MapUtil.defaultIfEmpty(requestParam, Tool.MapUtil.empty());

        List<Integer> syncAssetIdList = (List<Integer>) requestParam.get("ids");
        ValidateUtils.is(syncAssetIdList).notNull(RCode.ASSET_ID_ISNULL);

        Integer isSyncEndpoint = Tool.MapUtil.getInt(requestParam, "endpoint", 0);
        Integer isSyncDashboard = Tool.MapUtil.getInt(requestParam, "dashboard", 0);

        assetAssetService.syncAssetEndpointAndDashboard(syncAssetIdList, isSyncEndpoint, isSyncDashboard);
        return R.ok();
    }

    
    @GetMapping("/stateConf")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET_STATE_CONF)
    public R queryStateConfList(@RequestParam Map<String, Object> params) {
        PageUtils page = stateConfService.queryPage(params);
        return R.ok(page);
    }

    @GetMapping("/stateConf/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET_STATE_CONF)
    public R queryStateConfInfo(@PathVariable("id") Integer id) {
        AssetStateConf assetStateConf = stateConfService.queryConfInfo(id);
        return R.ok(assetStateConf);
    }

    @PostMapping("/stateConf")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ASSET_STATE_CONF)
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
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ASSET_STATE_CONF)
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
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ASSET_STATE_CONF)
    @RequiresPermissions({"asset:state:delete"})
    public R deleteAssetStateConf(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.STATECONF_ID_ISNULL);

        stateConfService.delete(ids);
        return R.ok();
    }


    @GetMapping("/typeConf/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET_TYPE_CONF)
    public R queryTypeConfInfo(@PathVariable("id") Integer id) {
        AssetTypeConf assetTypeConf = typeConfService.queryConfInfo(id);
        return R.ok(assetTypeConf);
    }

    @GetMapping("/typeConf")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET_TYPE_CONF)
    public R queryTypeConfList(@RequestParam Map<String, Object> params) {
        PageUtils page = typeConfService.queryPage(params);
        return R.ok(page);
    }

    @GetMapping("/typeConf/tree")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET_TYPE_CONF)
    public R queryTypeConfTree(Integer pid) {
        List<AssetTypeConf> typeConfTreeList = typeConfService.queryTypeConfTree(pid);
        Map map = new HashMap(2);
        map.put("list", typeConfTreeList);
        return R.ok(map);
    }

    @PostMapping("/typeConf")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ASSET_TYPE_CONF)
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
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ASSET_TYPE_CONF)
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
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ASSET_TYPE_CONF)
    @RequiresPermissions({"asset:type:delete"})
    public R deleteAssetTypeConf(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.TYPECONF_ID_ISNULL);

        typeConfService.delete(ids);
        return R.ok();
    }

    /**
     * 资产评论详情
     * @param id
     * @return
     */
    @GetMapping("/comment/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET_COMMENT)
    public R queryAssetCommentInfo(@PathVariable("id") Integer id) {
        return R.ok(assetCommentService.queryCommentInfo(id));
    }

    /**
     * 资产评论列表
     * @param params
     * @return
     */
    @GetMapping("/comment")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSET_COMMENT)
    public R queryAssetCommentPage(@RequestParam Map<String, Object> params) {
        return R.ok(assetCommentService.queryCommentPage(params));
    }

    /**
     * 保存
     * @param AssetComment
     * @return
     */
    @PostMapping("/comment")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ASSET_COMMENT)
    @RequiresPermissions({"assetComment:save"})
    public R saveAssetComment(@RequestBody AssetComment AssetComment) {
        return R.ok().putData("id", assetCommentService.saveComment(AssetComment));
    }

    /**
     * 修改
     * @param AssetComment
     * @return
     */
    @PutMapping("/comment")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ASSET_COMMENT)
    @RequiresPermissions({"assetComment:update"})
    public R updateAssetComment(@RequestBody AssetComment AssetComment) {
        ValidateUtils.is(AssetComment.getId()).notNull(RCode.ASSET_COMMENT_ID_ISNULL);
        return R.ok().putData("id", assetCommentService.updateComment(AssetComment));
    }

    /**
     * 删除
     * @param ids
     * @return
     */
    @DeleteMapping("/comment")
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ASSET_COMMENT)
    @RequiresPermissions({"assetComment:delete"})
    public R deleteAssetComment(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_COMMENT_ID_ISNULL);
        assetCommentService.deleteComment(ids);
        return R.ok();
    }

}
