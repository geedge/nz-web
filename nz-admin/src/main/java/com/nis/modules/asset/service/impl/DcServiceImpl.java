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
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.dao.CabinetDao;
import com.nis.modules.asset.dao.DcDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.stat.service.StatService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    private SysConfigService sysConfigService;

    @Autowired
    private StatService statService;

    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;

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

        List<AlertMessageEntity> alertMessageList = alertMessageService.list(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getDcId, dc.getId()));
        dc.setAlertNum(alertMessageList.size());

        Map<Integer, Long> ruleIdAndAlertCountMap = alertMessageList.parallelStream().collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
        List<Map<String, Object>> alert = statService.getSeverityStatByRuleMsgCount(Tool.MapUtil.empty(), ruleIdAndAlertCountMap);
        dc.setAlert(alert);

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
    @Transactional(rollbackFor = Exception.class)
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
    public void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_DC_EXPORT_HEADER);
        String fileName = "DatacenterTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
    }

    @Override
    public Map importDcData(MultipartFile multipartFile) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_DC_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = this.saveDatacentersFromImportDataList(importDataList, failDetails, new ArrayList<>(exportHeaderMap.keySet()), seqUUID);
        int totalNum = importDataList.size();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    private int saveDatacentersFromImportDataList(List<Map<Integer, String>> importDataList, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
        int successNum = 0;
        for (int i = 0; i < importDataList.size(); i++) {
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            Map<Integer, String> dataMap = importDataList.get(i);

            String name = dataMap.get(0);
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

            String location = dataMap.get(1);
            String tel = dataMap.get(2);

            SysUserEntity principalUser = null;
            String principalName = dataMap.get(3);
            if (StringUtils.isNotEmpty(principalName)) {
                principalUser = sysUserService.getOne(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getName, principalName));
                if (principalUser == null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.DC_PRINCIPAL_NOT_EXISTS.getMsg());
                }
            }

            String longitude = dataMap.get(4);
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

            String latitude = dataMap.get(5);
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

            String state = dataMap.get(6);
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
        PageUtils page = this.queryPage(params);
        List<Dc> list = (List<Dc>) page.getList();
        List<List<String>> exportDataList = this.handleExportDataList(list);

        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_DC_EXPORT_HEADER);
        String fileName = "Datacenters";
        String formatStr = params.get("format").toString();
        basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
    }

    private List<List<String>> handleExportDataList(List<Dc> list) {
        List<SysUserEntity> userList = sysUserService.list();
        Map<Long, String> userIdAndNameMap = userList.stream().collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getName));

        List<List<String>> exportDataList = new ArrayList<>();
        for (Dc dc : list) {
            List<String> tempList = new ArrayList<>();
            tempList.add(dc.getName());
            tempList.add(ObjectUtil.defaultIfNull(dc.getLocation(), ""));
            tempList.add(ObjectUtil.defaultIfNull(dc.getTel(), ""));
            String principalName = dc.getPrincipal() != null ? userIdAndNameMap.get(dc.getPrincipal().longValue()) : "";
            tempList.add(ObjectUtil.defaultIfNull(principalName, ""));
            tempList.add(ObjectUtil.defaultIfNull(dc.getLongitude(), "").toString());
            tempList.add(ObjectUtil.defaultIfNull(dc.getLatitude(), "").toString());
            tempList.add(dc.getState());
            exportDataList.add(tempList);
        }
        return exportDataList;
    }

    @Override
    public void cancelImport(String seq) {
        this.remove(new LambdaQueryWrapper<Dc>().eq(Dc::getSeq, seq));
    }

    @Override
    public List<AssetAsset> getAssetListByDcId(Integer dcId) {
        ValidateUtils.is(dcId).notNull(RCode.DC_ID_ISNULL);
        List<AssetAsset> assetList = assetDao.selectList(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getDcId, dcId));
        return assetList;
    }

    @Override
    public List<MonitorProject> getProjectListByDcId(Integer dcId) {
        ValidateUtils.is(dcId).notNull(RCode.DC_ID_ISNULL);

        List<MonitorProject> projectList = this.baseMapper.queryProjectListByDcId(dcId);
        return projectList;
    }

    @Override
    public List<MonitorModule> getModuleListByDcId(Integer dcId) {
        ValidateUtils.is(dcId).notNull(RCode.DC_ID_ISNULL);

        List<MonitorModule> moduleList = this.baseMapper.queryModuleListByDcId(dcId);
        return moduleList;
    }

    @Override
    public List<MonitorEndpoint> getEndpointListByDcId(Integer dcId) {
        ValidateUtils.is(dcId).notNull(RCode.DC_ID_ISNULL);

        List<MonitorEndpoint> endpointList = this.baseMapper.queryEndpointListByDcId(dcId);
        return endpointList;
    }


}
