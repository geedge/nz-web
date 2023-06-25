package com.nis.modules.asset.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.nis.modules.asset.entity.CabinetImportEnum;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private SysUserStarredService starredService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

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

        Long num = alertMessageDao.getAlertNumByCabinetId(id);
        cabinet.setAlertNum(num.intValue());
        // 收藏标识 0：未收藏 1：已收藏
        List<SysUserStarredEntity> tUserStatList = starredService.list(new LambdaQueryWrapper<SysUserStarredEntity>()
                .select(SysUserStarredEntity::getId)
                .eq(SysUserStarredEntity::getTid, cabinet.getId())
                .eq(SysUserStarredEntity::getType, "cabinet")
                .eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId())
                .last("limit 1")
        );
        cabinet.setStarred(CollUtil.isEmpty(tUserStatList) ? 0 : 1);
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

        // 当前登录用户
        params.put("currentLoginUserID", ShiroUtils.getUserId().intValue());

        IPage page = new Query(Cabinet.class).getPage(params);
        List<Cabinet> cabinetList = cabinetDao.queryPage(page, params);

        page.setRecords(cabinetList);
        return new PageUtils(page);
    }

    @Override
    public Integer saveCabinet(Cabinet cabinet) {
        // 校验
        cabinet.setName(StrUtil.trim(cabinet.getName()));
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
        cabinet.setName(StrUtil.trim(cabinet.getName()));
        this.validateCabinetInfo(cabinet);

        // 保存
        this.updateById(cabinet);
        return cabinet.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R deleteByIds(Integer force, Integer... ids) {
        boolean forceData = false;
        List resultList = Tool.ListUtil.list(true);

        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        List<Cabinet> delCabinets = this.list(new LambdaQueryWrapper<Cabinet>().in(Cabinet::getId, idList));
        if (Tool.CollUtil.isEmpty(delCabinets)) {
            return  R.ok().putData("list", resultList);
        }

        for (Cabinet cabinetEntity : delCabinets) {
            Map<Object, Object> relObjectInfoMap = this.getCabinetDelRelObjectInfoMap(cabinetEntity);
            if (Tool.CollUtil.isNotEmpty((List) relObjectInfoMap.get("relates"))) forceData = true;
            resultList.add(relObjectInfoMap);
        }

        force = Tool.ObjectUtil.defaultIfNull(force, 0);
        switch (force) {
            case 0: {// 0：存在关联数据时，删除失败
                if (forceData) {
                    return R.error(RCode.CABINET_RELOBJ_CAN_NOT_REMOVE).putData("list", resultList);
                }
            }
            case 1:  // 1：存在关联数据时，同时删除关联数据
                break;
            default:
                throw new NZException(RCode.ASSET_FORCE_INVALIDE);
        }

        // 删除自身和关联内容
        // cabinet
        this.removeByIds(idList);

        // asset & endpoint
        List<AssetAsset> assetList = assetAssetService.list(new LambdaQueryWrapper<AssetAsset>().in(AssetAsset::getCabinetId, idList));
        if (Tool.CollUtil.isNotEmpty(assetList)) {
            Integer[] removeAssetIdList = assetList.stream().map(AssetAsset::getId).toArray(n -> new Integer[n]);
            assetAssetService.deleteAsset(1, removeAssetIdList);
        }

        // starred
        starredService.remove(
                new LambdaQueryWrapper<SysUserStarredEntity>()
                        .eq(SysUserStarredEntity::getType, "cabinet")
                        .in(SysUserStarredEntity::getTid, idList)
        );
        return R.ok().putData("list", resultList);
    }

    /**
     * 获取 cabinet 删除提示关联信息：
     *      asset
     *        endpoint
     * @return
     */
    private Map<Object, Object> getCabinetDelRelObjectInfoMap(Cabinet cabinet) {
        List<Map> relatesList = Tool.ListUtil.list(true);

        // asset & endpoint
        List<AssetAsset> assetList = assetAssetService.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getCabinetId, cabinet.getId()));
        if (Tool.CollUtil.isNotEmpty(assetList)) {
            List<Integer> assetIdList = assetList.stream().map(AssetAsset::getId).collect(Collectors.toList());
            List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().in(MonitorEndpoint::getAssetId, assetIdList));

            for (AssetAsset asset : assetList) {
                List<MonitorEndpoint> endpointListByAssetId = endpointList.stream().filter(endpoint -> asset.getId().equals(endpoint.getAssetId())).collect(Collectors.toList());
                List<Map> relatesEndpointList = Tool.ListUtil.list(true);
                for (MonitorEndpoint endpoint : endpointListByAssetId) {
                    Map<Object, Object> map = Tool.MapUtil.builder()
                            .put("id", endpoint.getId())
                            .put("name", endpoint.getName())
                            .put("type", "endpoint")
                            .build();
                    relatesEndpointList.add(map);
                }

                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", asset.getId())
                        .put("name", asset.getName())
                        .put("type", "asset")
                        .build();
                if (Tool.CollUtil.isNotEmpty(relatesEndpointList))
                    map.put("relates", relatesEndpointList);
                relatesList.add(map);
            }
        }

        Map<Object, Object> delRelObjectMap = Tool.MapUtil.builder()
                .put("id", cabinet.getId())
                .put("name", cabinet.getName())
                .put("type", "cabinet")
                .put("relates", relatesList)
                .build();

        return delRelObjectMap;
    }

    @Override
    public Map getCabinetUsizeDetails(Integer id) {
        // 获取当前机柜
        Cabinet cabinet = this.getById(id);
        if (cabinet == null) {
            throw new NZException(RCode.CABINET_NOT_EXISTS);
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
    public Map importCabinetData(MultipartFile multipartFile, Map<String, Object> params) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_CABINET_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        // 表头信息 国际化之后的
        List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

        String fileExtension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
        params.put("fileExtension", fileExtension);

        // 返回统计信息
        Map statMap = this.saveCabinetsFromImportDataList(importDataList, headerInfos, params);
        return statMap;
    }

    private Map saveCabinetsFromImportDataList(List<Map<Integer, String>> importDataList, List<String> headerInfos, Map<String, Object> params) {
        String seq = StrUtil.uuid();
        params.put("seq", seq);
        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

        // 创建导入校验对象
        ImportValidateUtil validateUtil = ImportValidateUtil.builder()
                .existed(existed).headerInfos(headerInfos)
                .build();
        List<Dc> dcList = dcService.list();
        Integer addNum = 0, updateNum = 0;
        // 记录导入失败内容
        List<Map<Integer, String>> failContentList = ListUtil.list(true);
        for (int i = 0; i < importDataList.size(); i++) {
            int lineNum = i + 1;
            Map<Integer, String> dataMap = importDataList.get(i);

            String cabinetName = dataMap.get(CabinetImportEnum.NAME.getIndex());
            validateUtil.validateRequired(cabinetName, lineNum, CabinetImportEnum.NAME.getIndex());

            String dcName = dataMap.get(CabinetImportEnum.DC.getIndex());
            validateUtil.validateRequired(dcName, lineNum, CabinetImportEnum.DC.getIndex());
            Boolean dcExits = validateUtil.validateRelObjExits("dc", "name", dcName, false, lineNum, CabinetImportEnum.DC.getIndex());

            Integer yIntVal = null, xIntVal = null;
            String y = dataMap.get(CabinetImportEnum.Y.getIndex());
            if (StrUtil.isNotEmpty(y))
                yIntVal = validateUtil.validateInteger(y, lineNum, CabinetImportEnum.Y.getIndex());

            String x = dataMap.get(CabinetImportEnum.X.getIndex());
            if (StrUtil.isNotEmpty(x))
                xIntVal = validateUtil.validateInteger(x, lineNum, CabinetImportEnum.X.getIndex());

            if (BooleanUtil.and(dcExits, StrUtil.isNotEmpty(cabinetName))) {
                Dc dc = dcList.stream().filter(pojo -> StrUtil.equals(pojo.getName(), dcName)).findFirst().get();
                // 索引重复 & error or ignore
                List<Cabinet> cabinetList = this.list(new LambdaQueryWrapper<Cabinet>().eq(Cabinet::getName, cabinetName).eq(Cabinet::getDcId, dc.getId()));
                if (Tool.CollUtil.isNotEmpty(cabinetList)) {
                    if (StrUtil.equals("0", existed)) {
                        validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(CabinetImportEnum.NAME.getIndex()));
                    } else if (StrUtil.equals("2", existed)) {
                        validateUtil.ignoreThisRow(lineNum, CabinetImportEnum.NAME.getIndex());
                        failContentList.add(dataMap);
                        continue;
                    }
                }
                // 校验行列
                if (Tool.ObjectUtil.isAllNotEmpty(yIntVal, xIntVal)) {
                    if (yIntVal > dc.getY()) {
                        validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(CabinetImportEnum.Y.getIndex()));

                    }
                    if (xIntVal > dc.getX()) {
                        validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(CabinetImportEnum.X.getIndex()));
                    }
                    Cabinet cabinetOfXY = this.getOne(new LambdaQueryWrapper<Cabinet>().eq(Cabinet::getDcId, dc.getId()).eq(Cabinet::getY, yIntVal).eq(Cabinet::getX, xIntVal));
                    if (cabinetOfXY != null) {
                        if (StrUtil.equals("0", existed)) {
                            validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_SUPPORT.setParam(CabinetImportEnum.Y.getIndex()));
                        }
                        if (StrUtil.equals("1", existed) && !StrUtil.equals(cabinetOfXY.getName(), cabinetName)) {
                            validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_SUPPORT.setParam(CabinetImportEnum.Y.getIndex()));
                        }
                    }
                }
            }

            String uSize = dataMap.get(CabinetImportEnum.U_SIZE.getIndex());
            if (StrUtil.isNotEmpty(uSize)) {
                Integer uSizeIntVal = validateUtil.validateInteger(uSize, lineNum, CabinetImportEnum.U_SIZE.getIndex());
                if (uSizeIntVal != null && (uSizeIntVal < 1 || uSizeIntVal > Constant.CURRENT_CABINET_MAXU)) {
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(CabinetImportEnum.U_SIZE.getIndex()));
                }
            }
            String remark = dataMap.get(CabinetImportEnum.REMARK.getIndex());

            // 遇到错误是否继续导入，可选值： 0：不继续    1：继续    默认值：1
            String ignoreError = Tool.ObjectUtil.defaultIfEmpty((String) params.get("ignoreError"), "1");

            List<Map<String, String>> errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
            if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
                // 结束本次导入，自导入行开始 到结尾数据行，提示错误信息
                for (int j = i; j < importDataList.size(); j++) {
                    validateUtil.recordImportAttrErr(j + 1, RCode.IMPORT_ERROR_STOP);
                    failContentList.add(importDataList.get(j));
                }
                break;
            }

            if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
                Dc dc = dcList.stream().filter(pojo -> StrUtil.equals(pojo.getName(), dcName)).findFirst().get();

                Cabinet cabinet = new Cabinet();
                cabinet.setName(cabinetName);
                cabinet.setRemark(remark);
                if (StrUtil.isNotEmpty(x)) cabinet.setX(Double.valueOf(x).intValue());
                if (StrUtil.isNotEmpty(y)) cabinet.setY(Double.valueOf(y).intValue());
                if (StrUtil.isNotEmpty(uSize)) cabinet.setuSize(Double.valueOf(uSize).intValue());
                try {
                    this.saveOrUpdateRowData(dc, cabinet, params);
                    // save or update
                    Object saveFlag = params.get("save");
                    if (ObjectUtil.isNotNull(saveFlag)) {
                        addNum++;
                    } else {
                        updateNum++;
                    }
                } catch (Exception e) {
                    failContentList.add(dataMap);
                    validateUtil.recordImportAttrErr(lineNum, RCode.ERROR);
                    log.error("Cabinet import roll back the transaction failure", e);
                }
            } else {
                failContentList.add(dataMap);
            }
        }
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_CABINET_EXPORT_HEADER);
        String failedContentFileBase64Str = basicImportAndExportServices.getFailedContentFileBase64Str(failContentList, sysHeaderJsonStr, MapUtil.getStr(params, "fileExtension"));
        //  返回统计信息
        Map<Object, Object> statMap = Tool.MapUtil.builder()
                .put("seq", seq)
                .put("totalNum", importDataList.size())
                .put("successNum", addNum + updateNum)
                .put("failNum", failContentList.size())
                .put("addNum", addNum)
                .put("updateNum", updateNum)
                .put("failDetail", validateUtil.getImportErrMsg())
                .put("failContent", failedContentFileBase64Str)
                .build();
        return statMap;
    }

    private void saveOrUpdateRowData(Dc dc, Cabinet cabinet, Map<String, Object> params) {
        MapUtil.removeAny(params, "save", "update");
        String seq = params.get("seq").toString();
        cabinet.setSeq(seq);
        cabinet.setDcId(dc.getId());

        if (ObjectUtil.isEmpty(cabinet.getuSize())) {
            String defaultUSize = sysConfigService.getValue(Constant.SYSCONFIG_KEY_DEFAULT_CABINETU);
            cabinet.setuSize(Integer.valueOf(defaultUSize));
        }

        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore   在此只需要处理更新即可
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");
        Cabinet oldCabinet = this.getOne(new LambdaQueryWrapper<Cabinet>()
                .eq(Cabinet::getName, cabinet.getName())
                .eq(Cabinet::getDcId, dc.getId()));
        if (StrUtil.equals("1", existed) && oldCabinet != null) {
            params.put("update", true);
            // update
            cabinet.setId(oldCabinet.getId());

            LambdaUpdateWrapper<Cabinet> updateWrapper = new LambdaUpdateWrapper<Cabinet>()
                    .eq(Cabinet::getId, cabinet.getId())
                    .set(Cabinet::getY, cabinet.getY())
                    .set(Cabinet::getX, cabinet.getX());
            this.update(cabinet, updateWrapper);
        } else {
            params.put("save", true);
            // insert
            this.save(cabinet);
        }
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

    @Override
    public List<Cabinet> getCabinetWithAssetPositionInfo() {
        return cabinetDao.getCabinetWithAssetPositionInfo();
    }

}
