package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.dao.AssetModelDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetBrand;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.entity.AssetTypeConf;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetBrandService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.asset.service.AssetTypeConfService;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import com.sun.jmx.snmp.SnmpOid;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
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
    private AssetAssetDao assetDao;

    @Autowired
    private AssetTypeConfService typeConfService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

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
        AssetTypeConf typeConf = typeConfService.getById(model.getTypeId());
        //获取资产数量
    	List<AssetAsset> list = assetAssetService.list(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getModelId, model.getId()));
    	model.setAssetNum(list.size());
        model.setBrand(brand);
        model.setType(typeConf);
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

        // 4. type 是否存在
        AssetTypeConf typeConfObj = typeConfService.getById(model.getTypeId());
        ValidateUtils.is(typeConfObj).notNull(RCode.TYPECONF_NOT_FOUND);

        // 5. sysObjectId 校验符合 oid 格式 / 唯一
        if (StrUtil.isNotEmpty(model.getSysObjectId())) {
            // format
            try {
                new SnmpOid(model.getSysObjectId());
            } catch (IllegalArgumentException e) {
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
    }

    @Override
    public Integer saveModel(AssetModel model) {
        // 1. 校验
        this.validateAssetModelInfo(model);

        // 保存
        this.save(model);
        return model.getId();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateModel(AssetModel model) {
        // 1. 校验
        this.validateAssetModelInfo(model);

        AssetModel oldModel = this.getById(model.getId());

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
    public void delete(Integer... ids) {
        // 1. 已被 asset 引用的型号信息不允许删除
        List<AssetAsset> assetList = assetAssetService.list();
        List<Integer> assetUseModelIdList = assetList.stream().map(AssetAsset::getModelId).distinct().collect(Collectors.toList());

        assetUseModelIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(assetUseModelIdList)) {
            throw new NZException("These models have been used by assets and cannot be deleted. Details: " + assetUseModelIdList.toString(), RCode.ASSET_MODELRELATED_ASSET.getCode());
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }

    @Override
    public void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_MODEL_EXPORT_HEADER);
        String fileName = "AssetModelTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
    }

    @Override
    public Map importAssetModelData(MultipartFile multipartFile) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_MODEL_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = this.saveAssetModelsFromImportDataList(importDataList, failDetails, new ArrayList<>(exportHeaderMap.keySet()), seqUUID);
        int totalNum = importDataList.size();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    private int saveAssetModelsFromImportDataList(List<Map<Integer, String>> importDataList, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
        int successNum = 0;
        for (int i = 0; i < importDataList.size(); i++) {
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            Map<Integer, String> dataMap = importDataList.get(i);

            String modelName = dataMap.get(0);
            if (StringUtils.isEmpty(modelName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.ASSET_MODELNAME_ISNULL.getMsg());
            }

            String brandName = dataMap.get(1);
            if (StringUtils.isEmpty(brandName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.ASSET_BRAND_NAME_ISNULL.getMsg());
            }

            String chartNames = dataMap.get(2);
            StringBuilder relChartTemplateIds = new StringBuilder();
            if (StringUtils.isNotEmpty(chartNames)) {
                String[] split = chartNames.split(",");
                for (String chartName : split) {
                    List<VisualChart> list = visualChartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, 0).eq(VisualChart::getName, chartName));
                    list = list.stream().filter(chart -> chart.getVarType() == 1).collect(Collectors.toList());
                    if (Tool.CollectionUtil.isEmpty(list)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_NOTEXSITS.getMsg());
                    } else if (list.size() == 1) {
                        // 类型为 asset ,且通过名称只关联一个
                        relChartTemplateIds.append(list.get(0).getId());
                        relChartTemplateIds.append(",");
                    } else if (list.size() > 1) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_NAME_DUPLICATE_NOTLINK.getMsg());
                    }
                }
            }

            String remark = dataMap.get(3);

            String typeName = dataMap.get(4);
            Integer typeId = null;
            if (StringUtils.isEmpty(typeName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.TYPECONF_NAME_ISNULL.getMsg());
            } else {
                AssetTypeConf typeConf = typeConfService.getOne(new LambdaQueryWrapper<AssetTypeConf>().eq(AssetTypeConf::getName, typeName.trim()));
                if (typeConf == null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.TYPECONF_NOT_FOUND.getMsg());
                }
                typeId = Tool.ObjectUtil.isEmpty(typeConf) ? null : typeConf.getId();
            }

            String sysObjectIdStr = dataMap.get(5);
            if (StringUtils.isNotEmpty(sysObjectIdStr)) {
                // fotmat
                try {
                    new SnmpOid(sysObjectIdStr);
                } catch (IllegalArgumentException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.ASSET_MODEL_OID_INCORRECT.getMsg());
                }
                // only
                int count = this.count(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getSysObjectId, sysObjectIdStr));
                if(count > 0){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.ASSET_MODEL_OID_DUPLICATE.getMsg());
                }
            }

            if (saveFlag) {
                DefaultTransactionDefinition def = new DefaultTransactionDefinition();
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(def);
                AssetModel model = new AssetModel();
                try {
                    AssetBrand brand = brandService.getOne(new LambdaQueryWrapper<AssetBrand>().eq(AssetBrand::getName, brandName));
                    if (brand == null) {
                        brand = new AssetBrand();
                        brand.setSeq(seqUUID);
                        brand.setName(brandName);
                        brandService.save(brand);
                    }

                    // 名称校验
                    int countByName = this.count(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getBrandId, brand.getId()).eq(AssetModel::getName, modelName));
                    if (countByName > 0) {
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.ASSET_MODELNAME_DUPLICATE.getMsg());
                        dataSourceTransactionManager.commit(transactionStatus);
                        continue;
                    }

                    model.setName(modelName);
                    model.setBrandId(brand.getId());
                    model.setRemark(remark);
                    model.setUsize(1);
                    String chartIds = relChartTemplateIds.toString();
                    model.setChartIds(StrUtil.isNotEmpty(chartIds) ? chartIds.substring(0, chartIds.length() - 1) : "");
                    model.setTypeId(typeId);
                    model.setSysObjectId(StrUtil.isNotEmpty(sysObjectIdStr) ? sysObjectIdStr : StrUtil.EMPTY);
                    model.setSeq(seqUUID);
                    this.save(model);

                    dataSourceTransactionManager.commit(transactionStatus);
                    successNum++;
                } catch (RuntimeException e) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    log.error(String.format("asset model 导入保存失败,实体信息是：%s,错误信息是：%s", JSONObject.toJSONString(model), e.getMessage()), e);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", RCode.EXCELFILE_IMPORT_ERROR.getMsg());
                }
            }
        }
        return successNum;
    }

    private void addFailDetail(int i, List<Map> failDetails, String headerInfo, String msg) {
        Map failDetail = new HashMap(4);
        failDetail.put("lineNo", i);
        failDetail.put("errorMsg", headerInfo + ": " + msg);
        failDetails.add(failDetail);
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
