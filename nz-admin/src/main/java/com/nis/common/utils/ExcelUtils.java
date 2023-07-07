package com.nis.common.utils;


import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.exception.NZException;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;


/**
 * apache poi excel工具类
 */
public class ExcelUtils {

    private final static Log log = Log.get();

    // 2003- 版本的excel
    private final static String excel2003L = ".xls";
    // 2007+ 版本的excel
    private final static String excel2007U = ".xlsx";

    private static NumberFormat numberFormat = NumberFormat.getNumberInstance();

    static {
        numberFormat.setGroupingUsed(false);
        numberFormat.setMaximumFractionDigits(7);
    }

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
    public static Workbook getWork(String url, InputStream inputStream) throws IOException {
        Workbook wb = null;
        String str = url.substring(url.lastIndexOf("."));

        if(StrUtil.equals(excel2003L,str)) {
            wb = new HSSFWorkbook(inputStream);  // 2003-
        } else if (excel2007U.equals(str)) {
            wb = new XSSFWorkbook(inputStream);  // 2007+
        } else {
            throw new IOException("解析文件格式有误!");
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
                        String format = sdf.format(cell.getDateCellValue());
                        if(format.startsWith("1899-12-31")){ // 只有时分的日期格式会被默认设置为1899-12-31
                            sdf = new SimpleDateFormat("HH:mm");
                            format = sdf.format(cell.getDateCellValue());
                        }
                        result = format;
                    }
                    else {
                        // 数字也按照字符串类型获取值
                        // DecimalFormat df = new DecimalFormat("#");
                        // result = df.format(cell.getNumericCellValue());
                        // cell.setCellType(Cell.CELL_TYPE_STRING);
                        // result = cell.getStringCellValue();
                        double d = cell.getNumericCellValue();
                        result = numberFormat.format(d);
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
        return result.toString().trim();
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
    public static CellStyle createHeaderStyle(Workbook workbook) {
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
    public static CellStyle createCellStyle(Workbook workbook, short fontsize, boolean isBold) {
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
     * @param dataStyle
     * @param row
     * @param cellNum
     */
    public static void setDataCellStyle(CellStyle dataStyle, Row row, int cellNum) {
        Cell cell;
        for (int i = 0; i < cellNum; i++) {
            cell = row.getCell(i);
            cell = cell == null ? row.createCell(i) : cell;
            cell.setCellStyle(dataStyle);
        }
    }

    public static boolean validateExcelHeader(Sheet sheet, int firstRowNum, List<String> columnInfoList) {
        Row row = sheet.getRow(firstRowNum);
        for (int i = 0; i < columnInfoList.size(); i++) {
            if (!StrUtil.equalsIgnoreCase(Tool.StrUtil.trim(columnInfoList.get(i)), ExcelUtils.getCellValue(row.getCell(i)))) {
                return false;
            }
        }
        return true;
    }

    public static Workbook getWorkBookFromMultipartFile(MultipartFile excelFile) {
        if (!ExcelUtils.validateFileType(excelFile.getOriginalFilename())) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }

        Workbook work = null;
        try {
            work = ExcelUtils.getWork(excelFile.getOriginalFilename(), excelFile.getInputStream());
        } catch (Exception e) {
            log.error(e, "[getWorkBookFromMultipartFile] [error] [file: {}]", excelFile.getOriginalFilename());
            throw new NZException(RCode.EXCELFILE_PARSE_ERROR);
        } finally {
            if (work != null) {
                IOUtils.closeQuietly(work);
            }
        }
        return work;
    }

}
