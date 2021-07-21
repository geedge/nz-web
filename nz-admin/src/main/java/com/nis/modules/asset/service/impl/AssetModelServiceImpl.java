package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.dao.AssetModelDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetBrand;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetBrandService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
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
    private SysConfigDao sysConfigDao;

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
    		List<VisualChart> charts = visualChartService.list(new QueryWrapper<VisualChart>().lambda().in(VisualChart::getId, Arrays.asList(chartIds.split(","))));
        	model.setCharts(charts);
    	}
    	//获取资产数量
    	List<AssetAsset> list = assetAssetService.list(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getModelId, model.getId()));
    	model.setAssetNum(list.size());
        model.setBrand(brand);
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
        List<AssetModel> modelList = assetModelDao.queryList(page, params);
        for(AssetModel model : modelList) {
        	String chartIds = model.getChartIds();
        	if(!StrUtil.isBlank(chartIds)) {
        		List<VisualChart> charts = visualChartService.list(new QueryWrapper<VisualChart>().lambda().in(VisualChart::getId, Arrays.asList(chartIds.split(","))));
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
    }

    @Override
    public void saveModel(AssetModel model) {
        // 1. 校验
        this.validateAssetModelInfo(model);

        // 保存
        this.save(model);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModel(AssetModel model) {
        // 1. 校验
        this.validateAssetModelInfo(model);

        AssetModel oldModel = this.getById(model.getId());

        // 保存
        this.updateById(model);

        // 如果修改了Model, 处理关联 charts 变化
        if (!oldModel.getBrandId().equals(model.getBrandId())) {
            assetDao.updateBrandIdByModelId(model.getId(), model.getBrandId());
        }
    }

    @Override
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
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        SysConfigEntity configEntity = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MODEL_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        if (map.get(language) != null) {
            exportHeaderMap = (Map) map.get(language);
        } else {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "Asset model", true);
            String fileName = "AssetModelExportTemplate.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("Asset model 模板生成失败，错误信息是：" + e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("获取 Asset model 导入模板失败，表头语言是：" + language + " ，错误信息是：" + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
    }

    @Override
    public Map importAssetModelFromExcel(MultipartFile excelFile, String language) {
        Workbook work = ExcelUtils.getWorkBookFromMultipartFile(excelFile);

        Sheet sheet = work.getSheetAt(0);

        // 表头校验
        SysConfigEntity config = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MODEL_EXPORT_HEADER));
        HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
        boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), map.values());
        if (!headerFlag) {
            throw new NZException(RCode.EXCELFILE_HEADER_TEMPLATE_ERROR);
        }

        Map<String, String> exportHeader = (Map) map.get(language);
        List<String> headerInfos = new ArrayList(exportHeader.keySet());

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = this.saveAssetModelsFromSheet(sheet, failDetails, headerInfos, seqUUID);
        int totalNum = sheet.getLastRowNum() - sheet.getFirstRowNum();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    private int saveAssetModelsFromSheet(Sheet sheet, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
        int successNum = 0;
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;

            String modelName = ExcelUtils.getCellValue(row.getCell(0));
            if (StringUtils.isEmpty(modelName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.ASSET_MODELNAME_ISNULL.getMsg());
            }

            String brandName = ExcelUtils.getCellValue(row.getCell(1));
            if (StringUtils.isEmpty(brandName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.ASSET_BRAND_NAME_ISNULL.getMsg());
            }

            String chartNames = ExcelUtils.getCellValue(row.getCell(2));
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

            String remark = ExcelUtils.getCellValue(row.getCell(3));
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
    public void export(HttpServletResponse response, Map<String, Object> params) {
        SysConfigEntity configEntity = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MODEL_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);

        PageUtils page = this.queryPage(params);
        List<AssetModel> list = (List<AssetModel>) page.getList();

        SXSSFWorkbook workbook = null;
        try {
            // 添加标题行 默认语言 en
            String language = (String) params.get("language");
            language = ToolUtil.isEmpty(language) ? "en" : language;

            String sheetName = "Asset model";
            workbook = ExcelUtils.createExcel07((Map) map.get(language), sheetName, false);
            SXSSFSheet sheet = workbook.getSheet(sheetName);

            this.fillData(list, 1, sheet, workbook);

            // 响应为流
            String fileName = "AssetModels.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("Asset model 导出文件生成失败，错误信息是：" + e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("Asset model 导出失败，参数是：" + params.toString() + " ，错误信息是：" + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    private void fillData(List<AssetModel> list, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook) {
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        for (AssetModel model : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(model.getName());
            row.createCell(1).setCellValue(model.getBrand().getName());
            String chartIds = model.getChartIds();
            String chartNames = "";
            if (StrUtil.isNotEmpty(chartIds)) {
                List<VisualChart> chartList = visualChartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, 0).in(VisualChart::getId, Arrays.asList(chartIds.split(","))));
                for (VisualChart chart : chartList) {
                    chartNames += chart.getName() + ",";
                }
                chartNames = StrUtil.isNotEmpty(chartNames) ? chartNames.substring(0, chartNames.length() - 1) : chartNames;
            }
            row.createCell(2).setCellValue(chartNames);
            row.createCell(3).setCellValue(ObjectUtil.defaultIfNull(model.getRemark(), ""));

            // 设置单元格样式
            ExcelUtils.setDataCellStyle(dataStyle, row, 3);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelImport(String seq) {
        this.remove(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getSeq, seq));
        brandService.remove(new LambdaQueryWrapper<AssetBrand>().eq(AssetBrand::getSeq, seq));
    }

}
