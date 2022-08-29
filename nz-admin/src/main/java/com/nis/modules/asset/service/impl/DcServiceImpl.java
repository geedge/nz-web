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
import com.nis.modules.agent.dao.AgentDao;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.dao.CabinetDao;
import com.nis.modules.asset.dao.DcDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.ipam.entity.IpamIp;
import com.nis.modules.ipam.entity.IpamSubnet;
import com.nis.modules.ipam.service.IpamIpService;
import com.nis.modules.ipam.service.IpamSubnetService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.stat.service.StatService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
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
    private AssetAssetService assetService;

    @Autowired
    private CabinetDao cabinetDao;

    @Autowired
    private AgentDao agentDao;

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

    @Autowired
    private SysUserStarredService starredService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Autowired
    private CabinetService cabinetService;

    @Autowired
    private IpamSubnetService ipamSubnetService;

    @Autowired
    private IpamIpService ipamIpService;

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

        Integer assetNum = assetService.count(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getDcId, dc.getId()));
        dc.setAssetNum(assetNum);

        //收藏判断
        int count = starredService.count(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getTid,dc.getId())
                .eq(SysUserStarredEntity::getType,"datacenter")
                .eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId())
        );
        dc.setStarred(count == 0 ? 0 : 1);

        return dc;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String ids = (String) params.get("ids");
        if (StrUtil.isNotBlank(ids)) {
            List<String> listIds = Arrays.asList(ids.split(","));
            params.put("ids", listIds);
        }

        // 当前登录用户
        params.put("currentLoginUserID", ShiroUtils.getUserId().intValue());

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
    public List<Map> delete(Integer force, Integer... ids) {
        List<Map> delRelObjectMsgList = Tool.ListUtil.list(true);

        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        List<Dc> delDcs = this.list(new LambdaQueryWrapper<Dc>().in(Dc::getId, idList));
        if (Tool.CollUtil.isEmpty(delDcs)) {
            return delRelObjectMsgList;
        }

        delDcs.forEach(dcEntity -> {
            Map<Object, Object> relObjectInfoMap = this.getDcDelRelObjectInfoMap(dcEntity);
            if (Tool.CollUtil.isNotEmpty(relObjectInfoMap)) {
                delRelObjectMsgList.add(relObjectInfoMap);
            }
        });

        force = Tool.ObjectUtil.defaultIfNull(force, 0);
        switch (force) {
            case 0: {// 0：存在关联数据时，删除失败
                if (Tool.CollUtil.isNotEmpty(delRelObjectMsgList)) {
                    return delRelObjectMsgList;
                }
            }
            case 1:  // 1：存在关联数据时，同时删除关联数据
                break;
            default:
                throw new NZException(RCode.ASSET_FORCE_INVALIDE);
        }

        // 删除自身和关联内容
        // dc
        this.removeByIds(idList);

        // cabinet
        List<Cabinet> cabinetList = cabinetService.list(new LambdaQueryWrapper<Cabinet>().in(Cabinet::getDcId, idList));
        if (Tool.CollUtil.isNotEmpty(cabinetList)) {
            Integer[] removeCabinetIdList = cabinetList.stream().map(Cabinet::getId).toArray(n -> new Integer[n]);
            cabinetService.deleteByIds(1, removeCabinetIdList);
        }

        // asset & endpoint
        List<AssetAsset> assetList = assetService.list(new LambdaQueryWrapper<AssetAsset>().in(AssetAsset::getDcId, idList));
        if (Tool.CollUtil.isNotEmpty(assetList)) {
            Integer[] removeAssetIdList = assetList.stream().map(AssetAsset::getId).toArray(n -> new Integer[n]);
            assetService.deleteAsset(1, removeAssetIdList);
        }

        // agent
        agentDao.delete(new LambdaQueryWrapper<Agent>().in(Agent::getDcId, idList));

        // alert
        alertMessageService.removeByDcIds(idList);

        // ipam subnet & ipam ip
        List<IpamSubnet> ipamSubnetList = ipamSubnetService.list(new LambdaQueryWrapper<IpamSubnet>().in(IpamSubnet::getDcId, idList));
        if (Tool.CollUtil.isNotEmpty(ipamSubnetList)) {
            List<Integer> removeIpamSubnetIdList = ipamSubnetList.stream().map(IpamSubnet::getId).collect(Collectors.toList());
            ipamSubnetService.delIpamSubnets(1, removeIpamSubnetIdList);
        }

        // starred
        starredService.remove(
                new LambdaQueryWrapper<SysUserStarredEntity>()
                        .eq(SysUserStarredEntity::getType, "datacenter")
                        .in(SysUserStarredEntity::getTid, idList)
        );
        return delRelObjectMsgList;
    }

    /**
     * 获取 dc 删除提示关联信息：
     *      cabinet
     *      agent
     *      Ipam Subnet
     *        ipam ip
     *      asset
     *        endpoint
     * @return
     */
    private Map<Object, Object> getDcDelRelObjectInfoMap(Dc dc) {
        List<Map> relatesList = Tool.ListUtil.list(true);

        // cabinet
        List<Cabinet> cabinetList = cabinetDao.selectList(new LambdaQueryWrapper<Cabinet>().eq(Cabinet::getDcId, dc.getId()));
        if (Tool.CollUtil.isNotEmpty(cabinetList)) {
            for (Cabinet cabinet : cabinetList) {
                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", cabinet.getId())
                        .put("name", cabinet.getName())
                        .put("type", "cabinet")
                        .build();
                relatesList.add(map);
            }
        }

        // agent
        List<Agent> agentList = agentDao.selectList(new LambdaQueryWrapper<Agent>().eq(Agent::getDcId, dc.getId()));
        if (Tool.CollUtil.isNotEmpty(agentList)) {
            for (Agent agent : agentList) {
                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", agent.getId())
                        .put("name", agent.getName())
                        .put("type", "agent")
                        .build();
                relatesList.add(map);
            }
        }

        // ipam subnet & ipam ip detail
        List<IpamSubnet> ipamSubnetList = ipamSubnetService.list(new LambdaQueryWrapper<IpamSubnet>().eq(IpamSubnet::getDcId, dc.getId()));
        if (Tool.CollUtil.isNotEmpty(ipamSubnetList)) {
            List<Integer> ipamSubnetIdList = ipamSubnetList.stream().map(IpamSubnet::getId).collect(Collectors.toList());
            List<IpamIp> ipamIpList = ipamIpService.list(new LambdaQueryWrapper<IpamIp>().in(IpamIp::getSubnetId, ipamSubnetIdList));

            for (IpamSubnet ipamSubnet : ipamSubnetList) {
                List<IpamIp> ipamIpListBySubnetId = ipamIpList.stream().filter(ipamIp -> ipamSubnet.getId().equals(ipamIp.getSubnetId())).collect(Collectors.toList());
                List<Map> relList = Tool.ListUtil.list(true);
                for (IpamIp ipamIp : ipamIpListBySubnetId) {
                    Map<Object, Object> map = Tool.MapUtil.builder()
                            .put("id", ipamIp.getId())
                            .put("name", ipamIp.getName())
                            .put("type", "ipamIp")
                            .build();
                    relList.add(map);
                }

                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", ipamSubnet.getId())
                        .put("name", ipamSubnet.getName())
                        .put("type", "ipamSubnet")
                        .put("relates", relList)
                        .build();
                relatesList.add(map);
            }
        }

        // asset & endpoint
        List<AssetAsset> assetList = assetService.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getDcId, dc.getId()));
        if (Tool.CollUtil.isNotEmpty(assetList)) {
            List<Integer> assetIdList = assetList.stream().map(AssetAsset::getId).collect(Collectors.toList());
            List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().in(MonitorEndpoint::getAssetId, assetIdList));

            for (AssetAsset asset : assetList) {
                List<MonitorEndpoint> endpointListByAssetId = endpointList.stream().filter(endpoint -> asset.getId().equals(endpoint.getAssetId())).collect(Collectors.toList());
                List<Map> relList = Tool.ListUtil.list(true);
                for (MonitorEndpoint endpoint : endpointListByAssetId) {
                    Map<Object, Object> map = Tool.MapUtil.builder()
                            .put("id", endpoint.getId())
                            .put("name", endpoint.getName())
                            .put("type", "endpoint")
                            .build();
                    relList.add(map);
                }

                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", asset.getId())
                        .put("name", asset.getName())
                        .put("type", "asset")
                        .put("relates", relList)
                        .build();
                relatesList.add(map);
            }
        }

        if (Tool.CollUtil.isNotEmpty(relatesList)) {
            Map<Object, Object> delRelObjectMap = Tool.MapUtil.builder()
                    .put("id", dc.getId())
                    .put("name", dc.getName())
                    .put("type", "dc")
                    .put("relates", relatesList)
                    .build();

            return delRelObjectMap;
        }
        return Tool.MapUtil.empty();
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
        List<AssetAsset> assetList = assetService.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getDcId, dcId));
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
