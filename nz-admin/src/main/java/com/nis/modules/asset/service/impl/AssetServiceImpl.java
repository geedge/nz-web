package com.nis.modules.asset.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jcraft.jsch.Session;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.dao.*;
import com.nis.modules.asset.entity.*;
import com.nis.modules.asset.service.*;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.metric.dto.MetricsDto;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.PanelService;
import com.nis.modules.sys.dao.SysAreaDao;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysArea;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.terminal.config.Constants;
import com.nis.modules.terminal.entity.Account;
import com.nis.modules.terminal.service.AccountService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.TimeTicks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资产    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("assetService")
public class AssetServiceImpl extends ServiceImpl<AssetDao, Asset> implements AssetService {

    private static Logger logger = LoggerFactory.getLogger(AssetServiceImpl.class);

    @Autowired
    private IdcService idcService;

    @Autowired
    private CabinetService cabinetService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private SysDictService sysDictService;

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private AccountService accountService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private ModelDao modelDao;

    @Autowired
    private CabinetDao cabinetDao;

    @Autowired
    private PanelService panelService;

    @Autowired
    private ChartService chartService;

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private AssetPingDao assetPingDao;

    @Autowired
    private AssetTagService assetTagService;

    @Autowired
    private AssetFeatureService assetFeatureService;

    @Autowired
    private SysAreaDao sysAreaDao;

    @Autowired
    private AssetTagDao assetTagDao;


    /**
     * 处理查询 asset 请求参数
     * @param params
     */
    private void handleQueryParam(Map<String, Object> params){
        String[] vendorIds = {}, typeIds = {};
        if (ToolUtil.isNotEmpty(params.get("vendorIds"))) {
            vendorIds = String.valueOf(params.get("vendorIds")).split(StringPool.COMMA);
        }
        if (ToolUtil.isNotEmpty(params.get("typeIds"))) {
            typeIds = String.valueOf(params.get("typeIds")).split(StringPool.COMMA);
        }
        // assetType/vendor 间接关联 asset 采用关联的 model 方式 查询 asset
        if (vendorIds.length > 0 || typeIds.length > 0) {
            List<Integer> innerModelIds = modelDao.selectIdByDictIds(vendorIds, typeIds);
            if (CollectionUtils.isEmpty(innerModelIds)) {
                innerModelIds.add(-99999);
            }
            params.put("innerModelIds", innerModelIds);
        }
        if (ToolUtil.isNotEmpty(params.get("states"))) {
            params.put("states", String.valueOf(params.get("states")).split(StringPool.COMMA));
        }
        if (ToolUtil.isNotEmpty(params.get("pingStates"))) {
            params.put("pingStates", String.valueOf(params.get("pingStates")).split(StringPool.COMMA));
        }

        // idcids不为空时忽略 idcId 查询参数
        if(ToolUtil.isNotEmpty(params.get("idcIds"))){
            params.put("idcId", null);
            params.put("idcIds", String.valueOf(params.get("idcIds")).split(StringPool.COMMA));
        }

        if (ToolUtil.isNotEmpty(params.get("tags"))) {
            String tagStr = "";
            try {
                tagStr = URLDecoder.decode(String.valueOf(params.get("tags")), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error(String.format("tag url转码失败，转码字符串：%s,错误信息是：%s", params.get("tags"), e.getMessage()), e);
            }
            Map<String, List<String>> tagParams = JSONObject.parseObject(tagStr, Map.class);

            List<AssetTag> tagList = assetTagService.list(new QueryWrapper<AssetTag>().lambda().in(AssetTag::getTag, new ArrayList<>(tagParams.keySet())));
            Map<String, List<AssetTag>> tagKeyAndTagsMap = tagList.stream().collect(Collectors.groupingBy(AssetTag::getTag));

            List<List<Integer>> lists = new ArrayList<>();
            List<String> paramValues;
            List<AssetTag> tagListByKey, tagListByValue;
            for (Map.Entry<String, List<String>> entry : tagParams.entrySet()) {
                // 查询 values
                paramValues = entry.getValue();
                // 当前key 包含的 tag 集合
                tagListByKey = tagKeyAndTagsMap.get(entry.getKey());
                tagListByValue = new ArrayList<>();
                for (String val : paramValues) {
                    tagListByValue.addAll(tagListByKey.stream().filter(assetTag -> assetTag.getValue().equals(val)).collect(Collectors.toList()));
                }
                lists.add(tagListByValue.stream().map(AssetTag::getAssetId).distinct().collect(Collectors.toList()));
            }

            List<Integer> assetIds = lists.stream().flatMap(Collection::stream).collect(Collectors.toList());
            if (lists.size() == 1) {
                assetIds = assetIds.stream().distinct().collect(Collectors.toList());
            } else {
                assetIds = lists.stream().reduce((tempList1, tempList2) -> {
                    tempList1.retainAll(tempList2);
                    return tempList1;
                }).get();
            }
            if (CollectionUtils.isEmpty(assetIds)) {
                assetIds.add(-99999);
            }
            params.put("innerAssetIdsByTagParams", assetIds);
        }

        if (ToolUtil.isNotEmpty(params.get("moduleId"))) {
            List<Endpoint> endpoints = endpointService.list(new QueryWrapper<Endpoint>().lambda().eq(Endpoint::getModuleId, params.get("moduleId")));
            if (CollectionUtils.isEmpty(endpoints)) {
                params.put("innerAssetIdsByModuleId", -99999);
            } else {
                List<Integer> assetIds = endpoints.stream().map(Endpoint::getAssetId).distinct().collect(Collectors.toList());
                params.put("innerAssetIdsByModuleId", assetIds);
            }
        }
    }
    
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        // 处理请求参数
        this.handleQueryParam(params);

        IPage<Asset> page = new Query<Asset>().getPage(params);
        List<Asset> assetEntities = assetDao.queryList(page, params);

        // 查询结果不为空时处理返回结果
        if(CollectionUtils.isNotEmpty(assetEntities)){
            this.handleResuleList(assetEntities);
        }

        page.setRecords(assetEntities);
        return new PageUtils(page);
    }

    /**
     * 处理 asset 结果集
     * @param assetEntities
     */
    private void handleResuleList(List<Asset> assetEntities) {
        // user
        List<SysUserEntity> users = sysUserDao.selectList(null);
        Map<Long, SysUserEntity> userIdAndEntityMap = new HashMap();
        for (SysUserEntity user : users) {
            userIdAndEntityMap.put(user.getUserId(), user);
        }
        // idc area info
        List<SysArea> sysAreas = sysAreaDao.selectList(null);
        Map<Long, SysArea> areaIdAndAreaInfo = new HashMap();
        for (SysArea sysArea : sysAreas) {
            areaIdAndAreaInfo.put(sysArea.getId(), sysArea);
        }
        // account
        List<Account> accountList = accountService.list();
        Map<Integer, List<Account>> assetIdAndAccountsMap = accountList.stream().collect(Collectors.groupingBy(Account::getAssetId));
        // asset Tag
        List<AssetTag> assetTags = assetTagService.list();
        Map<Integer, List<AssetTag>> assetIdAndTagsMap = assetTags.stream().collect(Collectors.groupingBy(AssetTag::getAssetId));
        // alertNum 统计信息
        List<AssetStat> assetIdAndAlertNumsMap = assetDao.getAlertNumsMap();
        Map<Integer, Integer> assetIdAndAlertNumMap = assetIdAndAlertNumsMap.stream().collect(Collectors.toMap(AssetStat::getId, AssetStat::getTotal));
        // module endpoint 统计
        List<Endpoint> endpoints = endpointService.list();
        Map<Integer, List<Endpoint>> assetIdAndEndpointsMap = endpoints.stream().collect(Collectors.groupingBy(Endpoint::getAssetId));
        Map<Integer, Integer> assetIdAndEndpointNumMap = new HashMap<>();
        Map<Integer, Integer> assetIdAndMoudleNumMap = new HashMap<>();
        Integer id;
        List<Endpoint> epList;
        for (Map.Entry<Integer, List<Endpoint>> entry : assetIdAndEndpointsMap.entrySet()) {
            id = entry.getKey();
            epList = entry.getValue();
            assetIdAndEndpointNumMap.put(id, CollectionUtils.isEmpty(epList) ? 0 : epList.size());
            assetIdAndMoudleNumMap.put(id, CollectionUtils.isEmpty(epList) ? 0 : Integer.valueOf((int) epList.stream().map(Endpoint::getModuleId).distinct().count()));
        }
        // 处理返回格式
        String ipmi = null;
        Integer cabinetEnd, alertNum, moduleNum, endpointNum, assetId;
        Idc idc;
        List<Account> accounts;
        for (Asset asset : assetEntities) {
            assetId = asset.getId();
            // ipmi 格式转换   字符串 -> JSON
            ipmi = (String) asset.getIpmi();
            if (StringUtils.isNotEmpty(ipmi)) {
                asset.setIpmi(JSONObject.parseObject(ipmi, Map.class));
            }
            // cabinetEnd 存值的时候会+1 在此减一返回前台
            cabinetEnd = asset.getCabinetEnd();
            if (ToolUtil.isNotEmpty(cabinetEnd)) {
                asset.setCabinetEnd(cabinetEnd - 1);
            }
            // account param 格式转换 字符串 -> JSON
            accounts = assetIdAndAccountsMap.get(assetId);
            if (ToolUtil.isNotEmpty(accounts)) {
                accounts.forEach(pojo -> {
                    if (StringUtils.isNotEmpty((String) pojo.getParams())) {
                        pojo.setParams(JSONObject.parseObject((String) pojo.getParams(), Map.class));
                    }
                });
            }
            asset.setAccounts(accounts);
            // 用户
            asset.setCreateByUser(userIdAndEntityMap.get(asset.getCreateBy()));
            asset.setUpdateByUser(userIdAndEntityMap.get(asset.getUpdateBy()));
            // tag
            asset.setTags(assetIdAndTagsMap.get(assetId));
            // idc area info
            idc = asset.getIdc();
            if (ToolUtil.isNotEmpty(idc.getAreaId())) {
                idc.setArea(areaIdAndAreaInfo.get(idc.getAreaId().longValue()));
            }
            // alertNum
            alertNum = assetIdAndAlertNumMap.get(assetId);
            asset.setAlertNum(ToolUtil.isEmpty(alertNum) ? 0 : alertNum);
            // moduleNum
            moduleNum = assetIdAndMoudleNumMap.get(assetId);
            asset.setModuleNum(ToolUtil.isEmpty(moduleNum) ? 0 : moduleNum);
            // endpointNum
            endpointNum = assetIdAndEndpointNumMap.get(assetId);
            asset.setEndpointNum(ToolUtil.isEmpty(endpointNum) ? 0 : endpointNum);
        }
    }

    /**
     * 校验 asset 相关的信息
     * 1. sn 唯一校验
     * 2. host 地址唯一校验
     * 3. 型号有效性校验
     * 4. ipmi 信息校验
     * 5. 数据中心校验
     * 6. 机柜及资产起始位置校验
     * 7. 资产状态有效性校验
     *
     * @param asset
     */
    private void validateAssetInfo(Asset asset) {
        List<Asset> list = this.list();
        Integer assetId = asset.getId();

        // 1. sn 唯一校验
        Map<String, Integer> map = list.stream().collect(Collectors.toMap(Asset::getSn, Asset::getId));
        Integer id = map.get(asset.getSn());
        if (id != null && !id.equals(assetId)) {
            throw new NZException(RCode.ASSET_SN_DUPLICATE);
        }

        // 2. host 地址唯一校验
        map = list.stream().collect(Collectors.toMap(Asset::getHost, Asset::getId));
        id = map.get(asset.getHost());
        if (id != null && !id.equals(assetId)) {
            throw new NZException(RCode.ASSET_HOST_DUPLICATE);
        }

        // 3. 型号有效性校验
        Model model = modelService.getById(asset.getModelId());
        ValidateUtils.is(model).notNull(RCode.ASSET_MODEL_NOTFOUND);

        // 4. ipmi 信息校验 只有在 assetType 为 server 时才可填写 ipmi 信息
        if (asset.getIpmi() != null) {
            SysDictEntity assetType = sysDictService.getOne(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getType, Constant.DICT_ASSET_MODEL_TYPE).eq(SysDictEntity::getCode, model.getTypeCode()).eq(SysDictEntity::getDelFlag, 0));
            if (!Constant.ASSET_SERVER_TYPE.equals(assetType.getValue().toLowerCase())) {
                throw new NZException(RCode.ASSET_IPMITYPE_ERROR);
            } else {
                // 校验 ipmi 内容格式
                Map<String, Object> ipmiMap = (Map<String, Object>) asset.getIpmi();
                String host = (String) ipmiMap.get("host");
                if (StringUtils.isNotEmpty(host)) {
                    ValidateUtils.is(host).ip(RCode.ASSET_SERVERHOST_ERROR);
                }
                Integer port = (Integer) ipmiMap.get("port");
                if (port != null) {
                    ValidateUtils.is(port).port(RCode.ASSET_SERVERPORT_ERROR);
                }
            }
        }

        // 5. 数据中心校验
        Idc idc = idcService.getById(asset.getIdcId());
        ValidateUtils.is(idc).notNull(RCode.ASSET_IDC_NOTFOUND);

        // 6. 机柜及资产起始位置校验
        Integer cabinetId = asset.getCabinetId();
        Integer start = asset.getCabinetStart();
        Integer end = asset.getCabinetEnd();
        if (cabinetId != null) {
            Cabinet cabinet = cabinetService.getById(cabinetId);
            ValidateUtils.is(cabinet).notNull(RCode.ASSET_CABINET_NOTFOUND);
            if (!idc.getId().equals(cabinet.getIdcId())) {
                // 数据中心与机柜不符  当前数据中心没有含有所选机柜
                throw new NZException(RCode.IDC_NOTHAS_CABINET);
            }
            // 选择了机柜再进行处理 usize 相关数据
            if (start != null && end != null) {
                asset.setCabinetEnd(++end);
                Integer cabuSize = cabinet.getuSize();
                if (start < 0 || end < 0 || start >= end || start > cabuSize || end > cabuSize + 1) {
                    throw new NZException(RCode.ASSET_POSITION_INVALID);
                }
                // 校验所选位置在该机柜中是否被占用
                boolean flag = this.validateUSize(asset, model);
                if (!flag) {
                    throw new NZException(RCode.ASSET_POSITION_OCCUPY);
                }
            }
        } else {
            // 指定了起始位置 但未选择机柜情况
            if (start != null || end != null) {
                throw new NZException(RCode.ASSET_CABINET_ISNULL);
            }
        }

        // 7. 资产状态有效性校验
        if (asset.getState() != null) {
            List<SysDictEntity> assetTypes = sysDictService.list(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getType, Constant.DICT_ASSET_STATE_TYPE).eq(SysDictEntity::getDelFlag, 0));
            List<Integer> stateList = assetTypes.stream().map(SysDictEntity::getCode).collect(Collectors.toList());
            if (!stateList.contains(asset.getState())) {
                // 未找到资产状态
                throw new NZException(RCode.ASSET_STATE_NOTFOUND);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateAsset(Asset asset) {
        // 校验 asset 信息是否正确
        this.validateAssetInfo(asset);

        // 字段入库格式处理
        Object ipmi = asset.getIpmi();
        if (ipmi != null) {
            asset.setIpmi(JSONObject.toJSONString(ipmi));
        }

        // 设置状态默认值
        if (asset.getState() == null) {
            asset.setState(1);
        }

        Date now = DateUtil.getUTCTimeByConfigTimeZone();
        Long userId = ShiroUtils.getUserId();
        asset.setUpdateAt(now);
        asset.setUpdateBy(userId);

        if (asset.getId() == null) {
            asset.setCreateAt(now);
            asset.setCreateBy(userId);

            // 1.新增 asset
            assetDao.saveAsset(asset);

            // 2.如果指名了安装 node exporter 则需要保存 内置 endpoint 信息
            if (Constant.ASSET_BUILD_NODE_EXPORTER_FLAG.equals(asset.getExporter())) {
                this.saveInitEndpoint(asset);
            }

            // 3.新增时需要新增 panel chart
            chartService.saveAssetPanelAndChart(asset);

            // 4. 新增 accounts
            if (CollectionUtils.isNotEmpty(asset.getAccounts())) {
                Integer id = asset.getId();
                asset.getAccounts().forEach(account -> {
                    account.setAssetId(id);
                    accountService.saveOrUpdateAccount(account);
                });
            }
        } else {
            // 留作对比是否更改 model
            Asset oldAsset = this.getById(asset.getId());
            // 1.更新
            this.updateById(asset);

            // 2.更新时处理关联 account 信息
            this.updateAccountByAsset(asset.getAccounts(), asset.getId());

            // 3.如果修改了Model, 处理关联 charts 变化
            if (!oldAsset.getModelId().equals(asset.getModelId())) {
                chartService.updateAssetChartsByNewModel(asset);
            }
        }

        // 保存/修改 asset tag
        assetTagService.saveTagByAssetId(asset.getId(), asset.getTags());
    }


    /**
     * 修改 asset 时 处理关联 account 信息
     * 如果没有指定 account, 则视为删除 asset之前关联的 account 信息
     *
     * @param accounts
     * @param assetId
     */
    private void updateAccountByAsset(List<Account> accounts, Integer assetId) {
        if (CollectionUtils.isEmpty(accounts)) {
            accountService.remove(new QueryWrapper<Account>().lambda().eq(Account::getAssetId, assetId));
        } else {
            // 获取 asset 之前关联的 account 信息
            List<Account> list = accountService.list(new QueryWrapper<Account>().lambda().eq(Account::getAssetId, assetId));
            if (CollectionUtils.isNotEmpty(list)) {
                List<Integer> collect = accounts.stream().filter(account -> ToolUtil.isNotEmpty(account.getId()) ? true : false).map(Account::getId).collect(Collectors.toList());
                List<Integer> collect1 = list.stream().map(Account::getId).collect(Collectors.toList());
                collect1.removeAll(collect);
                if (CollectionUtils.isNotEmpty(collect1)) {
                    accountService.removeByIds(collect1);
                }
            }

            accounts.forEach(account -> {
                account.setAssetId(assetId);
                accountService.saveOrUpdateAccount(account);
            });
        }
    }

    /**
     * 保存内置module 下endpoint信息
     *
     * @param asset
     */
    private void saveInitEndpoint(Asset asset) {
        SysConfigEntity initModule = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_INIT_MODULE));
        if (initModule == null) {
            throw new NZException(RCode.SYS_CONFIG_MODULEKEY_ISNULL);
        }

        // 保存内置的endpoint
        Module module = moduleService.getById(initModule.getParamValue());
        Endpoint endpoint = new Endpoint(module.getId(), asset.getId(), asset.getHost(), module.getPort(), module.getParam(), module.getPath());
        endpointService.save(endpoint);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer... ids) {
        List<Integer> idList = Arrays.asList(ids);
        List<Asset> delAssets = this.list(new QueryWrapper<Asset>().lambda().in(Asset::getId, idList));
        Set<String> delAssetHosts = delAssets.stream().map(Asset::getHost).collect(Collectors.toSet());

        // 1. 删除
        this.removeByIds(idList);

        // 2. 删除关联的endponiet
        List<Endpoint> endpoints = endpointService.list(new QueryWrapper<Endpoint>().lambda().in(Endpoint::getAssetId, idList));
        if (CollectionUtils.isNotEmpty(endpoints)) {
            List<Integer> endpointIds = endpoints.stream().map(Endpoint::getId).collect(Collectors.toList());
            endpointService.deleteEndpointByIds(endpointIds);
        }

        // 3. 删除关联的alert信息
        alertMessageService.removeByAssetIds(idList);

        // 4. 删除关联的 panel chart  chartElement信息
        List<Panel> panels = panelService.list(new QueryWrapper<Panel>().lambda().eq(Panel::getType, Constant.PanelType.ASSET.getValue()).in(Panel::getLink, ids));
        if (CollectionUtils.isNotEmpty(panels)) {
            Set<Integer> panelIds = panels.stream().map(Panel::getId).collect(Collectors.toSet());
            panelService.delPanelsAndChartInfoByPanelIds(panelIds);
        }

        // 5. 删除关联的tag 信息
        assetTagService.remove(new QueryWrapper<AssetTag>().lambda().in(AssetTag::getAssetId, idList));

        // 6. 删除关联的account信息
        accountService.remove(new QueryWrapper<Account>().lambda().in(Account::getAssetId, idList));

        // 7. 删除残留 assetPing 数据
        if (CollectionUtils.isNotEmpty(delAssetHosts)) {
            assetPingDao.delete(new QueryWrapper<AssetPing>().lambda().in(AssetPing::getHost, delAssetHosts));
        }
   }

    @Override
    public void validateAccount(Asset asset, List<Account> accounts) {
        String protocol;
        Integer port;
        Object params;
        for (Account account : accounts) {
            // 1.协议校验
            protocol = account.getProtocol();
            ValidateUtils.is(protocol).notNull(RCode.ACCOUNT_PROTOCOL_ISNULL);
            if (!Constant.ACCOUNT_SSH_PROTOCOL.equals(protocol) && !Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol) && !Constant.ACCOUNT_SNMP_PROTOCOL.equals(protocol)) {
                throw new NZException(RCode.ACCOUNT_PROTOCOL_INCORRECT);
            }

            // 2.端口校验
            port = account.getPort();
            if(ToolUtil.isEmpty(port)){
                // 为空则设置默认值
                switch (protocol){
                    case Constant.ACCOUNT_SSH_PROTOCOL:{
                        account.setPort(22);
                        break;
                    }
                    case Constant.ACCOUNT_TELNET_PROTOCOL:{
                        account.setPort(23);
                        break;
                    }
                    case Constant.ACCOUNT_SNMP_PROTOCOL:{
                        account.setPort(161);
                        break;
                    }
                    default:
                }
            } else {
                boolean portFlag = CommonUtils.checkPort(port.toString());
                if (!portFlag) {
                    throw new NZException(RCode.ACCOUNT_PORT_ERROR);
                }
            }

            // 3.params认证信息校验
            params = account.getParams();
            if (ToolUtil.isEmpty(params)) {
                throw new NZException(RCode.ACCOUNT_PROTOCOL_PARAMS_ISNULL);
            }
            try {
                // 格式校验
                Map map = (Map) params;
                account.setParams(JSONObject.toJSONString(map));
            } catch (Exception e) {
                throw new NZException(RCode.ACCOUNT_PROTOCOL_PARAMS_FORMATERROR);
            }
        }

        // 4.一个协议只能有一个账号
        Map<String, List<Account>> map = accounts.stream().collect(Collectors.groupingBy(Account::getProtocol));
        for (Map.Entry<String, List<Account>> entry : map.entrySet()) {
            if(entry.getValue().size() > 1){
                throw new NZException(RCode.ACCOUNT_PROTOCOL_EXCEED_COUNT);
            }
        }
    }


    @Override
    public List<MetricsDto> queryAssetStatisticsByModel() {
        return assetDao.selectAssetStatisticsByModel();
    }

    @Override
    public List<MetricsDto> queryAssetStatisticsByModelOnLine() {
        return assetDao.selectAssetStatisticsByModelOnLine();
    }

    @Override
    public MetricsDto queryAssetStatisticsByModelOffLine() {
        return assetDao.selectAssetStatisticsByModelOffLine();
    }


    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private TransactionDefinition transactionDefinition;

    @Override
    public Map importAssetsFromExcel(MultipartFile assetsExcelFile, String seqUUID) {
        logger.info("asset 导入文件名是:" + assetsExcelFile.getOriginalFilename() + ",本次随机UUD:" + seqUUID);
        String originalFilename = assetsExcelFile.getOriginalFilename();

        // 校验文件格式
        boolean b = ExcelUtils.validateFileType(originalFilename);
        if (!b) {
            throw new NZException(RCode.ASSET_IMPORTFILE_TYPE);
        }

        Workbook work = null;
        try {
            work = ExcelUtils.getWork(originalFilename, assetsExcelFile.getInputStream());
        } catch (Exception e) {
            logger.error("解析文件格式有误", e);
            throw new NZException(RCode.ASSET_IMPORTFILE_FAILED);
        } finally {
            if (work != null) {
                try {
                    work.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 查询 asset 相关数据 用作导入时间校验 减少循环查表
        Map<String, SysDictEntity> assetTypesMap = new HashMap();
        Map<String, SysDictEntity> vendorsMap = new HashMap();
        List<Integer> assetStateList = new ArrayList<>();

        Map<String, Idc> idcMap = new HashMap();
        Map<String, String> configs = new HashMap<>();

        List<SysDictEntity> sysDictList = sysDictService.list(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getDelFlag, 0));
        sysDictList.stream().forEach(dict -> {
            switch (dict.getType()) {
                case Constant.DICT_ASSET_MODEL_TYPE: {
                    assetTypesMap.put(dict.getValue(), dict);
                    break;
                }
                case Constant.DICT_ASSET_MODEL_VENDOR: {
                    vendorsMap.put(dict.getValue(), dict);
                    break;
                }
                case Constant.DICT_ASSET_STATE_TYPE: {
                    assetStateList.add(dict.getCode());
                    break;
                }
                default:
            }
        });

        // 由于 model 名称可以重读，这里采取list结构存取
        List<Model> modelList = modelService.list();

        List<Idc> idcList = idcService.list();
        for (Idc idc : idcList) {
            idcMap.put(idc.getName(), idc);
        }

        List<Cabinet> cabinetList = cabinetDao.getCabinetWithAssets();

        List<Asset> assetList = this.list();
        List<String> assetSnList = assetList.stream().map(Asset::getSn).collect(Collectors.toList());
        List<String> assetHostList = assetList.stream().map(Asset::getHost).collect(Collectors.toList());

        List<SysConfigEntity> sysConfigs = sysConfigService.list(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getStatus, 1));
        for (SysConfigEntity config : sysConfigs) {
            configs.put(config.getParamKey(), config.getParamValue());
        }

        Integer defaultCabinetUsize = null;
        try {
            defaultCabinetUsize = Integer.parseInt(configs.get(Constant.SYSCONFIG_KEY_DEFAULT_CABINETU));
        } catch (NumberFormatException e) {
            logger.error("配置表中机柜默认值有误 param_key 为 {}", Constant.SYSCONFIG_KEY_DEFAULT_CABINETU, e);
            throw new NZException(RCode.SYS_CONFIG_DEFALUTCABINETU_INTERVAL);
        }

        Map resultInfo = new HashMap();

        // 默认得到第一个 sheet
        Sheet sheet = work.getSheetAt(0);
        // 获得当前sheet的开始行
        int firstRowNum = sheet.getFirstRowNum();
        // 校验表头是否与模板一致
        SysConfigEntity config = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_EXPORT_HEADER));
        HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
        boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, firstRowNum, map.values());
        if(!headerFlag){
            throw new NZException(RCode.ASSET_EXPORT_HEADER_TEMPLATE_ERROR);
        }

        try {
            // 返回导出错误信息
            List<Map> failDetails = new ArrayList<>();
            // 成功条数
            int successNum = 0;
            // 数据在excel表格中的行数
            int dataFromExcelRowNum;

            String sn, type, vendor, model, dcName, cabinetName, cabinetStart, cabinetEnd, host, state, purchaseDate, exporter, ipmiHost, ipmiPort;
            Integer start, end, stateInt;

            Map saveEntitys, ipmiMap;
            Long userId = ShiroUtils.getUserId();
            Model modelEntity = null;
            Asset asset;
            Account accountForUploadExporter;
            Row row;
            List<Account> accountList;

            // 获得当前sheet的结束行
            int maxRowNum = sheet.getLastRowNum();
            for (int i = firstRowNum + 1; i <= maxRowNum; i++) {
                dataFromExcelRowNum = i + 1;
                // 保证一行数据在同一事务内，将所有入库数据统一入库
                saveEntitys = new HashMap();
                start = end = stateInt = null;
                accountList = new ArrayList<>();
                boolean saveFlag = true;
                boolean modelFlag = true;
                asset = new Asset();
                row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                // 资产序列号
                sn = ExcelUtils.getCellValue(row.getCell(0));
                if (StringUtils.isEmpty(sn)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_SN_ISNULL.getMsg());
                } else {
                    // 校验资产序列号是否唯一
                    if (this.checkImportSnOnly(sn, assetSnList)) {
                        asset.setSn(sn);
                    } else {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_SN_DUPLICATE.getMsg());
                    }
                }
                // 资产类型
                type = ExcelUtils.getCellValue(row.getCell(1));
                if (StringUtils.isEmpty(type)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_TYPE_ISNULL.getMsg());
                }
                // 资产类型
                vendor = ExcelUtils.getCellValue(row.getCell(2));
                if (StringUtils.isEmpty(vendor)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_VENDOR_ISNULL.getMsg());
                }
                // 型号
                model = ExcelUtils.getCellValue(row.getCell(3));
                if (StringUtils.isEmpty(model)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_MODELNAME_ISNULL.getMsg());
                }
                // 处理类型 厂商 型号
                if (StringUtils.isNotEmpty(type) && StringUtils.isNotEmpty(vendor) && StringUtils.isNotEmpty(model)) {
                    modelEntity = this.checkAssetTypeVendorModel(type, assetTypesMap, vendor, vendorsMap, model, modelList, seqUUID, saveEntitys);
                    // modelid 可能为null,入库之后返回
                    Integer modelId = modelEntity.getId();
                    asset.setModelId(modelId);
                    // 如果 type 为 server 的情况下 处理 ipmi 信息
                    if (Constant.ASSET_SERVER_TYPE.equals(type.toLowerCase())) {
                        ipmiMap = new HashMap();
                        ipmiHost = ExcelUtils.getCellValue(row.getCell(34));
                        ipmiPort = ExcelUtils.getCellValue(row.getCell(35));
                        // 校验 ipmi信息
                        if (StringUtils.isNotEmpty(ipmiHost)) {
                            if (ipmiHost.matches(Constant.REGEX_IP)) {
                                ipmiMap.put("host", ipmiHost);
                            } else {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_SERVERHOST_ERROR.getMsg());
                            }
                        }
                        if (StringUtils.isNotEmpty(ipmiPort)) {
                            boolean flag = CommonUtils.checkPort(ipmiPort);
                            if (!flag) {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_SERVERPORT_ERROR.getMsg());
                            } else {
                                ipmiMap.put("port", Integer.parseInt(ipmiPort));
                            }
                        }
                        if (ToolUtil.isNotEmpty(ipmiMap)) {
                            asset.setIpmi(JSONObject.toJSONString(ipmiMap));
                        }
                    }
                }
                // DC
                Integer idcId = null;
                dcName = ExcelUtils.getCellValue(row.getCell(4));
                if (StringUtils.isEmpty(dcName)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.IDC_IDCNAME_ISNULL.getMsg());
                } else {
                    idcId = this.checkIdc(dcName, idcMap, seqUUID, saveEntitys);
                    // idcId 可能为null,入库之后返回
                    asset.setIdcId(idcId);
                }
                // cabinet
                cabinetName = ExcelUtils.getCellValue(row.getCell(5));
                cabinetStart = ExcelUtils.getCellValue(row.getCell(6));
                cabinetEnd = ExcelUtils.getCellValue(row.getCell(7));
                if (StringUtils.isNotEmpty(cabinetName) && StringUtils.isNotEmpty(dcName)) {
                    if (StringUtils.isNotEmpty(cabinetStart) && StringUtils.isNotEmpty(cabinetEnd)) {
                        try {
                            start = Integer.parseInt(cabinetStart);
                        } catch (NumberFormatException e) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_CABINETSTART_INVALIDE.getMsg());
                        }
                        try {
                            end = Integer.parseInt(cabinetEnd);
                        } catch (NumberFormatException e) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_CABINETEND_INVALIDE.getMsg());
                        }
                    }
                    if (idcId != null) { // idc已存在，不需要添加
                        Cabinet cabinet = this.checkCabinet(cabinetName, idcId, cabinetList, seqUUID, saveEntitys, defaultCabinetUsize);
                        // cabinetId 可能为null,入库之后返回
                        Integer cabinetId = cabinet.getId();
                        asset.setCabinetId(cabinetId);
                        if (cabinetId == null) {
                            // 因为这里是新增加的机柜 默认usize为 40 ,所以只简单校验一下start end值就可以
                            if (start != null && end != null) {
                                end = end + 1;
                                if (start < 0 || end < 0 || start >= end || start > defaultCabinetUsize || end > defaultCabinetUsize + 1) {
                                    saveFlag = false;
                                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_POSITION_INVALID.getMsg());
                                } else {
                                    asset.setCabinetStart(start);
                                    asset.setCabinetEnd(end);
                                }
                            }
                        } else {
                            // cabinetId不为空  证明此 cabinet 已存在
                            if (start != null && end != null) {
                                end = end + 1;
                                if (start < 0 || end < 0 || start >= end || start > cabinet.getuSize() || end > cabinet.getuSize() + 1) {
                                    saveFlag = false;
                                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_POSITION_INVALID.getMsg());
                                } else {
                                    if (modelEntity != null && modelEntity.getUsize() != null) {
                                        if ((end - start) < modelEntity.getUsize()) {
                                            saveFlag = false;
                                            modelFlag = false;
                                            this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_POSITION_MISMATCH.getMsg());
                                        }
                                    }
                                    // 符合型号大小则继续校验位置是否被占用
                                    if (modelFlag) {
                                        // 参数正确 校验位置是否被占用
                                        boolean flag = this.validateUSizeForImport(start, end, cabinet);
                                        if (!flag) {
                                            saveFlag = false;
                                            this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_POSITION_OCCUPY.getMsg());
                                        } else {
                                            // 位置正确
                                            asset.setCabinetStart(start);
                                            asset.setCabinetEnd(end);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // idc不存在 需要添加之后才能获取idcId供添加cabinet使用
                        Cabinet cabinet = new Cabinet();
                        cabinet.setName(cabinetName);
                        cabinet.setuSize(defaultCabinetUsize);
                        cabinet.setSeq(seqUUID);
                        saveEntitys.put("cabinet", cabinet);
                        // 因为这里是新增加的机柜 默认usize为 40 ,所以只简单校验一下start end值就可以
                        if (start != null && end != null) {
                            end = end + 1;

                            if (modelEntity != null && modelEntity.getUsize() != null) {
                                if ((end - start) < modelEntity.getUsize()) {
                                    saveFlag = false;
                                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_POSITION_MISMATCH.getMsg());
                                }
                            }

                            if (start < 0 || end < 0 || start >= end || start > defaultCabinetUsize || end > defaultCabinetUsize + 1) {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_POSITION_INVALID.getMsg());
                            } else {
                                asset.setCabinetStart(start);
                                asset.setCabinetEnd(end);
                            }
                        }
                    }
                }

                // host
                host = ExcelUtils.getCellValue(row.getCell(8));
                if (StringUtils.isEmpty(host)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_HOST_ISNULL.getMsg());
                } else {
                    // host唯一校验
                    if (this.checkImportHostOnly(host, assetHostList)) {
                        // 格式校验
                        if (host.matches(Constant.REGEX_IP)) {
                            asset.setHost(host);
                        } else {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_HOST_FORMAT.getMsg());
                        }
                    } else {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_HOST_DUPLICATE.getMsg());
                    }
                }

                // state
                state = ExcelUtils.getCellValue(row.getCell(9));
                if (StringUtils.isEmpty(state)) {
                    state = "1";
                }
                try {
                    stateInt = Integer.parseInt(state);
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_STATE_INVALIDE.getMsg());
                }

                // 校验asste state 是否存在
                if (!assetStateList.contains(stateInt)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_STATE_NOTFOUND.getMsg());
                } else {
                    asset.setState(stateInt);
                }

                // 购买日期
                purchaseDate = ExcelUtils.getCellValue(row.getCell(10));
                if (StringUtils.isNotEmpty(purchaseDate)) {
                    // 校验日期格式是否正确
                    if (!DateUtil.isValidDate(purchaseDate, "yyyy-MM-dd")) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_PURCHASEDATE_FORMAT.getMsg());
                    } else {
                        asset.setPurchaseDate(purchaseDate);
                    }
                }

                String sshPort = ExcelUtils.getCellValue(row.getCell(11));
                String sshUserName = ExcelUtils.getCellValue(row.getCell(12));
                String sshMethod = ExcelUtils.getCellValue(row.getCell(13));
                // ssh account 必备选项 如果三者则认为填写ssh账号
                accountForUploadExporter = null;
                if (StringUtils.isNotEmpty(sshPort) && StringUtils.isNotEmpty(sshMethod) && StringUtils.isNotEmpty(sshUserName)) {
                    if (!"key".equals(sshMethod.toLowerCase()) && !"password".equals(sshMethod.toLowerCase())) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_SSH_METHOD_INCORRECT.getMsg());
                    }
                    if (!CommonUtils.checkPort(sshPort)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_PORT_ERROR.getMsg());
                    } else {
                        String sshPassword = ExcelUtils.getCellValue(row.getCell(14));
                        String sshKey = ExcelUtils.getCellValue(row.getCell(15));
                        String sshKeyPassword = ExcelUtils.getCellValue(row.getCell(16));
                        Account exporterSshAccount = this.getImportSshAccount(sshPort, sshUserName, sshMethod, sshPassword, sshKey, sshKeyPassword, seqUUID);
                        accountList.add(exporterSshAccount);
                        // 深克隆 预备登陆服务器上传 node exporter
                        accountForUploadExporter = JSONObject.parseObject(JSON.toJSONString(exporterSshAccount), Account.class);
                    }
                }
                // telnet account 必备选项 都不为空则认为填写 telnet账号
                String telnetPort = ExcelUtils.getCellValue(row.getCell(17));
                String telnetUserName = ExcelUtils.getCellValue(row.getCell(18));
                if (StringUtils.isNotEmpty(telnetPort) && StringUtils.isNotEmpty(telnetUserName)) {
                    if (!CommonUtils.checkPort(telnetPort)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_PORT_ERROR.getMsg());
                    } else {
                        String telnetUserTip = ExcelUtils.getCellValue(row.getCell(19));
                        String telnetPassword = ExcelUtils.getCellValue(row.getCell(20));
                        String telnetPasswordTip = ExcelUtils.getCellValue(row.getCell(21));
                        String telnetReloginTip = ExcelUtils.getCellValue(row.getCell(22));
                        Account exporterTelnetAccount = this.getImportTelnetAccount(telnetPort, telnetUserName, telnetUserTip, telnetPassword, telnetPasswordTip, telnetReloginTip, seqUUID);
                        accountList.add(exporterTelnetAccount);
                    }
                }
                // SNMP 必填项
                String snmpPort = ExcelUtils.getCellValue(row.getCell(23));
                String snmpVersion = ExcelUtils.getCellValue(row.getCell(26));
                if (StringUtils.isNotEmpty(snmpPort) && StringUtils.isNotEmpty(snmpVersion)) {
                    String snmpCommunity = ExcelUtils.getCellValue(row.getCell(24));
                    String snmpContextName = ExcelUtils.getCellValue(row.getCell(25));
                    String snmpUserName = ExcelUtils.getCellValue(row.getCell(27));
                    String snmpSecurityLevel = ExcelUtils.getCellValue(row.getCell(28));
                    String snmpPassword = ExcelUtils.getCellValue(row.getCell(29));
                    String snmpAuthProtocol = ExcelUtils.getCellValue(row.getCell(30));
                    String snmpPrivProtocol = ExcelUtils.getCellValue(row.getCell(31));
                    String snmpPrivPassword = ExcelUtils.getCellValue(row.getCell(32));
                    // 校验snmp account 相关参数
                    if (!this.validateExporterSnmpAccount(snmpPort, snmpVersion, snmpUserName, snmpSecurityLevel, snmpAuthProtocol, snmpPrivProtocol, dataFromExcelRowNum, failDetails)) {
                        saveFlag = false;
                    } else {
                        Account exporterAccount = this.getImportSnmpAccount(snmpPort, snmpCommunity, snmpContextName, snmpVersion, snmpUserName, snmpPassword, snmpSecurityLevel, snmpAuthProtocol, snmpPrivProtocol, snmpPrivPassword, seqUUID);
                        accountList.add(exporterAccount);
                    }
                }
                // tag
                String tagsStr = ExcelUtils.getCellValue(row.getCell(33));
                if (StringUtils.isNotEmpty(tagsStr)) {
                    saveEntitys.put("tags", this.getTagsByTagStr(tagsStr, seqUUID));
                }
                // 是否内置exporter
                exporter = ExcelUtils.getCellValue(row.getCell(36));
                if (Constant.ASSET_BUILD_NODE_EXPORTER_FLAG.toString().equals(exporter)) {
                    asset.setExporter(1);
                }
                // 前面没有报错
                if (saveFlag) {
                    asset.setSeq(seqUUID);
                    asset.setCreateAt(DateUtil.getUTCTimeByConfigTimeZone());
                    asset.setCreateBy(userId);
                    asset.setUpdateBy(userId);
                    saveEntitys.put("asset", asset);
                    // 账号信息
                    if (CollectionUtils.isNotEmpty(accountList)) {
                        saveEntitys.put("accounts", accountList);
                        // 上传 exporter
                        if (Constant.ASSET_BUILD_NODE_EXPORTER_FLAG.toString().equals(exporter) && accountForUploadExporter != null) {
                            Map accountParam = JSONObject.parseObject(accountForUploadExporter.getParams().toString(), Map.class);
                            accountParam.put("password", new String(Base64.getDecoder().decode(accountParam.get("password").toString())));
                            accountForUploadExporter.setParams(JSONObject.toJSONString(accountParam));
                            R r = this.uploadBuildExporter(configs, asset, accountForUploadExporter);
                            Integer code = (Integer) r.get("code");
                            if (!RCode.SUCCESS.getCode().equals(code)) {
                                this.addFailDetail(dataFromExcelRowNum, failDetails, (String) r.get("msg"));
                            }
                        }
                    }
                    // 整体保存这行数据，保证单行数据在同一事务内
                    TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                    try {
                        // 调用入库方法
                        Map updateMap = this.saveRowData(saveEntitys);
                        // 手动提交事务
                        dataSourceTransactionManager.commit(transactionStatus);
                        // 更新 asset 关联的信息 用作后续导入数据对比校验
                        this.updateImportAssetRelatedInfo(updateMap, assetTypesMap, vendorsMap, modelList, idcMap, cabinetList, assetSnList, assetHostList);
                        successNum++;
                    } catch (Exception e) {
                        // 手动回滚事务
                        dataSourceTransactionManager.rollback(transactionStatus);
                        this.addFailDetail(dataFromExcelRowNum, failDetails, e.getMessage());
                        logger.error("回滚事务失败,错误信息:" + e.getMessage(), e);
                    }
                }
            }

            int totalNum = maxRowNum - firstRowNum;
            resultInfo.put("seq", seqUUID);
            resultInfo.put("totalNum", totalNum);
            resultInfo.put("successNum", successNum);
            resultInfo.put("failNum", totalNum - successNum);
            resultInfo.put("failDetail", failDetails);
            logger.info("asset导入成功,信息是 {}", resultInfo);
        } catch (Exception e) {
            logger.error("asset导入失败,错误信息:" + e.getMessage(), e);
        } finally {
            try {
                work.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resultInfo;
    }

    private void updateImportAssetRelatedInfo(Map updateMap, Map<String, SysDictEntity> assetTypesMap, Map<String, SysDictEntity> vendorsMap, List<Model> modelList, Map<String, Idc> idcMap, List<Cabinet> cabinetList, List<String> assetSnList, List<String> assetHostList) {
        SysDictEntity updateType = (SysDictEntity) updateMap.get("type");
        if (updateType != null) {
            assetTypesMap.put(updateType.getValue(), updateType);
        }
        SysDictEntity updateVendor = (SysDictEntity) updateMap.get("vendor");
        if (updateVendor != null) {
            vendorsMap.put(updateVendor.getValue(), updateVendor);
        }
        Model updateModel = (Model) updateMap.get("model");
        if (updateModel != null) {
            modelList.add(updateModel);
        }
        Idc updateIdc = (Idc) updateMap.get("idc");
        if (updateIdc != null) {
            idcMap.put(updateIdc.getName(), updateIdc);
        }
        Cabinet updateCabinet = (Cabinet) updateMap.get("cabinet");
        if (updateCabinet != null) {
            cabinetList.add(updateCabinet);
        }
        Asset updateAsset = (Asset) updateMap.get("asset");
        if (updateAsset != null) {
            assetSnList.add(updateAsset.getSn());
            assetHostList.add(updateAsset.getHost());
            if (updateAsset.getCabinetStart() != null && updateAsset.getCabinetEnd() != null) {
                // 更新cabinetList中对应的关系
                for (Cabinet cabinet : cabinetList) {
                    if (cabinet.getId().equals(updateAsset.getCabinetId())) {
                        if (CollectionUtils.isEmpty(cabinet.getAssetList())) {
                            cabinet.setAssetList(new ArrayList<>());
                        }
                        cabinet.getAssetList().add(updateAsset);
                        break;
                    }
                }
            }
        }
    }

    private Account getImportSshAccount(String sshPort, String sshUserName, String sshMethod, String sshPassword, String sshKey, String sshKeyPassword, String seqUUID) {
        Account account = new Account();
        account.setProtocol("SSH");
        account.setPort(Integer.parseInt(sshPort));
        Map<String, String> sshParam = new HashMap<>();
        sshParam.put("username", sshUserName);
        if (StringUtils.isNotEmpty(sshPassword)) {
            sshParam.put("password", Base64.getEncoder().encodeToString(sshPassword.getBytes()));
        } else {
            sshParam.put("password", "");
        }
        sshParam.put("method", sshMethod);
        sshParam.put("key", sshKey);
        sshParam.put("passwordKey", sshKeyPassword);

        account.setParams(JSONObject.toJSONString(sshParam));
        account.setSeq(seqUUID);
        return account;
    }

    private Account getImportTelnetAccount(String telnetPort, String telnetUserName, String telnetUserTip, String telnetPassword, String telnetPasswordTip, String telnetReloginTip, String seqUUID) {
        Account account = new Account();
        account.setProtocol("TELNET");
        account.setPort(Integer.parseInt(telnetPort));
        Map<String, String> telnetParam = new HashMap<>();
        telnetParam.put("username", telnetUserName);
        if (StringUtils.isNotEmpty(telnetPassword)) {
            telnetParam.put("password", Base64.getEncoder().encodeToString(telnetPassword.getBytes()));
        } else {
            telnetParam.put("password", "");
        }
        telnetParam.put("userTip", telnetUserTip);
        telnetParam.put("passwordTip", telnetPasswordTip);
        telnetParam.put("reloginTip", telnetReloginTip);

        account.setParams(JSONObject.toJSONString(telnetParam));
        account.setSeq(seqUUID);
        return account;
    }

    private Account getImportSnmpAccount(String snmpPort, String snmpCommunity, String snmpContextName, String snmpVersion, String snmpUserName, String snmpPassword, String snmpSecurityLevel, String snmpAuthProtocol, String snmpPrivProtocol, String snmpPrivPassword, String seqUUID) {
        Account account = new Account();
        Map<String, Object> snmpParam = new HashMap<>();
        snmpParam.put("community", snmpCommunity);
        snmpParam.put("contextName", snmpContextName);
        snmpParam.put("version", Integer.parseInt(snmpVersion));
        if (StringUtils.isNotEmpty(snmpPassword)) {
            snmpParam.put("password", Base64.getEncoder().encodeToString(snmpPassword.getBytes()));
        } else {
            snmpParam.put("password", "");
        }
        snmpParam.put("securityLevel", snmpSecurityLevel);
        snmpParam.put("username", snmpUserName);
        snmpParam.put("authProtocol", snmpAuthProtocol);
        snmpParam.put("privProtocol", snmpPrivProtocol);
        snmpParam.put("privPassword", snmpPrivPassword);

        account.setProtocol("SNMP");
        account.setPort(Integer.parseInt(snmpPort));
        account.setParams(JSONObject.toJSONString(snmpParam));
        account.setSeq(seqUUID);
        return account;
    }

    private boolean validateExporterSnmpAccount(String snmpPort, String snmpVersion, String snmpUserName, String snmpSecurityLevel, String snmpAuthProtocol, String snmpPrivProtocol, int dataFromExcelRowNum, List<Map> failDetails) {
        boolean saveFlag = true;
        if (!CommonUtils.checkPort(snmpPort)) {
            saveFlag = false;
            this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_PORT_ERROR.getMsg());
        }
        switch (snmpVersion) {
            case "2": {
                break;
            }
            case "3": {
                if (StringUtils.isEmpty(snmpUserName)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_SNMP_USERNAME_ISNULL.getMsg());
                }
                // 验证级别不为空验证相关参数
                if (StringUtils.isNotEmpty(snmpSecurityLevel)) {
                    if (!"noAuthNoPriv".equals(snmpSecurityLevel) && !"authNoPriv".equals(snmpSecurityLevel) && !"authPriv".equals(snmpSecurityLevel)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_SNMP_SECURITYLEVEL_INVALID.getMsg());
                    }
                    if ("authNoPriv".equals(snmpSecurityLevel)) {
                        if (!"MD5".equals(snmpAuthProtocol) && !"SHA".equals(snmpAuthProtocol)) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_SNMP_AUTHPROTOCOL_INVALIDE.getMsg());
                        }
                    }
                    if ("authPriv".equals(snmpSecurityLevel)) {
                        if (!"MD5".equals(snmpAuthProtocol) && !"SHA".equals(snmpAuthProtocol)) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_SNMP_AUTHPROTOCOL_INVALIDE.getMsg());
                        }
                        if (!"DES".equals(snmpPrivProtocol) && !"AES".equals(snmpPrivProtocol)) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_SNMP_PRIVPROTOCOL_INVALIDE.getMsg());
                        }
                    }
                }
                break;
            }
            default: {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_SNMP_VERSION_INVALIDE.getMsg());
            }
        }
        return saveFlag;
    }

    /**
     * 根据 tag 字符串获取tag实体
     *
     * @param tagsStr
     * @return
     */
    private List<AssetTag> getTagsByTagStr(String tagsStr, String seqUUID) {
        List<AssetTag> resultTags = new ArrayList<>();
        AssetTag assetTag;
        List<String> list = Arrays.asList(tagsStr.split("\n"));
        for (String str : list) {
            if (str.contains("=")) {
                assetTag = new AssetTag();
                String[] split = str.split("=");
                assetTag.setTag(split[0]);
                assetTag.setValue(split[1]);
                assetTag.setSeq(seqUUID);
                resultTags.add(assetTag);
            }
        }
        return resultTags;
    }

    /**
     * 添加错误信息
     *
     * @param i
     * @param failDetails
     * @param msg
     */
    private void addFailDetail(int i, List<Map> failDetails, String msg) {
        Map failDetail = new HashMap();
        failDetail.put("lineNo", i);
        failDetail.put("errorMsg", msg);
        failDetails.add(failDetail);
    }

    public Map saveRowData(Map saveEntitys) {
        Map updateMap = new HashMap();
        // 资产类型
        SysDictEntity type = (SysDictEntity) saveEntitys.get("type");
        if (type != null) {
            sysDictService.saveSysDict(type);
            updateMap.put("type", type);
        }
        // 厂商
        SysDictEntity vendor = (SysDictEntity) saveEntitys.get("vendor");
        if (vendor != null) {
            sysDictService.saveSysDict(vendor);
            updateMap.put("vendor", vendor);
        }
        // 型号
        Model model = (Model) saveEntitys.get("model");
        if (model != null) {
            if (type != null) {
                model.setTypeCode(type.getCode());
            }
            if (vendor != null) {
                model.setVendorCode(vendor.getCode());
            }
            modelService.save(model);
            updateMap.put("model", model);
            // model 添加的同时添加 panel chart
            modelService.saveModelPanelAndChart(model);
        }
        // DC
        Idc idc = (Idc) saveEntitys.get("idc");
        // Cabinet
        Cabinet cabinet = (Cabinet) saveEntitys.get("cabinet");
        if (idc == null && cabinet != null) {
            cabinetService.save(cabinet);
            updateMap.put("cabinet", cabinet);
        }
        if (idc != null) {
            idcService.save(idc);
            if (cabinet != null) {
                cabinet.setIdcId(idc.getId());
                cabinetService.save(cabinet);
                updateMap.put("cabinet", cabinet);
            }
            updateMap.put("idc", idc);
        }
        // asset
        Asset asset = (Asset) saveEntitys.get("asset");
        if (asset.getModelId() == null && model != null) {
            asset.setModelId(model.getId());
        }
        if (asset.getIdcId() == null && idc != null) {
            asset.setIdcId(idc.getId());
        }
        if (asset.getCabinetId() == null && cabinet != null) {
            asset.setCabinetId(cabinet.getId());
        }
        this.save(asset);
        // 如果指明上传 exporter 需要添加关联 exporter
        if (Constant.ASSET_BUILD_NODE_EXPORTER_FLAG.equals(asset.getExporter())) {
            this.saveInitEndpoint(asset);
        }
        updateMap.put("asset", asset);
        // accounts
        List<Account> accounts = (List<Account>) saveEntitys.get("accounts");
        if (CollectionUtils.isNotEmpty(accounts)) {
            accounts.forEach(account -> account.setAssetId(asset.getId()));
            accountService.saveBatch(accounts);
        }
        // tags
        List<AssetTag> tags = (List<AssetTag>) saveEntitys.get("tags");
        if (CollectionUtils.isNotEmpty(tags)) {
            tags.forEach(assetTag -> assetTag.setAssetId(asset.getId()));
            assetTagService.saveBatch(tags);
        }
        // asset 添加的同时添加 panel chart
        chartService.saveAssetPanelAndChart(asset);
        return updateMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelImport(String seq) {
        List<Asset> assets = this.list(new QueryWrapper<Asset>().lambda().eq(Asset::getSeq, seq));
        List<Integer> assetIds = assets.stream().map(Asset::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(assetIds)) {
            // asset
            this.remove(new QueryWrapper<Asset>().lambda().eq(Asset::getSeq, seq));
            // idc
            idcService.remove(new QueryWrapper<Idc>().lambda().eq(Idc::getSeq, seq));
            // cabinet
            cabinetService.remove(new QueryWrapper<Cabinet>().lambda().eq(Cabinet::getSeq, seq));
            List<Model> models = modelService.list(new QueryWrapper<Model>().lambda().eq(Model::getSeq, seq));
            List<Integer> modelIds = models.stream().map(Model::getId).collect(Collectors.toList());
            // model
            modelService.remove(new QueryWrapper<Model>().lambda().eq(Model::getSeq, seq));
            // sys dict （asset type ,vendor）
            sysDictService.remove(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getSeq, seq));
            // account
            accountService.remove(new QueryWrapper<Account>().lambda().eq(Account::getSeq, seq));
            // tag
            assetTagService.remove(new QueryWrapper<AssetTag>().lambda().eq(AssetTag::getSeq, seq));
            // endpoint
            endpointService.remove(new QueryWrapper<Endpoint>().lambda().in(Endpoint::getAssetId, assetIds));
            // 导入 asset 关联的 panel chart 信息
            List<Panel> assetPanels = panelService.list(new QueryWrapper<Panel>().lambda().eq(Panel::getType, Constant.PanelType.ASSET.getValue()).in(Panel::getLink, assetIds));
            if (CollectionUtils.isNotEmpty(assetPanels)) {
                Set<Integer> panelIds = assetPanels.stream().map(Panel::getId).collect(Collectors.toSet());
                panelService.delPanelsAndChartInfoByPanelIds(panelIds);
            }
            // 导入 model 关联的 panel chart 信息
            List<Panel> modelPanels = panelService.list(new QueryWrapper<Panel>().lambda().eq(Panel::getType, Constant.PanelType.MODEL.getValue()).in(Panel::getLink, modelIds));
            if (CollectionUtils.isNotEmpty(modelPanels)) {
                Set<Integer> panelIds = modelPanels.stream().map(Panel::getId).collect(Collectors.toSet());
                panelService.delPanelsAndChartInfoByPanelIds(panelIds);
            }
        }
    }

    @Override
    public R uploadBuildExporter(Map<String, String> configs, Asset asset, Account account) {
        if (ToolUtil.isEmpty(configs)) {
            List<SysConfigEntity> sysConfigs = sysConfigService.list(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getStatus, 1));
            configs = sysConfigs.stream().filter(entity -> ToolUtil.isNotEmpty(entity.getParamValue())).collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));
        }
        // 内置 node_exporter 默认安装服务器路径
        String targetExporterPath = configs.get(Constant.SYSCONFIG_KEY_NODEEXPORTER_TARGETPATH);
        // 内置 node_exporter 初始执行脚本名称
        String templateShellName = configs.get(Constant.SYSCONFIG_KEY_NODEEXPORTER_SHELLNAME);
        // 内置 node_exporter 项目路径
        String buildNodeExporterPath = configs.get(Constant.SYSCONFIG_KEY_NODEEXPORTER_PATH);
        // 内置 node_exporter 压缩包名称
        String buildNodeExporterName = configs.get(Constant.SYSCONFIG_KEY_NODEEXPORTER_NAME);

        // 临时脚本文件
        File file = null;

        // 临时脚本 用于替换上传路径
        String tempShellName = "tempShell_" + UUID.randomUUID() + ".sh";
        File shellFile = new File(buildNodeExporterPath + File.separator + templateShellName);
        if (!shellFile.exists()) {
            logger.error("内置exporter脚本文件不存在");
        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(shellFile));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(file = new File(buildNodeExporterPath + File.separator + tempShellName)))) {
                String str = null;
                while ((str = br.readLine()) != null) {
                    str = str.replaceAll(Constant.NODEEXPORTER_SHELLREPLACE_PATH, targetExporterPath);

                    bw.write(str + "\n");
                    bw.flush();
                }
            } catch (IOException e) {
                logger.error("替换脚本文件中上传目标路径失败", e);
                return R.error(RCode.ASSET_UPLOADFILE_FAILED);
            }
        }

        SftpUtils sftpUtils = new SftpUtils();
        try {
            // 登录
            try {
                Map<String, String> params = JSONObject.parseObject((String) account.getParams(), Map.class);
                if(ToolUtil.isEmpty(params)){
                    params = new HashMap<>();
                }
                Integer authType = null;
                String method = params.get("method");
                if (Constants.SshType.ACCOUNT.getValue().equals(method)) {
                    authType = 1;
                } else if (Constants.SshType.SSHKEY.getValue().equals(method)) {
                    authType = 2;
                }
                sftpUtils.loginIn(asset.getHost(), account.getPort(), params.get("username"), authType, params.get("password"), params.get("key"), params.get("keyPassword"));
            } catch (Exception e) {
                logger.error(String.format("用户登录sftp服务器失败,地址是：%s:%s,认证信息是：%s", asset.getHost(), account.getPort(), (String) account.getParams()), e);
                return R.error(RCode.ASSET_LOGINSERVER_FAILED);
            }

            // 上传压缩包和执行脚本
            try {
                sftpUtils.upload(targetExporterPath, buildNodeExporterPath, buildNodeExporterName, tempShellName);
            } catch (Exception e) {
                logger.error(String.format("上传脚本失败，错误信息：%s，目标路径：%s,内置文件路径：%s,文件名称：%s", e.getMessage(), targetExporterPath, buildNodeExporterPath, buildNodeExporterName), e);
                return R.error(RCode.ASSET_UPLOADFILE_FAILED);
            }

            // 执行脚本让exporter运行
            String execCommand = "chmod +x " + targetExporterPath + "/" + tempShellName + " && " + targetExporterPath + "/" + tempShellName;
            try {
                sftpUtils.exec(execCommand);
            } catch (Exception e) {
                logger.error(String.format("执行脚本文件失败,错误信息：%s,执行命令：%s", e.getMessage(), execCommand), e);
                return R.error(RCode.ASSET_EXECCOMMAND_FAILED);
            }
        } finally {
            // 退出
            sftpUtils.logout();
            // 删除临时文件
            if (file != null) {
                file.delete();
            }
        }
        return R.ok();
    }

    @Override
    public Map detail(Integer id) {
        ValidateUtils.is(id).notNull(RCode.ASSET_ID_ISNULL);

        Asset asset = this.getById(id);
        if(ToolUtil.isEmpty(asset)){
            throw new NZException(RCode.ASSET_NOT_EXIST);
        }

        Map resultMap = new HashMap();
        Map basicInfo = new HashMap();

        basicInfo.put("host", asset.getHost());
        basicInfo.put("id", asset.getId());
        basicInfo.put("sn", asset.getSn());
        basicInfo.put("state", asset.getState());
        basicInfo.put("purchaseDate", asset.getPurchaseDate());

        Idc idc = idcService.getById(asset.getIdcId());
        if (ToolUtil.isNotEmpty(idc.getPrincipal())) {
            SysUserEntity userEntity = sysUserDao.selectOne(new QueryWrapper<SysUserEntity>().lambda().eq(SysUserEntity::getUserId, idc.getPrincipal()));
            basicInfo.put("principal", userEntity.getUsername());
        } else {
            basicInfo.put("principal", "");
        }

        basicInfo.put("dataCenter",idc.getName());

        basicInfo.put("tel", idc.getTel());

        if (ToolUtil.isEmpty(asset.getCabinetId())) {
            basicInfo.put("cabinet", "");
        } else {
            Cabinet cabinet = cabinetDao.selectById(asset.getCabinetId());
            basicInfo.put("cabinet", cabinet.getName());
        }

        Model model = modelDao.getModelDetailById(asset.getModelId());
        basicInfo.put("model",model.getName() );
        basicInfo.put("assetType",model.getType().getValue());
        basicInfo.put("vendor",model.getVendor().getValue());

        int endpointNum = endpointService.count(new QueryWrapper<Endpoint>().lambda().eq(Endpoint::getAssetId, id));
        basicInfo.put("endpoint", endpointNum);

        int alertCount = alertMessageService.count(new QueryWrapper<AlertMessageEntity>().lambda().eq(AlertMessageEntity::getState, 1).eq(AlertMessageEntity::getAssetId, id));
        basicInfo.put("alert", alertCount);

        AssetPing assetPing = assetPingDao.selectOne(new QueryWrapper<AssetPing>().lambda().eq(AssetPing::getHost, asset.getHost()));
        if (ToolUtil.isEmpty(assetPing)) {
            basicInfo.put("pingStatus", "");
            basicInfo.put("pingRtt", "");
            basicInfo.put("pingLastReply", "");
        } else {
            basicInfo.put("pingStatus", assetPing.getStatus());
            basicInfo.put("pingRtt", assetPing.getRtt());
            basicInfo.put("pingLastReply", assetPing.getLastUpdate());
        }

        // asset  tags
        List<AssetTag> tags = assetTagService.list(new QueryWrapper<AssetTag>().lambda().eq(AssetTag::getAssetId, asset.getId()));
        basicInfo.put("tags", tags);

        resultMap.put("Basic", basicInfo);


        Map feature = new LinkedHashMap();
        Map assetFeatureHandle = new HashMap();
        List<AssetFeature> assetFeatures = assetFeatureService.list(new QueryWrapper<AssetFeature>().lambda().eq(AssetFeature::getAssetId, asset.getId()));
        for(AssetFeature assetFeature : assetFeatures) {
        	assetFeatureHandle.put(assetFeature.getItem(),assetFeature.getValue());
        }
        feature.put("hostname", assetFeatureHandle.get("hostname"));
        feature.put("cpu", JSON.parse((String)(assetFeatureHandle.get("cpu")),Feature.OrderedField));
        feature.put("memory", assetFeatureHandle.get("memory"));
        feature.put("disk", JSON.parse((String)(assetFeatureHandle.get("disk")),Feature.OrderedField));
        feature.put("interface", JSON.parse((String)(assetFeatureHandle.get("interface")),Feature.OrderedField));
        feature.put("core", assetFeatureHandle.get("core"));
        feature.put("os", assetFeatureHandle.get("os"));
        feature.put("uptime", assetFeatureHandle.get("uptime"));
        feature.put("sysName", assetFeatureHandle.get("sysName"));
        feature.put("sysLocation", assetFeatureHandle.get("sysLocation"));
        feature.put("sysContact", assetFeatureHandle.get("sysContact"));
        feature.put("sysServices", assetFeatureHandle.get("sysServices"));
        feature.put("sysUpTime", assetFeatureHandle.get("sysUpTime"));
        feature.put("ifNumber", assetFeatureHandle.get("ifNumber"));
        feature.put("ifEntry", JSON.parse((String)(assetFeatureHandle.get("ifEntry")),Feature.OrderedField));
        Iterator<Map.Entry<String, String>> it = feature.entrySet().iterator();
        // 清空值为null的键值对信息
        while(it.hasNext()){
            Map.Entry<String, String> entry = it.next();
            if(entry.getValue() == null){
                // 使用迭代器的remove()方法删除元素
                it.remove();
            }
        }
        // 如果都为空 则不响应 Attribute 信息
        if(ToolUtil.isNotEmpty(feature)) {
            resultMap.put("Attribute",feature);
        }
        return resultMap;
    }

    /**
     * 导出
     *
     * @param response
     * @param params
     */
    @Override
    public void export(HttpServletResponse response, Map<String, Object> params) {
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);

        PageUtils page = this.queryPage(params);
        List<Asset> list = (List<Asset>) page.getList();

        SXSSFWorkbook workbook = null;
        try {
            // 添加标题行 默认语言 en
            String language = (String) params.get("language");
            language = ToolUtil.isEmpty(language) ? "en" : language;

            // 去掉最后一列 Exporter 作为导出模板
            Map<String, String> exportHeader = (Map) map.get(language);
            List<String> keys = new ArrayList<>(exportHeader.keySet());
            String exporter = keys.get(keys.size() - 1);
            exportHeader.remove(exporter);

            String sheetName = "Assets";
            workbook = ExcelUtils.createExcel07(exportHeader, sheetName, false);
            SXSSFSheet sheet = workbook.getSheet(sheetName);

            // 填充数据
            this.fillData(list, 1, sheet, workbook);

            // 响应为流
            String fileName = "AssetsExport.xlsx"; // 默认名称
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            logger.error("asset导出文件生成失败，错误信息是：" + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("asset导出失败，参数是：" + params.toString() + " ，错误信息是：" + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void fillData(List<Asset> list, int rowNum, Sheet sheet, SXSSFWorkbook workbook) {
        List<SysDictEntity> assetStates = sysDictService.list(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getType, Constant.DICT_ASSET_STATE_TYPE).eq(SysDictEntity::getDelFlag, 0));
        Map<Integer, String> statesMap = assetStates.stream().collect(Collectors.toMap(SysDictEntity::getCode, SysDictEntity::getValue));

        Row row;
        Model model;
        Integer start, end, ipmiPort;
        String ipmiHost, tagStr;
        Map ipmiMap;
        for (Asset asset : list) {
            row = sheet.createRow(rowNum++);

            model = asset.getModel();
            row.createCell(0).setCellValue(asset.getSn());
            row.createCell(1).setCellValue(model.getType().getValue());
            row.createCell(2).setCellValue(model.getVendor().getValue());
            row.createCell(3).setCellValue(model.getName());
            row.createCell(4).setCellValue(asset.getIdc().getName());
            row.createCell(5).setCellValue(asset.getCabinet() == null ? null : asset.getCabinet().getName());
            start = asset.getCabinetStart();
            row.createCell(6).setCellValue(start == null ? "" : start + "");
            end = asset.getCabinetEnd();
            row.createCell(7).setCellValue(end == null ? "" : end + "");
            row.createCell(8).setCellValue(asset.getHost());
            row.createCell(9).setCellValue(statesMap.get(asset.getState()));
            row.createCell(10).setCellValue(asset.getPurchaseDate());

            if (CollectionUtils.isNotEmpty(asset.getAccounts())) {
                this.exportFillAccountData(row, asset.getAccounts());
            }

            if (CollectionUtils.isNotEmpty(asset.getTags())) {
                tagStr = "";
                for (AssetTag tag : asset.getTags()) {
                    tagStr += tag.getTag() + "=" + tag.getValue() + "\n";
                }
                row.createCell(33).setCellValue(tagStr);
            }
            ipmiMap = JSONObject.parseObject(JSONObject.toJSONString(asset.getIpmi()), Map.class);
            if (ToolUtil.isNotEmpty(ipmiMap)) {
                ipmiHost = (String) ipmiMap.get("host");
                if (ToolUtil.isNotEmpty(ipmiHost)) {
                    row.createCell(34).setCellValue(ipmiHost);
                }
                ipmiPort = (Integer) ipmiMap.get("port");
                if (ToolUtil.isNotEmpty(ipmiPort)) {
                    row.createCell(35).setCellValue(ipmiPort);
                }
            }

            // 设置单元格样式
            ExcelUtils.setDataCellStyle(workbook, row, 35);
        }
    }

    private void exportFillAccountData(Row row, List<Account> accountList) {
        List<Account> tempAccounts = accountList.stream().filter(account1 -> Constant.ACCOUNT_SSH_PROTOCOL.equals(account1.getProtocol())).collect(Collectors.toList());
        Account account = CollectionUtils.isNotEmpty(tempAccounts) ? tempAccounts.get(0) : null;
        Map<String, Object> accountParam;
        Base64.Decoder decoder = Base64.getDecoder();
        if (account != null) {
            row.createCell(11).setCellValue(account.getPort());
            accountParam = (Map<String, Object>) account.getParams();
            row.createCell(12).setCellValue(accountParam.get("username").toString());
            row.createCell(13).setCellValue(accountParam.get("method").toString());
            String method = accountParam.get("method").toString();
            if ("password".equals(method)) {
                row.createCell(14).setCellValue(new String(decoder.decode(accountParam.get("password").toString())));
            } else if ("key".equals(method)) {
                row.createCell(15).setCellValue(accountParam.get("key").toString());
                row.createCell(16).setCellValue(accountParam.get("passwordKey").toString());
            }
        }
        tempAccounts = accountList.stream().filter(account1 -> Constant.ACCOUNT_TELNET_PROTOCOL.equals(account1.getProtocol())).collect(Collectors.toList());
        account = CollectionUtils.isNotEmpty(tempAccounts) ? tempAccounts.get(0) : null;
        if (account != null) {
            row.createCell(17).setCellValue(account.getPort());
            accountParam = (Map<String, Object>) account.getParams();
            row.createCell(18).setCellValue(accountParam.get("username").toString());
            row.createCell(19).setCellValue(accountParam.get("userTip").toString());
            row.createCell(20).setCellValue(new String(decoder.decode(accountParam.get("password").toString())));
            row.createCell(21).setCellValue(accountParam.get("passwordTip").toString());
            row.createCell(22).setCellValue(accountParam.get("reloginTip").toString());
        }
        tempAccounts = accountList.stream().filter(account1 -> Constant.ACCOUNT_SNMP_PROTOCOL.equals(account1.getProtocol())).collect(Collectors.toList());
        account = CollectionUtils.isNotEmpty(tempAccounts) ? tempAccounts.get(0) : null;
        if (account != null) {
            row.createCell(23).setCellValue(account.getPort());
            accountParam = (Map<String, Object>) account.getParams();
            row.createCell(24).setCellValue(accountParam.get("community").toString());
            row.createCell(25).setCellValue(accountParam.get("contextName").toString());
            row.createCell(26).setCellValue(accountParam.get("version").toString());
            if ("3".equals(accountParam.get("version").toString())) {
                row.createCell(27).setCellValue(accountParam.get("username").toString());
                row.createCell(28).setCellValue(accountParam.get("securityLevel").toString());

                if ("authNoPriv".equals(accountParam.get("securityLevel").toString())) {
                    row.createCell(29).setCellValue(new String(decoder.decode(accountParam.get("password").toString())));
                    row.createCell(30).setCellValue(accountParam.get("authProtocol").toString());
                } else if ("authPriv".equals(accountParam.get("securityLevel").toString())) {
                    row.createCell(29).setCellValue(new String(decoder.decode(accountParam.get("password").toString())));
                    row.createCell(30).setCellValue(accountParam.get("authProtocol").toString());
                    row.createCell(31).setCellValue(accountParam.get("privProtocol").toString());
                    row.createCell(32).setCellValue(accountParam.get("privPassword").toString());
                }
            }
        }
    }

    @Override
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        switch (language) {
            case "en": {
                exportHeaderMap = (Map) map.get("en");
                break;
            }
            case "cn": {
                exportHeaderMap = (Map) map.get("cn");
                break;
            }
            case "ru": {
                exportHeaderMap = (Map) map.get("ru");
                break;
            }
            default: {
                throw new NZException(RCode.ASSET_EXPORT_HEADER_LANGUAGE_ERROR);
            }
        }
       /* Collection headers = exportHeaderMap.keySet();
        List<String> headerList = new ArrayList<>(headers.size());
        headers.stream().forEach(value -> headerList.add(value.toString()));*/
        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "Assets", true);
            // 响应为流
            String fileName = "AssetExportTemplate.xlsx"; // 默认文件名称
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            logger.error("asset模板生成失败，错误信息是：" + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("获取asset导入模板失败，表头语言是：" + language + " ，错误信息是：" + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public List<String> getTagsBykeyOrderCounts(String key) {
        key = key == null ? "" : key;
        List<AssetTag> tags = assetTagDao.getTagsBykeyOrderCounts(key);
        List<String> keys = tags.stream().map(AssetTag::getTag).collect(Collectors.toList());
        return keys;
    }

    @Override
    public List<String> getTagValsByKeyOrderCounts(String key, String value) {
        value = value == null ? "" : value;
        try {
            key = URLDecoder.decode(String.valueOf(key), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(String.format("tag key url转码失败，转码字符串：%s,错误信息是：%s", key, e.getMessage()), e);
        }
        List<AssetTag> tags = assetTagDao.getTagValsByKeyOrderCounts(key, value);
        List<String> values = tags.stream().map(AssetTag::getValue).collect(Collectors.toList());
        return values;
    }

    @Override
    public List<Asset> getAssetsWithTagsByModelId(Integer modelId) {
        return assetDao.getAssetsWithTagsByModelId(modelId);
    }

    @Override
    public List<Chart> getAssetsWithTagByChartIds(List<Integer> chartIds) {
        List<Chart> chartIdWithAssetEntity = assetDao.getAssetsByChartIds(chartIds);
        if(CollectionUtils.isEmpty(chartIdWithAssetEntity)){
            return Collections.emptyList();
        }
        List<AssetTag> tags = assetTagService.list();
        Map<Integer, List<AssetTag>> assetIdAndTagsMap = tags.stream().collect(Collectors.groupingBy(AssetTag::getAssetId));
        Asset asset;
        for (Chart chart : chartIdWithAssetEntity) {
            asset = chart.getAsset();
            asset.setTags(assetIdAndTagsMap.get(asset.getId()));
        }
        return chartIdWithAssetEntity;
    }

    private Cabinet checkCabinet(String cabinetName, Integer idcId, List<Cabinet> cabinetList, String seqUUID, Map saveEntitys, Integer defaultCabinetUsize) {
        Cabinet cabinet = new Cabinet();
        boolean flag = false;
        for (Cabinet entity : cabinetList) {
            // 如果该机房下有该机柜
            if (cabinetName.equals(entity.getName()) && idcId.equals(entity.getIdcId())) {
                cabinet = entity;
                flag = true;
                break;
            }
        }
        // 找不到机柜
        if (!flag) {
            cabinet.setName(cabinetName);
            cabinet.setIdcId(idcId);
            cabinet.setuSize(defaultCabinetUsize);
            cabinet.setSeq(seqUUID);
            saveEntitys.put("cabinet", cabinet);
        }
        return cabinet;
    }

    private Integer checkIdc(String dcName, Map<String, Idc> idcMap, String seqUUID, Map saveEntitys) {
        Idc idc = idcMap.get(dcName);
        if (idc == null) {
            idc = new Idc();
            idc.setName(dcName);
            idc.setSeq(seqUUID);
            saveEntitys.put("idc", idc);
        }
        return idc.getId();
    }

    public Model checkAssetTypeVendorModel(String assetType, Map<String, SysDictEntity> assetTypesMap, String assetVendor, Map<String, SysDictEntity> vendorsMap, String modelName, List<Model> modelList, String seqUUID, Map saveEntitys) {
        // 是否新增 model 标识 由于model 名称取消唯一限制
        boolean flag = false;

        // 如果没有获取倒该资产类型
        SysDictEntity type = assetTypesMap.get(assetType);
        if (type == null) {
            type = new SysDictEntity();
            type.setType(Constant.DICT_ASSET_MODEL_TYPE);
            type.setValue(assetType);
            type.setSeq(seqUUID);
            flag = true;
            saveEntitys.put("type", type);
        }

        // 如果没有厂商
        SysDictEntity vendor = vendorsMap.get(assetVendor);
        if (vendor == null) {
            vendor = new SysDictEntity();
            vendor.setType(Constant.DICT_ASSET_MODEL_VENDOR);
            vendor.setValue(assetVendor);
            vendor.setSeq(seqUUID);
            flag = true;
            saveEntitys.put("vendor", vendor);
        }

        // 有新增的type 或者 vendor 意味着 model 必将新增
        Model model = null;
        if (flag) {
            model = new Model();
            model.setName(modelName);
            model.setTypeCode(type.getCode());
            model.setVendorCode(vendor.getCode());
            model.setSeq(seqUUID);
            saveEntitys.put("model", model);
        } else {
            // type vendor 没有新增 比对 model name是否改变 没有需要新增
            boolean modelFlag = true;
            for (Model entity : modelList) {
                // 如果 名称 类型 厂商 都一致的话 不用添加model
                if (entity.getName().equals(modelName) && entity.getTypeCode().equals(type.getCode()) && entity.getVendorCode().equals(vendor.getCode())) {
                    model = entity;
                    modelFlag = false;
                    break;
                }
            }
            // 没有找到同名的 model 新增操作
            if (modelFlag) {
                model = new Model();
                model.setName(modelName);
                model.setTypeCode(type.getCode());
                model.setVendorCode(vendor.getCode());
                model.setSeq(seqUUID);
                saveEntitys.put("model", model);
            }
        }
        return model;
    }

    public boolean checkImportSnOnly(String sn, List<String> assetSnList) {
        if (ToolUtil.isNotEmpty(assetSnList)) {
            // 如果不唯一
            if (assetSnList.contains(sn)) {
                return false;
            } else { // 唯一
                return true;
            }
        }
        return true;
    }

    private boolean checkImportHostOnly(String host, List<String> assetHostList) {
        if (ToolUtil.isNotEmpty(assetHostList)) {
            // 如果不唯一
            if (assetHostList.contains(host)) {
                return false;
            } else { // 唯一
                return true;
            }
        }
        return true;
    }

    /**
     * 检查位置是否合理
     *
     * @param asset
     * @return
     */
    public boolean validateUSize(Asset asset, Model model) {
        Integer assetId = asset.getId();
        Integer cabinetId = asset.getCabinetId();
        Integer start = asset.getCabinetStart();
        Integer end = asset.getCabinetEnd();

        // 1.校验 开始和结束位置占用的机柜是否符合 关联 model 的 usize值
        Integer modelUsize = model.getUsize();
        if (modelUsize != null && (end - start) < modelUsize) {
            throw new NZException(RCode.ASSET_POSITION_MISMATCH);
        }

        // 2.得到机柜下包含的节点信息
        List<Asset> assets = this.list(new QueryWrapper<Asset>().lambda().eq(Asset::getCabinetId, cabinetId));

        // 3.获取当前机柜的全部位置 所有位置 从1开始到 uSize
        Cabinet cab = cabinetService.getById(cabinetId);
        Integer[] allPosition = new Integer[cab.getuSize()];
        for (int i = 0; i < allPosition.length; i++) {
            allPosition[i] = i + 1;
        }

        // 4.获取已经占用的位置
        List<Integer> occList = new ArrayList<>();

        // 同机柜修改的情况  需要将自身节点补充到所属机柜的位置中去  再次重新计算可用位置
        Integer oldCabinetId = null;
        if (assetId != null) {
            Asset assetEntity = this.getById(assetId);
            oldCabinetId = assetEntity.getCabinetId();
        }

        // 修改标识
        boolean updateFlag = oldCabinetId != null && oldCabinetId.equals(cabinetId);

        for (Asset nodeInfo : assets) {
            // 修改的情况下 不记录当前占用位置
            if (updateFlag && nodeInfo.getId().equals(assetId)) {
                continue;
            }
            if (nodeInfo.getCabinetStart() != null && nodeInfo.getCabinetEnd() != null) {
                for (int i = nodeInfo.getCabinetStart(); i < nodeInfo.getCabinetEnd(); i++) {
                    occList.add(i);
                }
            }
        }

        return this.validateFreePosition(allPosition, occList, start, end);
    }

    public boolean validateUSizeForImport(Integer start, Integer end, Cabinet cab) {
        // 所有位置 从1开始到uSize
        Integer[] allPosition = new Integer[cab.getuSize()];
        for (int i = 0; i < allPosition.length; i++) {
            allPosition[i] = i + 1;
        }

        // 已经占用的位置
        List<Integer> occList = new ArrayList<>();

        if (CollectionUtils.isEmpty(cab.getAssetList())) {
            return true;
        } else {
            for (Asset nodeInfo : cab.getAssetList()) {
                if (nodeInfo.getCabinetStart() != null && nodeInfo.getCabinetEnd() != null) {
                    for (int i = nodeInfo.getCabinetStart(); i < nodeInfo.getCabinetEnd(); i++) {
                        occList.add(i);
                    }
                }
            }
            return this.validateFreePosition(allPosition, occList, start, end);
        }
    }

    /**
     * 校验是否有可用位置
     *
     * @param allPosition
     * @param occList
     * @param start
     * @param end
     * @return
     */
    private boolean validateFreePosition(Integer[] allPosition, List<Integer> occList, Integer start, Integer end) {
        // 空闲的位置
        List<Integer> freePosition = new ArrayList<>(Arrays.asList(allPosition));
        // 例如 (1),2,3,4,(5,) 6,7,8,(9),10
        freePosition.removeAll(occList);

        if (!freePosition.contains(start)) {
            // 空闲位置中没有符合条件的位置
            return false;
        } else {
            // 判断结尾字段是否成立
            Integer uSize = end - start;
            if (uSize == 1) {
                return true;
            } else {
                // usize大于1 判断开始和结束位置中是否存在被占用的位置
                for (int i = start; i <= (end - 1); i++) {
                    if (!freePosition.contains(i)) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void collectData(Integer assetId){
		//根据assetId获取当前资产的ssh或snmp信息
		Asset asset = this.getById(assetId);
		List<Account> accounts = accountService.list(new QueryWrapper<Account>().lambda().eq(Account::getAssetId, assetId));
		if(ToolUtil.isEmpty(accounts)) {
			// 该资产无ssh或snmp信息 采集失败 
			logger.error("asset feature get data error,assetId : {} has no SSH or SNMP information",assetId);
			throw new NZException("This asset has no SSH or SNMP information acquisition failure");
		}
		
		for(Account account : accounts) {
			String protocol = account.getProtocol();
			
			if(accounts.size()==1 && Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)) {
				// 该资产无ssh或snmp信息 采集失败 
				logger.error("asset feature get data error,assetId : {} has no SSH or SNMP information",assetId);
				throw new NZException("This asset has no SSH or SNMP information acquisition failure");
			}
			
			List<AssetFeature> results = new ArrayList<AssetFeature>();
					
			// 查询该资产下是否有之前收集的信息  防止频繁查询数据库
			List<AssetFeature> assetFeatures = assetFeatureService.list(new QueryWrapper<AssetFeature>().lambda().eq(AssetFeature::getAssetId, assetId));
			HashMap<String,AssetFeature> assetFeatureMap = new HashMap<String,AssetFeature>();
			if(ToolUtil.isNotEmpty(assetFeatures)) {
				for(AssetFeature data : assetFeatures) {
					// 将数据以key为资产id 查询的信息名称 以及查询方式组合
					assetFeatureMap.put(data.getAssetId()+data.getItem()+data.getSource(), data);
				}
			}
			
			// ssh采集相关逻辑
			if(Constant.ACCOUNT_SSH_PROTOCOL.equals(protocol)) {
				Session session =null;
				try {
					// 进行ssh连接
					session=SshUtil.getSshConnection(asset, account);
			        
					// 采集cpu disk memory core uptime信息
					getSystemInfo(session,assetFeatureMap,results,assetId);
					
					// 采集网卡信息
			        // 获取所有网卡名称信息
			        getSystemInterfaceInfo(session,assetFeatureMap,results,assetId);
			        
			        // 针对不同操作系统获取os信息
			        getSystemOsInfo(session,assetFeatureMap,results,assetId);
			        
				} catch (Exception e) {
					e.printStackTrace();
					logger.error("asset feature get data error,assetId : {} info : {}",assetId,e);
					throw new NZException(e.getMessage());
				} finally {
					SshUtil.close(session);
				}
			}
			
			// snmp采集相关逻辑
			if(Constant.ACCOUNT_SNMP_PROTOCOL.equals(protocol)) {
				try {
					getSystemInfoBySnmp(assetFeatureMap, results, asset, account);
				} catch (Exception e) {
					e.printStackTrace();
					logger.error("asset feature get data error,assetId : {} info : {}",assetId,e);
					throw new NZException(e.getMessage());
				}
			}
			
			// 存入db 无则新增 有则修改
			if(ToolUtil.isNotEmpty(results)) {
				assetFeatureService.saveOrUpdateBatch(results);
			}
			
		}
	}

    @Override
    public Map filterStat() {
        Map resultMap = new LinkedHashMap();
        // 1.获取所有 dc 信息
        List<Idc> idcList = idcService.list();
        Map<Integer, String> idcIdAndNameMap = idcList.stream().collect(Collectors.toMap(Idc::getId, Idc::getName));

        // 以 dc分组统计asset 信息
        List<Map> statByDc = assetDao.getStatByDc();
        for (Map map : statByDc) {
            Integer idcId = (Integer) map.get("id");
            map.put("name", idcIdAndNameMap.get(idcId));
        }

        resultMap.put("dc", statByDc);

        // 2.查询 asset 表 以 modelId 分组
        List<Map> statByModel = assetDao.getStatByModel();
        // model 和 关联asset的数量
        Map<String, Long> modelIdAndAssetTotalMap = new HashMap<>();
        for (Map map : statByModel) {
            modelIdAndAssetTotalMap.put(map.get("id").toString(), (Long) map.get("total"));
        }

        // 3. assetType名称 和 关联的 modelids
        List<Map> assetTypeNameAndModelIds = modelDao.getModelIdsGroupByType();
        this.handleAssetTotalByModelids(assetTypeNameAndModelIds, modelIdAndAssetTotalMap);
        resultMap.put("assetType", assetTypeNameAndModelIds);

        // 4. vendor名称 和 关联的 modelids
        List<Map> vendorNameAndModelIds = modelDao.getModelIdsGroupByVendor();
        this.handleAssetTotalByModelids(vendorNameAndModelIds, modelIdAndAssetTotalMap);
        resultMap.put("vendor", vendorNameAndModelIds);

        // 5. asset ping 统计
        List<Map> assetStatGroupByStatus = assetPingDao.getAssetStatGroupByStatus();
        for (Map map : assetStatGroupByStatus) {
            map.put("name", "0".equals(map.get("status").toString()) ? "down" : "up");
        }
        resultMap.put("ping", assetStatGroupByStatus);
        // 6. tag 统计
        List<Map> tagStats = assetTagDao.getTagStat();
        Map<Object, List<Map>> tagStatMap = tagStats.stream().collect(Collectors.groupingBy(map -> map.get("key")));
        List<Map> tagStatList = new ArrayList<>();
        Map<String, Object> tempMap;
        for (Map.Entry<Object, List<Map>> entry : tagStatMap.entrySet()) {
            for (Map map : entry.getValue()) {
                map.remove("key");
            }
            tempMap = new HashMap();
            tempMap.put("key", entry.getKey());
            tempMap.put("values", entry.getValue());
            tagStatList.add(tempMap);
        }
        resultMap.put("tag", tagStatList);
        return resultMap;
    }

    /**
     * 通过 assetType/vendor关联的 modelId 统计关联asset 数量
     *
     * @param nameAndModelIds               assetType/vendor 和 关联的 modelIds
     * @param modelIdAndAssetTotalMap       modelId 和 关联的 asset 数量
     */
    private void handleAssetTotalByModelids(List<Map> nameAndModelIds, Map<String, Long> modelIdAndAssetTotalMap) {
        String modelIds;
        long total;
        Iterator<Map> iterator = nameAndModelIds.iterator();
        while (iterator.hasNext()) {
            Map map = iterator.next();
            total = 0;
            modelIds = (String) map.get("modelIds");
            if (StringUtils.isNotEmpty(modelIds)) {
                for (String modelId : Arrays.asList(modelIds.split(","))) {
                    if (modelIdAndAssetTotalMap.get(modelId) != null) {
                        total += modelIdAndAssetTotalMap.get(modelId);
                    }
                }
                if (total == 0) {
                    iterator.remove();
                    continue;
                }
            }
            map.put("total", total);
            map.remove("modelIds");
        }
    }

    public void getSystemInfo(Session session,HashMap<String,AssetFeature> assetFeatureMap,List<AssetFeature> results,Integer assetId) throws Exception {
		Set<String> sshKeySet = Constant.SSH_SHELL_MAP.keySet();
        for(String key : sshKeySet) {
        	String value = RuntimeUtil.sshCommondResult(session, Constant.SSH_SHELL_MAP.get(key));
        	if("cpu".equals(key)) {
        		String[] split = value.split("\r\n");
        		String cpuCount = split[0];
        		String cpuInfo = split[1];
        		String[] ss = new String[Integer.valueOf(cpuCount)];
        		for(int i=0;i<Integer.valueOf(cpuCount);i++) {
        			ss[i]=cpuInfo.replace("\r\n", "");
        		}
        		value=JSON.toJSONString(ss);
        	}
        	if("disk".equals(key)) {
        		String[] split = value.split("\r\n");
            	int diskCount = split.length/2;
            	List list =new ArrayList();
            	for(int i=0;i<diskCount;i++) {
            		Map map =new LinkedHashMap();
            		String[] split2 = split[i].split(" ");
            		String[] split3 = split[diskCount+i].split(" ");
            		map.put("vendor", split3[0]);
            		map.put("model", split3[1]);
            		map.put("size",split2[1]);
            		list.add(map);
            	}
            	value=JSON.toJSONString(list);
        	}
        	value=value.replace("\r\n", "");
        	//查询数据库是否已经存在数据
        	AssetFeature result = assetFeatureMap.get(assetId+key+Constant.ACCOUNT_SSH_PROTOCOL);;
        	if(ToolUtil.isEmpty(result)) {
        		result = new AssetFeature();
        	}
			result.setAssetId(assetId);
			result.setItem(key);
			if("memory".equals(key)) {
				result.setValue(value+"G");
			}else {
				result.setValue(value);
			}
			result.setSource(Constant.ACCOUNT_SSH_PROTOCOL);
			result.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
			results.add(result);
        }
	}
	
	public void getSystemInterfaceInfo(Session session,HashMap<String,AssetFeature> assetFeatureMap,List<AssetFeature> results,Integer assetId) throws Exception {
        String interfaceNames = RuntimeUtil.sshCommondResult(session, "ifconfig |grep mtu|awk -F ':' '{print $1}'|grep -v lo");
        String[] names = interfaceNames.split("\r\n");
        List interfaceInfos =new ArrayList();
        for(String name : names) {
        	Map map =new LinkedHashMap();
        	map.put("name", name);
        	map.put("inet", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" | grep -w inet | awk -F' ' '{print $2}'").replace("\r\n", ""));
        	map.put("netmask", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" | grep netmask|awk '{print $4}'").replace("\r\n", ""));
        	map.put("mtu", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" |grep mtu|awk '{print $4}'").replace("\r\n", ""));
        	map.put("inet6", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" | grep -w inet6 | awk -F' ' '{print $2}'").replace("\r\n", ""));
        	map.put("mac", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" |grep ether|awk '{print $2}'").replace("\r\n", ""));
        	map.put("state", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" |grep mtu|awk -F ',' '{print $1}'|awk -F '<' '{print $2}'").replace("\r\n", ""));
        	interfaceInfos.add(map);
        }
        AssetFeature result=assetFeatureMap.get(assetId+"interface"+Constant.ACCOUNT_SSH_PROTOCOL);
        if(ToolUtil.isEmpty(result)) {
    		result = new AssetFeature();
    	}
    	result.setAssetId(assetId);
		result.setItem("interface");
		result.setSource(Constant.ACCOUNT_SSH_PROTOCOL);
		result.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
        result.setValue(JSON.toJSONString(interfaceInfos));
        results.add(result);
	}
	
	public void getSystemOsInfo(Session session,HashMap<String,AssetFeature> assetFeatureMap,List<AssetFeature> results,Integer assetId) throws Exception {
		String value="";
		String osInfo = RuntimeUtil.sshCommondResult(session, "echo `cat /etc/system-release` `uname -p`");
		if(osInfo.toLowerCase().contains("centos")) {
        	value=JSON.toJSONString(osInfo.replaceAll("\r\n", ""));
        }else {
        	osInfo = RuntimeUtil.sshCommondResult(session, "echo `cat /etc/os-release | grep -w NAME | awk -F '=' '{print $2}' |sed 's/\\\"//g'` `cat /etc/os-release | grep -w VERSION | awk -F '=' '{print $2}' |sed 's/\\\"//g'` `uname -p`");
        	value=JSON.toJSONString(osInfo.replaceAll("\r\n", ""));
        }
        AssetFeature result=assetFeatureMap.get(assetId+"os"+Constant.ACCOUNT_SSH_PROTOCOL);
        if(ToolUtil.isEmpty(result)) {
    		result = new AssetFeature();
    	}
    	result.setAssetId(assetId);
		result.setItem("os");
		result.setSource(Constant.ACCOUNT_SSH_PROTOCOL);
		result.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
		
		result.setValue(value.replace("\"", ""));
        results.add(result);
	}
	
	public void getSystemInfoBySnmp(HashMap<String,AssetFeature> assetFeatureMap,List<AssetFeature> results,Asset asset,Account account) throws Exception {
		Map snmpInfos = JSONObject.parseObject((String) account.getParams(), Map.class);
		Set<String> keySet = Constant.SNMP_OID_MAP.keySet();
		for(String key:keySet) {
			List<Map> snmpResults = SnmpUtil.snmpWalk(asset.getHost(), account.getPort(), (Integer)snmpInfos.get("version"), (String)snmpInfos.get("community"), Constant.SNMP_OID_MAP.get(key), snmpInfos);
			if("ifEntry".equals(key)) {
				List l =new LinkedList();
				if (CollectionUtils.isNotEmpty(snmpResults)) {
					Map<String,LinkedHashMap> handleMap =new LinkedHashMap<String,LinkedHashMap>();
		            String resultOid;
		            String name;
		            for (Map map : snmpResults) {
		            	//截取最后一位oid数  
		            	resultOid = map.get("oid").toString();
		            	//去掉最后一位oid剩下值
		            	String first = resultOid.substring(0, resultOid.lastIndexOf("."));
		            	//最后一位oid数
		                String last = resultOid.substring(resultOid.lastIndexOf(".")+1,resultOid.length());
		                name = Constant.SNMP_IFENTRY_MAP.get(first);
		                if(ToolUtil.isEmpty(name)) {
		                	continue;
		                }
		                Map map2 = handleMap.get(last);
		                if(ToolUtil.isEmpty(map2)) {
		                	Map map3 =new LinkedHashMap();
		                	handleMap.put(last, (LinkedHashMap) map3);
		                	map3.put(name,map.get("value"));
		                }else {
		                	map2.put(name,map.get("value"));
		                }
		            }
		            
		            for(String k:handleMap.keySet()) {
		            	l.add(handleMap.get(k));
		            }
		        }
				//查询数据库是否已经存在数据
				AssetFeature result = assetFeatureMap.get(asset.getId()+key+Constant.ACCOUNT_SNMP_PROTOCOL);
				if(ToolUtil.isEmpty(result)) {
	        		result = new AssetFeature();
	        	}
				result.setAssetId(asset.getId());
				result.setItem(key);
				result.setValue(JSON.toJSONString(l));
				result.setSource(Constant.ACCOUNT_SNMP_PROTOCOL);
				result.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
				results.add(result);
			}else {
				for(Map snmpResult : snmpResults) {
					String value = (String) snmpResult.get("value");
					//查询数据库是否已经存在数据
					AssetFeature result = assetFeatureMap.get(asset.getId()+key+Constant.ACCOUNT_SNMP_PROTOCOL);
					if(ToolUtil.isEmpty(result)) {
		        		result = new AssetFeature();
		        	}
					result.setAssetId(asset.getId());
					result.setItem(key);
					if("sysUpTime".equals(key)) {
						TimeTicks t =new TimeTicks();
						t.setValue(value);
						long milliseconds = t.toMilliseconds();
						result.setValue(DateUtil.getTime(DateUtil.getUTCTimeByConfigTimeZone(new Date(DateUtil.getUTCTimeByConfigTimeZone().getTime()-milliseconds))));
					}else {
						result.setValue(value);
					}
					result.setSource(Constant.ACCOUNT_SNMP_PROTOCOL);
					result.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
					results.add(result);
				}
			}
		}
	}
}