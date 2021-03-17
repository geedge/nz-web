package com.nis.modules.panel.service.impl;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.panel.dao.ExpreTmplDao;
import com.nis.modules.panel.entity.ExpressionTmpl;
import com.nis.modules.panel.service.ExpreTmplService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
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
public class ExpreTmplServiceImpl extends ServiceImpl<ExpreTmplDao, ExpressionTmpl> implements ExpreTmplService {

    private static Log logger = Log.get();

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    public ExpressionTmpl queryTmplInfoById(Integer id) {
        ExpressionTmpl tmpl = this.getById(id);
        if (ToolUtil.isEmpty(tmpl)) {
            throw new NZException(RCode.EXPRETMPL_NOT_FOUND);
        }
        tmpl.setVars(this.parseExpre(tmpl.getExpression()));
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
        LambdaQueryWrapper<ExpressionTmpl> queryWrapper = new LambdaQueryWrapper<ExpressionTmpl>()
                .eq(ToolUtil.isNotEmpty(params.get("id")), ExpressionTmpl::getId, params.get("id"))
                .like(ToolUtil.isNotEmpty(params.get("name")), ExpressionTmpl::getName, params.get("name"))
                .eq(ToolUtil.isNotEmpty(params.get("gname")), ExpressionTmpl::getGname, params.get("gname"));

        IPage<ExpressionTmpl> page = this.page(new Query<ExpressionTmpl>().getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    @Override
    public List<String> queryTmplsByGname(String gname) {
        List<ExpressionTmpl> list = this.list(new LambdaQueryWrapper<ExpressionTmpl>().like(ToolUtil.isNotEmpty(gname), ExpressionTmpl::getGname, gname).orderByAsc(ExpressionTmpl::getGname));
        List<String> gnameList = list.stream().map(ExpressionTmpl::getGname).distinct().collect(Collectors.toList());
        return gnameList;
    }

    private void validateExpressionTmplInfo(ExpressionTmpl tmpl) {
        // 1. 名称重复
        List<ExpressionTmpl> list = this.list(new LambdaQueryWrapper<ExpressionTmpl>().eq(ExpressionTmpl::getGname, ToolUtil.isEmpty(tmpl.getGname()) ? "default" : tmpl.getGname()).eq(ExpressionTmpl::getName, tmpl.getName()));
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
    public Integer saveTmpl(ExpressionTmpl tmpl) {
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
    public Integer updateTmpl(ExpressionTmpl tmpl) {
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
        List<ExpressionTmpl> list = this.list();

        // 1. 内置的 配置不允许删除
        List<Integer> buildInIdList = list.stream().filter(assetTypeConf -> assetTypeConf.getBuildIn().equals(1)).map(ExpressionTmpl::getId).collect(Collectors.toList());
        buildInIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(buildInIdList)) {
            throw new NZException("These expression templates are built-in and cannot be deleted. Details: " + buildInIdList.toString(), RCode.EXPRETMPL_BUILDIN_CAN_NOT_REMOVE.getCode());
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }

    @Override
    public String renderExpression(ExpressionTmpl tmpl) {
        String expression = tmpl.getExpression();
        Map varsVal = tmpl.getVarsVal();

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
        } catch (Exception e) {
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

    private Workbook getWorkBookFromFile(MultipartFile excelFile) {
        if (!ExcelUtils.validateFileType(excelFile.getOriginalFilename())) {
            throw new NZException(RCode.EXPRETMPL_IMPORTFILE_TYPE);
        }

        Workbook work = null;
        try {
            work = ExcelUtils.getWork(excelFile.getOriginalFilename(), excelFile.getInputStream());
        } catch (IOException e) {
            logger.error("解析导入表达式模板文件格式有误", e);
            throw new NZException(RCode.EXPRETMPL_IMPORTFILE_FAILED);
        } finally {
            if (work != null) {
                IOUtils.closeQuietly(work);
            }
        }
        return work;
    }

    @Override
    public Map importExpreTmplsFromExcel(MultipartFile excelFile, String language) {
        Workbook work = this.getWorkBookFromFile(excelFile);

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
        List<ExpressionTmpl> tmpls = this.list();
        List<String> ganmes = tmpls.stream().map(ExpressionTmpl::getGname).distinct().collect(Collectors.toList());
        Map<String, List<String>> gnameAndTmplNameMap = new HashMap();
        ganmes.stream().forEach(gname -> {
            List<String> tmplNames = tmpls.stream().filter(tmpl -> tmpl.getGname().equals(gname)).map(ExpressionTmpl::getName).collect(Collectors.toList());
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
                ExpressionTmpl tmpl = new ExpressionTmpl();
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
                } catch (Exception e) {
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

        PageUtils page = this.queryPage(params);
        List<ExpressionTmpl> list = (List<ExpressionTmpl>) page.getList();

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
        } catch (Exception e) {
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

    private void fillData(List<ExpressionTmpl> list, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook) {
        Row row;
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        for (ExpressionTmpl tmpl : list) {
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
        this.remove(new LambdaQueryWrapper<ExpressionTmpl>().eq(ExpressionTmpl::getSeq, seq));
    }

}
