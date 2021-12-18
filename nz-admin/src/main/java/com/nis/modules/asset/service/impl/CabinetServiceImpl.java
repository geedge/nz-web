package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.asset.dao.CabinetDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 机柜    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("cabinetService")
public class CabinetServiceImpl extends ServiceImpl<CabinetDao, Cabinet> implements CabinetService {

    private static Log log = Log.get();

    @Autowired
    private CabinetDao cabinetDao;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private DcService dcService;

    @Autowired
    private AssetAssetService assetAssetService;

    @Autowired
    private AlertMessageDao alertMessageDao;
    
    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;
    
    /**
     * 校验 cabinet 相关信息
     * 1. 校验机房是否存在
     * 2. 校验机柜名称唯一
     * 3. 校验 usize 最小最大值
     * 4. 校验行列值
     * @param cabinet
     */
    private void validateCabinetInfo(Cabinet cabinet) {
        // 1. 校验机房是否存在
        Dc dc = dcService.getById(cabinet.getDcId());
        if (dc == null) {
            throw new NZException(RCode.DC_NOT_EXISTS);
        }

        // 2. 校验机柜名称唯一
        List<Cabinet> list = this.list(new QueryWrapper<Cabinet>().lambda().eq(Cabinet::getName, cabinet.getName()).eq(Cabinet::getDcId, dc.getId()));

        boolean flag = false;

        if ((list == null || list.size() == 0) ||  (list != null && list.size() == 1 && list.get(0).getId().equals(cabinet.getId()))) {
            flag = true;
        }

        if(!flag){
            throw new NZException(RCode.DC_CABINETNAME_DUPLICATE);
        }

        // 3.校验 usize 最小最大值
        if(cabinet.getuSize() != null){
            ValidateUtils.is(cabinet.getuSize()).min(1, RCode.DC_CABINETUSIZE_MINIMUM).max(Constant.CURRENT_CABINET_MAXU, RCode.DC_CABINET_MAXU);
        }

        // 4. 校验行列值
        this.validateXY(cabinet);
    }

    /**
     * 校验所在机房行列值是否正确
     * @param cabinet
     */
    private void validateXY(Cabinet cabinet) {
        Dc dc = dcService.getById(cabinet.getDcId());

        Integer y = cabinet.getY();
        Integer x = cabinet.getX();


        if (y != null) {
            Integer dcY = dc.getY();
            if (dcY != null) {
                ValidateUtils.is(cabinet.getY()).max(dcY, RCode.CABINET_Y_INCORRECT);
            }
        }

        if (x != null) {
            Integer dcX = dc.getX();
            if (dcX != null) {
                ValidateUtils.is(cabinet.getX()).max(dcX, RCode.CABINET_X_INCORRECT);
            }
        }

        // 当行列都不为空时 校验位置是否被占用 如果只单独填写了行或列的话 暂时不做具体位置校验
        if (y != null && x != null) {
            Cabinet entity = this.getOne(new QueryWrapper<Cabinet>().lambda().eq(Cabinet::getDcId, cabinet.getDcId()).eq(Cabinet::getY, y).eq(Cabinet::getX, x));
            if (entity != null) {
                Integer id = cabinet.getId();
                if (id == null) {
                    throw new NZException(RCode.CABINET_POSITION_OCCUPIED);
                } else {
                    if (!id.equals(entity.getId())) {
                        throw new NZException(RCode.CABINET_POSITION_OCCUPIED);
                    }
                }
            }
        }
    }


    @Override
    public Cabinet queryCabinetDetails(Integer id) {
        Cabinet cabinet = this.getById(id);
        if (ToolUtil.isEmpty(cabinet)) {
            throw new NZException(RCode.CABINET_NOT_EXISTS);
        }

        Dc dc = dcService.getById(cabinet.getDcId());
        cabinet.setDc(dc);

        List<AssetAsset> assetList = assetAssetService.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getCabinetId, cabinet.getId()));
        cabinet.setAssetNum(assetList.size());
        if(CollectionUtils.isNotEmpty(assetList)){
            // 包含 asset 则统计 alert
            List<Integer> cabinetAssetIdList = assetList.stream().map(AssetAsset::getId).collect(Collectors.toList());
            List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
            long alertNum = alertList.stream().filter(alert -> cabinetAssetIdList.contains(alert.getAssetId())).count();
            cabinet.setAlertNum((int) alertNum);
        }else{
            cabinet.setAlertNum(0);
        }
        return cabinet;
    }

    @Override
    public PageUtils queryCabinetPage(Map<String, Object> params) {
        String ids = (String) params.get("ids");
        if (StrUtil.isNotBlank(ids)) {
            params.put("ids", Arrays.asList(ids.split(",")));
        }

        String dcIds = (String) params.get("dcIds");
        if (StrUtil.isNotBlank(dcIds)) {
            params.put("dcIds", Arrays.asList(dcIds.split(",")));
        }

        IPage page = new Query(Cabinet.class).getPage(params);
        List<Cabinet> cabinetList = cabinetDao.queryPage(page, params);

        page.setRecords(cabinetList);
        return new PageUtils(page);
    }

    @Override
    public Integer saveCabinet(Cabinet cabinet) {
        // 校验
        this.validateCabinetInfo(cabinet);

        // Usize 为空 则从配置表中取出 cabinet u位默认值
        if (cabinet.getuSize() == null) {
            SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_DEFAULT_CABINETU).eq(SysConfigEntity::getStatus, 1));
            cabinet.setuSize(Integer.valueOf(configEntity.getParamValue()));
        }

        // 保存
        this.save(cabinet);
        return cabinet.getId();
    }

    @Override
    public Integer updateCabinet(Cabinet cabinet) {
        // 校验
        this.validateCabinetInfo(cabinet);

        // 保存
        this.updateById(cabinet);
        return cabinet.getId();
    }

    @Override
    public void deleteByIds(Integer... ids) {
        List<AssetAsset> assets = assetAssetService.list();

        // asset 使用的 cabinet
        Set<Integer> assetUsedCabinetIds = assets.stream().filter(asset -> {
            if (ToolUtil.isNotEmpty(asset.getCabinetId())) {
                return true;
            }
            return false;
        }).map(AssetAsset::getCabinetId).collect(Collectors.toSet());

        List<Integer> idList = Arrays.stream(ids).collect(Collectors.toList());
        idList.retainAll(assetUsedCabinetIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> cabNames = this.listByIds(idList).stream().map(Cabinet::getName).collect(Collectors.toList());
            throw new NZException("These cabinets contain assets and cannot be deleted, Details: " + cabNames.toString(), RCode.DC_CABINET_ASSETS_EXISTS.getCode());
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }

    @Override
    public Map getCabinetUsizeDetails(Integer id) {
        // 获取当前机柜
        Cabinet cabinet = this.getById(id);
        if (cabinet == null) {
            throw new NZException(RCode.ASSET_CABINET_NOTFOUND);
        }

        Map resuleData = new HashMap();
        Integer size = cabinet.getuSize();
        // 如果机柜没有大小 返回信息
        if (size == null) {
            resuleData.put("total", 0);
            resuleData.put("available", 0);
            resuleData.put("occupy", new ArrayList<>());
            return resuleData;
        }

        resuleData.put("total", size);

        //  查询当前机柜下资产
        List<AssetAsset> assets = assetAssetService.list(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getCabinetId, cabinet.getId()));
        assets = assets.stream().filter(asset -> asset.getCabinetStart() != null && asset.getCabinetEnd() != null).collect(Collectors.toList());

        // 已经被占用位置
        Set<Integer> occupyList = new TreeSet<>();
        for (AssetAsset asset : assets) {
            for (int i = asset.getCabinetStart(); i < asset.getCabinetEnd(); i++) {
                occupyList.add(i);
            }
        }

        // 对占用位置结果集排序
        List<Integer> tempList = new ArrayList<>(occupyList);
        Collections.sort(tempList);
        resuleData.put("occupy", tempList);
        resuleData.put("available", size - tempList.size());
        return resuleData;
    }

    @Override
    public void getCabinetTemplateByLanguage(HttpServletResponse response, String format) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_CABINET_EXPORT_HEADER);
        String fileName = "CabinetTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
    }

    @Override
    public Map importCabinetData(MultipartFile multipartFile) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_CABINET_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = this.saveCabinetsFromImportDataList(importDataList, failDetails, new ArrayList<>(exportHeaderMap.keySet()), seqUUID);
        int totalNum = importDataList.size();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    private int saveCabinetsFromImportDataList(List<Map<Integer, String>> importDataList, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
        int successNum = 0;
        for (int i = 0; i < importDataList.size(); i++) {
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
              Map<Integer, String> dataMap = importDataList.get(i);

              String cabinetName = dataMap.get(0);
            if (StringUtils.isEmpty(cabinetName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.DC_CABINETNAME_ISNULL.getMsg());
            }

            String dcName = dataMap.get(1);
            Dc dc = null;
            if (StringUtils.isEmpty(dcName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.DC_NAME_ISNULL.getMsg());
            } else {
                dc = dcService.getOne(new LambdaQueryWrapper<Dc>().eq(Dc::getName, dcName));
                if (ToolUtil.isEmpty(dc)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.DC_NOT_EXISTS.getMsg());
                }
            }

            String uSize = dataMap.get(2);
            if (StringUtils.isNotEmpty(uSize)) {
                Integer uSizeIntVal = null;
                try {
                    uSizeIntVal = Double.valueOf(uSize).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CABINET_USIZE_FORMAT.getMsg());
                }
                if (uSizeIntVal != null && (uSizeIntVal < 1 || uSizeIntVal > Constant.CURRENT_CABINET_MAXU)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CABINET_USIZE_ERROR.getMsg());
                }
            }

            String y = dataMap.get(3);
            if (StringUtils.isNotEmpty(y)) {
                try {
                    Double.valueOf(y).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CABINET_Y_FORMAT.getMsg());
                }
            }
            String x = dataMap.get(4);
            if (StringUtils.isNotEmpty(x)) {
                try {
                    Double.valueOf(x).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.CABINET_X_FORMAT.getMsg());
                }
            }
            String remark = dataMap.get(5);
            if (saveFlag) {
                // 名称校验
                int countByName = this.count(new QueryWrapper<Cabinet>().lambda().eq(Cabinet::getName, cabinetName).eq(Cabinet::getDcId, dc.getId()));
                if (countByName > 0) {
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.DC_CABINETNAME_DUPLICATE.getMsg());
                    continue;
                }
                // 校验行列
                if (StrUtil.isAllNotEmpty(x, y)) {
                    int xIntVal = Double.valueOf(x).intValue();
                    int yIntVal = Double.valueOf(y).intValue();
                    if (yIntVal > dc.getY()) {
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CABINET_Y_INCORRECT.getMsg());
                        continue;
                    }
                    if (xIntVal > dc.getX()) {
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.CABINET_X_INCORRECT.getMsg());
                        continue;
                    }
                    int countByPosition = this.count(new QueryWrapper<Cabinet>().lambda().eq(Cabinet::getDcId, dc.getId()).eq(Cabinet::getY, yIntVal).eq(Cabinet::getX, xIntVal));
                    if (countByPosition > 0) {
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CABINET_POSITION_OCCUPIED.getMsg());
                        continue;
                    }
                }
                Cabinet cabinet = new Cabinet();
                try {
                    cabinet.setName(cabinetName);
                    cabinet.setDcId(dc.getId());

                    if (StrUtil.isNotEmpty(uSize)) {
                        cabinet.setuSize(Double.valueOf(uSize).intValue());
                    } else {
                        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_DEFAULT_CABINETU).eq(SysConfigEntity::getStatus, 1));
                        cabinet.setuSize(Integer.valueOf(configEntity.getParamValue()));
                    }

                    cabinet.setRemark(remark);
                    if (StrUtil.isNotEmpty(x)) {
                        cabinet.setX(Double.valueOf(x).intValue());
                    }
                    if (StrUtil.isNotEmpty(y)) {
                        cabinet.setY(Double.valueOf(y).intValue());
                    }
                    cabinet.setSeq(seqUUID);
                    this.save(cabinet);
                    successNum++;
                } catch (RuntimeException e) {
                    log.error(String.format("cabinet 导入保存失败,实体信息是：%s,错误信息是：%s", JSONObject.toJSONString(cabinet), e.getMessage()), e);
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
    public void export(HttpServletResponse response, Map<String, Object> params) throws IOException {
        String maxLine = sysConfigService.getValue(Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE);
        long exportMaxLine = Integer.parseInt(StrUtil.emptyToDefault(maxLine, "10000"));
        // 导出最大条数限制
        Object limit = params.get(Constant.LIMIT) == null ? Constant.PAGESIZE : params.get(Constant.LIMIT);
        Long pageSize = Long.parseLong(limit.toString());
        if (pageSize == -1) {
            // 导出所有直接限制 pageSize
            params.put(Constant.PAGE, "1");
            params.put(Constant.LIMIT, Long.toString(exportMaxLine));
        } else if (pageSize > exportMaxLine) {
            // 分页超过最大限制 则设置最大限制为 系统配置值，和 pageSize == -1 判断区别在于 pageNo
            params.put(Constant.LIMIT, Long.toString(exportMaxLine));
        }
        PageUtils page = this.queryCabinetPage(params);
        List<Cabinet> list = (List<Cabinet>) page.getList();

        List<List<String>> exportDataList = this.handleExportDataList(list);

        String sysDcHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_CABINET_EXPORT_HEADER);
        String fileName = "Cabinets";
        String formatStr = params.get("format").toString();
        basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysDcHeaderJsonStr, formatStr, fileName);
    }

    private List<List<String>> handleExportDataList(List<Cabinet> list) {
        List<List<String>> exportDataList = new ArrayList<>();
        for (Cabinet cabinet : list) {
            List<String> tempList = new ArrayList<>();
            tempList.add(cabinet.getName());
            tempList.add(cabinet.getDc().getName());
            tempList.add(ObjectUtil.defaultIfNull(cabinet.getuSize(), "").toString());
            tempList.add(ObjectUtil.defaultIfNull(cabinet.getY(), "").toString());
            tempList.add(ObjectUtil.defaultIfNull(cabinet.getX(), "").toString());
            tempList.add(ObjectUtil.defaultIfNull(cabinet.getRemark(), ""));
            exportDataList.add(tempList);
        }
        return exportDataList;
    }

    @Override
    public void cancelImport(String seq) {
        this.remove(new LambdaQueryWrapper<Cabinet>().eq(Cabinet::getSeq, seq));
    }

}
