package com.nis.modules.asset.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.dao.AssetDao;
import com.nis.modules.asset.dao.AssetFeatureDao;
import com.nis.modules.asset.dao.AssetPingDao;
import com.nis.modules.asset.dao.CabinetDao;
import com.nis.modules.asset.dao.ModelDao;
import com.nis.modules.asset.entity.*;
import com.nis.modules.asset.service.*;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.metric.dto.MetricsDto;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.panel.dao.PanelDao;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SnmpMibService;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.terminal.config.Constants;
import com.nis.modules.terminal.config.SshUserInfo;
import com.nis.modules.terminal.entity.Account;
import com.nis.modules.terminal.service.AccountService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

import static com.nis.common.utils.CommonUtils.notEmpty;

import java.io.*;
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

    // 数据中心  service
    @Autowired
    private IdcService idcService;

    // 机柜  service
    @Autowired
    private CabinetService cabinetService;

    // 资产型号  service
    @Autowired
    private ModelService modelService;

    // 字典   service
    @Autowired
    private SysDictService sysDictService;

    // 资产  dao
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
    private PanelDao panelDao;

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
    private SnmpMibService snmpMibService;
    
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String[] vendorIds = {};
        String[] typeIds = {};
        if (ToolUtil.isNotEmpty(params.get("vendorIds"))) {
            vendorIds = String.valueOf(params.get("vendorIds")).split(StringPool.COMMA);
        }
        if (ToolUtil.isNotEmpty(params.get("typeIds"))) {
            typeIds = String.valueOf(params.get("typeIds")).split(StringPool.COMMA);
        }
        List<Integer> innerModelIds = new ArrayList<>();
        if (vendorIds.length > 0 || typeIds.length > 0) {
            innerModelIds = modelDao.selectIdByDictIds(vendorIds, typeIds);
            if (innerModelIds.size() == 0) {
                innerModelIds.add(-99999);
            }
        }
        params.put("innerModelIds", innerModelIds);
        if (ToolUtil.isNotEmpty(params.get("states"))) {
            params.put("states", String.valueOf(params.get("states")).split(StringPool.COMMA));
        }
        if (ToolUtil.isNotEmpty(params.get("pingStates"))) {
            params.put("pingStates", String.valueOf(params.get("pingStates")).split(StringPool.COMMA));
        }
        String idcIds = null;
        if (params != null && params.size() > 0) {
            idcIds = (String) params.get("idcIds");
        }

        if (StringUtils.isNotEmpty(idcIds)) {
            // 1. 清空idcid
            params.put("idcId", null);
            // 2. 校验idcids
            String[] idcIdsArr = idcIds.split(",");
            List<Integer> idcidList = new ArrayList<>();
            for (String id : idcIdsArr) {
                try {
                    if (id.length() > 0) {
                        idcidList.add(Integer.parseInt(id));
                    }
                } catch (NumberFormatException e) {
                    // ID转换异常
                    throw new NZException(RCode.IDC_ID_FORMAT);
                }
            }
            // 3. 放回参数集合中
            params.put("idcIds", idcidList);
        } else {
            // 4. 解决参数为空字符串问题
            params.put("idcIds", null);
        }

        if(ToolUtil.isNotEmpty(params.get("tag"))){
            String tag = String.valueOf(params.get("tag"));
            List<AssetTag> list = assetTagService.list(new QueryWrapper<AssetTag>().lambda().like(AssetTag::getTag, tag).or().like(AssetTag::getValue, tag));
            Set<Integer> ids = list.stream().map(AssetTag::getAssetId).collect(Collectors.toSet());
            params.put("innerTagRelationAssetIds", ids);
        }

        IPage<Asset> page = new Query<Asset>().getPage(params);

        List<Asset> assetEntities = assetDao.queryList(page, params);
        String ipmi = null;
        Integer cabinetEnd;
        List<Account> accounts;
        for (Asset asset : assetEntities) {
            ipmi = (String) asset.getIpmi();
            if (StringUtils.isNotEmpty(ipmi)) {
                asset.setIpmi(JSONObject.parseObject(ipmi, Map.class));
            }
            cabinetEnd = asset.getCabinetEnd();
            if (ToolUtil.isNotEmpty(cabinetEnd)) {
                asset.setCabinetEnd(cabinetEnd - 1);
            }
            accounts = asset.getAccounts();
            if(CollectionUtils.isNotEmpty(accounts)){
                accounts.forEach(pojo -> {
                    if(StringUtils.isNotEmpty((String) pojo.getParams())){
                        pojo.setParams(JSONObject.parseObject((String) pojo.getParams(), Map.class));
                    }
                });
            }
        }
        page.setRecords(assetEntities);

        return new PageUtils(page);
    }

    @Override
    public List<Asset> queryListByHosts(Collection<String> hosts) {
        List<Asset> assetList = assetDao.queryListByHosts(hosts);
        return assetList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateAsset(Asset asset) {
        // 1. sn 唯一校验
        if (!this.validateFieldExist(asset.getId(), "sn", asset.getSn())) {
            throw new NZException(RCode.ASSET_SN_DUPLICATE);
        }

        // 2. host地址唯一校验
        if (!this.validateFieldExist(asset.getId(), "host", asset.getHost())) {
            throw new NZException(RCode.ASSET_HOST_DUPLICATE);
        }

        // 3. 型号有效性校验
        Model model = modelService.getById(asset.getModelId());
        if (model == null) {
            // 所选资产型号不存在
            throw new NZException(RCode.ASSET_MODEL_NOTFOUND);
        } else {
            // 校验 ipmi 信息 只有在assetType 为 server 时才进行入库操作
            SysDictEntity type = sysDictService.getOne(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getType, Constant.DICT_ASSET_MODEL_TYPE).eq(SysDictEntity::getCode, model.getTypeCode()).eq(SysDictEntity::getDelFlag, 0));
            if (Constant.ASSET_SERVER_TYPE.equals(type.getValue().toLowerCase())) {
                // 校验 ipmi信息
                Map<String, Object> ipmi = (Map<String, Object>) asset.getIpmi();
                if (ipmi != null) {
                    String host = (String) ipmi.get("host");
                    if (StringUtils.isNotEmpty(host)) {
                        ValidateUtils.is(host).ip(RCode.ASSET_SERVERHOST_ERROR);
                    }
                    Integer port = (Integer) ipmi.get("port");
                    if (port != null) {
                        boolean flag = CommonUtils.checkPort(port.toString());
                        if (!flag) {
                            throw new NZException(RCode.ASSET_SERVERPORT_ERROR);
                        }
                    }
                    asset.setIpmi(JSONObject.toJSONString(ipmi));
                }
            } else {
                asset.setIpmi(null);
            }
        }

        // 4.数据中心有效性校验
        Idc idc = idcService.getById(asset.getIdcId());
        if (idc == null) {
            // 数据中心不存在
            throw new NZException(RCode.ASSET_IDC_NOTFOUND);
        }

        Integer start = asset.getCabinetStart();
        Integer end = asset.getCabinetEnd();
        // 5.机柜有效性校验
        if (asset.getCabinetId() != null) {
            Cabinet cabinet = cabinetService.getById(asset.getCabinetId());
            if (cabinet == null) {
                // 机柜未找到
                throw new NZException(RCode.ASSET_CABINET_NOTFOUND);
            }
            if (!idc.getId().equals(cabinet.getIdcId())) {
                // 数据中心与机柜不符  当前数据中心没有含有所选机柜
                throw new NZException(RCode.IDC_NOTHAS_CABINET);
            }

            // 选择了机柜再进行处理usize 相关数据
            if (start != null && end != null) {
                end = end + 1;
                asset.setCabinetEnd(end);
                Integer cabuSize = cabinet.getuSize();
                if (start < 0 || end < 0 || start >= end || start > cabuSize || end > cabuSize + 1) {
                    throw new NZException(RCode.ASSET_POSITION_INVALID);
                }

                // 校验所选位置在该机柜中是否被占用
                boolean flag = this.validateUSize(asset);
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

        // 6. 状态有效性校验
        if (asset.getState() != null) {
            Map<String, Object> map = new HashMap<>(2);

            map.put("type", Constant.DICT_ASSET_STATE_TYPE); // 字典表中资产状态 type 值 assetState
            List<SysDictEntity> assetTypes = sysDictService.queryList(map);
            List<Integer> stateList = assetTypes.stream().map(SysDictEntity::getCode).collect(Collectors.toList());
            if (!stateList.contains(asset.getState())) {
                // 未找到资产状态
                throw new NZException(RCode.ASSET_STATE_NOTFOUND);
            }
        } else {
            asset.setState(1);
        }

        Date now = DateUtil.getUTCTimeByConfigTimeZone();
        Long userId = ShiroUtils.getUserId();
        if (asset.getId() == null) {
            asset.setCreateAt(now);
            asset.setCreateBy(userId);
        }
        asset.setUpdateAt(now);
        asset.setUpdateBy(userId);

        if (asset.getId() == null) {
            assetDao.saveAsset(asset);
            // 如果指名了安装node exporter 则需要保存endpoint信息
            if (Constant.ASSET_BUILD_NODE_EXPORTER_FLAG.equals(asset.getExporter())) {
                this.saveInitEndpoint(asset);
            }
            // 新增时需要新增 panel chart
            chartService.saveAssetPanelAndChart(asset);

            // 添加tags
            List<AssetTag> tags = asset.getTags();
            if(CollectionUtils.isNotEmpty(tags)){
                // 校验key
                for (AssetTag pojo : tags) {
                    if(StringUtils.isEmpty(pojo.getTag())){
                        throw new NZException(RCode.ASSET_TAGKEY_ISNULL);
                    }
                    pojo.setAssetId(asset.getId());
                }

                assetTagService.saveBatch(tags);
            }

            List<Account> accounts = asset.getAccounts();
            if (CollectionUtils.isNotEmpty(accounts)) {
                accounts.forEach(account -> {
                    account.setAssetId(asset.getId());
                    accountService.saveOrUpdateAccount(account);
                });
            }
        } else {
            Asset oldAsset = this.getById(asset.getId());
            this.updateById(asset);
            // 修改的时候如果没有指定 account, 则视为删除asset关联account
            List<Account> accounts = asset.getAccounts();
            if (CollectionUtils.isEmpty(accounts)) {
                accountService.remove(new QueryWrapper<Account>().lambda().eq(Account::getAssetId, asset.getId()));
            } else {
                List<Account> list = accountService.list(new QueryWrapper<Account>().lambda().eq(Account::getAssetId, asset.getId()));
                if(CollectionUtils.isNotEmpty(list)){
                    List<Integer> collect = accounts.stream().filter(account -> ToolUtil.isNotEmpty(account.getId()) ? true : false).map(Account::getId).collect(Collectors.toList());
                    List<Integer> collect1 = list.stream().map(Account::getId).collect(Collectors.toList());
                    collect1.removeAll(collect);
                    if(CollectionUtils.isNotEmpty(collect1)){
                        accountService.removeByIds(collect1);
                    }
                }

                accounts.forEach(account -> {
                    account.setAssetId(asset.getId());
                    accountService.saveOrUpdateAccount(account);
                });
            }

            // 如果修改了Model
            if (!oldAsset.getModelId().equals(asset.getModelId())) {
                chartService.updateAssetChartsByNewModel(asset);
            }

            List<AssetTag> tags = asset.getTags();
            if (CollectionUtils.isNotEmpty(tags)) {
                List<AssetTag> list = assetTagService.list(new QueryWrapper<AssetTag>().lambda().eq(AssetTag::getAssetId, asset.getId()));
                List<Integer> tagIds = list.stream().map(AssetTag::getId).collect(Collectors.toList());

                List<AssetTag> saveList = new ArrayList<>();
                List<AssetTag> updateList = new ArrayList<>();

                for (AssetTag pojo : tags) {
                    if (StringUtils.isEmpty(pojo.getTag())) {
                        throw new NZException(RCode.ASSET_TAGKEY_ISNULL);
                    }

                    if (!tagIds.contains(pojo.getId())) {
                        saveList.add(pojo);
                    } else {
                        updateList.add(pojo);
                        tagIds.remove(pojo.getId());
                    }

                    pojo.setAssetId(asset.getId());
                }
                if (CollectionUtils.isNotEmpty(saveList)) {
                    assetTagService.saveBatch(saveList);
                }
                if (CollectionUtils.isNotEmpty(updateList)) {
                    assetTagService.updateBatchById(updateList);
                }
                if (CollectionUtils.isNotEmpty(tagIds)) {
                    assetTagService.removeByIds(tagIds);
                }
            }
        }

    }

    public boolean validateFieldExist(Integer id, String column, String columnValue) {
        List<Asset> list = this.list(new QueryWrapper<Asset>().eq(column, columnValue));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
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

        // 1. 删除
        this.removeByIds(idList);

        // 2. 删除关联的endponiet
        endpointService.remove(new QueryWrapper<Endpoint>().lambda().in(Endpoint::getAssetId, idList));

        // 3. 删除关联的alert信息
        alertMessageService.removeByAssetIds(idList);

        // 4. 删除关联的 panel chart  chartElement信息
        List<Panel> panels = panelDao.selectList(new QueryWrapper<Panel>().lambda().eq(Panel::getType, "asset").in(Panel::getLink, ids));

        Set<Integer> panelIds = panels.stream().map(Panel::getId).collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(panelIds)) {
            panelDao.deleteBatchIds(panelIds);
            Integer[] removePanelIds = panelIds.stream().toArray(n -> new Integer[n]);
            chartService.delChartsByPanelId(removePanelIds);
        }

        // 5. 删除关联的tag 信息
        assetTagService.remove(new QueryWrapper<AssetTag>().lambda().in(AssetTag::getAssetId, idList));

        // 6. 删除关联的account信息
        accountService.remove(new QueryWrapper<Account>().lambda().in(Account::getAssetId, idList));
   }

    @Override
    public void validateAccount(Asset asset, List<Account> accounts) {
        String protocol;
        Integer port;
        Object params;
        for (Account account : accounts) {
            // 协议校验
            protocol = account.getProtocol();
            ValidateUtils.is(protocol).notNull(RCode.ACCOUNT_PROTOCOL_ISNULL);
            if (!Constant.ACCOUNT_SSH_PROTOCOL.equals(protocol) && !Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol) && !Constant.ACCOUNT_SNMP_PROTOCOL.equals(protocol)) {
                throw new NZException(RCode.ACCOUNT_PROTOCOL_INCORRECT);
            }

            // 端口校验
            port = account.getPort();
            if(ToolUtil.isEmpty(port)){
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

            params = account.getParams();
            if (ToolUtil.isEmpty(params)) {
                throw new NZException(RCode.ACCOUNT_PROTOCOL_PARAMS_ISNULL);
            }
            try {
                Map map = (Map) params;
                account.setParams(JSONObject.toJSONString(map));
            } catch (Exception e) {
                throw new NZException(RCode.ACCOUNT_PROTOCOL_PARAMS_FORMATERROR);
            }
        }

        // 修改
        /*Integer assetId = asset.getId();
        if (ToolUtil.isNotEmpty(assetId)) {
            // 现有账号
            List<Account> list = accountService.list(new QueryWrapper<Account>().lambda().eq(Account::getAssetId, assetId));
            Map<Integer,Account> currentAccounts = new HashMap<>();
            for (Account pojo:list) {
                currentAccounts.put(pojo.getId(), pojo);
            }

            Iterator<Account> iterator = accounts.iterator();
            while(iterator.hasNext()){
                Account account = iterator.next();
                if(account.getId() != null && currentAccounts.get(account.getId()) != null){
                    currentAccounts.remove(account.getId());
                }
            }

            accounts.addAll(currentAccounts.values());
        }*/

        // 一个协议只能有一个账号
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
        logger.info("importAssets param assetsExcelFileName is {}", assetsExcelFile.getOriginalFilename());
        logger.info("importAssets param uuid is {}", seqUUID);
        String originalFilename = assetsExcelFile.getOriginalFilename();

        // 校验文件格式
        boolean b = ExcelUtils.validateFileType(originalFilename);
        if (!b) {
            throw new NZException(RCode.ASSET_IMPORTFILE_TYPE);
        }

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

        String configDefaultCabUsize = configs.get(Constant.SYSCONFIG_KEY_DEFAULT_CABINETU);
        Integer defaultCabinetUsize = null;
        try {
            defaultCabinetUsize = Integer.parseInt(configDefaultCabUsize);
        } catch (NumberFormatException e) {
            logger.error("配置表中机柜默认值有误 param_key 为 {}", Constant.SYSCONFIG_KEY_DEFAULT_CABINETU, e);
        }

        if (defaultCabinetUsize == null) {
            throw new NZException(RCode.SYS_CONFIG_DEFALUTCABINETU_INTERVAL);
        }

        Map info = new HashMap();
        Workbook work = null;

        try {
            work = ExcelUtils.getWork(originalFilename, assetsExcelFile.getInputStream());
        } catch (Exception e) {
            logger.error("解析文件格式有误", e);
            throw new NZException(RCode.ASSET_IMPORTFILE_FAILED);
        }

        try {
            // 默认得到第一个 sheet
            Sheet sheet = work.getSheetAt(0);
            // 获得当前sheet的开始行
            int firstRowNum = sheet.getFirstRowNum();

            // 校验表头是否与模板一致
            List<Map> mapList = this.validateExcelHeader(sheet, firstRowNum);
            if (CollectionUtils.isNotEmpty(mapList)) {
                info.put("seq", seqUUID);
                info.put("totalNum", 0);
                info.put("successNum", 0);
                info.put("failNum", 0);
                info.put("failDetail", mapList);
                return info;
            }
            // 获得当前sheet的结束行
            int maxRowNum = sheet.getLastRowNum();
            Asset asset;
            Account account, accountCli;
            Row row;

            // 返回导出错误信息
            List<Map> failDetails = new ArrayList<>();
            // 成功条数
            int successNum = 0;
            // 数据在excel表格中的行数
            int dataFromExcelRowNum;

            String sn, type, vendor, model, dcName, cabinetName, cabinetStart, cabinetEnd, host, state, purchaseDate, protocol, authType, user, pwd, privateKey, port, userTip, passwordTip, reloginPasswordTip, exporter, ipmiHost, ipmiPort;
            Integer accountAuthType, start, end, stateInt;
            Map ipmiMap;

            Map saveEntitys;
            Long userId = ShiroUtils.getUserId();
            Model modelEntity = null;

            // 没有关联关系的资产数据,采取批量入库的方式
            List<Asset> noRelationAssets = new ArrayList<>();
            // 第一行是表头 所以从第二行开始
            for (int i = firstRowNum + 1; i <= maxRowNum; i++) {
                dataFromExcelRowNum = i + 1;
                // 保证一行数据在同一事务内，将所有入库数据统一入库
                saveEntitys = new HashMap();

                account = accountCli = null;
                start = end = stateInt = null;
                boolean saveFlag = true;
                boolean relaFlag = true;
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
                    // 当前库中不存在改型号
                    if (modelId == null) {
                        relaFlag = false;
                    }
                    // 如果 type 为 server 的情况下 处理 ipmi 信息
                    if (Constant.ASSET_SERVER_TYPE.equals(type.toLowerCase())) {
                        ipmiMap = new HashMap();
                        ipmiHost = ExcelUtils.getCellValue(row.getCell(21));
                        ipmiPort = ExcelUtils.getCellValue(row.getCell(22));
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
                    // 当前库中不存在该机房
                    if (idcId == null) {
                        relaFlag = false;
                    }
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
                            relaFlag = false;
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
                } else {
                    // 指定了起始位置 但未选择机柜情况
                    if (StringUtils.isNotEmpty(cabinetStart) || StringUtils.isNotEmpty(cabinetEnd)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ASSET_CABINET_ISNULL.getMsg());
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

                // 账号相关
                protocol = ExcelUtils.getCellValue(row.getCell(11));       // 协议信息
                authType = ExcelUtils.getCellValue(row.getCell(12));      // 认证类型
                user = ExcelUtils.getCellValue(row.getCell(13));          // 用户名
                pwd = ExcelUtils.getCellValue(row.getCell(14));           // 密码
                privateKey = ExcelUtils.getCellValue(row.getCell(15));    // 密钥文件
                port = ExcelUtils.getCellValue(row.getCell(16));          // 端口
                userTip = ExcelUtils.getCellValue(row.getCell(17));       // telnet协议有效 用户名提示
                passwordTip = ExcelUtils.getCellValue(row.getCell(18));   // telnet协议有效 密码提示
                reloginPasswordTip = ExcelUtils.getCellValue(row.getCell(19));// telnet协议有效 重复登陆提示

                // 是否内置exporter
                exporter = ExcelUtils.getCellValue(row.getCell(20));

                // 满足添加Account所需
                if (StringUtils.isNotEmpty(protocol) && StringUtils.isNotEmpty(authType) && StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(port)) {
                    relaFlag = false;
                    account = new Account();
                    // 验证协议
                    if (!Constant.ACCOUNT_SSH_PROTOCOL.equals(protocol) && !Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_PROTOCOL_INCORRECT.getMsg());
                    }
                    // 验证认证类型
                    accountAuthType = Integer.parseInt(authType);
                    if (!Constant.AUTH_PASSWORD.equals(accountAuthType) && !Constant.AUTH_CERTIFICATE.equals(accountAuthType)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_AUTHTYPE_INVALIDE.getMsg());
                    }
                    if (Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)) {
                        if (!Constant.AUTH_PASSWORD.equals(accountAuthType)) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_AUTHTYPE_INVALIDE.getMsg());
                        }
                    }
                    account.setProtocol(protocol);
                    account.setAuthType(accountAuthType);
                    account.setUser(user);
                    account.setPort(Integer.parseInt(port));
                    if (Constant.ACCOUNT_SSH_PROTOCOL.equals(protocol)) {
                        if (Constant.AUTH_PASSWORD.equals(accountAuthType) && StringUtils.isNotEmpty(pwd)) {
                            account.setPwd(pwd);
                        }
                        if (Constant.AUTH_CERTIFICATE.equals(accountAuthType) && StringUtils.isNotEmpty(privateKey)) {
                            account.setPrivateKey(privateKey);
                        }
                    } else if (Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)) {
                        account.setUserTip(userTip);
                        account.setPasswordTip(passwordTip);
                        account.setReloginPasswordTip(reloginPasswordTip);
                    }

                    // 深克隆 预备登陆服务器上传node exporter
                    accountCli = JSONObject.parseObject(JSON.toJSONString(account), Account.class);

                    // 校验端口
                    if (!CommonUtils.checkPort(account.getPort().toString())) {
                        saveFlag = false;
                        account = accountCli = null;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, RCode.ACCOUNT_PORT_ERROR.getMsg());
                    }

                }


                // 前面没有报错
                if (saveFlag) {
                    asset.setSeq(seqUUID);
                    asset.setCreateAt(DateUtil.getUTCTimeByConfigTimeZone());
                    asset.setCreateBy(userId);
                    asset.setUpdateBy(userId);

                    // 当前数据没有任何关联关系 ，采取批量入库的方式
                    if (relaFlag) {
                        noRelationAssets.add(asset);
                        // 更新sn host map
                        assetSnList.add(asset.getSn());
                        assetHostList.add(asset.getHost());

                        if (asset.getCabinetStart() != null && asset.getCabinetEnd() != null) {
                            // 更新cabinetList中对应的关系
                            for (Cabinet cabinet : cabinetList) {
                                if (cabinet.getId().equals(asset.getCabinetId())) {
                                    cabinet.getAssetList().add(asset);
                                    break;
                                }
                            }
                        }
                        successNum++;
                    } else {
                        saveEntitys.put("asset", asset);
                        // 账号信息正常
                        if (account != null) {
                            // 密码加密
                            if (StringUtils.isNotEmpty(account.getPwd())) {
                                account.setPwd(Base64.getEncoder().encodeToString(account.getPwd().getBytes()));
                            }
                            account.setSeq(seqUUID);
                            saveEntitys.put("account", account);
                            // 内置exporter
                            if (Constant.ASSET_BUILD_NODE_EXPORTER_FLAG.toString().equals(exporter)) {
                                // 上传文件
                                R r = this.uploadBuildExporter(configs, asset, accountCli);
                                Integer code = (Integer) r.get("code");
                                if (!RCode.SUCCESS.getCode().equals(code)) {
                                    this.addFailDetail(dataFromExcelRowNum, failDetails, (String) r.get("msg"));
                                }
                            }
                        }
                        // 整体保存这行数据，保证单行数据在同一事务内
                        // 手动开启事务
                        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                        try {
                            // 调用入库方法
                            Map updateMap = this.saveRowData(saveEntitys);
                            // 手动提交事务
                            dataSourceTransactionManager.commit(transactionStatus);
                            // 更新map值
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

                            successNum++;
                        } catch (Exception e) {
                            // 手动回滚事务
                            dataSourceTransactionManager.rollback(transactionStatus);
                            this.addFailDetail(dataFromExcelRowNum, failDetails, e.getMessage());
                        }
                    }
                }
            }

            // 批量入库没有关联的资产数据
            this.saveBatch(noRelationAssets);

            int totalNum = maxRowNum - firstRowNum;
            info.put("seq", seqUUID);
            info.put("totalNum", totalNum);
            info.put("successNum", successNum);
            info.put("failNum", totalNum - successNum);
            info.put("failDetail", failDetails);
            logger.info("importAssets info {}", info);
        } finally {
            try {
                work.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return info;
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
            if (model.getTypeCode() == null) {
                model.setTypeCode(type.getCode());
            }
            if (model.getVendorCode() == null) {
                model.setVendorCode(vendor.getCode());
            }
            modelService.save(model);
            updateMap.put("model", model);
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
        if (asset.getModelId() == null) {
            asset.setModelId(model.getId());
        }
        if (asset.getIdcId() == null) {
            asset.setIdcId(idc.getId());
        }
        if (asset.getCabinetId() == null && cabinet != null) {
            asset.setCabinetId(cabinet.getId());
        }
        this.save(asset);
        updateMap.put("asset", asset);
        // account
        Account account = (Account) saveEntitys.get("account");
        if (account != null) {
            account.setAssetId(asset.getId());
            accountService.save(account);
        }

        return updateMap;
    }

    private List<Map> validateExcelHeader(Sheet sheet, int firstRowNum) {
        Row row = sheet.getRow(firstRowNum);

        List<Map> mapList = new ArrayList<>();
        Map map;

        String[] str = Constant.ASSET_EXCEL_IMPORT_HEADER;
        List<String> titles = Arrays.asList(str);

        String titleValue, title;
        for (int i = 0; i < titles.size(); i++) {
            titleValue = ExcelUtils.getCellValue(row.getCell(i)).toLowerCase();
            title = titles.get(i).toLowerCase();
            if (!titleValue.contains(title)) {
                map = new HashMap();
                map.put("lineNo", firstRowNum);
                map.put("errorMsg", "The " + (i + 1) + " column title is the " + titleValue + ". It doesn't match the template. Please check. Thank you!");
                mapList.add(map);
            }
        }

        return mapList;
    }

    @Override
    public void cancelImport(String seq) {
        // asset
        this.remove(new QueryWrapper<Asset>().lambda().eq(Asset::getSeq, seq));
        // idc
        idcService.remove(new QueryWrapper<Idc>().lambda().eq(Idc::getSeq, seq));
        // cabinet
        cabinetService.remove(new QueryWrapper<Cabinet>().lambda().eq(Cabinet::getSeq, seq));
        // model
        modelService.remove(new QueryWrapper<Model>().lambda().eq(Model::getSeq, seq));
        // sys dict （asset type ,vendor）
        sysDictService.remove(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getSeq, seq));
        // account
        accountService.remove(new QueryWrapper<Account>().lambda().eq(Account::getSeq, seq));
    }

    @Override
    public R uploadBuildExporter(Map<String, String> configs, Asset asset, Account account) {
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
                SftpUtils.loginIn(asset.getHost(), account.getPort(), params.get("user"), authType, params.get("password"), params.get("key"), params.get("keyPassword"));
            } catch (Exception e) {
                logger.error("用户登录sftp服务器失败", e);
                return R.error(RCode.ASSET_LOGINSERVER_FAILED);
            }

            // 上传压缩包和执行脚本
            try {
                SftpUtils.upload(targetExporterPath, buildNodeExporterPath, buildNodeExporterName, tempShellName);
            } catch (Exception e) {
                logger.error("上传脚本失败", e);
                return R.error(RCode.ASSET_UPLOADFILE_FAILED);
            }

            // 执行脚本让exporter运行
            try {
                String execCommand = "chmod +x " + targetExporterPath + "/" + tempShellName + " && " + targetExporterPath + "/" + tempShellName;
                SftpUtils.exec(execCommand);
            } catch (Exception e) {
                logger.error("执行脚本文件失败", e);
                return R.error(RCode.ASSET_EXECCOMMAND_FAILED);
            }
        } finally {
            // 退出
            SftpUtils.logout();
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

        // 暂未完成
        Map feature = new HashMap();

        resultMap.put("Feature",feature);
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
        Map<String, Object> queryParam = new HashMap<>(2);
        queryParam.put("type", Constant.DICT_ASSET_STATE_TYPE);
        List<SysDictEntity> assetStates = sysDictService.queryList(queryParam);
        Map<Integer, String> statesMap = new HashMap<>(4);
        for (SysDictEntity dict : assetStates) {
            statesMap.put(dict.getCode(), dict.getValue());
        }

        PageUtils page = this.queryPage(params);
        List<Asset> list = (List<Asset>) page.getList();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Assets");

        int rowNum = 0;
        // 添加标题行
        String[] columnNames = Constant.ASSET_EXCEL_EXPORT_HEADER;
        Row row = sheet.createRow(rowNum);
        Cell cell;

        // 创建样式对象
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);// 垂直
        style.setAlignment(HSSFCellStyle.ALIGN_CENTER);// 水平

        for (int i = 0; i < columnNames.length; i++) {
            // 创建列、单元格
            cell = row.createCell(i);
            cell.setCellValue(columnNames[i]);
            cell.setCellStyle(style);
        }

        // 添加数据
        Row row1;
        List<Account> accountList;
        Model model;
        Account account;
        Integer start, end, ipmiPort;
        Map ipmiMap;
        String ipmiHost;
        for (Asset asset : list) {
            account = null;
            row1 = sheet.createRow(++rowNum);

            model = asset.getModel();
            // sn
            row1.createCell(0).setCellValue(asset.getSn());
            // assettype
            row1.createCell(1).setCellValue(model.getType().getValue());
            // vendor
            row1.createCell(2).setCellValue(model.getVendor().getValue());
            // model
            row1.createCell(3).setCellValue(model.getName());
            // idc
            row1.createCell(4).setCellValue(asset.getIdc().getName());
            // cabinet
            row1.createCell(5).setCellValue(asset.getCabinet() == null ? null : asset.getCabinet().getName());
            // cabinetstart
            start = asset.getCabinetStart();
            row1.createCell(6).setCellValue(start == null ? "" : start + "");
            // cabinetend
            end = asset.getCabinetEnd();
            row1.createCell(7).setCellValue(end == null ? "" : end + "");
            // host
            row1.createCell(8).setCellValue(asset.getHost());
            // state
            row1.createCell(9).setCellValue(statesMap.get(asset.getState()));
            // PurchaseDate
            row1.createCell(10).setCellValue(asset.getPurchaseDate());

            accountList = asset.getAccounts();
            if (CollectionUtils.isNotEmpty(accountList)) {
                // 当前账号信息和资产是一对一
                account = accountList.get(0);
            }
            if (account != null) {
                // protocol
                row1.createCell(11).setCellValue(account.getProtocol());
                // authtype
                row1.createCell(12).setCellValue(account.getAuthType());
                // user
                row1.createCell(13).setCellValue(account.getUser());
                if (org.apache.commons.lang.StringUtils.isNotEmpty(account.getPwd())) {
                    // pwd
                    row1.createCell(14).setCellValue(account.getPwd());
                }
                if (org.apache.commons.lang.StringUtils.isNotEmpty(account.getPrivateKey())) {
                    // sshKeys
                    row1.createCell(15).setCellValue(account.getPrivateKey());
                }
                // port
                row1.createCell(16).setCellValue(account.getPort());
                if (org.apache.commons.lang.StringUtils.isNotEmpty(account.getUserTip())) {
                    // userTip
                    row1.createCell(17).setCellValue(account.getUserTip());
                }
                if (org.apache.commons.lang.StringUtils.isNotEmpty(account.getPasswordTip())) {
                    // passwordTip
                    row1.createCell(18).setCellValue(account.getPasswordTip());
                }
                if (org.apache.commons.lang.StringUtils.isNotEmpty(account.getReloginPasswordTip())) {
                    // reloginPasswordTip
                    row1.createCell(19).setCellValue(account.getReloginPasswordTip());
                }
            }

            // ipmi
            ipmiMap = JSONObject.parseObject(JSONObject.toJSONString(asset.getIpmi()), Map.class);
            if (ToolUtil.isNotEmpty(ipmiMap)) {
                ipmiHost = (String) ipmiMap.get("host");
                if (ToolUtil.isNotEmpty(ipmiHost)) {
                    row1.createCell(20).setCellValue(ipmiHost);
                }
                ipmiPort = (Integer) ipmiMap.get("port");
                if(ToolUtil.isNotEmpty(ipmiPort)){
                    row1.createCell(21).setCellValue(ipmiPort);
                }
            }
        }

        try {
            // 响应为流
            String fileName = "AssetsExport.xlsx"; // 默认名称
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public R uploadExporter(Asset asset, Account accountCli) {
        List<SysConfigEntity> sysConfigs = sysConfigService.list(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getStatus, 1));
        Map<String, String> configs = sysConfigs.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

        // 上传
        R r = this.uploadBuildExporter(configs, asset, accountCli);
        return r;
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
    public boolean validateUSize(Asset asset) {
        // 修改的情况  需要将自身节点补充到所属机柜的位置中去  再次重新计算可用位置
        Integer assetId = asset.getId();
        Integer cabinetId = asset.getCabinetId();
        Integer start = asset.getCabinetStart();
        Integer end = asset.getCabinetEnd();

        Model model = modelService.getById(asset.getModelId());
        // 校验 开始和结束位置占用的机柜是否符合 关联 model 的 usize值
        Integer modelUsize = model.getUsize();
        if (modelUsize != null && (end - start) < modelUsize) {
            throw new NZException(RCode.ASSET_POSITION_MISMATCH);
        }

        Integer oldCabinetId = null;
        if (assetId != null) {
            Asset assetEntity = this.getById(assetId);
            oldCabinetId = assetEntity.getCabinetId();
        }

        // 得到机柜下包含的节点信息
        List<Asset> assets = this.list(new QueryWrapper<Asset>().lambda().eq(Asset::getCabinetId, cabinetId));

        // 获取当前机柜的全部位置
        Cabinet cab = cabinetService.getById(cabinetId);

        // 所有位置 从1开始到uSize
        Integer[] allPosition = new Integer[cab.getuSize()];
        for (int i = 0; i < allPosition.length; i++) {
            allPosition[i] = i + 1;
        }

        // 已经占用的位置
        List<Integer> occList = new ArrayList<>();

        // 修改时节点的所属的机柜  应还回节点 再做校验
        if (oldCabinetId != null && oldCabinetId.equals(cabinetId)) {

            // 根据得到的节点信息 得到占用的位置
            for (Asset nodeInfo : assets) {
                if (nodeInfo.getId().equals(assetId)) {
                    // 修改的情况下 不记录当前占用位置
                    continue;
                }
                if (nodeInfo.getCabinetEnd() != null && nodeInfo.getCabinetEnd() != null) {
                    for (int i = nodeInfo.getCabinetStart(); i < nodeInfo.getCabinetEnd(); i++) {
                        occList.add(i);
                    }
                }
            }

        } else {
            for (Asset nodeInfo : assets) {
                if (nodeInfo.getCabinetEnd() != null && nodeInfo.getCabinetEnd() != null) {
                    for (int i = nodeInfo.getCabinetStart(); i < nodeInfo.getCabinetEnd(); i++) {
                        occList.add(i);
                    }
                }
            }

        }

        boolean b = this.handleFreePosition(allPosition, occList, start, end);
        return b;
    }

    public boolean validateUSizeForImport(Integer start, Integer end, Cabinet cab) {

        // 所有位置 从1开始到uSize
        Integer[] allPosition = new Integer[cab.getuSize()];
        for (int i = 0; i < allPosition.length; i++) {
            allPosition[i] = i + 1;
        }

        // 已经占用的位置
        List<Integer> occList = new ArrayList<>();

        for (Asset nodeInfo : cab.getAssetList()) {
            if (nodeInfo.getCabinetEnd() != null && nodeInfo.getCabinetEnd() != null) {
                for (int i = nodeInfo.getCabinetStart(); i < nodeInfo.getCabinetEnd(); i++) {
                    occList.add(i);
                }
            }
        }

        boolean b = this.handleFreePosition(allPosition, occList, start, end);
        return b;
    }

    private boolean handleFreePosition(Integer[] allPosition, List<Integer> occList, Integer start, Integer end) {
        // 空闲的位置
        List<Integer> freePosition = new ArrayList<>(Arrays.asList(allPosition));
        freePosition.removeAll(occList); // 例如 (1),2,3,4,(5,) 6,7,8,(9),10

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
		
		// 获取当前所有的mib tree
        Map<String, String> oidWithNameMap = snmpMibService.getAllTreeNodeOidAndNameMap();
		for(Account account : accounts) {
			String protocol = account.getProtocol();
			
			if(accounts.size()==1 && Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)) {
				// 该资产无ssh或snmp信息 采集失败 
				logger.error("asset feature get data error,assetId : {} has no SSH or SNMP information",assetId);
				throw new NZException("This asset has no SSH or SNMP information acquisition failure");
			}
			
			List<AssetFeature> results = new ArrayList<AssetFeature>();
						
			// ssh采集相关逻辑
			if(Constant.ACCOUNT_SSH_PROTOCOL.equals(protocol)) {
				String s="";
				Session session =null;
				try {
					Map<String, String> params = JSONObject.parseObject((String) account.getParams(), Map.class);
					JSch jSch =new JSch();
					session = jSch.getSession(params.get("user"), asset.getHost(), account.getPort());
					String sshType = params.get("method");
			        switch (sshType) {
			            case "key": {
			                String privateKey = params.get("key");
			                String keyPassword = params.get("keyPassword");
			                if (notEmpty(privateKey)) {
			                    File keyFile = new File(account.getPrivateKeyPath());
			                    if (!keyFile.exists()) {
			                        //将数据库中的私钥写到用户的机器上
			                        IOUtils.writeFile(keyFile, new ByteArrayInputStream(privateKey.getBytes()));
			                    }
			                    if (notEmpty(keyPassword)) {
			                        //设置带口令的密钥
			                        jSch.addIdentity(account.getPrivateKeyPath(), keyPassword);
			                    } else {
			                        //设置不带口令的密钥
			                        jSch.addIdentity(account.getPrivateKeyPath());
			                    }
			                    UserInfo userInfo = new SshUserInfo();
			                    session.setUserInfo(userInfo);
			                }
			                session.setConfig("StrictHostKeyChecking", "no");
			                break;
			            }
			            case "password": {
			                session.setConfig("StrictHostKeyChecking", "no");
			                session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
			                session.setPassword(new String(Base64.getDecoder().decode(params.get("password"))));
			                break;
			            }
			            default: {
			            }
			        }
//			        session.setServerAliveInterval(SERVER_ALIVE_INTERVAL);
			        session.connect(5000);
			        Set<String> sshKeySet = Constant.SSH_SHELL_MAP.keySet();
			       
			        // 采集cpu os disk memory core uptime信息
			        for(String key : sshKeySet) {
			        	String value = RuntimeUtil.sshCommondResult(session, Constant.SSH_SHELL_MAP.get(key));
			        	if(key.equals("cpu")) {
			        		String[] split = value.split("\r\n");
			        		String cpuCount = split[0];
			        		String cpuInfo = split[1];
			        		String[] ss = new String[Integer.valueOf(cpuCount)];
			        		for(int i=0;i<Integer.valueOf(cpuCount);i++) {
			        			ss[i]=cpuInfo;
			        		}
			        		value=JSON.toJSONString(ss);
			        	}
			        	if(key.equals("disk")) {
			        		String[] split = value.split("\r\n");
			            	int diskCount = split.length/2;
			            	List list =new ArrayList();
			            	for(int i=0;i<diskCount;i++) {
			            		Map map =new HashMap();
			            		String[] split2 = split[i].split(" ");
			            		String[] split3 = split[diskCount+i].split(" ");
			            		map.put("vendor", split3[0]);
			            		map.put("model", split3[1]);
			            		map.put("size",split2[1]);
			            		list.add(map);
			            	}
			            	value=JSON.toJSONString(list);
			        	}
			        	
			        	//查询数据库是否已经存在数据
			        	AssetFeature result = assetFeatureService.getOne(new QueryWrapper<AssetFeature>().lambda().eq(AssetFeature::getAssetId, assetId).eq(AssetFeature::getItem,key).eq(AssetFeature::getSource,Constant.ACCOUNT_SSH_PROTOCOL));
			        	if(ToolUtil.isEmpty(result)) {
			        		result = new AssetFeature();
			        	}
						result.setAssetId(assetId);
						result.setItem(key);
						if(key.equals("memory")) {
							result.setValue(value+"G");
						}else {
							result.setValue(value);
						}
						result.setSource(Constant.ACCOUNT_SSH_PROTOCOL);
						result.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
						results.add(result);
			        }
			        
			        // 采集网卡信息
			        // 获取所有网卡名称信息
			        String interfaceNames = RuntimeUtil.sshCommondResult(session, "ifconfig |grep mtu|awk -F ':' '{print $1}'|grep -v lo");
			        String[] names = interfaceNames.split("\r\n");
			        List interfaceInfos =new ArrayList();
			        for(String name : names) {
			        	Map map =new HashMap();
			        	map.put("name", name);
			        	map.put("inet", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" | grep -w inet | awk -F' ' '{print $2}'").replace("\r\n", ""));
			        	map.put("netmask", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" | grep netmask|awk '{print $4}'").replace("\r\n", ""));
			        	map.put("mtu", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" |grep mtu|awk '{print $4}'").replace("\r\n", ""));
			        	map.put("inet6", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" | grep -w inet6 | awk -F' ' '{print $2}'").replace("\r\n", ""));
			        	map.put("mac", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" |grep ether|awk '{print $2}'").replace("\r\n", ""));
			        	map.put("state", RuntimeUtil.sshCommondResult(session, "ifconfig "+name+" |grep mtu|awk -F ',' '{print $1}'|awk -F '<' '{print $2}'").replace("\r\n", ""));
			        	interfaceInfos.add(map);
			        }
			        AssetFeature result = assetFeatureService.getOne(new QueryWrapper<AssetFeature>().lambda().eq(AssetFeature::getAssetId, assetId).eq(AssetFeature::getItem,"interface").eq(AssetFeature::getSource,Constant.ACCOUNT_SSH_PROTOCOL));
		        	if(ToolUtil.isEmpty(result)) {
		        		result = new AssetFeature();
		        	}
		        	result.setAssetId(assetId);
					result.setItem("interface");
					result.setSource(Constant.ACCOUNT_SSH_PROTOCOL);
					result.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
			        result.setValue(JSON.toJSONString(interfaceInfos));
			        results.add(result);
			        session.disconnect();
				} catch (Exception e) {
					e.printStackTrace();
					logger.error("asset feature get data error,assetId : {} info : {}",assetId,e);
					throw new NZException(e.getMessage());
				} finally {
					if(session!=null) {
						session.disconnect();
					}
				}
			}
			
			// snmp采集相关逻辑
			if(Constant.ACCOUNT_SNMP_PROTOCOL.equals(protocol)) {
				Map snmpInfos = JSONObject.parseObject((String) account.getParams(), Map.class);
				try {
					Set<String> keySet = Constant.SNMP_OID_MAP.keySet();
					for(String key:keySet) {
						List<Map> snmpResults = SnmpUtil.snmpWalk(asset.getHost(), account.getPort(), (Integer)snmpInfos.get("version"), (String)snmpInfos.get("community"), Constant.SNMP_OID_MAP.get(key), snmpInfos);
						if(key.equals("ifEntry")) {
							List l =new ArrayList();
							if (CollectionUtils.isNotEmpty(snmpResults)) {
								Map<String,Map> handleMap =new LinkedHashMap<String,Map>();
					            String resultOid;
					            String name, str;
					            for (Map map : snmpResults) {
					                resultOid = map.get("oid").toString();
						   		    
					                String substring = resultOid.substring(0, resultOid.lastIndexOf("."));
					                str = resultOid;
					                // 处理对应关系
					                name = oidWithNameMap.get(resultOid);

					                // name 为null 截取之后再次获取
					                if (StringUtils.isEmpty(name)) {
					                    // 从最后开始截取 目前暂定截取最后一个 .
					                    for (int i = 0; i < Constant.OIDNAME_TRUNCATION_INDEX; i++) {
					                        resultOid = resultOid.substring(0, resultOid.lastIndexOf("."));
					                    }

					                    name = oidWithNameMap.get(resultOid);
					                    if (StringUtils.isNotEmpty(name)) {
					                        str = str.replaceAll(resultOid, "");
					                        name += str;
					                    }
					                }
					                map.put("name", name);
					                
					                //截取最后一位oid数
					                String[] split = map.get("oid").toString().split("\\.");
						   		    String string = split[split.length-1];
					                Map map2 = handleMap.get(string);
					                if(ToolUtil.isEmpty(map2)) {
					                	Map map3 =new HashMap();
					                	handleMap.put(string, map3);
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
				        	AssetFeature result = assetFeatureService.getOne(new QueryWrapper<AssetFeature>().lambda().eq(AssetFeature::getAssetId, assetId).eq(AssetFeature::getItem,key).eq(AssetFeature::getSource,Constant.ACCOUNT_SNMP_PROTOCOL));
				        	if(ToolUtil.isEmpty(result)) {
				        		result = new AssetFeature();
				        	}
							result.setAssetId(assetId);
							result.setItem(key);
							result.setValue(JSON.toJSONString(l));
							result.setSource(Constant.ACCOUNT_SNMP_PROTOCOL);
							result.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
							results.add(result);
						}else {
							for(Map snmpResult : snmpResults) {
								String value = (String) snmpResult.get("value");
								//查询数据库是否已经存在数据
					        	AssetFeature result = assetFeatureService.getOne(new QueryWrapper<AssetFeature>().lambda().eq(AssetFeature::getAssetId, assetId).eq(AssetFeature::getItem,key).eq(AssetFeature::getSource,Constant.ACCOUNT_SNMP_PROTOCOL));
					        	if(ToolUtil.isEmpty(result)) {
					        		result = new AssetFeature();
					        	}
								result.setAssetId(assetId);
								result.setItem(key);
								if(key.equals("sysUpTime")) {
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
}