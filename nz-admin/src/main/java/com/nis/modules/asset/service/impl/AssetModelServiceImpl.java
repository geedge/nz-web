package com.nis.modules.asset.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.dao.AssetModelDao;
import com.nis.modules.asset.entity.*;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetBrandService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.asset.service.AssetTypeConfService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.entity.VisualPanel;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.snmp4j.util.SimpleOIDTextFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产型号 service 实现类
 *
 * @author shizhendong
 * @date 2021-03-16
 */
@Service
public class AssetModelServiceImpl extends ServiceImpl<AssetModelDao, AssetModel> implements AssetModelService {

    private static Log log = Log.get();

    @Autowired
    private AssetAssetService assetAssetService;

    @Autowired
    private AssetBrandService brandService;

    @Autowired
    private AssetModelDao assetModelDao;
    
    @Autowired
    private VisualChartService visualChartService;

    @Autowired
    private VisualPanelService visualPanelService;

    @Autowired
    private AssetAssetDao assetDao;

    @Autowired
    private AssetTypeConfService typeConfService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private SysUserStarredService starredService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Override
    public AssetModel queryModelInfo(Integer id) {
        AssetModel model = this.getById(id);
        if (ToolUtil.isEmpty(model)) {
            throw new NZException(RCode.ASSET_MODEL_NOTFOUND);
        }
        AssetBrand brand = brandService.getById(model.getBrandId());
        String chartIds = model.getChartIds();
    	if(!StrUtil.isBlank(chartIds)) {
            List<VisualChart> charts = new ArrayList<>();
            for (String chartId : Arrays.asList(chartIds.split(","))) {
                VisualChart chartById = visualChartService.getById(chartId);
                charts.add(chartById);
            }
            model.setCharts(charts);
    	}
        String panelId = model.getPanelId();
    	if(StrUtil.isNotBlank(panelId)) {
            VisualPanel visualPanel = visualPanelService.getById(panelId);
            model.setPanel(visualPanel);
    	}
        AssetTypeConf typeConf = typeConfService.getById(model.getTypeId());
        //获取资产数量
    	List<AssetAsset> list = assetAssetService.list(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getModelId, model.getId()));
    	model.setAssetNum(list.size());
        model.setBrand(brand);
        model.setType(typeConf);

        //收藏判断
        int count = starredService.count(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getTid,model.getId())
                .eq(SysUserStarredEntity::getType,"assetModel")
                .eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId())
        );
        model.setStarred(count == 0 ? 0 : 1);

        return model;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage page = new Query(AssetModel.class).getPage(params);
        String ids = (String) params.get("ids");
        if(StrUtil.isNotBlank(ids)) {
        	List<String> listIds = Arrays.asList(ids.split(","));
        	params.put("ids",listIds);
        }
        String brandIds = (String) params.get("brandIds");
        if(StrUtil.isNotBlank(brandIds)) {
        	List<String> brandListIds = Arrays.asList(brandIds.split(","));
        	params.put("brandIds",brandListIds);
        }
        String typeIds = (String) params.get("typeIds");
        if(StrUtil.isNotBlank(typeIds)) {
            List<String> typeListIds = Arrays.asList(typeIds.split(","));
            params.put("typeIds",typeListIds);
        }

        // 当前登录用户
        params.put("userId", ShiroUtils.getUserId().intValue());

        List<AssetModel> modelList = assetModelDao.queryList(page, params);
        for(AssetModel model : modelList) {
        	String chartIds = model.getChartIds();
            if (!StrUtil.isBlank(chartIds)) {
                List<VisualChart> charts = new ArrayList<>();
                for (String id : Arrays.asList(chartIds.split(","))) {
                    VisualChart chartById = visualChartService.getById(id);
                    charts.add(chartById);
                }
                model.setCharts(charts);
            }
            String panelId = model.getPanelId();
            if(StrUtil.isNotBlank(panelId)) {
                VisualPanel visualPanel = visualPanelService.getById(panelId);
                model.setPanel(visualPanel);
            }
        }
        page.setRecords(modelList);
        return new PageUtils(page);
    }

    private void validateAssetModelInfo(AssetModel model) {
        // 1. brand 是否存在
        AssetBrand brand = brandService.getById(model.getBrandId());
        if (ToolUtil.isEmpty(brand)) {
            throw new NZException(RCode.ASSET_BRAND_NOT_FOUND);
        }

        // 2. 名称重复
        List<AssetModel> list = this.list(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getBrandId, model.getBrandId()).eq(AssetModel::getName, model.getName()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(model.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.ASSET_MODELNAME_DUPLICATE);
        }
        
        // 3. 校验chart是否为模板且存在
        String chartIds = model.getChartIds();
        if(StrUtil.isNotBlank(chartIds)) {
        	List<String> chartids = Arrays.asList(chartIds.split(","));
        	List<VisualChart> charts = visualChartService.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getPanelId, 0).in(VisualChart::getId, chartids));
        	if(!ToolUtil.equals(charts.size(), chartids.size())) {
        		throw new NZException(RCode.ASSET_MODEL_CHART_ERROR);
        	}
        }

        // 校验panel是否为模板且存在
        String panelId = model.getPanelId();
        if(StrUtil.isNotBlank(panelId)) {
            VisualPanel visualPanel = visualPanelService.getById(panelId);
            if(visualPanel == null || !ToolUtil.equals(visualPanel.getVarType(), 1)) {
                throw new NZException(RCode.ASSET_MODEL_PANEL_ERROR);
            }
        }

        // 4. type 是否存在
        AssetTypeConf typeConfObj = typeConfService.getById(model.getTypeId());
        ValidateUtils.is(typeConfObj).notNull(RCode.TYPECONF_NOT_FOUND);

        // 5. sysObjectId 校验符合 oid 格式 / 唯一
        if (StrUtil.isNotEmpty(model.getSysObjectId())) {
            // format
            try {
            	SimpleOIDTextFormat.parseOID(model.getSysObjectId());
            } catch (Exception e) {
            	throw new NZException(RCode.ASSET_MODEL_OID_INCORRECT);
			}

            // only
            List<AssetModel> tmplList = this.list(new QueryWrapper<AssetModel>().lambda().eq(AssetModel::getSysObjectId, model.getSysObjectId()));

            flag = false;
            // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
            if ((tmplList == null || tmplList.size() == 0) || (tmplList != null && tmplList.size() == 1 && tmplList.get(0).getId().equals(model.getId()))) {
                flag = true;
            }

            if (!flag) {
                throw new NZException(RCode.ASSET_MODEL_OID_DUPLICATE);
            }
        }
        //检查tsgAppliance
        if (model.getTsgAppliance() == null) model.setTsgAppliance(0);

    }

    @Override
    public Integer saveModel(AssetModel model) {
        // 1. 校验
        model.setName(StrUtil.trim(model.getName()));
        this.validateAssetModelInfo(model);

        // 保存
        this.save(model);
        return model.getId();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateModel(AssetModel model) {
        // 1. 校验
        model.setName(StrUtil.trim(model.getName()));
        this.validateAssetModelInfo(model);

        AssetModel oldModel = this.getById(model.getId());
        //被引用的model 不允许修改type
        if(!ToolUtil.equals(oldModel.getTypeId(), model.getTypeId())){
            int assetCount = assetAssetService.count(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getModelId, oldModel.getId()));
            if(assetCount > 0){
                throw new NZException(RCode.ASSET_MODEL_CONTAINS_CAN_NOT_CHANGE);
            }
        }

        // 保存
        this.updateById(model);

        // 如果修改了Model, 处理关联 charts 变化
        if (!oldModel.getBrandId().equals(model.getBrandId())) {
            assetDao.updateBrandIdByModelId(model.getId(), model.getBrandId());
        }
        return model.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R delete(Integer force, Integer... ids) {
        boolean forceData = false;
        List resultList = Tool.ListUtil.list(true);

        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        List<AssetModel> delAssetModels = this.list(new LambdaQueryWrapper<AssetModel>().in(AssetModel::getId, idList));
        if (Tool.CollUtil.isEmpty(delAssetModels)) {
            return  R.ok().putData("list", resultList);
        }

        for (AssetModel modelEntity : delAssetModels) {
            Map<Object, Object> relObjectInfoMap = this.getAssetModelDelRelObjectInfoMap(modelEntity);
            if (Tool.CollUtil.isNotEmpty((List) relObjectInfoMap.get("relates"))) forceData = true;
            resultList.add(relObjectInfoMap);
        }

        force = Tool.ObjectUtil.defaultIfNull(force, 0);
        switch (force) {
            case 0: {// 0：存在关联数据时，删除失败
                if (forceData) {
                    return R.error(RCode.ASSET_MODEL_RELOBJ_CAN_NOT_REMOVE).putData("list", resultList);
                }
            }
            case 1:  // 1：存在关联数据时，同时删除关联数据
                break;
            default:
                throw new NZException(RCode.ASSET_FORCE_INVALIDE);
        }

        // 删除自身和关联内容
        // model
        this.removeByIds(idList);

        // asset & endpoint
        List<AssetAsset> assetList = assetAssetService.list(new LambdaQueryWrapper<AssetAsset>().in(AssetAsset::getModelId, idList));
        if (Tool.CollUtil.isNotEmpty(assetList)) {
            Integer[] removeAssetIdList = assetList.stream().map(AssetAsset::getId).toArray(n -> new Integer[n]);
            assetAssetService.deleteAsset(1, removeAssetIdList);
        }

        // starred
        starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getType, "assetModel")
                .in(SysUserStarredEntity::getTid, idList)
        );
        return R.ok().putData("list", resultList);
    }

    /**
     * 获取 model 删除提示关联信息：
     *      asset
     *        endpoint
     * @return
     */
    private Map<Object, Object> getAssetModelDelRelObjectInfoMap(AssetModel model) {
        List<Map> relatesList = Tool.ListUtil.list(true);

        // asset & endpoint
        List<AssetAsset> assetList = assetAssetService.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getModelId, model.getId()));
        if (Tool.CollUtil.isNotEmpty(assetList)) {
            List<Integer> assetIdList = assetList.stream().map(AssetAsset::getId).collect(Collectors.toList());
            List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().in(MonitorEndpoint::getAssetId, assetIdList));

            for (AssetAsset asset : assetList) {
                List<MonitorEndpoint> endpointListByAssetId = endpointList.stream().filter(endpoint -> asset.getId().equals(endpoint.getAssetId())).collect(Collectors.toList());
                List<Map> relatesEndpointList = Tool.ListUtil.list(true);
                for (MonitorEndpoint endpoint : endpointListByAssetId) {
                    Map<Object, Object> map = Tool.MapUtil.builder()
                            .put("id", endpoint.getId())
                            .put("name", endpoint.getName())
                            .put("type", "endpoint")
                            .build();
                    relatesEndpointList.add(map);
                }

                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", asset.getId())
                        .put("name", asset.getName())
                        .put("type", "asset")
                        .put("relates", relatesEndpointList)
                        .build();
                relatesList.add(map);
            }
        }

        Map<Object, Object> delRelObjectMap = Tool.MapUtil.builder()
                .put("id", model.getId())
                .put("name", model.getName())
                .put("type", "model")
                .put("relates", relatesList)
                .build();

        return delRelObjectMap;
    }

    @Override
    public void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_MODEL_EXPORT_HEADER);
        String fileName = "AssetModelTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
    }

    @Override
    public Map importAssetModelData(MultipartFile multipartFile, Map<String, Object> params) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_MODEL_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        // 表头信息 国际化之后的
        List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

        // 返回统计信息
        Map statMap = this.saveAssetModelsFromImportDataList(importDataList, headerInfos, params);
        return statMap;
    }

    private Map saveAssetModelsFromImportDataList(List<Map<Integer, String>> importDataList, List<String> headerInfos, Map<String, Object> params) {
        String seq = StrUtil.uuid();
        params.put("seq", seq);
        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

        // 创建导入校验对象
        ImportValidateUtil validateUtil = ImportValidateUtil.builder()
                .existed(existed).headerInfos(headerInfos)
                .build();
        Integer addNum = 0, updateNum = 0;
        // 记录导入失败内容
        List<Map<Integer, String>> failContentList = ListUtil.list(true);
        for (int i = 0; i < importDataList.size(); i++) {
            int lineNum = i + 1;
            Map saveEntitys = Tool.MapUtil.newHashMap();
            Map<Integer, String> dataMap = importDataList.get(i);

            String modelName = dataMap.get(AssetModelImportEnum.NAME.getIndex());
            validateUtil.validateRequired(modelName, lineNum, AssetModelImportEnum.NAME.getIndex());

            // 自动添加
            String brandName = dataMap.get(AssetModelImportEnum.BRAND.getIndex());
            validateUtil.validateRequired(brandName, lineNum, AssetModelImportEnum.BRAND.getIndex());
            Boolean brandExist = validateUtil.validateRelObjExits("asset_brand", "name", brandName, true, lineNum, AssetModelImportEnum.BRAND.getIndex());
            if (!brandExist) {
                AssetBrand brand = new AssetBrand();
                brand.setName(brandName);
                saveEntitys.put("brand", brand);
            }
            if (BooleanUtil.and(brandExist, StrUtil.isNotEmpty(modelName))) {
                // brand 存在，进行名称校验
                AssetBrand brand = brandService.getOne(new LambdaQueryWrapper<AssetBrand>().eq(AssetBrand::getName, brandName));
                // 索引重复 & error or ignore
                AssetModel m = this.getOne(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getBrandId, brand.getId()).eq(AssetModel::getName, modelName));
                if (Tool.ObjectUtil.isNotEmpty(m)) {
                    if (StrUtil.equals("0", existed)) {
                        validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(AssetModelImportEnum.NAME.getIndex()));
                    } else if (StrUtil.equals("2", existed)) {
                        validateUtil.ignoreThisRow(lineNum, AssetModelImportEnum.NAME.getIndex());
                        failContentList.add(dataMap);
                        continue;
                    }
                }
            }

            String chartNames = dataMap.get(AssetModelImportEnum.CHART_TEMPLATES.getIndex());
            StringBuilder relChartTemplateIds = new StringBuilder();
            if (StrUtil.isNotEmpty(chartNames)) {
                // model 只能选择 asset 类型且 groupId = 0 的图表
                List<VisualChart> assetChartTemplateList = visualChartService.list(new LambdaQueryWrapper<VisualChart>()
                        .eq(VisualChart::getPanelId, 0).eq(VisualChart::getGroupId, 0)
                        .eq(VisualChart::getVarType, Constant.ChartVariableType.ASSET.getValue()));

                StrUtil.split(chartNames, StrUtil.C_COMMA).stream().forEach(chartName -> {
                    List<Integer> chartIdList = assetChartTemplateList.stream()
                            .filter(visualChart -> StrUtil.equals(visualChart.getName(), chartName))
                            .map(VisualChart::getId).collect(Collectors.toList());
                    if (Tool.CollUtil.isEmpty(chartIdList)) {
                        validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_RELATED_OBJ_NOT_EXIST.setParam(AssetModelImportEnum.CHART_TEMPLATES.getIndex()));
                    } else if (chartIdList.size() > 1) {
                        validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(AssetModelImportEnum.CHART_TEMPLATES.getIndex()));
                    } else if (chartIdList.size() == 1) {
                        relChartTemplateIds.append(StrUtil.concat(true, chartIdList.get(0).toString(), ","));
                    }
                });
            }

            String typeName = dataMap.get(AssetModelImportEnum.ASSET_TYPE_NAME.getIndex());
            validateUtil.validateRequired(typeName, lineNum, AssetModelImportEnum.ASSET_TYPE_NAME.getIndex());
            validateUtil.validateRelObjExits("asset_type_conf", "name", typeName, false, lineNum, AssetModelImportEnum.ASSET_TYPE_NAME.getIndex());

            String oid = dataMap.get(AssetModelImportEnum.OID.getIndex());
            if (StrUtil.isNotEmpty(oid)) {
                validateUtil.validateSnmpOID(oid, lineNum, AssetModelImportEnum.OID.getIndex());
                validateUtil.validateDuplicate("asset_model", "sys_object_id", oid, modelName, lineNum, AssetModelImportEnum.OID.getIndex());
            }

            // 遇到错误是否继续导入，可选值： 0：不继续    1：继续    默认值：1
            String ignoreError = Tool.ObjectUtil.defaultIfEmpty((String) params.get("ignoreError"), "1");
            List<Map<String, String>> errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
            if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
                // 结束本次导入，自导入行开始 到结尾数据行，提示错误信息
                for (int j = i; j < importDataList.size(); j++) {
                    validateUtil.recordImportAttrErr(j + 1, RCode.IMPORT_ERROR_STOP);
                    failContentList.add(importDataList.get(j));
                }
                break;
            }

            if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
                String remark = dataMap.get(AssetModelImportEnum.DESCRIPTION.getIndex());
                AssetTypeConf typeConf = typeConfService.getOne(new LambdaQueryWrapper<AssetTypeConf>().eq(AssetTypeConf::getName, typeName));

                AssetModel model = new AssetModel();
                model.setRemark(remark);
                model.setName(modelName);
                model.setSysObjectId(oid);
                model.setTypeId(typeConf.getId());
                model.setChartIds(StrUtil.isNotEmpty(relChartTemplateIds) ? relChartTemplateIds.substring(0, relChartTemplateIds.length() - 1) : "");
                if (saveEntitys.get("brand") == null) {
                    AssetBrand brand = brandService.getOne(new LambdaQueryWrapper<AssetBrand>().eq(AssetBrand::getName, brandName));
                    model.setBrandId(brand.getId());
                }
                saveEntitys.put("model", model);

                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(new DefaultTransactionDefinition());
                try {
                    this.saveOrUpdateRowData(saveEntitys, params);
                    dataSourceTransactionManager.commit(transactionStatus);
                    // save or update
                    Object saveFlag = params.get("save");
                    if (ObjectUtil.isNotNull(saveFlag)) {
                        addNum++;
                    } else {
                        updateNum++;
                    }
                } catch (Exception e) {
                    failContentList.add(dataMap);
                    dataSourceTransactionManager.rollback(transactionStatus);
                    validateUtil.recordImportAttrErr(lineNum, RCode.ERROR);
                    log.error("Asset model import roll back the transaction failure", e);
                }
            } else {
                failContentList.add(dataMap);
            }
        }
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_MODEL_EXPORT_HEADER);
        String failedContentFileBase64Str = basicImportAndExportServices.getFailedContentFileBase64Str(failContentList, sysHeaderJsonStr, Constant.ImportFileType.EXCEL.getValue());
        //  返回统计信息
        Map<Object, Object> statMap = Tool.MapUtil.builder()
                .put("seq", seq)
                .put("totalNum", importDataList.size())
                .put("successNum", addNum + updateNum)
                .put("failNum", failContentList.size())
                .put("addNum", addNum)
                .put("updateNum", updateNum)
                .put("failDetail", validateUtil.getImportErrMsg())
                .put("failContent", failedContentFileBase64Str)
                .build();
        return statMap;
    }

    private void saveOrUpdateRowData(Map saveEntitys, Map<String, Object> params) {
        MapUtil.removeAny(params, "save", "update");
        String seq = params.get("seq").toString();

        AssetBrand brand = (AssetBrand) saveEntitys.get("brand");
        if (brand != null) {
            brand.setSeq(seq);
            brandService.save(brand);
        }

        AssetModel model = (AssetModel) saveEntitys.get("model");
        model.setSeq(seq);
        model.setUsize(1);
        if (model.getBrandId() == null && brand != null) {
            model.setBrandId(brand.getId());
        }

        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore   在此只需要处理更新即可
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");
        AssetModel oldModel = this.getOne(new LambdaQueryWrapper<AssetModel>()
                .eq(AssetModel::getName, model.getName())
                .eq(AssetModel::getBrandId, model.getBrandId()));
        if (StrUtil.equals("1", existed) && oldModel != null) {
            params.put("update", true);
            // update
            model.setId(oldModel.getId());
            this.updateById(model);

            // 是否同步更新关联的 dashboard，可选值： 0：不同步  1：同步  默认值：1
            String syncDashboard = Tool.ObjectUtil.defaultIfEmpty((String) params.get("syncDashboard"), "1");
            if (StrUtil.equals("1", syncDashboard)) {
                Map<String, Object> syncChartParamMap = Tool.MapUtil.newHashMap();
                syncChartParamMap.put("models", ListUtil.of(model.getId()));
                visualChartService.syncTmpl(syncChartParamMap);
            }
        } else {
            params.put("save", true);
            // insert
            this.save(model);
        }
    }

    @Override
    public void export(HttpServletResponse response, Map<String, Object> params) throws IOException {
        String maxLine = sysConfigService.getValue(Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE);
        long exportMaxLine = Integer.parseInt(StrUtil.emptyToDefault(maxLine, "10000"));
        // 导出最大条数限制
        Object limit = params.get(Constant.LIMIT) == null ? Constant.PAGESIZE : params.get(Constant.LIMIT);
        Long pageSize = Long.parseLong(limit.toString());
        if (pageSize == -1) {
            // 导出所有直接限制 pageSize
            params.put(Constant.PAGE, "1");
            params.put(Constant.LIMIT, Long.toString(exportMaxLine));
        } else if (pageSize > exportMaxLine) {
            // 分页超过最大限制 则设置最大限制为 系统配置值，和 pageSize == -1 判断区别在于 pageNo
            params.put(Constant.LIMIT, Long.toString(exportMaxLine));
        }

        PageUtils page = this.queryPage(params);
        List<AssetModel> list = (List<AssetModel>) page.getList();

        List<List<String>> exportDataList = this.handleExportDataList(list);

        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_MODEL_EXPORT_HEADER);
        String fileName = "AssetModels";
        String formatStr = params.get("format").toString();
        basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
    }

    private List<List<String>> handleExportDataList(List<AssetModel> list) {
        List<List<String>> exportDataList = new ArrayList<>();
        for (AssetModel model : list) {
            List<String> tempList = new ArrayList<>();
            tempList.add(model.getName());
            tempList.add(model.getBrand().getName());
            String chartIds = model.getChartIds();
            String chartNames = "";
            if (StrUtil.isNotEmpty(chartIds)) {
                List<String> chartIdList = Arrays.asList(chartIds.split(","));
                for (String chartIdStr : chartIdList) {
                    VisualChart chartServiceById = visualChartService.getById(chartIdStr);
                    if (chartServiceById != null) {
                        chartNames += chartServiceById.getName() + ",";
                    }
                }
                chartNames = StrUtil.isNotEmpty(chartNames) ? chartNames.substring(0, chartNames.length() - 1) : chartNames;
            }
            tempList.add(chartNames);
            tempList.add(ObjectUtil.defaultIfNull(model.getRemark(), ""));
            AssetTypeConf typeConf = typeConfService.getById(model.getTypeId());
            tempList.add(typeConf.getName());
            tempList.add(model.getSysObjectId());
            exportDataList.add(tempList);
        }
        return exportDataList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelImport(String seq) {
        this.remove(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getSeq, seq));
        brandService.remove(new LambdaQueryWrapper<AssetBrand>().eq(AssetBrand::getSeq, seq));
    }

}
