package com.nis.modules.sys.service.impl;

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
import com.nis.modules.sys.dao.BasicMapper;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysI18nService;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasicImportAndExportServicesImpl implements BasicImportAndExportServices {

    private static final Log log = Log.get();

    @Autowired
    private SysI18nService sysI18nService;

    @Autowired
    private BasicMapper basicMapper;

    @Autowired
    private RedisTemplate redisTemplate;

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
        switch (format) {
            case "1": { // XLSX
                SXSSFWorkbook workbook = null;
                try {
                    workbook = ExcelUtils.createExcel07(exportHeaderMap, "Datasheet", true);
                    fileName += ".xlsx";
                    response.setContentType("application/excel");
                    response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
                    workbook.write(response.getOutputStream());
                } finally {
                    if (workbook != null) IOUtils.closeQuietly(workbook);
                }
                break;
            }
            case "2": {// CSV
                fileName += ".csv";
                File file = FileUtil.file(Constant.TMP_ROOT_PATH + fileName);
                try {
                    CsvWriter writer = CsvUtil.getWriter(file, CharsetUtil.CHARSET_UTF_8);
                    writer.write(
                            exportHeaderMap.keySet().toArray(new String[]{})
                    );
                    Tool.ResponseUtil.downloadFile(response, fileName, FileUtil.readBytes(file));
                } finally {
                    if (file != null && file.exists()) file.delete();
                }
                break;
            }
            case "3": {// JSON
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
    public List<Map<Integer, String>> getImportDataFromMultipartFile(MultipartFile multipartFile, String sysHeaderJsonStr) throws IOException {
        String originalFilename = multipartFile.getOriginalFilename();
        String fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        fileType = fileType.toLowerCase();

        Map<String, String> exportHeaderMap = this.getI18nHeaderMap(sysHeaderJsonStr);

        List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());
        List<Map<Integer, String>> importDataList = new ArrayList<>();
        switch (fileType) {
            case "xlsx":
            case "xls": {
                Workbook work = ExcelUtils.getWorkBookFromMultipartFile(multipartFile);
                Sheet sheet = work.getSheetAt(0);
                // 表头校验
                boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), headerInfos);
                if (!headerFlag) {
                    throw new NZException(RCode.EXCELFILE_HEADER_TEMPLATE_ERROR);
                }
                for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    Map<Integer, String> tmpMap = new HashMap<>();
                    for (int j = 0; j < headerInfos.size(); j++) {
                        tmpMap.put(j, ExcelUtils.getCellValue(row.getCell(j)));
                    }
                    importDataList.add(tmpMap);
                }
                break;
            }
            case "csv": {
                CsvData csvData = CSVUtils.getCsvDataFromMultipartFile(multipartFile);
                List<CsvRow> rows = csvData.getRows();
                CsvRow headers = rows.get(0);
                // 表头校验
                if (!CSVUtils.validateCSVHeader(headers, headerInfos)) {
                    throw new NZException(RCode.EXCELFILE_HEADER_TEMPLATE_ERROR);
                }
                for (int i = 1; i < rows.size(); i++) {
                    CsvRow csvRow = rows.get(i);
                    Map<Integer, String> tmpMap = new HashMap<>();
                    for (int j = 0; j < headerInfos.size(); j++) {
                        tmpMap.put(j, csvRow.get(j));
                    }
                    importDataList.add(tmpMap);
                }
                break;
            }
            case "json": {
                String str = IoUtil.read(multipartFile.getInputStream(), CharsetUtil.charset("UTF-8"));
                List<LinkedHashMap> mapList = JSONObject.parseArray(str, LinkedHashMap.class);
                LinkedHashMap map = mapList.get(0);
                // 表头校验
                List<String> arrayList = new ArrayList<>(map.keySet());
                for (int i = 0; i < headerInfos.size(); i++) {
                    if (!Tool.StrUtil.equalsIgnoreCase(headerInfos.get(i), arrayList.get(i))) {
                        throw new NZException(RCode.EXCELFILE_HEADER_TEMPLATE_ERROR);
                    }
                }
                mapList.forEach(linkedHashMap -> {
                    List<String> keys = new ArrayList<>(linkedHashMap.keySet());
                    Map<Integer, String> tmpMap = new HashMap<>();
                    for (int i = 0; i < keys.size(); i++) {
                        Object o = linkedHashMap.get(keys.get(i));
                        String var = o == null ? StrUtil.EMPTY : o.toString().trim();
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
        String formatStr = format.toString();
        switch (formatStr) {
            case "1": { // XLSX
                SXSSFWorkbook workbook = null;
                try {
                    String sheetName = "Datasheet";
                    workbook = ExcelUtils.createExcel07(exportHeaderMap, sheetName, true);
                    SXSSFSheet sheet = workbook.getSheet(sheetName);
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
                    if (workbook != null) IOUtils.closeQuietly(workbook);
                }
                break;
            }
            case "2": {// CSV
                fileName += ".csv";
                File file = FileUtil.file(Constant.TMP_ROOT_PATH + fileName);
                try {
                    CsvWriter writer = CsvUtil.getWriter(file, CharsetUtil.CHARSET_UTF_8);
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
                }
                break;
            }
            case "3": {// JSON
                List list = new ArrayList();
                List<String> keyList = new ArrayList<>(exportHeaderMap.keySet());
                exportDataList.forEach(dataList -> {
                    Map<String, String> tempMap = new LinkedHashMap<>();
                    for (int i = 0; i < dataList.size(); i++) {
                        tempMap.put(keyList.get(i), dataList.get(i));
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
    public List queryList(String tableName, String colName, String value) {
        List<Object> objectList = basicMapper.selectList(tableName, colName, value);
        return objectList;
    }

    @Override
    public List<Map> readImportErrMsgAndRemoveKey(String key) {
        List<Map> failDetailList = Collections.emptyList();
        try {
            log.info("read import error message,key: " + key);
            // 读取错误信息
            List<String> datas = redisTemplate.opsForList().range(key, 0, -1);
            failDetailList = datas.stream().map(data -> JSONObject.parseObject(data, Map.class)).collect(Collectors.toList());
            log.info("read import error message, content: " + JSONObject.toJSONString(failDetailList));

            // 删除 key
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("read import error message error", e);
        }
        return failDetailList;
    }

    @Override
    public List<Map> readImportErrMsgForThisLine(String errMsgRedisKey, Integer lineNum) {
        List<String> errMsgList = redisTemplate.opsForList().range(errMsgRedisKey, 0, -1);

        List<Map> list = errMsgList.stream()
                .map(data -> JSONObject.parseObject(data, Map.class))
                .filter(map -> lineNum.equals((Integer) map.get("lineNo")))
                .collect(Collectors.toList());

        return list;
    }
}
