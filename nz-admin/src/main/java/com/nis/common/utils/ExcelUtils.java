package com.nis.common.utils;


import cn.hutool.core.io.FileMagicNumber;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.exception.NZException;
import org.apache.commons.io.FilenameUtils;
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

    private static NumberFormat numberFormat = NumberFormat.getNumberInstance();
    public static String DEFAULT_SHEETNAME = "Datasheet";

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
    public static void validateFileType(String filename) {
        String extension = FilenameUtils.getExtension(filename);

        if (Tool.BooleanUtil.and(
                !StrUtil.equalsIgnoreCase(FileMagicNumber.XLS.getExtension(), extension),
                !StrUtil.equalsIgnoreCase(FileMagicNumber.XLSX.getExtension(), extension)
        )) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }
    }

    /**
     * get Work book By Stream
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static Workbook getWorkbookByStream(InputStream inputStream) throws IOException {
        byte[] fileBytes = Tool.IoUtil.readBytes(inputStream);
        FileMagicNumber magicNumber = FileMagicNumber.getMagicNumber(fileBytes);
        if (StrUtil.equals(FileMagicNumber.XLS.getExtension(), magicNumber.getExtension())) {
            // 2003-
            return new HSSFWorkbook(IoUtil.toStream(fileBytes));
        } else if (StrUtil.equals(FileMagicNumber.XLSX.getExtension(), magicNumber.getExtension())) {
            // 2007+
            return new XSSFWorkbook(IoUtil.toStream(fileBytes));
        } else {
            log.error("[getWorkbookByStream] [File parsing failed,format error] [file type: {}]", magicNumber.getExtension());
            throw new NZException(RCode.EXCELFILE_PARSE_ERROR);
        }
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
     * create SXSSFWorkbook
     *
     * @param headerInfo 表头信息 结构为 Map  key=cellName,value=cellComment
     * @return
     */
    public static SXSSFWorkbook createSXSSFWorkbook(Map<String, String> headerInfo) {
        SXSSFWorkbook wb = new SXSSFWorkbook();

        // sheet & header
        createSheetWithHeader(wb, headerInfo);
        return wb;
    }

    /**
     * create XSSFWorkbook
     *
     * @param headerInfo 表头信息 结构为 Map  key=cellName,value=cellComment
     * @return
     */
    public static XSSFWorkbook createXSSFWorkbook(Map<String, String> headerInfo) {
        XSSFWorkbook wb = new XSSFWorkbook();

        // sheet & header
        createSheetWithHeader(wb, headerInfo);
        return wb;
    }

    /**
     * 创建默认 sheet 页
     * 添加标题行
     * 添加标题行样式
     *
     * @param wb
     * @param headerInfo
     */
    private static void createSheetWithHeader(Workbook wb, Map<String, String> headerInfo) {
        // 标题样式
        CellStyle colStyle = createHeaderStyle(wb);
        Sheet sheet = wb.createSheet(DEFAULT_SHEETNAME);
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

            cellComment = dp.createCellComment(dp.createAnchor(Units.EMU_PER_POINT * 9, Units.EMU_PER_PIXEL, Units.EMU_PER_CENTIMETER * 4, Units.EMU_PER_POINT * 9, i, 0, i, 4));
            cellComment.setString(new XSSFRichTextString(entry.getValue()));
            cellComment.setAuthor("nezha");
            headerCell.setCellComment(cellComment);
            i++;
        }
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
        // validate file type
        ExcelUtils.validateFileType(excelFile.getOriginalFilename());

        Workbook work = null;
        try {
            work = ExcelUtils.getWorkbookByStream(excelFile.getInputStream());
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
