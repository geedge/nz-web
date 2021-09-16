package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.dao.CabinetDao;
import com.nis.modules.asset.dao.DcDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserService;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据中心    serviceImpl
 * @author shizhendong
 * @date 2021-4-6
 */
@Service
public class DcServiceImpl extends ServiceImpl<DcDao, Dc> implements DcService {

    private static Log log = Log.get();

    @Autowired
    private AssetAssetDao assetDao;

    @Autowired
    private CabinetDao cabinetDao;

    @Autowired
    private PromserverDao promserverDao;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DcDao dcDao;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Override
    public Dc queryDcDetails(Integer id) {
        Dc dc = this.getById(id);
        if (ToolUtil.isEmpty(dc)) {
            throw new NZException(RCode.DC_NOT_EXISTS);
        }

        if(ToolUtil.isNotEmpty(dc.getPrincipal())){
            SysUserEntity principal = sysUserService.getById(dc.getPrincipal());
            dc.setAdminUser(principal);
        }

        Integer cabinetNum = cabinetDao.selectCount(new LambdaQueryWrapper<Cabinet>().eq(Cabinet::getDcId, dc.getId()));
        dc.setCabinetNum(cabinetNum);

        Integer alertNum = alertMessageService.count(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getDcId, dc.getId()));
        dc.setAlertNum(alertNum);

        Integer assetNum = assetDao.selectCount(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getDcId, dc.getId()));
        dc.setAssetNum(assetNum);

        return dc;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String ids = (String) params.get("ids");
        if (StrUtil.isNotBlank(ids)) {
            List<String> listIds = Arrays.asList(ids.split(","));
            params.put("ids", listIds);
        }

        IPage page = new Query(Dc.class).getPage(params);
        List<Dc> dcList = dcDao.queryDcList(page, params);

        if(CollectionUtils.isNotEmpty(dcList)){
            // 处理返回结果
            this.handleResuleList(dcList);
        }
        page.setRecords(dcList);
        return new PageUtils(page);
    }

    private void handleResuleList(List<Dc> dcList) {
        List<SysUserEntity> userEntityList = sysUserService.list();
        Map<Long, SysUserEntity> userIdAndEntityMap = userEntityList.stream().collect(Collectors.toMap(SysUserEntity::getId, Function.identity()));

        for (Dc dc : dcList) {
            if (ToolUtil.isNotEmpty(dc.getPrincipal())) {
                dc.setAdminUser(userIdAndEntityMap.get(dc.getPrincipal().longValue()));
            }
            if (dc.getAssetNum() == null) {
                dc.setAssetNum(0);
            }
            if (dc.getCabinetNum() == null) {
                dc.setCabinetNum(0);
            }
            if (dc.getAlertNum() == null) {
                dc.setAlertNum(0);
            }
        }
    }


    /**
     * 校验 dc 相关的信息
     * @param dc
     */
    private void validateDcInfo(Dc dc) {
        // 1. 校验机房负责人是否存在
        if (dc.getPrincipal() != null) {
            SysUserEntity principal = sysUserService.getById(dc.getPrincipal());
            if (principal == null) {
                throw new NZException(RCode.DC_PRINCIPAL_NOT_EXISTS);
            }
        }

        // 2. 校验名称重复
        List<Dc> list = this.list(new QueryWrapper<Dc>().lambda().eq(Dc::getName, dc.getName()));

        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(dc.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.DC_NAME_DUPLICATE);
        }

        // 3.修改行列时校验行列值是否正确
        if (dc.getId() != null) {
            Integer y = dc.getY();
            Integer x = dc.getX();

            // 查询数据库中 机柜已占用机房最大行列作为修改的最小值
            Map<String, Integer> cabinetMaxXY = cabinetDao.getCabinetMaxXY(dc.getId());
            if (ToolUtil.isNotEmpty(cabinetMaxXY)) {
                // 行 不为 null ，校验行
                Integer maxY = cabinetMaxXY.get("y");
                if (y != null && maxY != null) {
                    if (y < maxY) {
                        throw new NZException(RCode.DC_Y_ALREADYINUSE);
                    }
                }
                // 列 不为 null ，校验列
                Integer maxX = cabinetMaxXY.get("x");
                if (x != null && maxX != null) {
                    if (x < maxX) {
                        throw new NZException(RCode.DC_X_ALREADYINUSE);
                    }
                }
            }
        }

        // 4. 校验 State 是否合法
        String state = dc.getState();
        if (!StrUtil.equals(Constant.SWITCH_OPEN.toUpperCase(), state) && !StrUtil.equals(Constant.SWITCH_CLOSE.toUpperCase(), state)) {
            throw new NZException(RCode.DC_STATE_ERROR);
        }

        // 5. 经纬度校验
        Double longitude = dc.getLongitude();
        if (longitude != null) {
            if (longitude > 180 || longitude < -180) {
                throw new NZException(RCode.DC_LONGITUDE_ERROR);
            }
        }

        Double latitude = dc.getLatitude();
        if (latitude != null) {
            if (latitude > 90 || latitude < -90) {
                throw new NZException(RCode.DC_LATITUDE_ERROR);
            }
        }
    }

    @Override
    public Integer saveDc(Dc dc) {
        // 校验 dc 相关信息
        this.validateDcInfo(dc);

        // 设置属性默认值
        if (dc.getY() == null) {
            dc.setY(1);
        }

        if (dc.getX() == null) {
            dc.setX(1);
        }

        // 保存
        this.save(dc);
        return dc.getId();
    }

    @Override
    public Integer updateDc(Dc dc) {
        // 校验 dc 相关信息
        this.validateDcInfo(dc);

        // 保存
        this.updateById(dc);
        return dc.getId();
    }

    @Override
    public void delete(Integer... ids) {
        // 1. 校验是否包含 cabinet
        List<Cabinet> cabinets = cabinetDao.selectList(null);
        Set<Integer> cabinetUsedIdcIds = cabinets.stream().map(Cabinet::getDcId).collect(Collectors.toSet());

        List<Integer> idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(cabinetUsedIdcIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> dcNames = this.listByIds(idList).stream().map(Dc::getName).collect(Collectors.toList());
            throw new NZException("These dc contain cabinets and cannot be deleted, Details: " + dcNames.toString(), RCode.DC_CABINETS_EXISTS.getCode());
        }

        // 2. 校验是否包含 asset
        List<AssetAsset> assets = assetDao.selectList(null);
        Set<Integer> assetUsedIdcIds = assets.stream().map(AssetAsset::getDcId).collect(Collectors.toSet());

        idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(assetUsedIdcIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> dcNames = this.listByIds(idList).stream().map(Dc::getName).collect(Collectors.toList());
            throw new NZException("These dc contain assets and cannot be deleted, Details: " + dcNames.toString(), RCode.DC_ASSETS_EXISTS.getCode());
        }

        // 3. 校验是否包含 promeserver
        List<Promserver> promservers = promserverDao.selectList(null);
        Set<Integer> promserverUsedIdcIds = promservers.stream().map(Promserver::getDcId).collect(Collectors.toSet());

        idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(promserverUsedIdcIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> dcNames = this.listByIds(idList).stream().map(Dc::getName).collect(Collectors.toList());
            throw new NZException("These dc contain prometheus servers and cannot be deleted, Details: " + dcNames.toString(), RCode.DC_CABINETS_EXISTS.getCode());
        }

        // 4. 删除
        this.removeByIds(Arrays.asList(ids));

        // 5. 删除 alert
        alertMessageService.removeByDcIds(Arrays.asList(ids));
    }

    @Override
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        SysConfigEntity configEntity = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_DC_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        if (map.get(language) != null) {
            exportHeaderMap = (Map) map.get(language);
        } else {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "Dc", true);
            String fileName = "DcExportTemplate.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("dc 模板生成失败，错误信息是：" + e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("获取 dc 导入模板失败，表头语言是：" + language + " ，错误信息是：" + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
    }

    @Override
    public Map importDatacentersFromExcel(MultipartFile excelFile, String language) {
        Workbook work = ExcelUtils.getWorkBookFromMultipartFile(excelFile);

        Sheet sheet = work.getSheetAt(0);

        // 表头校验
        SysConfigEntity config = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_DC_EXPORT_HEADER));
        HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
        boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), map.values());
        if (!headerFlag) {
            throw new NZException(RCode.EXCELFILE_HEADER_TEMPLATE_ERROR);
        }

        Map<String, String> exportHeader = (Map) map.get(language);
        List<String> headerInfos = new ArrayList(exportHeader.keySet());

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = this.saveDatacentersFromSheet(sheet, failDetails, headerInfos, seqUUID);
        int totalNum = sheet.getLastRowNum() - sheet.getFirstRowNum();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    private int saveDatacentersFromSheet(Sheet sheet, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
        int successNum = 0;
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;

            String name = ExcelUtils.getCellValue(row.getCell(0));
            if (StringUtils.isEmpty(name)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.DC_NAME_ISNULL.getMsg());
            } else {
                int countByName = this.count(new LambdaQueryWrapper<Dc>().eq(Dc::getName, name));
                if (countByName > 0) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.DC_NAME_DUPLICATE.getMsg());
                }
            }

            String location = ExcelUtils.getCellValue(row.getCell(1));
            String tel = ExcelUtils.getCellValue(row.getCell(2));

            SysUserEntity principalUser = null;
            String principalName = ExcelUtils.getCellValue(row.getCell(3));
            if (StringUtils.isNotEmpty(principalName)) {
                principalUser = sysUserService.getOne(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getName, principalName));
                if (principalUser == null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.DC_PRINCIPAL_NOT_EXISTS.getMsg());
                }
            }

            String longitude = ExcelUtils.getCellValue(row.getCell(4));
            if (StringUtils.isNotEmpty(longitude)) {
                try {
                    Double aDouble = Double.valueOf(longitude);
                    if (aDouble > 180 || aDouble < -180) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.DC_LONGITUDE_ERROR.getMsg());
                    }
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.DC_LONGITUDE_FORMAT.getMsg());
                }
            }

            String latitude = ExcelUtils.getCellValue(row.getCell(5));
            if (StringUtils.isNotEmpty(latitude)) {
                try {
                    Double aDouble = Double.valueOf(latitude);
                    if (aDouble > 90 || aDouble < -90) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.DC_LATITUDE_ERROR.getMsg());
                    }
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.DC_LATITUDE_FORMAT.getMsg());
                }
            }

            String state = ExcelUtils.getCellValue(row.getCell(6));
            if (StringUtils.isEmpty(state)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.DC_STATE_ISNULL.getMsg());
            } else {
                if (!StrUtil.equals(Constant.SWITCH_OPEN.toUpperCase(), state) && !StrUtil.equals(Constant.SWITCH_CLOSE.toUpperCase(), state)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.DC_STATE_ERROR.getMsg());
                }
            }

            if (saveFlag) {
                Dc dc = new Dc();
                try {
                    dc.setName(name);
                    dc.setLocation(location);
                    dc.setTel(tel);
                    dc.setPrincipal(principalUser == null ? null : principalUser.getId().intValue());
                    dc.setY(1);
                    dc.setX(1);
                    dc.setLongitude(StrUtil.isNotEmpty(longitude) ? Double.valueOf(longitude) : null);
                    dc.setLatitude(StrUtil.isNotEmpty(latitude) ? Double.valueOf(latitude) : null);
                    dc.setState(state);
                    dc.setSeq(seqUUID);
                    this.save(dc);
                    successNum++;
                } catch (RuntimeException e) {
                    log.error(String.format("datacenter 导入保存失败,实体信息是：%s,错误信息是：%s", JSONObject.toJSONString(dc), e.getMessage()), e);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", RCode.EXCELFILE_IMPORT_ERROR.getMsg());
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
        SysConfigEntity configEntity = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_DC_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);

        PageUtils page = this.queryPage(params);
        List<Dc> list = (List<Dc>) page.getList();

        SXSSFWorkbook workbook = null;
        try {
            // 添加标题行 默认语言 en
            String language = (String) params.get("language");
            language = ToolUtil.isEmpty(language) ? "en" : language;

            String sheetName = "Data center";
            workbook = ExcelUtils.createExcel07((Map) map.get(language), sheetName, false);
            SXSSFSheet sheet = workbook.getSheet(sheetName);

            this.fillData(list, 1, sheet, workbook);

            // 响应为流
            String fileName = "Datacenters.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("Dc 导出文件生成失败，错误信息是：" + e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("Dc 导出失败，参数是：" + params.toString() + " ，错误信息是：" + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    private void fillData(List<Dc> list, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook) {
        List<SysUserEntity> userList = sysUserService.list();
        Map<Long, String> userIdAndNameMap = userList.stream().collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getName));
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        for (Dc dc : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(dc.getName());
            row.createCell(1).setCellValue(ObjectUtil.defaultIfNull(dc.getLocation(), ""));
            row.createCell(2).setCellValue(ObjectUtil.defaultIfNull(dc.getTel(), ""));
            String principalName = dc.getPrincipal() != null ? userIdAndNameMap.get(dc.getPrincipal().longValue()) : "";
            row.createCell(3).setCellValue(ObjectUtil.defaultIfNull(principalName, ""));
            row.createCell(4).setCellValue(ObjectUtil.defaultIfNull(dc.getLongitude(), "").toString());
            row.createCell(5).setCellValue(ObjectUtil.defaultIfNull(dc.getLatitude(), "").toString());
            row.createCell(6).setCellValue(dc.getState());

            // 设置单元格样式
            ExcelUtils.setDataCellStyle(dataStyle, row, 6);
        }
    }

    @Override
    public void cancelImport(String seq) {
        this.remove(new LambdaQueryWrapper<Dc>().eq(Dc::getSeq, seq));
    }
}
