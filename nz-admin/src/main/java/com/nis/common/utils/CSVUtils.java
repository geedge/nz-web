package com.nis.common.utils;


import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.log.Log;
import org.apache.commons.io.FileUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class CSVUtils {

    private static Log logger = Log.get();

    static final CsvReader reader = CsvUtil.getReader();

    public static CsvData getCsvDataFromMultipartFile(MultipartFile multipartFile) {
        File tempCsvFile = new File(Constant.TEMP_PATH + File.separator + "csv_" + UUID.randomUUID());
        CsvData data = null;
        try {
            FileUtils.copyInputStreamToFile(multipartFile.getInputStream(), tempCsvFile);
            //data = reader.read(tempCsvFile);
            // https://hutool.cn/docs 注意 CSV文件本身为一种简单文本格式，有编码区分。Excel读取CSV文件中含有中文时时必须为GBK编码（Windows平台下），否则会出现乱码。
            data = reader.read(ResourceUtil.getReader(tempCsvFile.getPath(), CharsetUtil.CHARSET_GBK));
        } catch (IOException e) {
            logger.error(e);
        } finally {
            if (tempCsvFile != null && tempCsvFile.exists()) tempCsvFile.delete();
        }
        return data;
    }

    public static boolean validateCSVHeader(CsvRow headers, List<String> headerInfos) {
        for (int i = 0; i < headerInfos.size(); i++) {
            if (!Tool.StrUtil.equalsIgnoreCase(headerInfos.get(i), headers.get(i))) {
                return false;
            }
        }
        return true;
    }
}
