package com.nis.modules.sys.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface BasicImportAndExportServices {

    Map<String, String> getI18nHeaderMap(String sysHeaderJsonStr);

    void getTemplate(String sysHeaderJsonStr, String format, String fileName, HttpServletResponse response) throws IOException;

    List<Map<Integer, String>> getImportDataFromMultipartFile(MultipartFile multipartFile, String sysHeaderJsonStr) throws IOException;

    void exportDataByFormat(HttpServletResponse response, List<List<String>> exportDataList, String sysHeaderJsonStr, String format, String fileName) throws IOException;
}
