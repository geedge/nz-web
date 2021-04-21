package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.dao.AssetFieldMetaDao;
import com.nis.modules.asset.dao.CabinetDao;
import com.nis.modules.asset.entity.*;
import com.nis.modules.asset.service.*;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.overview.service.OverviewService;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 资产    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service
public class AssetAssetServiceImpl extends ServiceImpl<AssetAssetDao, AssetAsset> implements AssetAssetService {

    private static Log logger = Log.get();

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private AssetAssetDao assetDao;

    @Autowired
    private AssetFieldMetaDao assetFieldMetaDao;

    @Autowired
    private DcService dcService;

    @Autowired
    private CabinetService cabinetService;

    @Autowired
    private AssetBrandService brandService;

    @Autowired
    private AssetModelService modelService;

    @Autowired
    private AssetAccountService accountService;

    @Autowired
    private AssetTypeConfService typeConfService;

    @Autowired
    private AssetStateConfService stateConfService;

    @Autowired
    private AssetFieldMetaService assetFieldMetaService;

    @Autowired
    private AssetFieldValueService assetFieldValueService;

    @Autowired
    private AssetAccountRelService assetAccountRelService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private AssetPingService assetPingService;

    @Autowired
    private OverviewService overviewService;

    @Autowired
    private AssetFeatureService assetFeatureService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private CabinetDao cabinetDao;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private TransactionDefinition transactionDefinition;

    @Override
    public AssetAsset queryAssetInfo(Integer id) {
        AssetAsset asset = this.getById(id);
        if (ToolUtil.isEmpty(asset)) {
            throw new NZException(RCode.ASSET_NOT_EXIST);
        }
        AssetTypeConf type = typeConfService.getById(asset.getTypeId());
        asset.setType(type);

        AssetStateConf state = stateConfService.getById(asset.getStateId());
        asset.setState(state);

        AssetBrand brand = brandService.getById(asset.getBrandId());
        asset.setBrand(brand);

        AssetModel model = modelService.getById(asset.getModelId());
        asset.setModel(model);

        Dc dc = dcService.getById(asset.getDcId());
        asset.setDc(dc);

        if (asset.getCabinetId() != null) {
            Cabinet cabinet = cabinetService.getById(asset.getCabinetId());
            asset.setCabinet(cabinet);
        }

        if (asset.getCreateBy() != null) {
            SysUserEntity userEntity = sysUserDao.selectById(asset.getCreateBy());
            asset.setCreateUser(userEntity);
        }

        if (asset.getUpdateBy() != null) {
            SysUserEntity userEntity = sysUserDao.selectById(asset.getUpdateBy());
            asset.setUpdateUser(userEntity);
        }

        if (asset.getPid() != -1) {
            AssetAsset parent = this.getById(asset.getPid());
            asset.setParent(parent);
        }

        List<AssetFieldMeta> metaList = assetFieldMetaService.getAssetFieldMetaListByAssetId(asset.getId());
        asset.setFields(metaList);
        return asset;
    }

    private void handleQueryAssetParam(Map<String, Object> params) {
        if (ToolUtil.isNotEmpty(params.get("brandIds"))) {
            params.put("brandIds", Arrays.asList(String.valueOf(params.get("brandIds")).split(StringPool.COMMA)));
        }

        if (ToolUtil.isNotEmpty(params.get("modelIds"))) {
            params.put("modelIds", Arrays.asList(String.valueOf(params.get("modelIds")).split(StringPool.COMMA)));
        }

        if (ToolUtil.isNotEmpty(params.get("dcIds"))) {
            params.put("dcIds", Arrays.asList(String.valueOf(params.get("dcIds")).split(StringPool.COMMA)));
        }

        if (ToolUtil.isNotEmpty(params.get("cabinetIds"))) {
            params.put("cabinetIds", Arrays.asList(String.valueOf(params.get("cabinetIds")).split(StringPool.COMMA)));
        }

        if (ToolUtil.isNotEmpty(params.get("typeIds"))) {
            params.put("typeIds", Arrays.asList(String.valueOf(params.get("typeIds")).split(StringPool.COMMA)));
        }

        if (ToolUtil.isNotEmpty(params.get("stateIds"))) {
            params.put("stateIds", Arrays.asList(String.valueOf(params.get("stateIds")).split(StringPool.COMMA)));
        }

        if (ToolUtil.isNotEmpty(params.get("fields"))) {
            Map<String, List<String>> fieldsParams = JSONObject.parseObject(params.get("fields").toString(), Map.class);
            // 包含 field value 信息
            List<AssetFieldMeta> metaList = assetFieldMetaDao.selectMateInfoListByAssetId(null);
            Map<String, List<AssetFieldMeta>> metaKeyAndMetaInfoMap = metaList.stream().collect(Collectors.groupingBy(AssetFieldMeta::getMetaKey));

            List<List<Integer>> assetIdList = new ArrayList<>();
            List<String> paramValues;
            List<AssetFieldMeta> metaListByKey, metaListByValue;
            for (Map.Entry<String, List<String>> entry : fieldsParams.entrySet()) {
                // 查询 values
                paramValues = entry.getValue();
                // 当前 key 包含的 meta 集合
                metaListByKey = metaKeyAndMetaInfoMap.get(entry.getKey());
                if (metaListByKey == null) {
                    continue;
                }
                metaListByValue = new ArrayList<>();
                for (String val : paramValues) {
                    metaListByValue.addAll(metaListByKey.stream().filter(meta -> meta.getValue().equals(val)).collect(Collectors.toList()));
                }
                assetIdList.add(metaListByValue.stream().map(AssetFieldMeta::getAssetId).distinct().collect(Collectors.toList()));
            }

            List<Integer> assetIds = assetIdList.stream().flatMap(Collection::stream).collect(Collectors.toList());
            if (assetIdList.size() == 1) {
                assetIds = assetIds.stream().distinct().collect(Collectors.toList());
            } else {
                assetIds = assetIdList.stream().reduce((tempList1, tempList2) -> {
                    tempList1.retainAll(tempList2);
                    return tempList1;
                }).get();
            }
            if (CollectionUtils.isEmpty(assetIds)) {
                assetIds.add(-99999);
            }
            params.put("innerAssetIdsByFields", assetIds);
        }
    }

    @Override
    public PageUtils queryAssetPage(Map<String, Object> params) {
        // 处理请求参数
        this.handleQueryAssetParam(params);

        IPage<AssetAsset> page = new Query<AssetAsset>().getPage(params);
        List<AssetAsset> assetList = assetDao.queryAssetList(page, params);

        // 查询结果不为空时处理返回结果
        if (CollectionUtils.isNotEmpty(assetList)) {
            this.handleResuleList(assetList);
        }

        page.setRecords(assetList);
        return new PageUtils(page);
    }

    private void handleResuleList(List<AssetAsset> assetList) {
        List<SysUserEntity> userList = sysUserDao.selectList(null);
        Map<Long, SysUserEntity> userIdAndEntityMap = userList.stream().collect(Collectors.toMap(SysUserEntity::getId, Function.identity()));

        List<AssetAsset> list = this.list();
        Map<Integer, AssetAsset> assetIdAndEntityMap = list.stream().collect(Collectors.toMap(AssetAsset::getId, Function.identity()));

        List<AssetFieldMeta> metaInfoList = assetFieldMetaDao.selectMateInfoListByAssetId(null);
        Map<Integer, List<AssetFieldMeta>> assetIdAndMetaInfoList = metaInfoList.stream().collect(Collectors.groupingBy(AssetFieldMeta::getAssetId));

        for (AssetAsset asset : assetList) {
            if (ToolUtil.isNotEmpty(asset.getCreateBy())) {
                asset.setCreateUser(userIdAndEntityMap.get(asset.getCreateBy()));
            }
            if (ToolUtil.isNotEmpty(asset.getUpdateBy())) {
                asset.setUpdateUser(userIdAndEntityMap.get(asset.getUpdateBy()));
            }
            if (asset.getPid() != -1) {
                asset.setParent(assetIdAndEntityMap.get(asset.getPid()));
            }
            asset.setFields(assetIdAndMetaInfoList.get(asset.getId()));
        }
    }

    private void validateAssetInfo(AssetAsset asset) {
        List<AssetAsset> list = this.list();
        Integer assetId = asset.getId();
        // name 唯一
        Map<String, Integer> map = list.stream().collect(Collectors.toMap(AssetAsset::getName, AssetAsset::getId));
        Integer id = map.get(asset.getName());
        if (id != null && !id.equals(assetId)) {
            throw new NZException(RCode.ASSET_NAME_DUPLICATE);
        }

        // number 唯一 不重复，可以为空串
        if (StringUtils.isNotEmpty(asset.getNumber())) {
            map = list.stream().filter(pojo -> StringUtils.isNotEmpty(pojo.getNumber())).collect(Collectors.toMap(AssetAsset::getNumber, AssetAsset::getId));
            id = map.get(asset.getNumber());
            if (id != null && !id.equals(assetId)) {
                throw new NZException(RCode.ASSET_NUMBER_DUPLICATE);
            }
        }

        // sn 唯一
        map = list.stream().collect(Collectors.toMap(AssetAsset::getSn, AssetAsset::getId));
        id = map.get(asset.getSn());
        if (id != null && !id.equals(assetId)) {
            throw new NZException(RCode.ASSET_SN_DUPLICATE);
        }

        // manageIp 唯一
        map = list.stream().collect(Collectors.toMap(AssetAsset::getManageIp, AssetAsset::getId));
        id = map.get(asset.getManageIp());
        if (id != null && !id.equals(assetId)) {
            throw new NZException(RCode.ASSET_MANAGEIP_DUPLICATE);
        }

        // brand
        AssetBrand brand = brandService.getById(asset.getBrandId());
        ValidateUtils.is(brand).notNull(RCode.ASSET_BRAND_NOT_FOUND);

        // model
        AssetModel model = modelService.getById(asset.getModelId());
        ValidateUtils.is(model).notNull(RCode.ASSET_MODEL_NOTFOUND);

        if (!model.getBrandId().equals(brand.getId())) {
            throw new NZException(RCode.ASSET_BRAND_NOT_CONTAIN_MODEL);
        }

        // state
        AssetStateConf stateConf = stateConfService.getById(asset.getStateId());
        ValidateUtils.is(stateConf).notNull(RCode.STATECONF_NOT_FOUND);

        // type
        AssetTypeConf typeConf = typeConfService.getById(asset.getTypeId());
        ValidateUtils.is(typeConf).notNull(RCode.TYPECONF_NOT_FOUND);

        // 修改时，如果本身为宿主机，切包含虚拟机，则不能修改为虚拟机
        if (asset.getId() != null) {
            List<AssetAsset> babyAsset = this.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getPid, asset.getId()));
            // 包含虚拟机
            if (CollectionUtils.isNotEmpty(babyAsset)) {
                if (typeConf.getVm().equals(1)) {
                    throw new NZException(RCode.ASSET_NOT_UPDATE);
                }
            }
        }

        // 虚拟机 vm info 校验
        if (typeConf.getVm().equals(1)) {
            if (asset.getPid() == null) {
                // 虚拟机必须选择宿主
                throw new NZException(RCode.ASSET_NOT_SELECT_PARENT);
            }

            // pid 是否关联 asset
            AssetAsset parentAsset = this.getById(asset.getPid());
            ValidateUtils.is(parentAsset).notNull(RCode.ASSET_PARENT_NOT_EXIST);

            // 宿主机是否是不是虚拟机、是否支持挂载
            AssetTypeConf parentAssetTypeConf = typeConfService.getById(parentAsset.getTypeId());
            if (!parentAssetTypeConf.getVmh().equals(1) || !parentAssetTypeConf.getVm().equals(0)) {
                // 只有宿主机，才能支持挂载虚拟机
                throw new NZException(RCode.ASSET_NOT_SUPPORT_MOUNTING);
            }
        } else {
            // 本身不是虚拟机，不能选择宿主机
            if (asset.getPid() != null) {
                throw new NZException(RCode.ASSET_NOT_SUPPORT_MOUNTING);
            }
        }

        // 不是虚拟机进行 dc cabinet 校验，虚拟机直接集成 宿主机 dc cabinet 信息
        if (typeConf.getVm().equals(0)) {
            // dc
            Dc dc = dcService.getById(asset.getDcId());
            ValidateUtils.is(dc).notNull(RCode.ASSET_DC_NOTFOUND);

            // cab
            if (ToolUtil.isNotEmpty(asset.getCabinetId())) {
                Cabinet cabinet = cabinetService.getById(asset.getCabinetId());
                ValidateUtils.is(cabinet).notNull(RCode.ASSET_CABINET_NOTFOUND);

                // cabinet dc 不匹配
                if (!dc.getId().equals(cabinet.getDcId())) {
                    throw new NZException(RCode.DC_NOTHAS_CABINET);
                }

                // 校验机柜可用位置
                boolean flag = this.validateCabinetPosition(asset, cabinet, model);
                if (!flag) {
                    throw new NZException(RCode.ASSET_POSITION_OCCUPY);
                }
            } else {
                // 指定了起始位置 但未选择机柜情况
                if (asset.getCabinetStart() != null || asset.getCabinetEnd() != null) {
                    throw new NZException(RCode.ASSET_CABINET_ISNULL);
                }
            }
        }
        
        // account
        List<Integer> accountIds = asset.getAccounts();
        if (CollectionUtils.isNotEmpty(accountIds)) {
            List<AssetAccount> assetAccounts = accountService.list();
            List<Integer> idList = assetAccounts.stream().map(AssetAccount::getId).collect(Collectors.toList());

            List<Integer> templateAccountIds = new ArrayList<>(accountIds);
            templateAccountIds.removeAll(idList);
            if (CollectionUtils.isNotEmpty(templateAccountIds)) {
                throw new NZException("The account information was not found. Details: " + templateAccountIds.toString(), RCode.ASSET_ACCOUNT_NOT_FOUND.getCode());
            }
        }

        // fields
        List<AssetFieldMeta> fields = asset.getFields();
        if (CollectionUtils.isNotEmpty(fields)) {
            List<AssetFieldMeta> metaList = assetFieldMetaService.list();
            List<Integer> metaIdList = metaList.stream().map(AssetFieldMeta::getId).collect(Collectors.toList());
            // 需要保存的 meta id
            List<Integer> saveMetaIds = fields.stream().map(AssetFieldMeta::getId).collect(Collectors.toList());
            saveMetaIds.removeAll(metaIdList);
            if (CollectionUtils.isNotEmpty(saveMetaIds)) {
                throw new NZException("These meta data were not found. Details: " + saveMetaIds.toString(), RCode.ASSET_FIELD_META_NOT_FOUND.getCode());
            }
        }

    }

    public boolean validateUSizeForImport(Integer start, Integer end, Cabinet cab) {
        // 所有位置 从1开始到uSize
        Integer[] allPosition = new Integer[cab.getuSize()];
        for (int i = 0; i < allPosition.length; i++) {
            allPosition[i] = i + 1;
        }

        // 已经占用的位置
        Set<Integer> occList = new HashSet<>();
        if (CollectionUtils.isEmpty(cab.getAssets())) {
            return true;
        } else {
            for (AssetAsset nodeInfo : cab.getAssets()) {
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
     * 检查位置是否合理
     *
     * @param asset
     * @return
     */
    public boolean validateCabinetPosition(AssetAsset asset, Cabinet cabinet, AssetModel model) {
        Integer start = asset.getCabinetStart();
        Integer end = asset.getCabinetEnd();

        if (start != null && end != null) {
            end++;
            Integer cabuSize = cabinet.getuSize();
            if (start < 0 || end < 0 || start >= end || start > cabuSize || end > cabuSize + 1) {
                throw new NZException(RCode.ASSET_POSITION_INVALID);
            }
        } else {
            return false;
        }

        // 校验 开始和结束位置占用的机柜是否符合 关联 model 的 usize值
        Integer modelUsize = model.getUsize();
        if (modelUsize != null && (end - start) < modelUsize) {
            throw new NZException(RCode.ASSET_POSITION_MISMATCH);
        }

        Integer assetId = asset.getId();
        Integer cabinetId = asset.getCabinetId();

        // 得到机柜下包含的节点信息
        List<AssetAsset> assets = this.list(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getCabinetId, cabinetId));

        // 获取当前机柜的全部位置 所有位置 从1开始到 uSize
        Cabinet cab = cabinetService.getById(cabinetId);
        Integer[] allPosition = new Integer[cab.getuSize()];
        for (int i = 0; i < allPosition.length; i++) {
            allPosition[i] = i + 1;
        }

        // 获取已经占用的位置
        Set<Integer> occList = new HashSet<>();

        // 同机柜修改的情况  需要将自身节点补充到所属机柜的位置中去  再次重新计算可用位置
        Integer oldCabinetId = null;
        Integer pid = null;
        if (assetId != null) {
            AssetAsset assetEntity = this.getById(assetId);
            oldCabinetId = assetEntity.getCabinetId();
            pid = assetEntity.getPid();
        }

        // 修改标识   pid = -1.代表原来是宿主机
        boolean updateFlag = oldCabinetId != null && oldCabinetId.equals(cabinetId) && pid != null && pid.equals(-1);

        for (AssetAsset nodeInfo : assets) {
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

    private boolean validateFreePosition(Integer[] allPosition, Set<Integer> occList, Integer start, Integer end) {
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
    public Integer saveAsset(AssetAsset asset) {
        // 校验参数
        this.validateAssetInfo(asset);

        // 设置属性值
        asset.setCreateAt(DateUtil.getUTCTimeByConfigTimeZone());
        asset.setCreateBy(ShiroUtils.getUserId());
        // cabinet end 处理
        if (asset.getCabinetEnd() != null && asset.getCabinetStart() != null) {
            asset.setCabinetEnd(asset.getCabinetEnd() + 1);
        }

        if (asset.getPid() != null) {
            // pid 不为空 证明是 虚拟极，继承宿主机 dc cabinet state end 信息
            AssetAsset parent = this.getById(asset.getPid());
            asset.setDcId(parent.getDcId());
            asset.setCabinetId(parent.getCabinetId());
            asset.setCabinetStart(parent.getCabinetStart());
            asset.setCabinetEnd(parent.getCabinetEnd());
        } else {
            asset.setPid(-1);
        }

        // 保存
        this.save(asset);

        // 保存 asset 关联数据
        this.saveAssetRelData(asset);
        return asset.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateAsset(AssetAsset asset) {
        // 校验参数
        this.validateAssetInfo(asset);

        // 设置属性值
        asset.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
        asset.setUpdateBy(ShiroUtils.getUserId());
        // cabinet end 处理
        if (asset.getCabinetEnd() != null && asset.getCabinetStart() != null) {
            asset.setCabinetEnd(asset.getCabinetEnd() + 1);
        }

        // 宿主机
        if (asset.getPid() == null || asset.getPid().equals(-1)) {
            asset.setPid(-1);
            List<AssetAsset> babyAssets = this.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getPid, asset.getId()));
            if (CollectionUtils.isNotEmpty(babyAssets)) {
                for (AssetAsset pojo : babyAssets) {
                    pojo.setDcId(asset.getDcId());
                    pojo.setCabinetId(asset.getCabinetId());
                    pojo.setCabinetStart(asset.getCabinetStart());
                    pojo.setCabinetEnd(asset.getCabinetEnd());
                }
                this.saveOrUpdateBatch(babyAssets);
            }
        } else {
            // 虚拟机
            AssetAsset parent = this.getById(asset.getPid());
            asset.setDcId(parent.getDcId());
            asset.setCabinetId(parent.getCabinetId());
            asset.setCabinetStart(parent.getCabinetStart());
            asset.setCabinetEnd(parent.getCabinetEnd());
        }

        // 保存
        this.updateById(asset);

        // 如果没有指定 account, 则视为删除 asset之前关联的 account 信息
        assetAccountRelService.remove(new LambdaQueryWrapper<AssetAccountRel>().eq(AssetAccountRel::getAssetId, asset.getId()));

        // 如果没有指定 meta, 则视为删除 asset之前关联的 meta 信息
        assetFieldValueService.remove(new LambdaQueryWrapper<AssetFieldValue>().eq(AssetFieldValue::getAssetId, asset.getId()));

        // 保存 asset 关联数据
        this.saveAssetRelData(asset);
        return asset.getId();
    }

    private void saveAssetRelData(AssetAsset asset) {
        // accounts
        List<Integer> accounts = asset.getAccounts();
        if (CollectionUtils.isNotEmpty(accounts)) {
            List<AssetAccountRel> rels = new ArrayList<>(accounts.size());
            AssetAccountRel tmpl;
            for (Integer accountId : accounts) {
                tmpl = new AssetAccountRel();
                tmpl.setAssetId(asset.getId());
                tmpl.setAccountId(accountId);
                rels.add(tmpl);
            }
            assetAccountRelService.saveBatch(rels);
        }

        // fields
        List<AssetFieldMeta> fields = asset.getFields();
        if (CollectionUtils.isNotEmpty(fields)) {
            // 需要保存的 meta id
            List<AssetFieldValue> values = new ArrayList<>(fields.size());
            AssetFieldValue temp;
            for (AssetFieldMeta fieldMeta : fields) {
                temp = new AssetFieldValue();
                temp.setAssetId(asset.getId());
                temp.setMetaId(fieldMeta.getId());
                temp.setValue(fieldMeta.getValue());
                values.add(temp);
            }
            assetFieldValueService.saveBatch(values);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAsset(Integer... ids) {
        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        // 宿主机 虚拟机
        List<AssetAsset> delAssets = this.list(new LambdaQueryWrapper<AssetAsset>().in(AssetAsset::getId, idList));
        List<AssetAsset> delInnerAssets = this.list(new LambdaQueryWrapper<AssetAsset>().in(AssetAsset::getPid, idList));

        Set<String> delAssetHosts = delAssets.stream().map(AssetAsset::getManageIp).collect(Collectors.toSet());
        Set<String> delInnerAssetHosts = delInnerAssets.stream().map(AssetAsset::getManageIp).collect(Collectors.toSet());
        delAssetHosts.addAll(delInnerAssetHosts);

        List<Integer> delInnerAssetIds = delInnerAssets.stream().map(AssetAsset::getId).collect(Collectors.toList());
        idList.addAll(delInnerAssetIds);

        // 删除 asset
        this.removeByIds(idList);
        // 如果删除的 asset 存在 宿主机 则删除关联的虚拟机
        this.remove(new LambdaQueryWrapper<AssetAsset>().in(AssetAsset::getPid, idList));

        // 删除 endponiet
        List<MonitorEndpoint> endpoints = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().in(MonitorEndpoint::getAssetId, idList));
        if (CollectionUtils.isNotEmpty(endpoints)) {
            List<Integer> endpointIds = endpoints.stream().map(MonitorEndpoint::getId).collect(Collectors.toList());
            monitorEndpointService.deleteEndpointByIds(endpointIds);
        }

        // 删除 alert
        alertMessageService.removeByAssetIds(idList);

        // 删除 account
        assetAccountRelService.remove(new LambdaQueryWrapper<AssetAccountRel>().in(AssetAccountRel::getAssetId, idList));

        // 删除 assetPing
        if (CollectionUtils.isNotEmpty(delAssetHosts)) {
            assetPingService.remove(new LambdaQueryWrapper<AssetPing>().in(AssetPing::getHost, delAssetHosts));
        }

        // 删除 field
        assetFieldValueService.remove(new LambdaQueryWrapper<AssetFieldValue>().in(AssetFieldValue::getAssetId, idList));
    }

    @Override
    public Map alertStatByAsset(Integer assetId) {
        List<AlertMessageEntity> alerts = alertMessageService.list(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getAssetId, assetId));
        long p1 = alerts.stream().filter(alert -> Constant.ALERT_SEVERITY_HIGH.equals(alert.getSeverity())).count();
        long p2 = alerts.stream().filter(alert -> Constant.ALERT_SEVERITY_MEDIUM.equals(alert.getSeverity())).count();
        long p3 = alerts.stream().filter(alert -> Constant.ALERT_SEVERITY_LOW.equals(alert.getSeverity())).count();

        Map resultMap = new HashMap(8);
        resultMap.put("total", alerts.size());
        resultMap.put(Constant.ALERT_SEVERITY_HIGH, p1);
        resultMap.put(Constant.ALERT_SEVERITY_MEDIUM, p2);
        resultMap.put(Constant.ALERT_SEVERITY_LOW, p3);
        return resultMap;
    }

    @Override
    public Map endpointStatByAsset(Integer assetId) {
        List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getAssetId, assetId));

        List<Integer> suspendedEndpointIdList = monitorEndpointService.getSuspendedEndpointIdList(assetId);
        Set<Integer> upStatusEndpointIds = overviewService.getUpStatusEndpointIds(suspendedEndpointIdList);

        int all = endpointList.size();
        Map resultMap = new HashMap(8);
        resultMap.put("total", all);

        List<Integer> endpointIdList = endpointList.stream().map(MonitorEndpoint::getId).collect(Collectors.toList());
        endpointIdList.retainAll(upStatusEndpointIds);
        resultMap.put("up", endpointIdList.size());
        resultMap.put("down", all - endpointIdList.size() - suspendedEndpointIdList.size());
        resultMap.put("suspended", suspendedEndpointIdList.size());
        return resultMap;
    }

    @Override
    public Map featureStatByAsset(Integer assetId) {
        Map feature = new LinkedHashMap();
        Map assetFeatureHandle = new HashMap();
        List<AssetFeature> assetFeatures = assetFeatureService.list(new QueryWrapper<AssetFeature>().lambda().eq(AssetFeature::getAssetId, assetId));
        for (AssetFeature assetFeature : assetFeatures) {
            assetFeatureHandle.put(assetFeature.getItem(), assetFeature.getValue());
        }
        feature.put("hostname", assetFeatureHandle.get("hostname"));
        feature.put("cpu", JSON.parse((String) (assetFeatureHandle.get("cpu")), Feature.OrderedField));
        feature.put("memory", assetFeatureHandle.get("memory"));
        feature.put("disk", JSON.parse((String) (assetFeatureHandle.get("disk")), Feature.OrderedField));
        feature.put("interface", JSON.parse((String) (assetFeatureHandle.get("interface")), Feature.OrderedField));
        feature.put("core", assetFeatureHandle.get("core"));
        feature.put("os", assetFeatureHandle.get("os"));
        feature.put("uptime", assetFeatureHandle.get("uptime"));
        feature.put("sysName", assetFeatureHandle.get("sysName"));
        feature.put("sysLocation", assetFeatureHandle.get("sysLocation"));
        feature.put("sysContact", assetFeatureHandle.get("sysContact"));
        feature.put("sysServices", assetFeatureHandle.get("sysServices"));
        feature.put("sysUpTime", assetFeatureHandle.get("sysUpTime"));
        feature.put("ifNumber", assetFeatureHandle.get("ifNumber"));
        feature.put("ifEntry", JSON.parse((String) (assetFeatureHandle.get("ifEntry")), Feature.OrderedField));
        Iterator<Map.Entry<String, String>> it = feature.entrySet().iterator();
        // 清空值为 null 的键值对信息
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (entry.getValue() == null) {
                // 使用迭代器的remove()方法删除元素
                it.remove();
            }
        }
        return feature;
    }

    @Override
    public Map queryParams() {
        Map resultMap = new LinkedHashMap();
        // dc
        List<AssetAsset> assetList = this.list();

        resultMap.put("dc", this.getDcStatByAsset(assetList));
        // type
        resultMap.put("type", this.getTypeStatByAsset(assetList));
        // state
        resultMap.put("state", this.getStateStatByAsset(assetList));
        // brand
        resultMap.put("brand", this.getBrandStatByAsset(assetList));
        // field
        resultMap.put("field", this.getFieldStatByAsset());
        // ping
        resultMap.put("ping", this.getPingStat());
        return resultMap;
    }

    @Override
    public List<AssetFieldMeta> queryAssetFieldValue(Integer assetId, Integer metaId) {
        List<AssetFieldMeta> metaList = assetFieldMetaDao.selectMateInfoListByAssetId(assetId);
        metaList = metaList.stream().filter(assetFieldMeta -> assetFieldMeta.getId().equals(metaId)).collect(Collectors.toList());
        for (AssetFieldMeta meta : metaList) {
            meta.setKey(meta.getMetaKey());
        }
        return metaList;
    }

    @Override
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_ASSET_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        if (map.get(language) != null) {
            exportHeaderMap = (Map) map.get(language);
        } else {
            throw new NZException(RCode.ASSET_EXPORT_HEADER_LANGUAGE_ERROR);
        }
        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "Assets", true);
            String fileName = "AssetExportTemplate.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            logger.error("asset模板生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            logger.error("获取asset导入模板失败，表头语言是：" + language + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectData(Integer assetId) {

    }


    private List<Map> getFieldStatByAsset() {
        List<AssetFieldMeta> allMetas = assetFieldMetaDao.selectList(new LambdaQueryWrapper<AssetFieldMeta>());
        Map<Integer, AssetFieldMeta> metaIdAndEntityMap = allMetas.stream().collect(Collectors.toMap(AssetFieldMeta::getId, Function.identity()));

        List<AssetFieldMeta> metaInfoList = assetFieldMetaDao.selectMateInfoListByAssetId(null);
        Map<Integer, List<AssetFieldMeta>> metaIdAndListMap = metaInfoList.stream().collect(Collectors.groupingBy(AssetFieldMeta::getId));

        List<Map> fieldStat = new ArrayList<>(metaInfoList.size());
        Map metaStatMap, childrenMap;
        for (Map.Entry<Integer, List<AssetFieldMeta>> entry : metaIdAndListMap.entrySet()) {
            AssetFieldMeta fieldMeta = metaIdAndEntityMap.get(entry.getKey());
            List<AssetFieldMeta> values = entry.getValue();
            Map<String, List<AssetFieldMeta>> statByValue = values.stream().collect(Collectors.groupingBy(AssetFieldMeta::getValue));
            List children = new ArrayList();
            for (Map.Entry<String, List<AssetFieldMeta>> valueEntry : statByValue.entrySet()) {
                childrenMap = new HashMap(4);
                childrenMap.put("value", valueEntry.getKey());
                childrenMap.put("total", CollectionUtils.isEmpty(valueEntry.getValue()) ? 0 : valueEntry.getValue().size());
                children.add(childrenMap);
            }

            metaStatMap = new HashMap(8);
            metaStatMap.put("id", fieldMeta.getId());
            metaStatMap.put("name", fieldMeta.getName());
            metaStatMap.put("key", fieldMeta.getMetaKey());
            metaStatMap.put("children", children);
            fieldStat.add(metaStatMap);
        }
        return fieldStat;
    }

    private List<Map> getBrandStatByAsset(List<AssetAsset> assetList) {
        List<AssetBrand> brandList = brandService.list();
        List<AssetModel> modelList = modelService.list();

        Map<Integer, List<AssetAsset>> brandIdAndAssetListMap = assetList.stream().collect(Collectors.groupingBy(AssetAsset::getBrandId));
        Map<Integer, List<AssetAsset>> modelIdAndAssetListMap = assetList.stream().collect(Collectors.groupingBy(AssetAsset::getModelId));

        List<Map> brandStatList = new ArrayList<>();
        for (AssetBrand brand : brandList) {
            List<AssetAsset> brandAssetList = brandIdAndAssetListMap.get(brand.getId());
            if (CollectionUtils.isEmpty(brandAssetList)) {
                continue;
            }
            List<Map> brandBabyModel = new ArrayList();
            for (AssetModel model : modelList) {
                List<AssetAsset> modelAssetList = modelIdAndAssetListMap.get(model.getId());
                if (model.getBrandId().equals(brand.getId()) && CollectionUtils.isNotEmpty(modelAssetList)) {
                    Map modelStat = new HashMap(4);
                    modelStat.put("id", model.getId());
                    modelStat.put("name", model.getName());
                    modelStat.put("total", modelAssetList.size());
                    brandBabyModel.add(modelStat);
                }
            }
            Map statByBrand = new HashMap(8);
            statByBrand.put("id", brand.getId());
            statByBrand.put("name", brand.getName());
            statByBrand.put("total", brandAssetList.size());
            statByBrand.put("children", brandBabyModel);
            brandStatList.add(statByBrand);
        }
        return brandStatList;
    }

    private List<Map> getPingStat() {
        List<AssetPing> pingList = assetPingService.list();
        long pingDownCount = pingList.stream().filter(assetPing -> assetPing.getStatus().equals(0)).count();
        long pingUpCount = pingList.stream().filter(assetPing -> assetPing.getStatus().equals(1)).count();
        List<Map> pingStatList = new ArrayList<>(2);
        Map pingStat = new HashMap();
        pingStat.put("status", 0);
        pingStat.put("name", "down");
        pingStat.put("total", pingDownCount);
        pingStatList.add(pingStat);

        pingStat = new HashMap();
        pingStat.put("status", 1);
        pingStat.put("name", "up");
        pingStat.put("total", pingUpCount);
        pingStatList.add(pingStat);
        return pingStatList;
    }

    private List<Map> getStateStatByAsset(List<AssetAsset> assetList) {
        List<AssetStateConf> stateConfList = stateConfService.list();
        Map<Integer, List<AssetAsset>> stateIdAndAssetListMap = assetList.stream().collect(Collectors.groupingBy(AssetAsset::getStateId));
        List<Map> stateStats = new ArrayList<>();
        for (AssetStateConf stateConf : stateConfList) {
            List<AssetAsset> stateAssetList = stateIdAndAssetListMap.get(stateConf.getId());
            if (CollectionUtils.isNotEmpty(stateAssetList)) {
                Map stateStat = new HashMap(4);
                stateStat.put("id", stateConf.getId());
                stateStat.put("name", stateConf.getName());
                stateStat.put("total", stateAssetList.size());
                stateStats.add(stateStat);
            }
        }
        return stateStats;
    }

    private List<Map> getTypeStatByAsset(List<AssetAsset> assetList) {
        List<AssetTypeConf> typeConfList = typeConfService.list();
        Map<Integer, List<AssetAsset>> typeIdAndAssetListMap = assetList.stream().collect(Collectors.groupingBy(AssetAsset::getTypeId));
        List<Map> typeStats = new ArrayList<>();
        for (AssetTypeConf typeConf : typeConfList) {
            List<AssetAsset> typeAssetList = typeIdAndAssetListMap.get(typeConf.getId());
            if (CollectionUtils.isNotEmpty(typeAssetList)) {
                Map typeStat = new HashMap(4);
                typeStat.put("id", typeConf.getId());
                typeStat.put("name", typeConf.getName());
                typeStat.put("total", typeAssetList.size());
                typeStats.add(typeStat);
            }
        }
        return typeStats;
    }

    private List<Map> getDcStatByAsset(List<AssetAsset> assetList) {
        List<Dc> dcList = dcService.list();
        List<Cabinet> cabinetList = cabinetService.list();

        Map<Integer, List<AssetAsset>> dcIdAndAssetListMap = assetList.stream().collect(Collectors.groupingBy(AssetAsset::getDcId));
        Map<Integer, List<AssetAsset>> cabIdAndAssetListMap = assetList.stream().filter(assetAsset -> ToolUtil.isNotEmpty(assetAsset.getCabinetId())).collect(Collectors.groupingBy(AssetAsset::getCabinetId));

        List<Map> dcStats = new ArrayList<>();
        for (Dc dc : dcList) {
            List<AssetAsset> dcAssetList = dcIdAndAssetListMap.get(dc.getId());
            if (CollectionUtils.isEmpty(dcAssetList)) {
                continue;
            }
            List<Map> dcBabyCab = new ArrayList();
            for (Cabinet cabinet : cabinetList) {
                List<AssetAsset> cabAssetList = cabIdAndAssetListMap.get(cabinet.getId());
                if (cabinet.getDcId().equals(dc.getId()) && CollectionUtils.isNotEmpty(cabAssetList)) {
                    Map cabStat = new HashMap(4);
                    cabStat.put("id", cabinet.getId());
                    cabStat.put("name", cabinet.getName());
                    cabStat.put("total", cabAssetList.size());
                    dcBabyCab.add(cabStat);
                }
            }
            Map statByDc = new HashMap(8);
            statByDc.put("id", dc.getId());
            statByDc.put("name", dc.getName());
            statByDc.put("total", dcAssetList.size());
            statByDc.put("children", dcBabyCab);
            dcStats.add(statByDc);
        }
        return dcStats;
    }


    @Override
    public void export(HttpServletResponse response, Map<String, Object> params) {
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_ASSET_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);

        PageUtils page = this.queryAssetPage(params);
        List<AssetAsset> list = (List<AssetAsset>) page.getList();

        SXSSFWorkbook workbook = null;
        try {
            // 添加标题行 默认语言 en
            String language = (String) params.get("language");
            language = ToolUtil.isEmpty(language) ? "en" : language;
            Map<String, String> exportHeader = (Map) map.get(language);

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
            logger.error("asset导出文件生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            logger.error("asset导出失败，参数是：" + params.toString() + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }

    @Override
    public Map importAssetsFromExcel(MultipartFile excelFile, String language) {
        Workbook work = ExcelUtils.getWorkBookFromMultipartFile(excelFile);

        Sheet sheet = work.getSheetAt(0);

        // 表头校验
        SysConfigEntity config = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_ASSET_EXPORT_HEADER));
        HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
        boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), map.values());
        if (!headerFlag) {
            throw new NZException(RCode.ASSET_EXPORT_HEADER_TEMPLATE_ERROR);
        }
        Map<String, String> exportHeader = (Map) map.get(language);
        List<String> headerInfos = new ArrayList(exportHeader.keySet());

        // 预先查询出校验数据 避免循环查表
        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = this.saveAssetsDataFromSheet(sheet, failDetails, headerInfos, seqUUID);
        int totalNum = sheet.getLastRowNum() - sheet.getFirstRowNum();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    @Override
    public void cancelImport(String seq) {
        List<AssetAsset> assets = this.list(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getSeq, seq));
        List<Integer> assetIds = assets.stream().map(AssetAsset::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(assetIds)) {
            // 删除残留 assetPing 数据
            Set<String> delAssetHosts = assets.stream().map(AssetAsset::getManageIp).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(delAssetHosts)) {
                assetPingService.remove(new QueryWrapper<AssetPing>().lambda().in(AssetPing::getHost, delAssetHosts));
            }
            // asset
            this.remove(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getSeq, seq));

            // dc
            dcService.remove(new QueryWrapper<Dc>().lambda().eq(Dc::getSeq, seq));

            // cabinet
            cabinetService.remove(new QueryWrapper<Cabinet>().lambda().eq(Cabinet::getSeq, seq));

            // brand
            brandService.remove(new LambdaQueryWrapper<AssetBrand>().eq(AssetBrand::getSeq, seq));
            // model
            modelService.remove(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getSeq, seq));
        }
    }

    @Override
    public List<AssetAsset> getBlackboxAssetInfoList() {
        List<AssetAsset> assetList = this.list();

        // 支持 ping 的 state_conf
        List<AssetStateConf> stateConfList = stateConfService.list(new LambdaQueryWrapper<AssetStateConf>().eq(AssetStateConf::getPing,1));
        List<Integer> enablePingStateConfIdList = stateConfList.stream().map(AssetStateConf::getId).collect(Collectors.toList());

        // 支持 ping 的 asset
        assetList = assetList.stream().filter(asset -> enablePingStateConfIdList.contains(asset.getStateId())).collect(Collectors.toList());

        List<Dc> dcList = dcService.list();
        Map<Integer, Dc> dcIdAndNameMap = dcList.stream().collect(Collectors.toMap(Dc::getId, Function.identity()));

        for (AssetAsset entity : assetList) {
            entity.setDc(dcIdAndNameMap.get(entity.getDcId()));
        }
        return assetList;
    }

    private int saveAssetsDataFromSheet(Sheet sheet, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
        List<AssetAsset> assetList = this.list();
        Map<String, AssetAsset> assetNameAndEntityMap = assetList.stream().collect(Collectors.toMap(AssetAsset::getName, Function.identity()));
        List<String> assetSnList = assetList.stream().map(AssetAsset::getSn).collect(Collectors.toList());
        List<String> assetIpList = assetList.stream().map(AssetAsset::getManageIp).collect(Collectors.toList());

        List<Dc> dcList = dcService.list();
        Map<String, Dc> dcNameAndEntityMap = dcList.stream().collect(Collectors.toMap(Dc::getName, Function.identity()));

        List<Cabinet> cabinetList = cabinetDao.getCabinetWithAssetPositionInfo();

        List<AssetBrand> brandList = brandService.list();
        Map<String, AssetBrand> brandNameAndEntityMap = brandList.stream().collect(Collectors.toMap(AssetBrand::getName, Function.identity()));

        List<AssetModel> modelList = modelService.list();
        Map<Integer, List<AssetModel>> brandIdAndModelListMap = modelList.stream().collect(Collectors.groupingBy(AssetModel::getBrandId));
        Map<Integer, Integer> modelIdAndUsizeMap = modelList.stream().collect(Collectors.toMap(AssetModel::getId, AssetModel::getUsize));

        List<AssetStateConf> stateConfList = stateConfService.list();
        Map<String, AssetStateConf> stateNameAndEntityMap = stateConfList.stream().collect(Collectors.toMap(AssetStateConf::getName, Function.identity()));

        List<AssetTypeConf> typeConfList = typeConfService.list();
        Map<String, AssetTypeConf> typeNameAndEntityMap = typeConfList.stream().collect(Collectors.toMap(AssetTypeConf::getName, Function.identity()));
        Map<Integer, AssetTypeConf> typeIdAndEntityMap = typeConfList.stream().collect(Collectors.toMap(AssetTypeConf::getId, Function.identity()));

        SysConfigEntity defaultCabinetUsizeConfig = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_DEFAULT_CABINETU));
        Integer defaultCabinetUsize = Integer.parseInt(defaultCabinetUsizeConfig.getParamValue());

        Pattern sshPortPattern = Pattern.compile("ssh/\\d+");
        Pattern telnetPortPattern = Pattern.compile("telnet/\\d+");

        int successNum = 0;
        // 处理 excel 数据，将含宿主机资产排到前面
        List<Row> rows = this.handleExcelData(sheet);
        for (Row row : rows) {
            int dataFromExcelRowNum = row.getRowNum() + 1;
            Map saveEntitys = new HashMap();
            boolean saveFlag = true;
            // name
            String name = ExcelUtils.getCellValue(row.getCell(0));
            if (StringUtils.isEmpty(name)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.ASSET_NAME_ISNULL.getMsg());
            } else {
                // NAME 重复校验
                if (assetNameAndEntityMap.get(name) != null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.ASSET_NAME_DUPLICATE.getMsg());
                }
            }

            // sn
            String sn = ExcelUtils.getCellValue(row.getCell(1));
            if (StringUtils.isEmpty(sn)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.ASSET_SN_ISNULL.getMsg());
            } else {
                // sn 重复校验
                if (assetSnList.contains(sn)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.ASSET_SN_DUPLICATE.getMsg());
                }
            }

            // ip
            String host = ExcelUtils.getCellValue(row.getCell(3));
            if (StringUtils.isEmpty(host)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ASSET_MANAGEIP_ISNULL.getMsg());
            } else {
                // ip 唯一校验
                if (assetIpList.contains(host)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ASSET_MANAGEIP_DUPLICATE.getMsg());
                } else {
                    if (!host.matches(Constant.REGEX_IP)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ASSET_MANAGEIP_FORMAT.getMsg());
                    }
                }
            }

            // brand  model
            Integer brandId = null, modelId = null;
            String brandName = ExcelUtils.getCellValue(row.getCell(4));
            if (StringUtils.isEmpty(brandName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ASSET_BRAND_NAME_ISNULL.getMsg());
            } else {
                brandId = this.validateBrand(brandName, brandNameAndEntityMap, seqUUID, saveEntitys);
            }
            String modelName = ExcelUtils.getCellValue(row.getCell(5));
            if (StringUtils.isEmpty(modelName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.ASSET_MODELNAME_ISNULL.getMsg());
            } else {
                modelId = this.validateModel(brandId, modelName, brandIdAndModelListMap, seqUUID, saveEntitys);
            }

            // state
            String stateName = ExcelUtils.getCellValue(row.getCell(10));
            AssetStateConf stateConf = null;
            if (StringUtils.isEmpty(stateName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(10), RCode.STATECONF_NAME_ISNULL.getMsg());
            } else {
                // state 未找到
                stateConf = stateNameAndEntityMap.get(stateName);
                if (stateConf == null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(10), RCode.STATECONF_NOT_FOUND.getMsg());
                }
            }

            // type
            AssetTypeConf typeConf = null;
            String typeName = ExcelUtils.getCellValue(row.getCell(11));
            if (StringUtils.isEmpty(typeName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(11), RCode.TYPECONF_NAME_ISNULL.getMsg());
            } else {
                // type 未找到
                typeConf = typeNameAndEntityMap.get(typeName);
                if (typeConf == null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(11), RCode.TYPECONF_NOT_FOUND.getMsg());
                }
            }

            

            // 虚拟机宿主
            String parentName = ExcelUtils.getCellValue(row.getCell(2));
            Integer start = null, end = null, cabinetId = null, dcId = null;
            AssetAsset parentAsset = null;
            if (StringUtils.isNotEmpty(parentName)) {// 虚拟机 parentname 不为空
                parentAsset = assetNameAndEntityMap.get(parentName);
                if (parentAsset == null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.ASSET_PARENT_NOT_EXIST.getMsg());
                } else {
                    // 校验是否可以挂载虚拟机 选择宿主机是虚拟机或者不支持挂载
                    AssetTypeConf parentAssetTypeConf = typeIdAndEntityMap.get(parentAsset.getTypeId());
                    if (parentAssetTypeConf.getVm().equals(1) || parentAssetTypeConf.getVmh().equals(0)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.ASSET_NOT_SUPPORT_MOUNTING.getMsg());
                    }
                }
                // 校验当前 asset 是否可以选择宿主机
                if (typeConf != null && typeConf.getVm().equals(0)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.ASSET_NOT_SUPPORT_MOUNTING.getMsg());
                }
            } else {
                String dcName = ExcelUtils.getCellValue(row.getCell(6));
                if (StringUtils.isEmpty(dcName)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.DC_NAME_ISNULL.getMsg());
                } else {
                    dcId = this.validateDc(dcName, dcNameAndEntityMap, seqUUID, saveEntitys);
                }

                // 虚拟机 vm info 校验 / 虚拟机必须选择宿主
                if (typeConf.getVm().equals(1) && StringUtils.isEmpty(parentName)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.ASSET_NOT_SELECT_PARENT.getMsg());
                }
                // parent Name 为 null , 当前是 宿主机 ， 并且前面没有发生错误，校验 cabinet start end 相关数据
                if (saveFlag) {
                    String cabinetName = ExcelUtils.getCellValue(row.getCell(7));
                    String cabinetStart = ExcelUtils.getCellValue(row.getCell(8));
                    String cabinetEnd = ExcelUtils.getCellValue(row.getCell(9));
                    if (StringUtils.isNotEmpty(cabinetStart) && StringUtils.isNotEmpty(cabinetEnd)) {
                        start = this.getIntValFromStr(cabinetStart);
                        if (start == null) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ASSET_CABINETSTART_INVALIDE.getMsg());
                        }
                        end = this.getIntValFromStr(cabinetEnd);
                        if (end == null) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(9), RCode.ASSET_CABINETEND_INVALIDE.getMsg());
                        }
                    }
                    Integer modelUsize = modelIdAndUsizeMap.get(modelId);
                    modelUsize = ToolUtil.isEmpty(modelUsize) ? 1 : modelUsize;
                    boolean posttionFlag = start != null && end != null;
                    if (dcId != null) { // dc已存在，不需要添加
                        Cabinet cabinet = this.validateCabinet(cabinetName, dcId, cabinetList, seqUUID, saveEntitys, defaultCabinetUsize);
                        cabinetId = cabinet.getId();
                        boolean cabinetExitFlag = cabinet.getId() != null;
                        if (posttionFlag) {
                            end = end + 1;
                            Integer cabinetUsize = cabinetExitFlag ? cabinet.getuSize() : defaultCabinetUsize;
                            if (start < 0 || end < 0 || start >= end || start > cabinetUsize || end > cabinetUsize + 1) { // 基本信息校验
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ASSET_POSITION_INVALID.getMsg());
                            } else if ((end - start) < modelUsize) { // 关联 model usize 校验
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ASSET_POSITION_MISMATCH.getMsg());
                            } else { // 校验通过 ， 开始校验占用位置
                                if (cabinetExitFlag) {// 机柜存在开始校验位置
                                    boolean flag = this.validateUSizeForImport(start, end, cabinet);
                                    if (!flag) {
                                        saveFlag = false;
                                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ASSET_POSITION_OCCUPY.getMsg());
                                    }
                                }
                            }
                        }
                    } else { // dc不存在
                        this.saveEntityAddCabEntity(saveEntitys, cabinetName, defaultCabinetUsize, seqUUID);
                        if (posttionFlag) {
                            end = end + 1;
                            if (start < 0 || end < 0 || start >= end || start > defaultCabinetUsize || end > defaultCabinetUsize + 1) { // 基本信息校验
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ASSET_POSITION_INVALID.getMsg());
                            } else if ((end - start) < modelUsize) { // 关联 model usize 校验
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ASSET_POSITION_MISMATCH.getMsg());
                            }
                        }
                    }
                }
            }

            String purchaseDate = ExcelUtils.getCellValue(row.getCell(12));
            if (StringUtils.isNotEmpty(purchaseDate)) {
                if (!DateUtil.isValidDate(purchaseDate, "yyyy-MM-dd")) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(12), RCode.ASSET_PURCHASEDATE_FORMAT.getMsg());
                }
            }

            String fieldsStr = ExcelUtils.getCellValue(row.getCell(13));
            if (StringUtils.isNotEmpty(fieldsStr)) {
                saveEntitys.put("fields", this.getFieldsByFieldsStr(fieldsStr, seqUUID));
            }
            
            
            String authProtocolPort = ExcelUtils.getCellValue(row.getCell(14));
            String authType = ExcelUtils.getCellValue(row.getCell(15));
            String authUsername = ExcelUtils.getCellValue(row.getCell(16));
            String authPin = ExcelUtils.getCellValue(row.getCell(17));
            String authPriKey = ExcelUtils.getCellValue(row.getCell(18));
            String authUserTip = ExcelUtils.getCellValue(row.getCell(19));
            String authPinTip = ExcelUtils.getCellValue(row.getCell(20));
            String snmpCredentialId = ExcelUtils.getCellValue(row.getCell(21));
            if(ObjectUtil.equals(authType, "1")) {
            	if(ObjectUtil.isEmpty(authUsername)) {
            		saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(16), RCode.ASSET_AUTHUSERNAME_ISNULL.getMsg());
            	}else if(ObjectUtil.isEmpty(authPin)) {
            		saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(17), RCode.ASSET_AUTHPIN_ISNULL.getMsg());
            	}
            }else if(ObjectUtil.equals(authType, "2")) {
            	if(ObjectUtil.isEmpty(authPriKey)) {
            		saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(18), RCode.ASSET_AUTHPRIKEY_ISNULL.getMsg());
            	}
            }
            
            
            // 前面没有报错
            if (saveFlag) {
                AssetAsset asset = new AssetAsset();
                asset.setName(name);
                asset.setSn(sn);
                asset.setPid(parentAsset == null ? -1 : parentAsset.getId());
                asset.setTypeId(typeConf.getId());
                asset.setStateId(stateConf.getId());
                asset.setPurchaseDate(StringUtils.isEmpty(purchaseDate) ? null : purchaseDate);
                asset.setCreateAt(DateUtil.getUTCTimeByConfigTimeZone());
                asset.setCreateBy(ShiroUtils.getUserId());
                asset.setManageIp(host);
                asset.setSeq(seqUUID);
                asset.setDcId(dcId);
                asset.setBrandId(brandId);
                asset.setModelId(modelId);
                asset.setCabinetId(cabinetId);
                asset.setCabinetStart(start);
                asset.setCabinetEnd(end);
                asset.setAuthProtocolPort(Integer.valueOf(authProtocolPort));
                asset.setAuthType(Integer.valueOf(authType));
                asset.setAuthUsername(authUsername);
                asset.setAuthPin(authPinTip);
                asset.setAuthPriKey(authPriKey);
                asset.setAuthUserTip(authUserTip);
                asset.setAuthPinTip(authPinTip);
                asset.setSnmpCredentialId(Integer.valueOf(snmpCredentialId));
                if (parentAsset == null) {
                    asset.setPid(-1);
                } else {
                    asset.setPid(parentAsset.getId());
                    asset.setDcId(parentAsset.getDcId());
                    asset.setCabinetId(parentAsset.getCabinetId());
                    asset.setCabinetStart(parentAsset.getCabinetStart());
                    asset.setCabinetEnd(parentAsset.getCabinetEnd());
                }
                saveEntitys.put("asset", asset);
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                try {
                    // 调用入库方法
                    Map updateMap = this.saveRowData(saveEntitys);
                    dataSourceTransactionManager.commit(transactionStatus);
                    this.updateImportAssetRelatedInfo(updateMap, assetNameAndEntityMap, assetSnList, assetIpList, dcNameAndEntityMap, brandNameAndEntityMap, brandIdAndModelListMap, cabinetList);
                    successNum++;
                } catch (TransactionException e) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", e.getMessage());
                    logger.error("回滚事务失败,错误信息:" + e);
                }
            }
        }
        return successNum;
    }

    private List<AssetFieldValue> getFieldsByFieldsStr(String fieldsStr, String seqUUID) {
        List<AssetFieldValue> valueList = new ArrayList<>();
        List<String> list = Arrays.asList(fieldsStr.split("\n"));
        for (String str : list) {
            if (str.contains("=")) {
                String[] split = str.split("=");
                String metaKey = split[0];
                AssetFieldMeta meta = assetFieldMetaService.getOne(new LambdaQueryWrapper<AssetFieldMeta>().eq(AssetFieldMeta::getMetaKey, metaKey));
                if (meta == null) {
                    meta = new AssetFieldMeta();
                    meta.setMetaKey(metaKey);
                    meta.setName(metaKey);
                    meta.setGroupId("1");
                    meta.setParam("{}");
                    assetFieldMetaService.save(meta);
                }
                AssetFieldValue value = new AssetFieldValue();
                value.setMetaId(meta.getId());
                value.setValue(split[1]);
                value.setSeq(seqUUID);
                valueList.add(value);
            }
        }
        return valueList;
    }

    private void saveEntityAddCabEntity(Map saveEntitys, String cabinetName, Integer defaultCabinetUsize, String seqUUID) {
        Cabinet cabinet = new Cabinet();
        cabinet.setName(cabinetName);
        cabinet.setuSize(defaultCabinetUsize);
        cabinet.setSeq(seqUUID);
        saveEntitys.put("cabinet", cabinet);
    }


    private Cabinet validateCabinet(String cabinetName, Integer dcId, List<Cabinet> cabinetList, String seqUUID, Map saveEntitys, Integer defaultCabinetUsize) {
        Cabinet cabinet = new Cabinet();
        boolean flag = false;
        for (Cabinet entity : cabinetList) {
            // 如果该机房下有该机柜
            if (StrUtil.equals(cabinetName, entity.getName()) && dcId.equals(entity.getDcId())) {
                cabinet = entity;
                flag = true;
                break;
            }
        }
        // 找不到机柜
        if (!flag) {
            cabinet.setName(cabinetName);
            cabinet.setDcId(dcId);
            cabinet.setuSize(defaultCabinetUsize);
            cabinet.setSeq(seqUUID);
            saveEntitys.put("cabinet", cabinet);
        }
        return cabinet;
    }

    private Integer getIntValFromStr(String cabinetStart) {
        try {
            return Integer.parseInt(cabinetStart);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateImportAssetRelatedInfo(Map updateMap, Map<String, AssetAsset> assetNameAndEntityMap, List<String> assetSnList, List<String> assetIpList, Map<String, Dc> dcNameAndEntityMap, Map<String, AssetBrand> brandNameAndEntityMap, Map<Integer, List<AssetModel>> brandIdAndModelListMap, List<Cabinet> cabinetList) {
        Dc updateIdc = (Dc) updateMap.get("dc");
        if (updateIdc != null) {
            dcNameAndEntityMap.put(updateIdc.getName(), updateIdc);
        }

        AssetBrand updateBrand = (AssetBrand) updateMap.get("brand");
        if (updateBrand != null) {
            brandNameAndEntityMap.put(updateBrand.getName(), updateBrand);
        }

        AssetModel updateModel = (AssetModel) updateMap.get("model");
        if (updateModel != null) {
            Integer brandId = updateModel.getBrandId();
            List<AssetModel> modelList = brandIdAndModelListMap.get(brandId);
            modelList = CollectionUtils.isEmpty(modelList) ? new ArrayList<>() : modelList;
            modelList.add(updateModel);
            brandIdAndModelListMap.put(brandId, modelList);
        }

        Cabinet updateCabinet = (Cabinet) updateMap.get("cabinet");
        if (updateCabinet != null) {
            cabinetList.add(updateCabinet);
        }
        AssetAsset updateAsset = (AssetAsset) updateMap.get("asset");
        if (updateAsset != null) {
            assetSnList.add(updateAsset.getSn());
            assetIpList.add(updateAsset.getManageIp());
            assetNameAndEntityMap.put(updateAsset.getName(), updateAsset);
            if (updateAsset.getCabinetStart() != null && updateAsset.getCabinetEnd() != null) {
                // 更新cabinetList中对应的关系
                for (Cabinet cabinet : cabinetList) {
                    if (cabinet.getId().equals(updateAsset.getCabinetId())) {
                        if (CollectionUtils.isEmpty(cabinet.getAssets())) {
                            cabinet.setAssets(new ArrayList<>());
                        }
                        cabinet.getAssets().add(updateAsset);
                        break;
                    }
                }
            }
        }
    }

    public Map saveRowData(Map saveEntitys) {
        Map updateMap = new HashMap();
        // DC
        Dc dc = (Dc) saveEntitys.get("dc");
        // Cabinet
        Cabinet cabinet = (Cabinet) saveEntitys.get("cabinet");
        if (dc == null && cabinet != null) {
            cabinetService.save(cabinet);
            updateMap.put("cabinet", cabinet);
        }
        if (dc != null) {
            dcService.save(dc);
            if (cabinet != null) {
                cabinet.setDcId(dc.getId());
                cabinetService.save(cabinet);
                updateMap.put("cabinet", cabinet);
            }
            updateMap.put("dc", dc);
        }

        // brand
        AssetBrand brand = (AssetBrand) saveEntitys.get("brand");
        // model
        AssetModel model = (AssetModel) saveEntitys.get("model");
        if (brand == null && model != null) {
            modelService.save(model);
            updateMap.put("model", model);
        }

        if (brand != null) {
            brandService.save(brand);
            if (model != null) {
                model.setBrandId(brand.getId());
                modelService.save(model);
                updateMap.put("model", model);
            }
            updateMap.put("brand", brand);
        }

        // asset
        AssetAsset asset = (AssetAsset) saveEntitys.get("asset");

        if (asset.getBrandId() == null && brand != null) {
            asset.setBrandId(brand.getId());
        }

        if (asset.getModelId() == null && model != null) {
            asset.setModelId(model.getId());
        }
        if (asset.getDcId() == null && dc != null) {
            asset.setDcId(dc.getId());
        }
        if (asset.getCabinetId() == null && cabinet != null) {
            asset.setCabinetId(cabinet.getId());
        }
        this.save(asset);
        updateMap.put("asset", asset);

        // fields
        List<AssetFieldValue> fields = (List<AssetFieldValue>) saveEntitys.get("fields");
        if (CollectionUtils.isNotEmpty(fields)) {
            fields.forEach(assetFieldValue -> assetFieldValue.setAssetId(asset.getId()));
            assetFieldValueService.saveBatch(fields);
        }
        return updateMap;
    }

    private List<Row> handleExcelData(Sheet sheet) {
        List<Row> rows = new ArrayList<>();
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            rows.add(row);
        }
        List<Row> list = rows.stream().filter(cells -> {
            String parentName = ExcelUtils.getCellValue(cells.getCell(2));
            if (StringUtils.isEmpty(parentName)) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        rows.removeAll(list);
        list.addAll(rows);
        return list;
    }


    private Integer validateDc(String dcName, Map<String, Dc> dcNameAndEntityMap, String seqUUID, Map saveEntitys) {
        Dc dc = dcNameAndEntityMap.get(dcName);
        if (dc == null) {
            dc = new Dc();
            dc.setName(dcName);
            dc.setState(Constant.SWITCH_OPEN.toUpperCase());
            dc.setSeq(seqUUID);
            saveEntitys.put("dc", dc);
        }
        return dc.getId();
    }

    private List<String> getPortListByPattern(String portStr, Pattern portPattern) {
        Matcher matcher = portPattern.matcher(portStr);
        List<String> portInfos = new ArrayList<>();
        while (matcher.find()) {
            portInfos.add(matcher.group());
        }
        return portInfos;
    }


    private Integer validateModel(Integer brandId, String modelName, Map<Integer, List<AssetModel>> brandIdAndModelListMap, String seqUUID, Map saveEntitys) {
        // brand 不存在,model肯定也不在
        if (brandId == null) {
            AssetModel model = new AssetModel();
            model.setName(modelName);
            model.setSeq(seqUUID);
            model.setUsize(1);
            saveEntitys.put("model", model);
            return model.getId();
        } else {
            List<AssetModel> modelList = brandIdAndModelListMap.get(brandId);
            modelList = CollectionUtils.isEmpty(modelList) ? new ArrayList<>() : modelList;
            AssetModel getModelByName = modelList.stream().filter(model -> model.getName().equals(modelName)).findAny().orElse(null);
            if (getModelByName == null) {
                AssetModel model = new AssetModel();
                model.setBrandId(brandId);
                model.setName(modelName);
                model.setSeq(seqUUID);
                model.setUsize(1);
                saveEntitys.put("model", model);
                return model.getId();
            } else {
                return getModelByName.getId();
            }
        }
    }

    private Integer validateBrand(String brandName, Map<String, AssetBrand> brandNameAndEntityMap, String seqUUID, Map saveEntitys) {
        AssetBrand brand = brandNameAndEntityMap.get(brandName);
        if (brand == null) {
            brand = new AssetBrand();
            brand.setName(brandName);
            brand.setSeq(seqUUID);
            saveEntitys.put("brand", brand);
        }
        return brand.getId();
    }

    private void addFailDetail(int i, List<Map> failDetails, String headerInfo, String msg) {
        Map failDetail = new HashMap(4);
        failDetail.put("lineNo", i);
        failDetail.put("errorMsg", headerInfo + ": " + msg);
        failDetails.add(failDetail);
    }

    private void fillData(List<AssetAsset> list, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook) {
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        Row row;
        String fieldStr;
        for (AssetAsset asset : list) {
            row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(asset.getName());
            row.createCell(1).setCellValue(asset.getSn());
            row.createCell(2).setCellValue(ToolUtil.isEmpty(asset.getParent()) ? "" : asset.getParent().getName());
            row.createCell(3).setCellValue(asset.getManageIp());
            row.createCell(4).setCellValue(asset.getBrand().getName());
            row.createCell(5).setCellValue(asset.getModel().getName());
            row.createCell(6).setCellValue(asset.getDc().getName());
            row.createCell(7).setCellValue(ToolUtil.isEmpty(asset.getCabinet()) ? "" : asset.getCabinet().getName());
            row.createCell(8).setCellValue(ToolUtil.isEmpty(asset.getCabinetStart()) ? "" : String.valueOf(asset.getCabinetStart()));
            row.createCell(9).setCellValue(ToolUtil.isEmpty(asset.getCabinetEnd()) ? "" : String.valueOf(asset.getCabinetEnd()));
            row.createCell(10).setCellValue(asset.getState().getName());
            row.createCell(11).setCellValue(asset.getType().getName());

            
            row.createCell(12).setCellValue(asset.getPurchaseDate());
            if (CollectionUtils.isNotEmpty(asset.getFields())) {
                fieldStr = "";
                for (AssetFieldMeta meta : asset.getFields()) {
                    fieldStr += meta.getMetaKey() + "=" + meta.getValue() + "\n";
                }
                row.createCell(13).setCellValue(fieldStr);
            }
            
            row.createCell(14).setCellValue(ToolUtil.isEmpty(asset.getAuthProtocolPort()) ? "" : String.valueOf(asset.getAuthProtocolPort()));
            row.createCell(15).setCellValue(ToolUtil.isEmpty(asset.getAuthType()) ? "" : String.valueOf(asset.getAuthType()));
            row.createCell(16).setCellValue(ToolUtil.isEmpty(asset.getAuthUsername()) ? "" : asset.getAuthUsername());
            row.createCell(17).setCellValue(ToolUtil.isEmpty(asset.getAuthPin()) ? "" : asset.getAuthPin());
            row.createCell(18).setCellValue(ToolUtil.isEmpty(asset.getAuthPriKey()) ? "" : asset.getAuthPriKey());
            row.createCell(19).setCellValue(ToolUtil.isEmpty(asset.getAuthUserTip()) ? "" : asset.getAuthUserTip());
            row.createCell(20).setCellValue(ToolUtil.isEmpty(asset.getAuthPinTip()) ? "" : asset.getAuthPinTip());
            row.createCell(21).setCellValue(ToolUtil.isEmpty(asset.getSnmpCredentialId()) ? "" : String.valueOf(asset.getSnmpCredentialId()));
            
            // 设置单元格样式
            ExcelUtils.setDataCellStyle(dataStyle, row, 21);
        }
    }

	@Override
	public void bulkEdit(Map<String, Object> params) {
		
		ValidateUtils.is(params.get("editType")).notNull(RCode.ASSET_EDITTYPE_ISNULL)
		.and(params.get("ids")).notNull(RCode.ASSET_ID_ISNULL);
		Integer editType = (Integer) params.get("editType");
		List<Integer> ids = (List<Integer>) params.get("ids");
		
		switch(editType) {
			// 1 account 2 label 3 state 4 snmp credentials
			case 1:
				Integer authType = (Integer) params.get("authType");
				String authUsername	= (String) params.get("authUsername");
				String authPin	= (String) params.get("authPin");
				String authPriKey	= (String) params.get("authPriKey");
				if(ObjectUtil.equal(authType, 1)) {
		        	ValidateUtils.is(authUsername).notNull(RCode.ASSET_AUTHUSERNAME_ISNULL)
		        	.and(authPin).notNull(RCode.ASSET_AUTHPIN_ISNULL);
		        }else if(ObjectUtil.equal(authType, 2)){
		        	ValidateUtils.is(authPriKey).notNull(RCode.ASSET_AUTHPRIKEY_ISNULL);
		        }
				this.baseMapper.updateAssetStateBatch(ids, params);
				break;
			case 2:
				List<Map<String,Object>> object = (List<Map<String, Object>>) params.get("fields");
				if(ToolUtil.isEmpty(object)) {
					break;
				}
				// 将标签操作区分 批量删除还是批量新增或者更新
				List<Integer> removeFieldIds = new ArrayList<Integer>();
				List<AssetFieldValue> addOrUpdateFields = new ArrayList<AssetFieldValue>();
				for(Map<String,Object> map : object) {
					Integer action = (Integer) map.get("action");
					if(ToolUtil.equals(action, 1)) {
						removeFieldIds.add((Integer)map.get("id"));
					}else {
						List<AssetFieldValue> assetFieldValues = assetFieldValueService.list(new QueryWrapper<AssetFieldValue>().lambda()
								.eq(AssetFieldValue::getMetaId, map.get("id"))
								.in(AssetFieldValue::getAssetId,ids));
						// 分拣出更新数据和新增数据
						List<Integer> assetIds = assetFieldValues.stream().map(AssetFieldValue::getAssetId).collect(Collectors.toList());
						List<Integer> addIds = ids.stream().filter(item -> !assetIds.contains(item)).collect(Collectors.toList());
						
						for(AssetFieldValue assetFieldValue : assetFieldValues) {
							assetFieldValue.setValue((String)map.get("value"));
							addOrUpdateFields.add(assetFieldValue);
						}
						
						for(Integer id : addIds) {
							AssetFieldValue data  = new AssetFieldValue();
							data.setAssetId(id);
							data.setMetaId((Integer)map.get("id"));
							data.setValue((String)map.get("value"));
							addOrUpdateFields.add(data);
						}
					}
				}
				
				if(ToolUtil.isNotEmpty(removeFieldIds)) {
					assetFieldValueService.remove(new QueryWrapper<AssetFieldValue>()
							.lambda().in(AssetFieldValue::getMetaId, removeFieldIds)
							.in(AssetFieldValue::getAssetId, ids));
				}
				
				if(ToolUtil.isNotEmpty(addOrUpdateFields)) {
					assetFieldValueService.saveOrUpdateBatch(addOrUpdateFields);
				}
				break;
			case 3:
				ValidateUtils.is(params.get("stateId")).notNull(RCode.ASSET_STATE_ID_ISNULL);
				this.baseMapper.updateAssetStateBatch(ids, params);
				break;
			case 4:
				ValidateUtils.is(params.get("snmpCredentialId")).notNull(RCode.SNMP_CREDENTIAL_ID_ISNULL);
				this.baseMapper.updateAssetStateBatch(ids, params);
				break;
			default:
				break;
		}
	}

}
