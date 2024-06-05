package com.nis.modules.sys.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.common.utils.Constant.ImportFileType;
import com.nis.modules.sys.dao.BasicMapper;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysI18nService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.TempFile;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BasicImportAndExportServicesImpl implements BasicImportAndExportServices {

    private static final Log log = Log.get();

    @Autowired
    private SysI18nService sysI18nService;

    @Autowired
    private BasicMapper basicMapper;

    @Override
    public Map<String, String> getI18nHeaderMap(String sysHeaderJsonStr) {
        List<Map> sysHeaderList = JSONObject.parseArray(sysHeaderJsonStr, Map.class);
        Map<String, String> exportHeaderMap = new LinkedHashMap<>();
        for (Map<String, String> header : sysHeaderList) {
            String name = sysI18nService.queryValue(header.get("i18n"));
            String comment = sysI18nService.queryValue(header.get("comment"));
            exportHeaderMap.put(name, comment);
        }
        return exportHeaderMap;
    }

    @Override
    public void getTemplate(String sysHeaderJsonStr, String format, String fileName, HttpServletResponse response) throws IOException {
        Map<String, String> exportHeaderMap = this.getI18nHeaderMap(sysHeaderJsonStr);
        switch (ImportFileType.getInstanceByValue(format)) {
            case XLS:
            case XLSX: {
                SXSSFWorkbook workbook = null;
                try {
                    workbook = ExcelUtils.createSXSSFWorkbook(exportHeaderMap);
                    fileName += ".xlsx";
                    response.setContentType("application/excel");
                    response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
                    workbook.write(response.getOutputStream());
                } finally {
                    IOUtils.closeQuietly(workbook);
                }
                break;
            }
            case CSV: {
                fileName += ".csv";
                File file = FileUtil.file(Constant.TMP_ROOT_PATH + fileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                try {
                    CsvWriter writer = CsvUtil.getWriter(file, CharsetUtil.CHARSET_UTF_8, true);
                    writer.write(
                            exportHeaderMap.keySet().toArray(new String[]{})
                    );
                    Tool.ResponseUtil.downloadFile(response, fileName, FileUtil.readBytes(file));
                } finally {
                    if (file != null && file.exists()) file.delete();
                    IoUtil.close(fos);
                }
                break;
            }
            case JSON: {
                for (Map.Entry<String, String> entry : exportHeaderMap.entrySet()) {
                    entry.setValue(StrUtil.EMPTY);
                }
                List list = new ArrayList();
                list.add(exportHeaderMap);
                fileName += ".json";
                File file = FileUtil.writeUtf8String(JSONObject.toJSONString(list, SerializerFeature.PrettyFormat), Constant.TMP_ROOT_PATH + fileName);
                try {
                    Tool.ResponseUtil.downloadFile(response, fileName, FileUtil.readBytes(file));
                } finally {
                    if (file != null && file.exists()) file.delete();
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public List<Map<Integer, String>> getImportDataFromMultipartFile(MultipartFile multipartFile, String sysHeaderJsonStr) {
        String fileType = FilenameUtils.getExtension(multipartFile.getOriginalFilename());

        Map<String, String> exportHeaderMap = this.getI18nHeaderMap(sysHeaderJsonStr);
        List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

        List<Map<Integer, String>> importDataList = new ArrayList<>();
        switch (ImportFileType.getInstanceByType(fileType)) {
            case XLS:
            case XLSX: {
                Workbook work = ExcelUtils.getWorkBookFromMultipartFile(multipartFile);
                Sheet sheet = work.getSheetAt(0);
                // 表头校验
                boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), headerInfos);
                if (!headerFlag) {
                    throw new NZException(RCode.EXCELFILE_HEADER_TEMPLATE_ERROR);
                }
                if (sheet.getFirstRowNum() == sheet.getLastRowNum()){
                    throw new NZException(RCode.IMPORT_FILE_DATA_ISNULL);
                }
                for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    Map<Integer, String> tmpMap = Tool.MapUtil.newHashMap();
                    for (int j = 0; j < headerInfos.size(); j++) {
                        tmpMap.put(j, StrUtil.trim(ExcelUtils.getCellValue(row.getCell(j))));
                    }
                    importDataList.add(tmpMap);
                }
                break;
            }
            case CSV: {
                CsvData csvData = CSVUtils.getCsvDataFromMultipartFile(multipartFile);
                List<CsvRow> rows = csvData.getRows();
                if (rows.size() == 1){
                    throw new NZException(RCode.IMPORT_FILE_DATA_ISNULL);
                }
                CsvRow headers = rows.get(0);
                // 表头校验
                if (!CSVUtils.validateCSVHeader(headers, headerInfos)) {
                    throw new NZException(RCode.EXCELFILE_HEADER_TEMPLATE_ERROR);
                }
                for (int i = 1; i < rows.size(); i++) {
                    CsvRow csvRow = rows.get(i);
                    Map<Integer, String> tmpMap = Tool.MapUtil.newHashMap();
                    for (int j = 0; j < headerInfos.size(); j++) {
                        tmpMap.put(j, StrUtil.trim(csvRow.get(j)));
                    }
                    importDataList.add(tmpMap);
                }
                break;
            }
            case JSON: {
                List<LinkedHashMap> mapList = ListUtil.list(true);
                try {
                    String str = IoUtil.read(multipartFile.getInputStream(), CharsetUtil.charset("UTF-8"));
                    mapList = JSONObject.parseArray(str, LinkedHashMap.class);
                } catch (Exception e) {
                    log.error(e, "[getJSONDataFromMultipartFile] [error] [file: {}]", multipartFile.getOriginalFilename());
                    throw new NZException(RCode.IMPORT_FILE_PARSE_ERROR);
                }
                if (CollUtil.isEmpty(mapList)){
                    throw new NZException(RCode.IMPORT_FILE_CONTENT_ISNULL);
                }
                LinkedHashMap map = mapList.get(0);
                // 表头校验
                List<String> arrayList = new ArrayList<>(map.keySet());
                for (int i = 0; i < headerInfos.size(); i++) {
                    if (!Tool.StrUtil.equalsIgnoreCase(StrUtil.trim(headerInfos.get(i)), StrUtil.trim(arrayList.get(i)))) {
                        throw new NZException(RCode.EXCELFILE_HEADER_TEMPLATE_ERROR);
                    }
                }
                mapList.forEach(linkedHashMap -> {
                    List<String> keys = new ArrayList<>(linkedHashMap.keySet());
                    Map<Integer, String> tmpMap = Tool.MapUtil.newHashMap();
                    for (int i = 0; i < keys.size(); i++) {
                        Object o = linkedHashMap.get(keys.get(i));
                        String var = o == null ? StrUtil.EMPTY : StrUtil.trim(o.toString());
                        tmpMap.put(i, var);
                    }
                    importDataList.add(tmpMap);
                });
                break;
            }
            default: {
                throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
            }
        }
        return importDataList;
    }

    @Override
    public void exportDataByFormat(HttpServletResponse response, List<List<String>> exportDataList, String sysHeaderJsonStr, String format, String fileName) throws IOException {
        Map<String, String> exportHeaderMap = this.getI18nHeaderMap(sysHeaderJsonStr);
        switch (ImportFileType.getInstanceByValue(format)) {
            case XLS:
            case XLSX: {
                SXSSFWorkbook workbook = null;
                try {
                    workbook = ExcelUtils.createSXSSFWorkbook(exportHeaderMap);
                    SXSSFSheet sheet = workbook.getSheetAt(0);
                    CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
                    int rowNum = 1;
                    for (List<String> dataList : exportDataList) {
                        Row row = sheet.createRow(rowNum++);
                        for (int i = 0; i < dataList.size(); i++) {
                            row.createCell(i).setCellValue(dataList.get(i));
                        }
                        // 设置单元格样式
                        ExcelUtils.setDataCellStyle(dataStyle, row, dataList.size());
                    }
                    fileName += ".xlsx";
                    response.setContentType("application/excel");
                    response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
                    workbook.write(response.getOutputStream());
                } finally {
                    IOUtils.closeQuietly(workbook);
                }
                break;
            }
            case CSV: {
                fileName += ".csv";
                File file = FileUtil.file(Constant.TMP_ROOT_PATH + fileName);
                // https://hutool.cn/docs 注意 CSV文件本身为一种简单文本格式，有编码区分。Excel读取CSV文件中含有中文时时必须为GBK编码（Windows平台下），否则会出现乱码；或者增加BOM头
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(new byte[] { (byte) 0xEF, (byte) 0xBB,(byte) 0xBF});
                try {
                    CsvWriter writer = CsvUtil.getWriter(file, CharsetUtil.CHARSET_UTF_8, true);
                    writer.write(
                            exportHeaderMap.keySet().toArray(new String[]{})
                    );
                    exportDataList.forEach(dataList -> {
                        writer.write(
                                dataList.toArray(new String[]{})
                        );
                    });
                    Tool.ResponseUtil.downloadFile(response, fileName, FileUtil.readBytes(file));
                } finally {
                    if (file != null && file.exists()) file.delete();
                    IoUtil.close(fos);
                }
                break;
            }
            case JSON: {
                List list = new ArrayList();
                List<String> keyList = new ArrayList<>(exportHeaderMap.keySet());
                exportDataList.forEach(dataList -> {
                    Map<String, String> tempMap = new LinkedHashMap<>();
                    for (int i = 0; i < dataList.size(); i++) {
                        tempMap.put(keyList.get(i), StrUtil.emptyToDefault(dataList.get(i), ""));
                    }
                    list.add(tempMap);
                });
                fileName += ".json";
                File file = FileUtil.writeUtf8String(JSONObject.toJSONString(list, SerializerFeature.PrettyFormat), Constant.TMP_ROOT_PATH + fileName);
                try {
                    Tool.ResponseUtil.downloadFile(response, fileName, FileUtil.readBytes(file));
                } finally {
                    if (file != null && file.exists()) file.delete();
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public String getFailedContentFileBase64Str(List<Map<Integer, String>> failContentList, String sysHeaderJsonStr, String fileExtension) {
        if (CollUtil.isEmpty(failContentList)) return StrUtil.EMPTY;

        List<List<String>> list = ListUtil.list(true);
        failContentList.stream().forEach(dataMap -> {
            List<String> valueList = dataMap.values().stream().collect(Collectors.toList());
            list.add(valueList);
        });

        Map<String, String> exportHeaderMap = this.getI18nHeaderMap(sysHeaderJsonStr);
        try {
            switch (ImportFileType.getInstanceByType(fileExtension)) {
                case XLS:
                case XLSX: {
                    SXSSFWorkbook workbook = ExcelUtils.createSXSSFWorkbook(exportHeaderMap);
                    SXSSFSheet sheet = workbook.getSheetAt(0);
                    CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
                    int rowNum = 1;
                    for (List<String> dataList : list) {
                        Row row = sheet.createRow(rowNum++);
                        for (int i = 0; i < dataList.size(); i++) {
                            row.createCell(i).setCellValue(dataList.get(i));
                        }
                        ExcelUtils.setDataCellStyle(dataStyle, row, dataList.size());
                    }
                    // Save the template
                    File tmplFile = TempFile.createTempFile("poi-sxssf-template", ".xlsx");
                    FileOutputStream os = new FileOutputStream(tmplFile);
                    try {
                        workbook.write(os);
                        return Base64.encode(tmplFile);
                    } finally {
                        IOUtils.closeQuietly(os, workbook);
                        if (!tmplFile.delete()) {
                            throw new IOException("Could not delete temporary file after processing: " + tmplFile);
                        }
                    }
                }
                case CSV: {
                    File tmplFile = TempFile.createTempFile("csv-template", ".csv");
                    CsvWriter writer = CsvUtil.getWriter(tmplFile, CharsetUtil.CHARSET_UTF_8);
                    try {
                        writer.write(
                                exportHeaderMap.keySet().toArray(new String[]{})
                        );
                        list.forEach(dataList -> {
                            writer.write(
                                    dataList.toArray(new String[]{})
                            );
                        });
                        return Base64.encode(tmplFile);
                    } finally {
                        IOUtils.closeQuietly(writer);
                        if (!tmplFile.delete()) {
                            throw new IOException("Could not delete temporary file after processing: " + tmplFile);
                        }
                    }
                }
                case JSON: {
                    List jsonObjectList = new ArrayList();
                    List<String> keyList = new ArrayList<>(exportHeaderMap.keySet());
                    list.forEach(dataList -> {
                        Map<String, String> tempMap = new LinkedHashMap<>();
                        for (int i = 0; i < dataList.size(); i++) {
                            tempMap.put(keyList.get(i), dataList.get(i));
                        }
                        jsonObjectList.add(tempMap);
                    });
                    File tmplFile = TempFile.createTempFile("json-template", ".json");
                    try {
                        FileUtil.writeUtf8String(JSONObject.toJSONString(jsonObjectList, SerializerFeature.PrettyFormat), tmplFile);
                        return Base64.encode(tmplFile);
                    } finally {
                        if (!tmplFile.delete()) {
                            throw new IOException("Could not delete temporary file after processing: " + tmplFile);
                        }
                    }
                }
                default:
            }
        } catch (Exception e) {
            log.error(e);
            return RCode.ERROR.getMsg();
        }
        return StrUtil.EMPTY;
    }

    @Override
    public List queryList(String tableName, String colName, String value) {
        List<Object> objectList = basicMapper.selectList(tableName, colName, value);
        return objectList;
    }

    @Override
    public List queryList(String tableName, String colName, List values) {
        List<Object> objectList = basicMapper.queryList(tableName, colName, values);
        return objectList;
    }

}
