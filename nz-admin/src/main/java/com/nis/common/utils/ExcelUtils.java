package com.nis.common.utils;


import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * apache poi excel工具类
 */
public class ExcelUtils {
    // 2003- 版本的excel
    private final static String excel2003L = ".xls";
    // 2007+ 版本的excel
    private final static String excel2007U = ".xlsx";

    /**
     * 校验文件格式
     *
     * @param filename
     * @return
     */
    public static boolean validateFileType(String filename) {
        if (!filename.contains(".")) {
            return false;
        } else {
            String fileType = filename.substring(filename.lastIndexOf("."));
            switch (fileType) {
                case excel2003L: {
                    return true;
                }
                case excel2007U: {
                    return true;
                }
                default: {
                    return false;
                }
            }
        }
    }

    /**
     * 根据版本获取excel对象
     *
     * @param url
     * @param inputStream
     * @return
     * @throws Exception
     */
    public static Workbook getWork(String url, InputStream inputStream) throws Exception {
        Workbook wb = null;
        String str = url.substring(url.lastIndexOf("."));

        if (excel2003L.equals(str)) {
            wb = new HSSFWorkbook(inputStream);  // 2003-
        } else if (excel2007U.equals(str)) {
            wb = new XSSFWorkbook(inputStream);  // 2007+
        } else {
            throw new Exception("解析文件格式有误!");
        }
        return wb;
    }

    /**
     * 获取单元格的值
     *
     * @param cell
     * @return
     */
    public static String getCellValue(Cell cell) {
        Object result = "";
        if (cell != null) {
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_STRING:
                    result = cell.getStringCellValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    // 在excel里,日期也是数字,在此要进行判断
                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = cell.getDateCellValue();
                        result = sdf.format(date);
                    } else {
                        DecimalFormat df = new DecimalFormat("#");
                        result = df.format(cell.getNumericCellValue());
                    }
                    break;
                case Cell.CELL_TYPE_BOOLEAN:// 布尔
                    result = cell.getBooleanCellValue();
                    break;
                case Cell.CELL_TYPE_FORMULA:// 公式
                    result = cell.getCellFormula();
                    break;
                case Cell.CELL_TYPE_ERROR:
                    result = cell.getErrorCellValue();
                    break;
                case Cell.CELL_TYPE_BLANK: // null
                    break;
                default:
                    break;
            }
        }
        return result.toString();
    }
}
