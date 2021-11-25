package com.nis.modules.panel.service.impl;

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.bean.BeanUtil;
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
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.panel.dao.VisualExpreTmplDao;
import com.nis.modules.panel.entity.VisualExpressionTmpl;
import com.nis.modules.panel.service.VisualExpreTmplService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 表达式模板 service 实现类
 *
 * @author shizhendong
 * @date 2021-03-11
 */
@Service
public class VisualExpreTmplServiceImpl extends ServiceImpl<VisualExpreTmplDao, VisualExpressionTmpl> implements VisualExpreTmplService {

    private static Log logger = Log.get();

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private DcService dcService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private MonitorModuleService moduleService;

    @Autowired
    private MonitorProjectService projectService;

    @Autowired
    private MonitorEndpointService endpointService;

    @Override
    public VisualExpressionTmpl queryTmplInfoById(Integer id) {
    	VisualExpressionTmpl tmpl = this.getById(id);
        if (ToolUtil.isEmpty(tmpl)) {
            throw new NZException(RCode.EXPRETMPL_NOT_FOUND);
        }
        String[] vars = this.parseExpre(tmpl.getExpression());
        if (Tool.ObjectUtil.isNotEmpty(vars)) {
            List<String> list = Arrays.stream(vars).distinct().collect(Collectors.toList());
            vars = list.toArray(new String[list.size()]);
            tmpl.setVars(vars);
        }
        return tmpl;
    }

    private String[] parseExpre(String expre) {
        Pattern pattern = Pattern.compile("(\\{\\{)(.+?)(\\}\\})");
        Matcher matcher = pattern.matcher(expre);

        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group();
            group = group.replaceAll("(\\{\\{)|(\\}\\})", "");
            list.add(group);
        }
        String[] vars = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            vars[i] = list.get(i);
        }
        return vars;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
    	String ids = (String) params.get("ids");
    	List<String> asList =null;
    	if(StrUtil.isNotEmpty(ids)) {
    		asList = Arrays.asList(ids.split(","));
    	}
        LambdaQueryWrapper<VisualExpressionTmpl> queryWrapper = new LambdaQueryWrapper<VisualExpressionTmpl>()
                .in(ToolUtil.isNotEmpty(params.get("ids")), VisualExpressionTmpl::getId, asList)
                .like(ToolUtil.isNotEmpty(params.get("name")), VisualExpressionTmpl::getName, params.get("name"))
                .eq(ToolUtil.isNotEmpty(params.get("gname")), VisualExpressionTmpl::getGname, params.get("gname"));

        IPage page = this.page(new Query(VisualExpressionTmpl.class).getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    @Override
    public List<String> queryTmplsByGname(String gname) {
        List<VisualExpressionTmpl> list = this.list(new LambdaQueryWrapper<VisualExpressionTmpl>().like(ToolUtil.isNotEmpty(gname), VisualExpressionTmpl::getGname, gname).orderByAsc(VisualExpressionTmpl::getGname));
        List<String> gnameList = list.stream().map(VisualExpressionTmpl::getGname).distinct().collect(Collectors.toList());
        return gnameList;
    }

    private void validateExpressionTmplInfo(VisualExpressionTmpl tmpl) {
        // 1. 名称重复
        List<VisualExpressionTmpl> list = this.list(new LambdaQueryWrapper<VisualExpressionTmpl>().eq(VisualExpressionTmpl::getGname, ToolUtil.isEmpty(tmpl.getGname()) ? "default" : tmpl.getGname()).eq(VisualExpressionTmpl::getName, tmpl.getName()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(tmpl.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.EXPRETMPL_NAME_DUPLICATE);
        }

        // 2. 校验 build in 值
        List<Integer> switchList = Arrays.asList(new Integer[]{1, 0});
        if (ToolUtil.isNotEmpty(tmpl.getBuildIn()) && !switchList.contains(tmpl.getBuildIn())) {
            throw new NZException(RCode.EXPRETMPL_BUILDIN_INCORRECT);
        }
    }

    @Override
    public Integer saveTmpl(VisualExpressionTmpl tmpl) {
        // 1. 校验
        this.validateExpressionTmplInfo(tmpl);

        if (StringUtils.isEmpty(tmpl.getGname())) {
            tmpl.setGname("default");
        }

        // 保存
        this.save(tmpl);
        return tmpl.getId();
    }

    @Override
    public Integer updateTmpl(VisualExpressionTmpl tmpl) {
        // 1. 校验
        this.validateExpressionTmplInfo(tmpl);

        if (StringUtils.isEmpty(tmpl.getGname())) {
            tmpl.setGname("default");
        }

        // 保存
        this.updateById(tmpl);
        return tmpl.getId();
    }

    @Override
    public void delete(Integer... ids) {
        List<VisualExpressionTmpl> list = this.list();

        // 1. 内置的 配置不允许删除
        List<Integer> buildInIdList = list.stream().filter(assetTypeConf -> assetTypeConf.getBuildIn().equals(1)).map(VisualExpressionTmpl::getId).collect(Collectors.toList());
        buildInIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(buildInIdList)) {
            throw new NZException("These expression templates are built-in and cannot be deleted. Details: " + buildInIdList.toString(), RCode.EXPRETMPL_BUILDIN_CAN_NOT_REMOVE.getCode());
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }

    /**
     * 表达式渲染
     * @param tmpl ExpressionTmpl 对象 ，
     *             expression为表达式内容
     *             varsVal 是替换内容，其中key asset.datacenter project module endpoint ，value 为 id
     * @return
     */
    @Override
    public String renderExpression(VisualExpressionTmpl tmpl) {
        String expression = tmpl.getExpression();
        Map varsVal = tmpl.getVarsVal();

        String[] vars = this.parseExpre(expression);
        for (String var : vars) {
            Integer assetId = var.startsWith("asset.") ? Integer.parseInt(varsVal.get(var).toString()) : null;
            Integer dcId = var.startsWith("datacenter.") ? Integer.parseInt(varsVal.get(var).toString()) : null;
            Integer projectId = var.startsWith("project.") ? Integer.parseInt(varsVal.get(var).toString()) : null;
            Integer moduleId = var.startsWith("module.") ? Integer.parseInt(varsVal.get(var).toString()) : null;
            Integer endpointId = var.startsWith("endpoint.") ? Integer.parseInt(varsVal.get(var).toString()) : null;

            AssetAsset asset = ToolUtil.isNotEmpty(assetId) ? assetService.getById(assetId) : null;
            Dc dc = ToolUtil.isNotEmpty(dcId) ? dcService.getById(dcId) : null;
            MonitorProject project = ToolUtil.isNotEmpty(projectId) ? projectService.getById(projectId) : null;
            MonitorModule module = ToolUtil.isNotEmpty(moduleId) ? moduleService.getById(moduleId) : null;
            MonitorEndpoint endpoint = ToolUtil.isNotEmpty(endpointId) ? endpointService.getById(endpointId) : null;

            Object renderValue = null;
            boolean flag = true;
            if (var.startsWith("asset.") || var.startsWith("datacenter.") || var.startsWith("project.") || var.startsWith("module.") || var.startsWith("endpoint.")) {
                flag = false;
            }
            renderValue = (var.startsWith("asset.") && ToolUtil.isNotEmpty(asset) && renderValue == null) ? BeanUtil.getProperty(asset, var.replaceAll("asset.", "")) : renderValue;
            renderValue = (var.startsWith("datacenter.") && ToolUtil.isNotEmpty(dc) && renderValue == null) ? BeanUtil.getProperty(dc, var.replaceAll("datacenter.", "")) : renderValue;
            renderValue = (var.startsWith("project.") && ToolUtil.isNotEmpty(project) && renderValue == null) ? BeanUtil.getProperty(project, var.replaceAll("project.", "")) : renderValue;
            renderValue = (var.startsWith("module.") && ToolUtil.isNotEmpty(module) && renderValue == null) ? BeanUtil.getProperty(module, var.replaceAll("module.", "")) : renderValue;
            renderValue = (var.startsWith("endpoint.") && ToolUtil.isNotEmpty(endpoint) && renderValue == null) ? BeanUtil.getProperty(endpoint, var.replaceAll("endpoint.", "")) : renderValue;
            renderValue = (renderValue == null && flag) ? varsVal.get(var) : renderValue;
            if (renderValue == null && flag) {
                continue;
            }
            expression = expression.replaceAll("\\{\\{" + var + "\\}\\}", renderValue == null ? "" : renderValue.toString());
        }
        return expression;
    }

    /**
     * 渲染表达式
     * @param expression  表达式
     * @param varsVal     表达式内容 直接进行 key value 渲染
     * @return
     */
    @Override
    public String renderExpression(String expression, Map varsVal) {
        expression = this.generateExpression(expression, varsVal);
        return expression;
    }

    private String generateExpression(String expression, Map varsVal) {
        String[] vars = this.parseExpre(expression);
        for (int i = 0; i < vars.length; i++) {
            Object o = varsVal.get(vars[i]);
            if (ToolUtil.isEmpty(o)) {
                continue;
            }
            expression = expression.replaceAll("\\{\\{" + vars[i] + "\\}\\}", o.toString());
        }
        return expression;
    }

    @Override
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_EXPRETMPL_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        if (map.get(language) != null) {
            exportHeaderMap = (Map) map.get(language);
        } else {
            throw new NZException(RCode.EXPRETMPL_EXPORT_HEADER_LANGUAGE_ERROR);
        }

        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "Expression Templates", true);
            // 响应为流
            String fileName = "ExpressionTemplate.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            logger.error("表达式模板生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            logger.error("获取表达式导入模板失败，表头语言是：" + language + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }

    @Override
    public Map importExpreTmplsFromExcel(MultipartFile excelFile, String language) {
        Workbook work = ExcelUtils.getWorkBookFromMultipartFile(excelFile);

        Sheet sheet = work.getSheetAt(0);

        // 表头校验
        SysConfigEntity config = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_EXPRETMPL_EXPORT_HEADER));
        HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
        boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), map.values());
        if (!headerFlag) {
            throw new NZException(RCode.ASSET_EXPORT_HEADER_TEMPLATE_ERROR);
        }

        Map<String, String> exportHeader = (Map) map.get(language);
        List<String> headerInfos = new ArrayList(exportHeader.keySet());

        // 预先查询出校验数据 避免循环查表
        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = this.saveTmplsDataFromSheet(sheet, failDetails, headerInfos, seqUUID);
        int totalNum = sheet.getLastRowNum() - sheet.getFirstRowNum();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    private int saveTmplsDataFromSheet(Sheet sheet, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
        List<VisualExpressionTmpl> tmpls = this.list();
        List<String> ganmes = tmpls.stream().map(VisualExpressionTmpl::getGname).distinct().collect(Collectors.toList());
        Map<String, List<String>> gnameAndTmplNameMap = new HashMap();
        ganmes.stream().forEach(gname -> {
            List<String> tmplNames = tmpls.stream().filter(tmpl -> tmpl.getGname().equals(gname)).map(VisualExpressionTmpl::getName).collect(Collectors.toList());
            gnameAndTmplNameMap.put(gname, tmplNames);
        });

        int successNum = 0;
        Row row;
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i);
            if (row == null) continue;

            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            String name = ExcelUtils.getCellValue(row.getCell(0));
            String gname = ExcelUtils.getCellValue(row.getCell(1));
            gname = StringUtils.isEmpty(gname) ? "default" : gname;

            if (StringUtils.isEmpty(name)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.EXPRETMPL_NAME_ISNULL.getMsg());
            } else {
                // 校验 名称是否重复
                List<String> nameLisInGroup = gnameAndTmplNameMap.get(gname);
                if (CollectionUtils.isNotEmpty(nameLisInGroup) && nameLisInGroup.contains(name)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.EXPRETMPL_NAME_DUPLICATE.getMsg());
                }
            }
            String expression = ExcelUtils.getCellValue(row.getCell(2));
            if (StringUtils.isEmpty(expression)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.EXPRETMPL_EXPRESSION_ISNULL.getMsg());
            }
            if (saveFlag) {
                String remark = ExcelUtils.getCellValue(row.getCell(3));
                VisualExpressionTmpl tmpl = new VisualExpressionTmpl();
                tmpl.setName(name);
                tmpl.setGname(gname);
                tmpl.setExpression(expression);
                tmpl.setRemark(remark);
                tmpl.setSeq(seqUUID);
                try {
                    this.save(tmpl);
                    successNum++;
                    List<String> nameLisInGroup = gnameAndTmplNameMap.get(gname);
                    nameLisInGroup = CollectionUtils.isEmpty(nameLisInGroup) ? new ArrayList<>() : nameLisInGroup;
                    nameLisInGroup.add(name);
                    gnameAndTmplNameMap.put(gname, nameLisInGroup);
                } catch (RuntimeException e) {
                    logger.error(String.format("表达式模板导入保存失败,表达式信息是：%s,错误信息是：%s", tmpl.toString(), e));
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", RCode.EXPRETMPL_IMPORT_ERROR.getMsg());
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
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_EXPRETMPL_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);

        SysConfigEntity maxLine = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE));
        long exportMaxLine = 10000;
		// 导出最大条数限制
        if(ObjectUtil.isNotEmpty(maxLine) && ObjectUtil.isNotEmpty(maxLine.getParamValue())) {
        	exportMaxLine = Integer.parseInt(maxLine.getParamValue());
        }
        Long pageSize = Long.parseLong((String)params.get(Constant.LIMIT));
		if(ObjectUtil.isEmpty(pageSize) || pageSize==-1 || pageSize>exportMaxLine) {
			params.put(Constant.LIMIT,Long.toString(exportMaxLine));
		}
        PageUtils page = this.queryPage(params);
        List<VisualExpressionTmpl> list = (List<VisualExpressionTmpl>) page.getList();

        SXSSFWorkbook workbook = null;
        try {
            // 添加标题行 默认语言 en
            String language = (String) params.get("language");
            language = ToolUtil.isEmpty(language) ? "en" : language;

            String sheetName = "Expression Template";
            workbook = ExcelUtils.createExcel07((Map) map.get(language), sheetName, false);
            SXSSFSheet sheet = workbook.getSheet(sheetName);

            this.fillData(list, 1, sheet, workbook);

            // 响应为流
            String fileName = "ExpressionTemplates.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            logger.error("表达式模板导出文件生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            logger.error("表达式模板导出失败，参数是：" + params.toString() + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }

    private void fillData(List<VisualExpressionTmpl> list, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook) {
        Row row;
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        for (VisualExpressionTmpl tmpl : list) {
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(tmpl.getName());
            row.createCell(1).setCellValue(tmpl.getGname());
            row.createCell(2).setCellValue(tmpl.getExpression());
            row.createCell(3).setCellValue(tmpl.getRemark());

            // 设置单元格样式
            ExcelUtils.setDataCellStyle(dataStyle, row, 3);
        }
    }

    @Override
    public void cancelImport(String seq) {
        this.remove(new LambdaQueryWrapper<VisualExpressionTmpl>().eq(VisualExpressionTmpl::getSeq, seq));
    }

}
