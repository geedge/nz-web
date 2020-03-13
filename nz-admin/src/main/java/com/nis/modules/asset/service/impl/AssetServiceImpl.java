package com.nis.modules.asset.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.dao.AssetDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.asset.service.ModelService;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.metric.dto.MetricsDto;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.sys.controller.SysDictController;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.terminal.dao.AccountDao;
import com.nis.modules.terminal.entity.Account;
import com.nis.modules.terminal.service.AccountService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

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
    private AlertRuleService alertRuleService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private AccountDao accountDao;

    @Autowired
    private SysDictController sysDictController;

    @Autowired
    private SftpUtils sftpUtils;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
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

        IPage<Asset> page = new Query<Asset>().getPage(params);

        List<Asset> assetEntities = assetDao.queryList(page, params);
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
        if (!this.checkFieldExist(asset.getId(), "sn", asset.getSn())) {
            throw new NZException(RCode.ASSET_SN_DUPLICATE);
        }

        // 2. host地址唯一校验
        if (!this.checkFieldExist(asset.getId(), "host", asset.getHost())) {
            throw new NZException(RCode.ASSET_HOST_DUPLICATE);
        }

        // 3. 型号有效性校验
        Model model = modelService.getById(asset.getModelId());
        if (model == null) {
            // 所选资产型号不存在
            throw new NZException(RCode.ASSET_MODEL_NOTFOUND);
        }

        // 4.数据中心有效性校验
        Idc idc = idcService.getById(asset.getIdcId());
        if (idc == null) {
            // 数据中心不存在
            throw new NZException(RCode.ASSET_IDC_NOTFOUND);
        }

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
        }

        // 6. 状态有效性校验
        if (asset.getState() != null) {
            Map<String, Object> map = new HashMap<>(2);

            map.put("type", Constant.DICT_ASSET_STATE_TYPE); // 字典表中资产状态 type 值 assetState
            List<SysDictEntity> assetTypes = sysDictService.queryList(map);
            boolean flag = false;
            for (SysDictEntity dict : assetTypes) {
                if (asset.getState().equals(dict.getCode())) {
                    flag = true;
                }
            }
            if (!flag) {
                // 未找到资产状态
                throw new NZException(RCode.ASSET_STATE_NOTFOUND);
            }
        } else {
            asset.setState(1);
        }

        Date now = new Date();
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
        } else {
            this.updateById(asset);
            // 修改的时候如果没有指定 account, 则视为删除asset关联account
            if (CollectionUtils.isEmpty(asset.getAccounts())) {
                accountService.remove(new QueryWrapper<Account>().eq("asset_id", asset.getId()));
            }
        }

        // 保存 accounts
        if (CollectionUtils.isNotEmpty(asset.getAccounts())) {
            for (int i = 0; i < asset.getAccounts().size(); i++) {
                Account account = asset.getAccounts().get(i);
                account.setAssetId(asset.getId());

                accountService.saveOrUpdateAccount(account);
               /* // 在SSH协议 telnet提示信息无效
                if(Constant.ACCOUNT_SSH_PROTOCOL.equals(account.getProtocol())){
                    account.setUserTip(null);
                    account.setPasswordTip(null);
                    account.setReloginPasswordTip(null);
                } else {
                    // 在telnet协议中pwd 与 privateKey字段无效
                    account.setPwd(null);
                    account.setPrivateKey(null);
                }
                // 添加
                if (account.getId() == null) {
                    // 处理密码
                    if (Constant.AUTH_PASSWORD.equals(account.getAuthType())) {
                        if (StringUtils.isNotEmpty(account.getPwd())) {
                            String encodePwd = Base64.getEncoder().encodeToString(account.getPwd().getBytes());
                            account.setPwd(encodePwd);
                        }

                        account.setPrivateKey(null);
                    } else {
                        account.setPwd(null);
                    }
                    account.setAssetId(asset.getId());

                    accountService.save(account);
                } else {
                    // 修改参数含有密码
                    if (Constant.AUTH_PASSWORD.equals(account.getAuthType())) {
                        if (StringUtils.isNotEmpty(account.getPwd())) {
                            Account accountEntity = accountService.getById(account.getId());
                            // 前台传参密码和数据库存储密码不对应 证明修改了密码
                            if (!account.getPwd().equals(accountEntity.getPwd())) {
                                String encodePwd = Base64.getEncoder().encodeToString(account.getPwd().getBytes());
                                account.setPwd(encodePwd);
                            }
                        }
                        account.setPrivateKey(null);
                    } else {
                        account.setPwd(null);
                    }

                    accountService.updateById(account);
                }*/
            }
        }
    }

    /**
     * 保存内置module 下endpoint信息
     *
     * @param asset
     */
    private void saveInitEndpoint(Asset asset) {
        SysConfigEntity initModule = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", Constant.SYSCONFIG_KEY_INIT_MODULE));
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
        endpointService.remove(new QueryWrapper<Endpoint>().in("asset_id", idList));

        // 3. 删除关联的alert信息
        alertRuleService.removeByTypeIds(Constant.AlertType.ALERT_TYPE_ASSET.getCode(), idList);
        alertMessageService.removeByTypeIds(Constant.AlertType.ALERT_TYPE_ASSET.getCode(), idList);
    }

    @Override
    public void validateAccount(Account account) {
        // 1.协议protocol不能为空
        String protocol = account.getProtocol();
        ValidateUtils.is(protocol).notNull(RCode.ACCOUNT_PROTOCOL_ISNULL);
        if (!Constant.ACCOUNT_SSH_PROTOCOL.equals(protocol) && !Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)) {
            throw new NZException(RCode.ACCOUNT_PROTOCOL_INCORRECT);
        }
        // 2.认证类型必须合法
        Integer authType = account.getAuthType();
        ValidateUtils.is(authType).notNull(RCode.ACCOUNT_AUTHTYPE_ISNULL);
        if (!Constant.AUTH_PASSWORD.equals(authType) && !Constant.AUTH_CERTIFICATE.equals(authType)) {
            throw new NZException(RCode.ACCOUNT_AUTHTYPE_INVALIDE);
        }
        if (Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)) {
            if (!Constant.AUTH_PASSWORD.equals(authType)) {
                throw new NZException(RCode.ACCOUNT_AUTHTYPE_INVALIDE);
            }
        }
        // 3.用户名不能为空
        ValidateUtils.is(account.getUser()).notNull(RCode.ACCOUNT_USER_ISNULL);
        // 4.port 不能为空
        ValidateUtils.is(account.getPort()).notNull(RCode.ACCOUNT_PORT_ISNULL);
        boolean portFlag = CommonUtils.checkPort(account.getPort().toString());
        if (!portFlag) {
            // 账号端口不正确
            throw new NZException(RCode.ACCOUNT_PORT_ERROR);
        }
    }

    public boolean checkFieldExist(Integer id, String column, String columnValue) {
        List<Asset> list = this.list(new QueryWrapper<Asset>().eq(column, columnValue));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
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

        Map<String, SysDictEntity> assetTypesMap = new HashMap();
        Map<String, SysDictEntity> vendorsMap = new HashMap();
        Map<String, Model> modelMap = new HashMap();
        Map<String, Idc> idcMap = new HashMap();
        Map<String, Integer> assetSnMap = new HashMap();
        Map<String, Integer> assetHostMap = new HashMap();
        Map<String, String> configs = new HashMap<>();


        Map<String, Object> queryParam = new HashMap<>(2);
        queryParam.put("type", Constant.DICT_ASSET_MODEL_TYPE); // 字典表中资产型号类型type值 assetType
        List<SysDictEntity> assetTypes = sysDictService.queryList(queryParam);
        for (SysDictEntity entity : assetTypes) {
            assetTypesMap.put(entity.getValue(), entity);
        }

        queryParam.put("type", Constant.DICT_ASSET_MODEL_VENDOR); // 字典表中资产型号厂商type值 vendor
        List<SysDictEntity> vendors = sysDictService.queryList(queryParam);
        for (SysDictEntity entity : vendors) {
            vendorsMap.put(entity.getValue(), entity);
        }

        List<Model> modelList = modelService.list();
        for (Model model : modelList) {
            modelMap.put(model.getName(), model);
        }

        List<Idc> idcList = idcService.list();
        for (Idc idc : idcList) {
            idcMap.put(idc.getName(), idc);
        }

        List<Cabinet> cabinetList = cabinetService.list();

        List<Asset> assetList = this.list();
        for (Asset asset : assetList) {
            assetSnMap.put(asset.getSn(), 1);
        }

        for (Asset asset : assetList) {
            assetHostMap.put(asset.getHost(), 1);
        }

        List<SysConfigEntity> sysConfigs = sysConfigService.list(new QueryWrapper<SysConfigEntity>().eq("status", 1));
        for (SysConfigEntity config : sysConfigs) {
            configs.put(config.getParamKey(), config.getParamValue());
        }

        Map info = new HashMap();
        Workbook work = null;
        try {
            work = ExcelUtils.getWork(originalFilename, assetsExcelFile.getInputStream());
            // 默认得到第一个 sheet
            Sheet sheet = work.getSheetAt(0);
//            Sheet sheet = work.getSheet("assets");
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
            Map failDetail;
            // 成功条数
            int successNum = 0;

            String sn, type, vendor, model, dcName, cabinetName, host, state, purchaseDate, protocol, authType, user, pwd, privateKey, port, userTip, passwordTip, reloginPasswordTip, exporter;
            Integer accountAuthType;

            Map saveEntitys;
            Long userId = ShiroUtils.getUserId();

            // 没有关联关系的资产数据,采取批量入库的方式
            List<Asset> noRelationAssets = new ArrayList<>();
            // 第一行是表头 所以从第二行开始
            for (int i = firstRowNum + 1; i <= maxRowNum; i++) {
                // 保证一行数据在同一事务内，将所有入库数据统一入库
                saveEntitys = new HashMap();

                account = accountCli = null;
                boolean saveFlag = true;
                boolean relaFlag = true;
                asset = new Asset();
                row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                // 资产序列号
                sn = ExcelUtils.getCellValue(row.getCell(0));
                if (StringUtils.isEmpty(sn)) {
                    saveFlag = false;
                    failDetail = new HashMap();
                    failDetail.put("lineNo", i);
                    failDetail.put("errorMsg", RCode.ASSET_SN_ISNULL);
                    failDetails.add(failDetail);
                } else {
                    // 校验资产序列号是否唯一
                    if (this.checkImportSnOnly(sn, assetSnMap)) {
                        asset.setSn(sn);
                    } else {
                        saveFlag = false;
                        failDetail = new HashMap();
                        failDetail.put("lineNo", i);
                        failDetail.put("errorMsg", RCode.ASSET_SN_DUPLICATE);
                        failDetails.add(failDetail);
                    }
                }
                // 资产类型
                type = ExcelUtils.getCellValue(row.getCell(1));
                if (StringUtils.isEmpty(type)) {
                    saveFlag = false;
                    failDetail = new HashMap();
                    failDetail.put("lineNo", i);
                    failDetail.put("errorMsg", RCode.ASSET_TYPE_ISNULL);
                    failDetails.add(failDetail);
                }
                // 资产类型
                vendor = ExcelUtils.getCellValue(row.getCell(2));
                if (StringUtils.isEmpty(vendor)) {
                    saveFlag = false;
                    failDetail = new HashMap();
                    failDetail.put("lineNo", i);
                    failDetail.put("errorMsg", RCode.ASSET_VENDOR_ISNULL);
                    failDetails.add(failDetail);
                }
                // 型号
                model = ExcelUtils.getCellValue(row.getCell(3));
                if (StringUtils.isEmpty(model)) {
                    saveFlag = false;
                    failDetail = new HashMap();
                    failDetail.put("lineNo", i);
                    failDetail.put("errorMsg", RCode.ASSET_MODELNAME_ISNULL);
                    failDetails.add(failDetail);
                }

                // 处理类型 厂商 型号
                if (StringUtils.isNotEmpty(type) && StringUtils.isNotEmpty(vendor) && StringUtils.isNotEmpty(model)) {
                    Integer modelId = this.checkAssetTypeVendorModel(type, assetTypesMap, vendor, vendorsMap, model, modelMap, seqUUID, saveEntitys);
                    // modelid 可能为null,入库之后返回
                    asset.setModelId(modelId);
                    // 当前库中不存在改型号
                    if (modelId == null) {
                        relaFlag = false;
                    }
                }

                // DC
                Integer idcId = null;
                dcName = ExcelUtils.getCellValue(row.getCell(4));
                if (StringUtils.isEmpty(dcName)) {
                    saveFlag = false;
                    failDetail = new HashMap();
                    failDetail.put("lineNo", i);
                    failDetail.put("errorMsg", RCode.IDC_IDCNAME_ISNULL);
                    failDetails.add(failDetail);
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
                if (StringUtils.isNotEmpty(cabinetName) && StringUtils.isNotEmpty(dcName)) {
                    if (idcId != null) { // idc已存在，不需要添加
                        Integer cabinetId = this.checkCabinet(cabinetName, idcId, cabinetList, seqUUID, saveEntitys);
                        // cabinetId 可能为null,入库之后返回
                        asset.setCabinetId(cabinetId);
                        if (cabinetId == null) {
                            relaFlag = false;
                        }
                    } else {
                        // idc不存在 需要添加之后才能获取idcId供添加cabinet使用
                        Cabinet cabinet = new Cabinet();
                        cabinet.setName(cabinetName);
                        cabinet.setuSize(1);
                        cabinet.setSeq(seqUUID);
                        saveEntitys.put("cabinet", cabinet);
                    }
                }

                // host
                host = ExcelUtils.getCellValue(row.getCell(6));
                if (StringUtils.isEmpty(host)) {
                    saveFlag = false;
                    failDetail = new HashMap();
                    failDetail.put("lineNo", i);
                    failDetail.put("errorMsg", RCode.ASSET_HOST_ISNULL);
                    failDetails.add(failDetail);
                } else {
                    // host唯一校验
                    if (this.checkImportHostOnly(host, assetHostMap)) {
                        // 格式校验
                        if (host.matches(Constant.REGEX_IP)) {
                            asset.setHost(host);
                        } else {
                            saveFlag = false;
                            failDetail = new HashMap();
                            failDetail.put("lineNo", i);
                            failDetail.put("errorMsg", RCode.ASSET_HOST_FORMAT);
                            failDetails.add(failDetail);
                        }
                    } else {
                        saveFlag = false;
                        failDetail = new HashMap();
                        failDetail.put("lineNo", i);
                        failDetail.put("errorMsg", RCode.ASSET_HOST_DUPLICATE);
                        failDetails.add(failDetail);
                    }
                }

                // state
                state = ExcelUtils.getCellValue(row.getCell(7));
                if (StringUtils.isEmpty(state)) {
                    state = "1";
                }
                asset.setState(Integer.parseInt(state));

                // 购买日期
                purchaseDate = ExcelUtils.getCellValue(row.getCell(8));
                if (StringUtils.isNotEmpty(purchaseDate)) {
                    asset.setPurchaseDate(purchaseDate);
                }

                // 账号相关
                /*authType = ExcelUtils.getCellValue(row.getCell(9));
                user = ExcelUtils.getCellValue(row.getCell(10));
                pwd = ExcelUtils.getCellValue(row.getCell(11));
                privateKey = ExcelUtils.getCellValue(row.getCell(12));
                port = ExcelUtils.getCellValue(row.getCell(13));

                // 是否内置exporter
                exporter = ExcelUtils.getCellValue(row.getCell(14));*/

                // 满足添加Account所需
               /* if (StringUtils.isNotEmpty(authType) && StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(port)) {
                    relaFlag = false;
                    account = new Account();
                    accountAuthType = Integer.parseInt(authType);
                    if (Constant.AUTH_PASSWORD.equals(accountAuthType) && StringUtils.isNotEmpty(pwd)) {
                        account.setPwd(pwd);
                    }
                    if (Constant.AUTH_CERTIFICATE.equals(accountAuthType) && StringUtils.isNotEmpty(privateKey)) {
                        account.setPrivateKey(privateKey);
                    }
                    account.setAuthType(accountAuthType);
                    account.setUser(user);
                    account.setPort(Integer.parseInt(port));

                    // 深克隆 预备登陆服务器上传node exporter
                    accountCli = JSONObject.parseObject(JSON.toJSONString(account), Account.class);

                    // 校验端口
                    if (!CommonUtils.checkPort(account.getPort().toString())) {
                        saveFlag = false;
                        failDetail = new HashMap();
                        failDetail.put("lineNo", i);
                        failDetail.put("errorMsg", RCode.ACCOUNT_PORT_ERROR);
                        failDetails.add(failDetail);
                        account = accountCli = null;
                    }
                }*/

                // 账号相关
                protocol = ExcelUtils.getCellValue(row.getCell(9));       // 协议信息
                authType = ExcelUtils.getCellValue(row.getCell(10));      // 认证类型
                user = ExcelUtils.getCellValue(row.getCell(11));          // 用户名
                pwd = ExcelUtils.getCellValue(row.getCell(12));           // 密码
                privateKey = ExcelUtils.getCellValue(row.getCell(13));    // 密钥文件
                port = ExcelUtils.getCellValue(row.getCell(14));          // 端口
                userTip = ExcelUtils.getCellValue(row.getCell(15));       // telnet协议有效 用户名提示
                passwordTip = ExcelUtils.getCellValue(row.getCell(16));   // telnet协议有效 密码提示
                reloginPasswordTip = ExcelUtils.getCellValue(row.getCell(17));// telnet协议有效 重复登陆提示

                // 是否内置exporter
                exporter = ExcelUtils.getCellValue(row.getCell(18));

                // 满足添加Account所需
                if (StringUtils.isNotEmpty(protocol) && StringUtils.isNotEmpty(authType) && StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(port)) {
                    relaFlag = false;
                    account = new Account();
                    // 验证协议
                    if (!Constant.ACCOUNT_SSH_PROTOCOL.equals(protocol) && !Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)) {
                        saveFlag = false;
                        failDetail = new HashMap();
                        failDetail.put("lineNo", i);
                        failDetail.put("errorMsg", RCode.ACCOUNT_PROTOCOL_INCORRECT);
                        failDetails.add(failDetail);
                    }
                    // 验证认证类型
                    accountAuthType = Integer.parseInt(authType);
                    if (!Constant.AUTH_PASSWORD.equals(accountAuthType) && !Constant.AUTH_CERTIFICATE.equals(accountAuthType)) {
                        saveFlag = false;
                        failDetail = new HashMap();
                        failDetail.put("lineNo", i);
                        failDetail.put("errorMsg", RCode.ACCOUNT_AUTHTYPE_INVALIDE);
                        failDetails.add(failDetail);
                    }
                    if (Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)) {
                        if (!Constant.AUTH_PASSWORD.equals(accountAuthType)) {
                            saveFlag = false;
                            failDetail = new HashMap();
                            failDetail.put("lineNo", i);
                            failDetail.put("errorMsg", RCode.ACCOUNT_AUTHTYPE_INVALIDE);
                            failDetails.add(failDetail);
                        }
                    }
                    account.setProtocol(protocol);
                    account.setAuthType(accountAuthType);
                    account.setUser(user);
                    account.setPort(Integer.parseInt(port));
                    if(Constant.ACCOUNT_SSH_PROTOCOL.equals(protocol)){
                        if (Constant.AUTH_PASSWORD.equals(accountAuthType) && StringUtils.isNotEmpty(pwd)) {
                            account.setPwd(pwd);
                        }
                        if (Constant.AUTH_CERTIFICATE.equals(accountAuthType) && StringUtils.isNotEmpty(privateKey)) {
                            account.setPrivateKey(privateKey);
                        }
                    }else if(Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)){
                        account.setUserTip(userTip);
                        account.setPasswordTip(passwordTip);
                        account.setReloginPasswordTip(reloginPasswordTip);
                    }

                    // 深克隆 预备登陆服务器上传node exporter
                    accountCli = JSONObject.parseObject(JSON.toJSONString(account), Account.class);

                    // 校验端口
                    if (!CommonUtils.checkPort(account.getPort().toString())) {
                        saveFlag = false;
                        failDetail = new HashMap();
                        failDetail.put("lineNo", i);
                        failDetail.put("errorMsg", RCode.ACCOUNT_PORT_ERROR);
                        failDetails.add(failDetail);
                        account = accountCli = null;
                    }

                }


                // 前面没有报错
                if (saveFlag) {
                    asset.setSeq(seqUUID);
                    asset.setCreateAt(new Date());
                    asset.setCreateBy(userId);
                    asset.setUpdateBy(userId);

                    // 当前数据没有任何关联关系 ，采取批量入库的方式
                    if (relaFlag) {
                        noRelationAssets.add(asset);
                        // 更新sn host map
                        assetSnMap.put(asset.getSn(), 1);
                        assetHostMap.put(asset.getHost(), 1);
                        successNum++;
                    } else {
                        saveEntitys.put("asset", asset);
                        // 账号信息正常
                        if (account != null) {
                            // 密码加密
                            if(StringUtils.isNotEmpty(account.getPwd())){
                                account.setPwd(Base64.getEncoder().encodeToString(account.getPwd().getBytes()));
                            }
                            account.setSeq(seqUUID);
                            saveEntitys.put("account", account);
                            // 内置exporter
                            if (Constant.ASSET_BUILD_NODE_EXPORTER_FLAG.toString().equals(exporter)) {
                                // 上传文件
                                if (!this.uploadBuildNodeExporter(configs, asset, accountCli)) {
                                    failDetail = new HashMap();
                                    failDetail.put("lineNo", i);
                                    failDetail.put("errorMsg", RCode.ASSET_EXPORTER_UPLOAD_FAIL);
                                    failDetails.add(failDetail);
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
                                modelMap.put(updateModel.getName(), updateModel);
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
                                assetSnMap.put(updateAsset.getSn(), 1);
                                assetHostMap.put(updateAsset.getHost(), 1);
                            }

                            successNum++;
                        } catch (Exception e) {
                            // 手动回滚事务
                            dataSourceTransactionManager.rollback(transactionStatus);
                            failDetail = new HashMap();
                            failDetail.put("lineNo", i);
                            failDetail.put("errorMsg", e.getMessage());
                            failDetails.add(failDetail);
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
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("importAssets error {}", e.getMessage());
        } finally {
            try {
                work.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return info;
        }
    }

    public Map saveRowData(Map saveEntitys) {
        Map updateMap = new HashMap();
        // 资产类型
        SysDictEntity type = (SysDictEntity) saveEntitys.get("type");
        if (type != null) {
            sysDictController.save(type);
            updateMap.put("type", type);
        }
        // 厂商
        SysDictEntity vendor = (SysDictEntity) saveEntitys.get("vendor");
        if (vendor != null) {
            sysDictController.save(vendor);
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

        String[] str = {"sn", "type", "vendor", "model", "idc", "cabinet", "host", "state", "purchasedate", "protocol", "authtype", "user", "pwd", "cert", "port", "usertip", "passwordtip", "reloginpasswordtip", "exporter"};
        List<String> titles = Arrays.asList(str);

        String titleValue, title;
        for (int i = 0; i < titles.size(); i++) {
            titleValue = ExcelUtils.getCellValue(row.getCell(i)).toLowerCase();
            title = titles.get(i);
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
        this.remove(new QueryWrapper<Asset>().eq("seq", seq));
        // idc
        idcService.remove(new QueryWrapper<Idc>().eq("seq", seq));
        // cabinet
        cabinetService.remove(new QueryWrapper<Cabinet>().eq("seq", seq));
        // model
        modelService.remove(new QueryWrapper<Model>().eq("seq", seq));
        // sys dict （asset type ,vendor）
        sysDictService.remove(new QueryWrapper<SysDictEntity>().eq("seq", seq));
        // account
        accountService.remove(new QueryWrapper<Account>().eq("seq", seq));
    }


    private Integer checkCabinet(String cabinetName, Integer idcId, List<Cabinet> cabinetList, String seqUUID, Map saveEntitys) {
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
            cabinet.setuSize(1);
            cabinet.setSeq(seqUUID);
            saveEntitys.put("cabinet", cabinet);
        }
        return cabinet.getId();
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

    public Integer checkAssetTypeVendorModel(String assetType, Map<String, SysDictEntity> assetTypesMap, String assetVendor, Map<String, SysDictEntity> vendorsMap, String modelName, Map<String, Model> modelMap, String seqUUID, Map saveEntitys) {
        // 是否新增 model 标识 由于model名称目前是唯一的，所以暂且不处理资产类型，厂商更改，型号名称不变情况
        //boolean flag = false;

        // 如果没有获取倒该资产类型
        SysDictEntity type = assetTypesMap.get(assetType);
        if (type == null) {
            type = new SysDictEntity();
            type.setType(Constant.DICT_ASSET_MODEL_TYPE);
            type.setValue(assetType);
            type.setSeq(seqUUID);
            //flag = true;
            saveEntitys.put("type", type);
        }

        // 如果没有厂商
        SysDictEntity vendor = vendorsMap.get(assetVendor);
        if (vendor == null) {
            vendor = new SysDictEntity();
            vendor.setType(Constant.DICT_ASSET_MODEL_VENDOR);
            vendor.setValue(assetVendor);
            vendor.setSeq(seqUUID);
            //flag = true;
            saveEntitys.put("vendor", vendor);
        }

        // 如果没有资产型号
        Model model = modelMap.get(modelName);
        if (model == null) {
            model = new Model();
            model.setName(modelName);
            model.setTypeCode(type.getCode());
            model.setVendorCode(vendor.getCode());
            model.setSeq(seqUUID);
            saveEntitys.put("model", model);
        }
        // 如果型号存在则会返回modelId
        return model.getId();
    }

    public boolean checkImportSnOnly(String sn, Map<String, Integer> assetSnMap) {
        if (assetSnMap != null && assetSnMap.size() > 0) {
            // 如果不唯一
            if (assetSnMap.get(sn) != null) {
                return false;
            } else { // 唯一
                return true;
            }
        }
        return true;
    }

    private boolean checkImportHostOnly(String host, Map<String, Integer> assetHostMap) {
        if (assetHostMap != null && assetHostMap.size() > 0) {
            // 如果不唯一
            if (assetHostMap.get(host) != null) {
                return false;
            } else { // 唯一
                return true;
            }
        }
        return true;
    }

    public boolean uploadBuildNodeExporter(Map<String, String> configs, Asset asset, Account account) {
        // 上传文件
        try {
            // 内置 node_exporter 默认安装服务器路径
            String targetExporterPath = configs.get(Constant.SYSCONFIG_KEY_NODEEXPORTER_TARGETPATH);
            // 内置 node_exporter 初始执行脚本名称
            String shellName = configs.get(Constant.SYSCONFIG_KEY_NODEEXPORTER_SHELLNAME);
            // 内置 node_exporter 项目路径
            String buildNodeExporterPath = configs.get(Constant.SYSCONFIG_KEY_NODEEXPORTER_PATH);
            // 内置 node_exporter 压缩包名称
            String buildNodeExporterName = configs.get(Constant.SYSCONFIG_KEY_NODEEXPORTER_NAME);

            // 登录 导入这里的 sshkey 暂未处理 目前只是 user password 模式登陆
            sftpUtils.loginIn(asset.getHost(), account.getPort(), account.getUser(), account.getAuthType(), account.getPwd(), null);

            // 上传
            sftpUtils.upload(targetExporterPath, buildNodeExporterPath, buildNodeExporterName, shellName);

            // 执行脚本让exporter运行
            String execCommand = "chmod +x " + targetExporterPath + "/" + shellName + " && " + targetExporterPath + "/" + shellName;
            sftpUtils.exec(execCommand);
        } catch (Exception e) {
            logger.info("node_exporter upload error {}", asset);
            // 上传失败
            return false;
        } finally {
            // 退出
            sftpUtils.logout();
        }
        return true;
    }
}