package com.nis.common.utils;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;


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
                        // 数字也按照字符串类型获取值
                        /*DecimalFormat df = new DecimalFormat("#");
                        result = df.format(cell.getNumericCellValue());*/
                        cell.setCellType(Cell.CELL_TYPE_STRING);
                        result = cell.getStringCellValue();
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

    /**
     * 创建 excel
     *
     * @param headerInfo 表头信息 结构为 Map  key=cellName,value=cellComment
     * @param sheetName
     * @param isComment  是否需要批注信息
     * @return
     * @throws IOException
     */
    public static SXSSFWorkbook createExcel07(Map<String, String> headerInfo, String sheetName, boolean isComment) throws IOException {
        SXSSFWorkbook wb = new SXSSFWorkbook();
        // 标题样式
        CellStyle colStyle = createHeaderStyle(wb);
        Sheet sheet = wb.createSheet(sheetName);
        Drawing dp = sheet.createDrawingPatriarch();
        // 设置默认列宽
        sheet.setDefaultColumnWidth(25);
        Row headerRow = sheet.createRow(0);
        Comment cellComment;
        int i = 0;
        for (Map.Entry<String, String> entry : headerInfo.entrySet()) {
            // 创建列、单元格
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(entry.getKey());
            headerCell.setCellStyle(colStyle);

            if (isComment) {
                cellComment = dp.createCellComment(dp.createAnchor(Units.EMU_PER_POINT * 9, Units.EMU_PER_PIXEL, Units.EMU_PER_CENTIMETER * 4, Units.EMU_PER_POINT * 9, i, 0, i, 4));
                cellComment.setString(new XSSFRichTextString(entry.getValue()));
                cellComment.setAuthor("nezha");
                headerCell.setCellComment(cellComment);
            }
            i++;
        }
        return wb;
    }

    /**
     * 返回表头样式
     *
     * @param workbook excel
     * @return
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // 上下左右边框
        style.setBorderBottom(HSSFCellStyle.THIN_BACKWARD_DIAG);
        style.setBorderLeft(HSSFCellStyle.THIN_BACKWARD_DIAG);
        style.setBorderRight(HSSFCellStyle.THIN_BACKWARD_DIAG);
        style.setBorderTop(HSSFCellStyle.THIN_BACKWARD_DIAG);
        // 水平居中
        style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
        //垂直居中
        style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        //设置单元格背景颜色
        style.setFillForegroundColor(IndexedColors.BLUE_GREY.index);
        style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        //设置表头属性
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.WHITE.index);
        style.setFont(font);
        return style;
    }

    /**
     * 返回单元格样式
     *
     * @param workbook excel
     * @param fontsize 字体大小
     * @param isBold   字体是否加粗
     * @return
     */
    private static CellStyle createCellStyle(Workbook workbook, short fontsize, boolean isBold) {
        CellStyle style = workbook.createCellStyle();
        // 上下左右边框
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setBorderTop(CellStyle.BORDER_THIN);
        // 水平居中
        style.setAlignment(CellStyle.ALIGN_CENTER);
        //垂直居中
        style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        //创建字体
        Font font = workbook.createFont();
        if (isBold) {
            font.setBold(true);
        }
        font.setFontHeightInPoints(fontsize);
        //加载字体
        style.setFont(font);
        // 自动换行
        style.setWrapText(true);
        return style;
    }

    /**
     * 设置单元格样式
     * @param workbook
     * @param sheet
     * @param colNum    列
     */
    public static void setDataCellStyle(Workbook workbook, Sheet sheet, int colNum) {
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        int firstRowNum = sheet.getFirstRowNum();
        // 获得当前sheet的结束行
        int maxRowNum = sheet.getLastRowNum();
        Cell cell;
        for (int i = firstRowNum + 1; i <= maxRowNum; i++) {
            Row row = sheet.getRow(i);
            for (int j = 0; j <= colNum; j++) {
                cell = row.getCell(j);
                cell = cell == null ? row.createCell(j) : cell;
                cell.setCellStyle(dataStyle);
            }
        }
    }

    public static boolean validateExcelHeader(Sheet sheet, int firstRowNum, Collection<Object> columnInfoList) {
        Row row = sheet.getRow(firstRowNum);
        List<Boolean> flags = new ArrayList<>();
        List<String> columns;
        LinkedHashMap colsMap;
        for (Object obj : columnInfoList) {
            colsMap = JSONObject.parseObject(obj.toString(), LinkedHashMap.class, Feature.OrderedField);
            columns = new ArrayList<>(colsMap.keySet());
            if (CollectionUtils.isNotEmpty(columns)) {
                boolean flag = true;
                for (int i = 0; i < columns.size(); i++) {
                    if (!columns.get(i).toLowerCase().equals(ExcelUtils.getCellValue(row.getCell(i)).toLowerCase())) {
                        flag = false;
                        break;
                    }
                }
                flags.add(flag);
            }
        }

        boolean headerFlag = false;
        for (Boolean flag : flags) {
            headerFlag = (headerFlag || flag);
        }
        return headerFlag;
    }
}
