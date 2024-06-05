package com.nis.modules.sys.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface BasicImportAndExportServices {

    Map<String, String> getI18nHeaderMap(String sysHeaderJsonStr);

    void getTemplate(String sysHeaderJsonStr, String format, String fileName, HttpServletResponse response) throws IOException;

    List<Map<Integer, String>> getImportDataFromMultipartFile(MultipartFile multipartFile, String sysHeaderJsonStr);

    void exportDataByFormat(HttpServletResponse response, List<List<String>> exportDataList, String sysHeaderJsonStr, String format, String fileName) throws IOException;

    String getFailedContentFileBase64Str(List<Map<Integer, String>> failContentList, String sysHeaderJsonStr, String fileExtension);

    /**
     * 根据表名、列名、value值查询列表，导入判定对象是否存在时使用
     * @param tableName
     * @param colName
     * @param value
     * @return
     */
    List<Map<String, Object>> queryList(String tableName, String colName, String value);

    /**
     * 根据表名、列名、values 值查询列表，是否包含
     * @param tableName
     * @param colName
     * @param values
     * @return
     */
    List<Map<String, Object>> queryList(String tableName, String colName, List values);

}
