package com.nis.modules.asset.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.entity.*;
import com.nis.modules.asset.service.*;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.endpoint.service.MonitorEndpointStateService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.entity.VisualChartElement;
import com.nis.modules.panel.entity.VisualPanel;
import com.nis.modules.panel.service.VisualChartElementService;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.stat.service.StatService;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SnmpCredentialService;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.vsys.entity.VsysAssetRel;
import com.nis.modules.vsys.service.VsysAssetRelService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.ShiroException;
import org.snmp4j.smi.TimeTicks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
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
    private RedisTemplate redisTemplate;

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private DcService dcService;

    @Autowired
    private CabinetService cabinetService;

    @Autowired
    private AssetBrandService brandService;

    @Autowired
    private AssetModelService modelService;

    @Autowired
    private AssetTypeConfService typeConfService;

    @Autowired
    private AssetStateConfService stateConfService;

    @Autowired
    private AssetFieldMetaService assetFieldMetaService;

    @Autowired
    private AssetFieldValueService assetFieldValueService;

    @Autowired
    private AssetPingService assetPingService;

    @Autowired
    private AssetFeatureService assetFeatureService;

    @Autowired
    private AssetTalonStatusService assetTalonStatusService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private StatService statService;

    @Autowired
    private SnmpCredentialService snmpCredentialService;

    @Autowired
    private VisualPanelService panelService;

    @Autowired
    private VisualChartService chartService;

    @Autowired
    private VisualChartElementService chartElementService;

    @Autowired
    private MonitorProjectService projectService;

    @Autowired
    private MonitorModuleService moduleService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Autowired
    private MonitorEndpointStateService monitorEndpointStateService;

    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;

    @Autowired
    private SysUserStarredService starredService;

    @Autowired
    private VsysAssetRelService vsysAssetRelService;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;


    @Autowired
    private AssetCommentService assetCommentService;

    @SuppressWarnings("unchecked")
    @Override
    public AssetAsset queryAssetInfo(Integer id) {
        AssetAsset asset = this.getById(id);
        if (ToolUtil.isEmpty(asset)) {
            throw new NZException(RCode.ASSET_NOT_EXIST);
        }
        AssetTypeConf type = typeConfService.getById(asset.getTypeId());
        asset.setType(type);

        if (type.getVmh() == 1) {
            List<AssetAsset> children = this.list(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getPid, id));
            if (ToolUtil.isNotEmpty(children)) {
                asset.setChildrenNum(children.size());
                asset.setChildren(children);
            }
        }

        AssetStateConf state = stateConfService.getById(asset.getStateId());
        asset.setState(state);

        AssetBrand brand = brandService.getById(asset.getBrandId());
        asset.setBrand(brand);

        AssetModel model = modelService.getById(asset.getModelId());
        asset.setModel(model);

        Dc dc = dcService.getById(asset.getDcId());
        asset.setDc(dc);

        AssetPing assetPing = assetPingService.getOne(new QueryWrapper<AssetPing>().lambda().eq(AssetPing::getAssetId, asset.getId()));
        Object object = redisTemplate.opsForHash().get(Constant.SYSCONFIG_KEY_ASSET_PING, asset.getId().toString());
        if (ToolUtil.isNotEmpty(object)) {
            assetPing = JSON.parseObject((String) object, AssetPing.class);
        }
        asset.setPingInfo(assetPing);

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

        // cabinetEnd 存值的时候会+1 在此减一返回前台
        if (ToolUtil.isNotEmpty(asset.getCabinetEnd())) {
            asset.setCabinetEnd(asset.getCabinetEnd() - 1);
        }

        int endpointCount = monitorEndpointService.count(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getAssetId, asset.getId()));
        asset.setEndpointNum(endpointCount);

        List<AlertMessageEntity> alertMessageList = alertMessageService.list(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getAssetId, asset.getId()));
        asset.setAlertNum(alertMessageList.size());

        Map<Integer, Long> ruleIdAndAlertCountMap = alertMessageList.parallelStream().collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
        List<Map<String, Object>> alert = statService.getSeverityStatByRuleMsgCount(Tool.MapUtil.empty(), ruleIdAndAlertCountMap);
        asset.setAlert(alert);

        // 不返回 密码 / 密钥信息
        asset.setAuthPin(null);
        asset.setAuthPriKey(null);
        asset.setClientPin(null);

        try {
            ///收藏标识 0：未收藏 1：已收藏
            int count = starredService.count(new LambdaQueryWrapper<SysUserStarredEntity>()
                    .eq(SysUserStarredEntity::getTid, asset.getId())
                    .eq(SysUserStarredEntity::getType, "asset")
                    .eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId())
            );
            asset.setStarred(count == 0 ? 0 : 1);
        } catch (ShiroException e) {
            // 快照线程无法获取ShiroSession
        }
        // 已配置的 module 名单
        asset.setModules(moduleService.queryAssetMoudules(asset.getId()));
        return asset;
    }

    private void handleQueryAssetParam(Map<String, Object> params) {
        String ids = (String) params.get("ids");
        if (StrUtil.isNotBlank(ids)) {
            List<String> listIds = Arrays.asList(ids.split(","));
            params.put("ids", listIds);
        }
        if (ToolUtil.isNotEmpty(params.get("brandIds"))) {
            params.put("brandIds", Arrays.asList(String.valueOf(params.get("brandIds")).split(StringPool.COMMA)));
        }

        if (ToolUtil.isNotEmpty(params.get("modelIds"))) {
            params.put("modelIds", Arrays.asList(String.valueOf(params.get("modelIds")).split(StringPool.COMMA)));
        }

        if (ToolUtil.isNotEmpty(params.get("dcIds"))) {
            params.put("dcIds", Arrays.asList(String.valueOf(params.get("dcIds")).split(StringPool.COMMA)));
        }

        if (ToolUtil.isNotEmpty(params.get("ping"))) {
            params.put("ping", Arrays.asList(String.valueOf(params.get("ping")).split(StringPool.COMMA)));
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

        if (ToolUtil.isNotEmpty(params.get("moduleIds"))) {
            params.put("moduleIds", Arrays.asList(String.valueOf(params.get("moduleIds")).split(StringPool.COMMA)));
        }

        if (ToolUtil.isNotEmpty(params.get("pids"))) {
            params.put("pids", Arrays.asList(String.valueOf(params.get("pids")).split(StringPool.COMMA)));
        }

        if (ToolUtil.isNotEmpty(params.get("projectIds"))) {
            List<String> projectIdList = Arrays.asList(params.get("projectIds").toString().split(StringPool.COMMA));
            List<MonitorModule> moduleListByProjectIds = moduleService.list(new LambdaQueryWrapper<MonitorModule>().in(MonitorModule::getProjectId, projectIdList));
            List<Integer> moduleIds = moduleListByProjectIds.stream().map(MonitorModule::getId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(moduleIds)) {
                moduleIds.add(-99999);
            }
            params.put("innerModuleIdsByPorjects", moduleIds);
        }

        if (ToolUtil.isNotEmpty(params.get("fields"))) {
            Map<String, List<String>> fieldsParams = JSONObject.parseObject(params.get("fields").toString(), Map.class);
            List<AssetFieldValue> fieldValues = assetFieldValueService.list();
            List<List<Integer>> assetIdList = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : fieldsParams.entrySet()) {
                String metaId = entry.getKey();
                List<String> paramValues = entry.getValue();

                List<Integer> idList = fieldValues.stream().filter(assetFieldValue -> {
                    if (assetFieldValue.getMetaId().equals(Integer.valueOf(metaId)) && paramValues.contains(assetFieldValue.getValue())) {
                        return true;
                    }
                    return false;
                }).map(AssetFieldValue::getAssetId).distinct().collect(Collectors.toList());
                assetIdList.add(idList);
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

        if (ToolUtil.isNotEmpty(params.get("comment"))) {
            Map commentParams = JSONUtil.toBean(JSONUtil.createObj().set("pageSize", "-1").set("content", params.get("comment")), Map.class);
            List<AssetComment> assetComments = (List<AssetComment>)assetCommentService.queryCommentPage(commentParams).getList();
            List<Integer> assetIds = assetComments.stream().map(AssetComment::getAssetId).collect(Collectors.toList());
            if(Tool.CollectionUtil.isEmpty(assetComments)){
                assetIds.add(-99999);
            }
            params.put("innerAssetIdsByComment", assetIds);
        }
    }

    @Override
    public PageUtils queryAssetPage(Map<String, Object> params) {
        // 处理请求参数
        this.handleQueryAssetParam(params);

        //当前用户id
        params.put("userId", ShiroUtils.getUserId().intValue());

        IPage page = new Query(AssetAsset.class).getPage(params);
        List<AssetAsset> assetList = this.baseMapper.queryAssetList(page, params);

        // 查询结果不为空时处理返回结果
        if (CollectionUtils.isNotEmpty(assetList)) {
            this.handleResuleList(assetList);
        }

        page.setRecords(assetList);
        return new PageUtils(page);
    }

    @Override
    public Map<String, List> queryAssetGroupBy(Map<String, Object> params) {
        Map<String, List> result = new HashMap<String, List>();
        QueryWrapper wrapper = new QueryWrapper().select("id", "name");
        wrapper.orderByAsc("name");
        //dc
        List<Map<String, Object>> dcList = dcService.listMaps(wrapper);
        List<Map<String, Object>> dcGroupList = this.baseMapper.queryAssetGroupBy("asset.dc_id", params);
        this.assetGroupBysetNum(dcList, dcGroupList);
        result.put("dc", dcList);
        //type
        List<Map<String, Object>> typeList = typeConfService.listMaps(wrapper);
        List<Map<String, Object>> typeGroupList = this.baseMapper.queryAssetGroupBy("asset.type_id", params);
        this.assetGroupBysetNum(typeList, typeGroupList);
        result.put("type", typeList);
        //model
        List<AssetBrand> brandList = brandService.list();
        Map<String, AssetBrand> brandMap = Tool.MapUtil.newHashMap();
        brandList.forEach(brand -> {
            brandMap.put(brand.getId().toString(), brand);
        });
        QueryWrapper modelWrapper = new QueryWrapper().select("id", "name", "brand_id");
        modelWrapper.orderByAsc("name");
        List<Map<String, Object>> modelList = modelService.listMaps(modelWrapper);
        modelList.forEach(model -> {
            AssetBrand brand = brandMap.get(String.valueOf(model.get("brand_id")));
            model.put("brand", brand);
        });
        List<Map<String, Object>> modelGroupList = this.baseMapper.queryAssetGroupBy("asset.model_id", params);
        this.assetGroupBysetNum(modelList, modelGroupList);
        result.put("model", modelList);
        //ping
        Map<String, Object> upMap = Tool.MapUtil.newHashMap();
        upMap.put("id", 1);
        upMap.put("name", "up");
        Map<String, Object> downMap = Tool.MapUtil.newHashMap();
        downMap.put("id", 0);
        downMap.put("name", "down");
        List<Map<String, Object>> pingList = Tool.ListUtil.toList(upMap, downMap);
        List<Map<String, Object>> pingGroupList = this.baseMapper.queryAssetGroupBy("assp.status", params);
        this.assetGroupBysetNum(pingList, pingGroupList);
        result.put("ping", pingList);
        //label
        List<Map<String, Object>> metaGroupList = this.baseMapper.queryAssetGroupBy("fvm.meta_id", params);
        metaGroupList = metaGroupList.stream().filter(map -> map.get("id") != null).collect(Collectors.toList());
        result.put("meta", metaGroupList);
        return result;
    }

    private void assetGroupBysetNum(List<Map<String, Object>> allList, List<Map<String, Object>> groupList) {
        if (Tool.CollUtil.isEmpty(groupList)) {
            return;
        }
        Map<String, Object> groupMap = new HashMap<String, Object>();
        for (Map<String, Object> m : groupList) {
            groupMap.put(String.valueOf(m.get("id")), m.get("num"));
        }
        for (Map<String, Object> m : allList) {
            m.put("num", groupMap.getOrDefault(String.valueOf(m.get("id")), 0));
        }
		/* 排序会导致筛选条件跳动 先注释
		allList.sort((Map<String,Object> m1, Map<String,Object> m2) -> {
			Integer i1 = Integer.valueOf(String.valueOf(m1.getOrDefault("num", 0)));
			Integer i2 = Integer.valueOf(String.valueOf(m2.getOrDefault("num", 0)));
			return Integer.compare(i2, i1);
		});*/
    }

    @SuppressWarnings("unchecked")
    private void handleResuleList(List<AssetAsset> assetList) {
        List<SysUserEntity> userList = sysUserDao.selectList(null);
        Map<Long, SysUserEntity> userIdAndEntityMap = userList.stream().collect(Collectors.toMap(SysUserEntity::getId, Function.identity()));

        List<AssetAsset> list = this.list();
        Map<Integer, AssetAsset> assetIdAndEntityMap = list.stream().collect(Collectors.toMap(AssetAsset::getId, Function.identity()));
        Map<Integer, List<AssetAsset>> pidAndEntity = list.stream().collect(Collectors.groupingBy(AssetAsset::getPid));

        List<AssetFieldMeta> metaInfoList = assetFieldMetaService.getAssetFieldMetaListByAssetId(null);
        Map<Integer, List<AssetFieldMeta>> assetIdAndMetaInfoList = metaInfoList.stream().collect(Collectors.groupingBy(AssetFieldMeta::getAssetId));

        Map assetCachePings = redisTemplate.opsForHash().entries(Constant.SYSCONFIG_KEY_ASSET_PING);
        List<AssetPing> assetPinglist = assetPingService.list();
        Map<Integer, AssetPing> assetPings = assetPinglist.stream().collect(Collectors.toMap(AssetPing::getAssetId, Function.identity()));

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

            List<AssetAsset> children = pidAndEntity.get(asset.getId());
            if (ToolUtil.isNotEmpty(children)) {
                asset.setChildrenNum(children.size());
            }

            asset.setFields(assetIdAndMetaInfoList.get(asset.getId()));

            // cabinetEnd 存值的时候会+1 在此减一返回前台
            if (ToolUtil.isNotEmpty(asset.getCabinetEnd())) {
                asset.setCabinetEnd(asset.getCabinetEnd() - 1);
            }

            // 获取缓存中ping状态信息
            AssetPing assetPing = assetPings.get(asset.getId());
            Object object = assetCachePings.get(asset.getId().toString());
            if (ToolUtil.isNotEmpty(object)) {
                assetPing = JSON.parseObject((String) object, AssetPing.class);
            }
            asset.setPingInfo(assetPing);
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
        AssetBrand brand = null;
        if (asset.getBrandId() != null) {
            brand = brandService.getById(asset.getBrandId());
            ValidateUtils.is(brand).notNull(RCode.ASSET_BRAND_NOT_FOUND);
        }

        // model
        AssetModel model = modelService.getById(asset.getModelId());
        ValidateUtils.is(model).notNull(RCode.ASSET_MODEL_NOTFOUND);

        if (brand != null && !model.getBrandId().equals(brand.getId())) {
            throw new NZException(RCode.ASSET_BRAND_NOT_CONTAIN_MODEL);
        }

        // state
        AssetStateConf stateConf = stateConfService.getById(asset.getStateId());
        ValidateUtils.is(stateConf).notNull(RCode.STATECONF_NOT_FOUND);

        // type
        AssetTypeConf typeConf = typeConfService.getById(asset.getTypeId());
        ValidateUtils.is(typeConf).notNull(RCode.TYPECONF_NOT_FOUND);

        // 虚拟机 vm info 校验
        if (typeConf.getVm().equals(1)) {
            if (asset.getPid() == null) {
                // 虚拟机必须选择宿主
                throw new NZException(RCode.ASSET_NOT_SELECT_PARENT);
            }

            // pid 是否关联 asset
            AssetAsset parentAsset = this.getById(asset.getPid());
            ValidateUtils.is(parentAsset).notNull(RCode.ASSET_PARENT_NOT_EXIST);

            // 宿主机是否支持挂载
            AssetTypeConf parentAssetTypeConf = typeConfService.getById(parentAsset.getTypeId());
            if (!parentAssetTypeConf.getVmh().equals(1)) {
                // 只有宿主机，才能支持挂载虚拟机
                throw new NZException(RCode.ASSET_NOT_SUPPORT_MOUNTING);
            }
        } else {
            // 宿主机，不能选择宿主机
            if (asset.getPid() != null && !asset.getPid().equals(-1)) {
                throw new NZException(RCode.ASSET_NOT_SUPPORT_MOUNTING);
            }
        }

        // 不是虚拟机进行 dc cabinet 校验，虚拟机直接集成 宿主机 dc cabinet 信息
        if (typeConf.getVm().equals(0)) {
            // dc
            Dc dc = dcService.getById(asset.getDcId());
            ValidateUtils.is(dc).notNull(RCode.DC_NOT_EXISTS);

            // cab
            if (ToolUtil.isNotEmpty(asset.getCabinetId())) {
                Cabinet cabinet = cabinetService.getById(asset.getCabinetId());
                ValidateUtils.is(cabinet).notNull(RCode.CABINET_NOT_EXISTS);

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

        // auth_pin 密码加密处理
        String authPin = asset.getAuthPin();
        if (StrUtil.isNotBlank(authPin)) {
            asset.setAuthPin(Base64.getEncoder().encodeToString(authPin.getBytes()));
        } else {
            asset.setAuthPin(null);
        }

        // 修改时， 当 authType 与原来不一致时，则按照新增时规则校验
        if (asset.getId() != null) {
            AssetAsset oldAssetById = this.getById(asset.getId());
            if (oldAssetById == null) {
                throw new NZException(RCode.ASSET_NOT_EXIST);
            }
            if (asset.getAuthType() != oldAssetById.getAuthType()) {
                if (ObjectUtil.equal(asset.getAuthType(), 1)) {
                    ValidateUtils.is(asset.getAuthUsername()).notNull(RCode.ASSET_AUTHUSERNAME_ISNULL).maxLength(64).and(asset.getAuthPin()).notNull(RCode.ASSET_AUTHPIN_ISNULL).maxLength(256);
                } else if (ObjectUtil.equal(asset.getAuthType(), 2)) {
                    ValidateUtils.is(asset.getAuthPriKey()).notNull(RCode.ASSET_AUTHPRIKEY_ISNULL);
                }
            }
        }

        // client port
        if (asset.getClientPort() != null) {
            ValidateUtils.is(asset.getClientPort()).port(RCode.ASSET_CLIENTPORT_ERROR);
        }

        // client enable 为 1 时 token 必填
        if (ObjectUtil.equal(1, asset.getClientEnable())) {
            ValidateUtils.is(asset.getClientToken()).notNull(RCode.ASSET_CLIENT_TOKEN_ISNULL);
        }
    }

    /**
     * 校验U位置占用情况
     * 补充 isUpdate 参数
     * @return
     */
    public boolean validateUSizeForImport(Integer start, Integer end, Cabinet cab, String assetName, boolean isUpdate) {
        // 所有位置 从1开始到uSize
        Integer[] allPosition = new Integer[cab.getuSize()];
        for (int i = 0; i < allPosition.length; i++) {
            allPosition[i] = i + 1;
        }

        // 已经占用的位置
        Set<Integer> occList = new HashSet<>();

        // 修改的情况下，处理同机柜修改情况
        Integer oldCabinetId = null;
        Integer pid = null;
        if (isUpdate) {
            // 同机柜修改的情况  需要将自身节点补充到所属机柜的位置中去  再次重新计算可用位置
            if (StrUtil.isNotEmpty(assetName)) {
                AssetAsset assetEntity = this.getOne(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getName, assetName));
                if (assetEntity != null) {
                    oldCabinetId = assetEntity.getCabinetId();
                    pid = assetEntity.getPid();
                }
            }
        }

        // 修改标识   pid = -1, 代表原来是宿主机
        boolean updateFlag = oldCabinetId != null && oldCabinetId.equals(cab.getId()) && pid != null && pid.equals(-1);

        if (CollectionUtils.isEmpty(cab.getAssets())) {
            return true;
        } else {
            for (AssetAsset nodeInfo : cab.getAssets()) {
                // 修改的情况下 不记录当前占用位置
                if (updateFlag && nodeInfo.getName().equals(assetName) || nodeInfo.getPid() != -1) {
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
            asset.setCabinetStart(null);
            asset.setCabinetEnd(null);
            return true;
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
            if (updateFlag && (nodeInfo.getId().equals(assetId) || nodeInfo.getPid().equals(assetId))) {
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
        asset.setName(StrUtil.trim(asset.getName()));
        this.validateAssetInfo(asset);

        // 设置属性值
        asset.setCreateAt(new Date());
        asset.setCreateBy(ShiroUtils.getUserId());
        // cabinet end 处理
        if (asset.getCabinetEnd() != null && asset.getCabinetStart() != null) {
            asset.setCabinetEnd(asset.getCabinetEnd() + 1);
        }

        if (asset.getPid() != null && asset.getPid() != -1) {
            // pid 不为空 证明是 虚拟极，继承宿主机 dc cabinet state end 信息
            AssetAsset parent = this.getById(asset.getPid());
            asset.setDcId(parent.getDcId());
            asset.setCabinetId(parent.getCabinetId());
            asset.setCabinetStart(parent.getCabinetStart());
            asset.setCabinetEnd(parent.getCabinetEnd());
        } else {
            asset.setPid(-1);
        }

        if (asset.getBrandId() == null) {
            AssetModel model = modelService.getById(asset.getModelId());
            asset.setBrandId(model.getBrandId());
        }

        if (asset.getClientAddress() == null) {
            asset.setClientAddress(asset.getManageIp());
        }

        // 保存
        this.save(asset);

        // 保存 asset 关联数据
        this.saveAssetRelData(asset);

        // asset panel & info chart
        this.saveAssetInfoPanel(asset);

        // 通过 model chart tepmlate 新增 asset charts
        this.saveAssetChartsViaModelChartTmpl(asset);

        // 保存 默认 VSYS 和 Asset 关联关系
        this.saveOrUpdateDefaultVsysAssetRel(asset.getId(), asset.getModelId());
        return asset.getId();
    }

    /**
     * 保存 默认 VSYS 和 Asset 关联关系
     *
     * @param assetId
     * @param modelId
     */
    private void saveOrUpdateDefaultVsysAssetRel(Integer assetId, Integer modelId) {
        //  hasEmpty return
        if (ObjectUtil.hasEmpty(assetId, modelId)) return;

        AssetModel model = modelService.getById(modelId);
        if (model.getTsgAppliance() == 1) {
            // tsg asset
            // 先查询是否存在对应关系，存在则不添加默认关联关系
            List<VsysAssetRel> relList = vsysAssetRelService.list(new LambdaQueryWrapper<VsysAssetRel>().eq(VsysAssetRel::getAssetId, assetId));
            if (CollUtil.isNotEmpty(relList)) return;

            VsysAssetRel rel = new VsysAssetRel();
            rel.setAssetId(assetId);
            // taskId = 0 代表 内置 VSYS_ID
            rel.setTaskId(0);
            // 内置 VSYS_ID = 1
            rel.setVsysId(1);
            // 内置 VSYS result_id = 0
            rel.setResultId(0);
            rel.setState(1);
            rel.setDelFlag(0);
            rel.setUts(System.currentTimeMillis());
            vsysAssetRelService.save(rel);
        } else {
            // not tsg asset
            // remove rel
            vsysAssetRelService.remove(new LambdaQueryWrapper<VsysAssetRel>()
                    .eq(VsysAssetRel::getAssetId, assetId));
        }
    }

    /**
     * 新增 asset penel & info chart
     *
     * @param asset
     */
    private void saveAssetInfoPanel(AssetAsset asset) {
        // 配置信息获取
        SysConfigEntity assetChartConfig = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_CHART_TPL));
        // asset config
        VisualChart chartTemplate = JSONObject.parseObject(assetChartConfig.getParamValue(), VisualChart.class);
        // 新增时需要新增 panel chart
        chartService.saveRelPanelAndChart(Constant.PanelType.ASSET.getValue(), asset.getId(), null, chartTemplate);
    }

    /**
     * 通过 model chart tepmlate 新增 asset charts
     *
     * @param asset
     */
    private void saveAssetChartsViaModelChartTmpl(AssetAsset asset) {
        // 通过关联 model charts 添加 asset charts
        AssetModel model = modelService.getById(asset.getModelId());
        if (StringUtils.isNotEmpty(model.getChartIds())) {
            List<String> list = Arrays.asList(model.getChartIds().split(","));
            List<Integer> chartIdList = list.stream().map(Integer::parseInt).collect(Collectors.toList());
            // 补充 group 下的 chart id
            List<VisualChart> childCharts = chartService.list(new LambdaQueryWrapper<VisualChart>().in(VisualChart::getGroupId, chartIdList));
            List<Integer> childChartIdList = childCharts.stream().map(VisualChart::getId).distinct().collect(Collectors.toList());
            chartIdList.addAll(childChartIdList);
            chartIdList = chartIdList.stream().distinct().collect(Collectors.toList());
            List<VisualChart> chartList = chartService.queryChartWithElementByIds(chartIdList);

            AssetAsset assetInfo = this.queryAssetInfo(asset.getId());

            VisualPanel assetPanel = panelService.getOne(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, Constant.PanelType.ASSET.getValue()).eq(VisualPanel::getLink, asset.getId()));

            // model chart 组 和 asset 新增 chart 组关联关系，用来做 asset 新增 chart 组关系映射
            Map<Integer, Integer> groupIdMapping = new HashMap<>();

            // 按照 group 升序，先添加组
            chartList = chartList.stream().sorted(Comparator.comparing(VisualChart::getGroupId)).collect(Collectors.toList());

            // 渲染新增
            for (VisualChart chart : chartList) {
                VisualChart saveChart = new VisualChart();
                Tool.BeanUtil.copyProperties(chart, saveChart);
                saveChart.setId(null);
                saveChart.setPanelId(assetPanel.getId());
                saveChart.setUpdateAt(new Date());
                saveChart.setUpdateBy(ShiroUtils.getUserId().intValue());
                saveChart.setPid(chart.getId());

                saveChart.setVarType(Constant.ChartVariableType.ASSET.getValue());
                saveChart.setVarId(asset.getId());

                Integer newGroupId = groupIdMapping.get(saveChart.getGroupId());
                saveChart.setGroupId(newGroupId == null ? 0 : newGroupId);

                saveChart.setName(Tool.NzExpressionUtil.renderExpression(chart.getName(), assetInfo));
                saveChart.setParam(ToolUtil.isNotEmpty(chart.getParam()) ? Tool.NzExpressionUtil.renderExpression(chart.getParam().toString(), asset) : null);

                chartService.save(saveChart);

                if (Constant.CHART_GROUP_TYPE.equalsIgnoreCase(chart.getType())) {
                    groupIdMapping.put(chart.getId(), saveChart.getId());
                }

                if (CollectionUtils.isNotEmpty(saveChart.getElements())) {
                    for (VisualChartElement chartElement : saveChart.getElements()) {
                        chartElement.setId(null);
                        chartElement.setChartId(saveChart.getId());
                        chartElement.setExpression(ToolUtil.isNotEmpty(chartElement.getExpression()) ? Tool.NzExpressionUtil.renderExpression(chartElement.getExpression(), asset) : null);
                    }
                    chartElementService.saveBatch(saveChart.getElements());
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateAsset(AssetAsset asset) {
        // 校验参数
        asset.setName(StrUtil.trim(asset.getName()));
        this.validateAssetInfo(asset);

        // 设置属性值
        asset.setUpdateAt(new Date());
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

        // 留作对比是否更改 model
        AssetAsset oldAsset = this.getById(asset.getId());

        // authPrivKey
        if (StrUtil.isEmpty(asset.getAuthPriKey())) {
            asset.setAuthPriKey(null);
        }

        // 保存
        this.updateById(asset);

        // 如果修改了Model, 处理关联 charts 变化
        if (!oldAsset.getModelId().equals(asset.getModelId())) {
            this.updateModelCharts(asset);
        }

        // 如果没有指定 meta, 则视为删除 asset之前关联的 meta 信息
        assetFieldValueService.remove(new LambdaQueryWrapper<AssetFieldValue>().eq(AssetFieldValue::getAssetId, asset.getId()));

        // 保存 asset 关联数据
        this.saveAssetRelData(asset);

        // 保存 默认 VSYS 和 Asset 关联关系
        this.saveOrUpdateDefaultVsysAssetRel(asset.getId(), asset.getModelId());

        // 修改后处理 ping & Talon 数据
        this.afterUpdateAsset(asset.getId());
        return asset.getId();
    }

    /**
     * 修改后处理asset ping数据 删除缓存和数据库中的数据
     * 修改后处理asset talon status数据 删除缓存和数据库中的数据
     *
     * @param assetId
     */
    private void afterUpdateAsset(Integer assetId) {
        assetPingService.remove(new QueryWrapper<AssetPing>().lambda().eq(AssetPing::getAssetId, assetId));
        redisTemplate.opsForHash().delete(Constant.SYSCONFIG_KEY_ASSET_PING, assetId.toString());

        assetTalonStatusService.removeById(assetId);
        redisTemplate.opsForHash().delete(Constant.ASSET_TALON_STATUS_CACHE_KEY, assetId.toString());
    }

    /**
     * asset 更改了 model , 对应更新 asset charts,自己的 charts 保留
     * @param asset
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateModelCharts(AssetAsset asset) {
        VisualPanel assetPanel = panelService.getOne(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, Constant.PanelType.ASSET).eq(VisualPanel::getLink, asset.getId()));
        List<VisualChart> chartList = chartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, assetPanel.getId()));
        List<Integer> delIds = chartList.stream().filter(chart -> chart.getPid() != null ? true : false).map(VisualChart::getId).collect(Collectors.toList());

        // 删除之前 model 关联的图表
        Integer[] ids = new Integer[delIds.size()];
        delIds.toArray(ids);

        if (ToolUtil.isNotEmpty(ids)) {
            // 删除旧 model 关联的 charts
            chartService.delCharts(ids, null);
        }

        // 通过 model chart tepmlate 新增 asset charts
        this.saveAssetChartsViaModelChartTmpl(asset);
    }

    private void saveAssetRelData(AssetAsset asset) {
        // fields
        List<AssetFieldMeta> fields = asset.getFields();
        if (CollectionUtils.isNotEmpty(fields)) {
            // 需要保存的 meta id
            List<AssetFieldValue> values = new ArrayList<AssetFieldValue>();
            for (AssetFieldMeta fieldMeta : fields) {
                for (String value : fieldMeta.getValue()) {
                    AssetFieldValue temp = new AssetFieldValue();
                    temp.setAssetId(asset.getId());
                    temp.setMetaId(fieldMeta.getId());
                    temp.setValue(value);
                    values.add(temp);
                }
            }
            assetFieldValueService.saveBatch(values);
        }
    }

    /**
     * asset 删除方法
     * asset 删除提示关联内容：
     *      baby asset
     *      endpoint
     * @param force 0：存在关联数据时，删除失败 1：存在关联数据时，同时删除关联数据 默认：0
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R deleteAsset(Integer force, Integer... ids) {
        boolean forceData = false;
        List resultList = Tool.ListUtil.list(true);

        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        List<AssetAsset> delAssets = this.list(new LambdaQueryWrapper<AssetAsset>().in(AssetAsset::getId, idList));
        if (Tool.CollUtil.isEmpty(delAssets)) {
            return  R.ok().putData("list", resultList);
        }

        for (AssetAsset assetEntity : delAssets) {
            Map<Object, Object> relObjectInfoMap = this.getAssetDelRelObjectInfoMap(assetEntity);
            if (Tool.CollUtil.isNotEmpty((List) relObjectInfoMap.get("relates"))) forceData = true;
            resultList.add(relObjectInfoMap);
        }

        switch (force) {
            case 0: {// 0：存在关联数据时，删除失败
                if (forceData) {
                    return R.error(RCode.ASSET_RELOBJ_CAN_NOT_REMOVE).putData("list", resultList);
                }
            }
            case 1:  // 1：存在关联数据时，同时删除关联数据
                break;
            default:
                throw new NZException(RCode.ASSET_FORCE_INVALIDE);
        }

        // 删除自身和关联内容
        // 需要删除的 asset id list
        List<AssetAsset> delChildrenAssets = this.list(new LambdaQueryWrapper<AssetAsset>().in(AssetAsset::getPid, idList));
        delAssets.addAll(delChildrenAssets);
        List<Integer> removeAssetIdList = delAssets.stream().map(AssetAsset::getId).distinct().collect(Collectors.toList());

        // asset
        this.removeByIds(removeAssetIdList);

        // endponiet
        List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().in(MonitorEndpoint::getAssetId, removeAssetIdList));
        if (Tool.CollUtil.isNotEmpty(endpointList)) {
            List<Integer> endpointIds = endpointList.stream().map(MonitorEndpoint::getId).collect(Collectors.toList());
            monitorEndpointService.deleteEndpointByIds(endpointIds);
        }

        // panel chart chartElement
        List<VisualPanel> panelList = panelService.list(new LambdaQueryWrapper<VisualPanel>()
                .eq(VisualPanel::getType, Constant.PanelType.ASSET.getValue())
                .in(VisualPanel::getLink, removeAssetIdList));
        if (Tool.CollUtil.isNotEmpty(panelList)) {
            Set<Integer> panelIds = panelList.stream().map(VisualPanel::getId).collect(Collectors.toSet());
            panelService.delPanelsAndChartInfoByPanelIds(panelIds);
        }

        // chart : create by asset chart tmplate
        chartService.remove(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getVarType, Constant.ChartVariableType.ASSET.getValue()).in(VisualChart::getVarId, removeAssetIdList));

        // alert
        alertMessageService.removeByAssetIds(removeAssetIdList);

        // asset Ping
        List<String> removeRelInRedisIdList = removeAssetIdList.stream().map(id -> id.toString()).collect(Collectors.toList());
        assetPingService.remove(new LambdaQueryWrapper<AssetPing>().in(AssetPing::getAssetId, removeAssetIdList));
        redisTemplate.opsForHash().delete(Constant.SYSCONFIG_KEY_ASSET_PING, removeRelInRedisIdList.toArray());

        // asset Talon
        assetTalonStatusService.removeByIds(removeAssetIdList);
        redisTemplate.opsForHash().delete(Constant.ASSET_TALON_STATUS_CACHE_KEY, removeRelInRedisIdList.toArray());

        // field
        assetFieldValueService.remove(new LambdaQueryWrapper<AssetFieldValue>().in(AssetFieldValue::getAssetId, removeAssetIdList));

        // starred
        starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getType, "asset")
                .in(SysUserStarredEntity::getTid, removeAssetIdList)
        );

        // vsys asset rel
        vsysAssetRelService.remove(new LambdaQueryWrapper<VsysAssetRel>()
                .in(VsysAssetRel::getAssetId, removeAssetIdList));

        // comment
        assetCommentService.remove(new LambdaQueryWrapper<AssetComment>()
                .in(AssetComment::getAssetId, removeAssetIdList));
        return R.ok().putData("list", resultList);
    }

    /**
     * 获取 asset 删除提示关联信息：
     *      baby asset
     *      endpoint
     * @return
     */
    private Map<Object, Object> getAssetDelRelObjectInfoMap(AssetAsset asset) {
        List<Map> relatesList = Tool.ListUtil.list(true);

        // children Asset
        List<AssetAsset> childrenAssetList = this.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getPid, asset.getId()));

        // children asset & endpoint
        if (Tool.CollUtil.isNotEmpty(childrenAssetList)) {
            // endpoint list
            List<MonitorEndpoint> endpointList = monitorEndpointService.list();
            for (AssetAsset chasset : childrenAssetList) {
                List<Map> relList = Tool.ListUtil.list(true);
                List<MonitorEndpoint> endpointListOfAsset = endpointList.stream().filter(e -> e.getAssetId().equals(chasset.getId())).collect(Collectors.toList());
                for (MonitorEndpoint endpoint : endpointListOfAsset) {
                    Map<Object, Object> map = Tool.MapUtil.builder()
                            .put("id", endpoint.getId())
                            .put("name", endpoint.getName())
                            .put("type", "endpoint")
                            .build();
                    relList.add(map);
                }

                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", chasset.getId())
                        .put("name", chasset.getName())
                        .put("type", "asset")
                        .put("relates", relList)
                        .build();
                relatesList.add(map);
            }
        }

        // rel endpoint
        List<MonitorEndpoint> endpointListOfAsset = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getAssetId, asset.getId()));
        for (MonitorEndpoint endpoint : endpointListOfAsset) {
            Map<Object, Object> map = Tool.MapUtil.builder()
                    .put("id", endpoint.getId())
                    .put("name", endpoint.getName())
                    .put("type", "endpoint")
                    .build();
            relatesList.add(map);
        }

        Map<Object, Object> delRelObjectMap = Tool.MapUtil.builder()
                .put("id", asset.getId())
                .put("name", asset.getName())
                .put("type", "asset")
                .put("relates", relatesList)
                .build();

        return delRelObjectMap;
    }

    @Override
    public Map endpointStatByAsset(Integer assetId) {
        List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getAssetId, assetId));
        int all = endpointList.size();
        Map resultMap = new HashMap(8);
        resultMap.put("total", all);

        if (CollectionUtils.isNotEmpty(endpointList)) {
            List<Integer> suspendedEndpointIdList = monitorEndpointService.getSuspendedEndpointIdList(assetId);
            List<Integer> endpointIdList = endpointList.stream().map(MonitorEndpoint::getId).distinct().collect(Collectors.toList());
            List<MonitorEndpointState> upEndpointStates = monitorEndpointStateService.list(new LambdaQueryWrapper<MonitorEndpointState>().in(MonitorEndpointState::getEndpointId, endpointIdList).eq(MonitorEndpointState::getState, 0));

            Set<Integer> upStatusEndpointIds = upEndpointStates.stream().map(MonitorEndpointState::getEndpointId).collect(Collectors.toSet());
            endpointIdList.retainAll(upStatusEndpointIds);
            resultMap.put("up", endpointIdList.size());
            resultMap.put("down", all - endpointIdList.size() - suspendedEndpointIdList.size());
            resultMap.put("suspended", suspendedEndpointIdList.size());
        }
        resultMap.put("up", 0);
        resultMap.put("down", 0);
        resultMap.put("suspended", 0);
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
        List<AssetAsset> assetList = this.list();
        // dc
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
        List<AssetFieldMeta> metaList = assetFieldMetaService.getAssetFieldMetaListByAssetId(assetId);
        metaList = metaList.stream().filter(assetFieldMeta -> assetFieldMeta.getId().equals(metaId)).collect(Collectors.toList());
        return metaList;
    }

    @Override
    public void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_ASSET_ASSET_EXPORT_HEADER);
        String fileName = "AssetTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectData(Integer assetId) {
        AssetAsset asset = this.getById(assetId);
        // 根据当前资产获取state设置是否进行采集
        AssetTypeConf assetType = typeConfService.getById(asset.getTypeId());
        if (ToolUtil.isEmpty(assetType)) {
            return;
        }
        Integer sshCollect = assetType.getSshCollect();
        Integer snmpCollect = assetType.getSnmpCollect();
        Integer authProtocol = assetType.getAuthProtocol();
        List<AssetFeature> results = new ArrayList<AssetFeature>();
        // 查询该资产下是否有之前收集的信息  防止频繁查询数据库
        List<AssetFeature> assetFeatures = assetFeatureService.list(new QueryWrapper<AssetFeature>().lambda().eq(AssetFeature::getAssetId, assetId));
        HashMap<String, AssetFeature> assetFeatureMap = new HashMap<String, AssetFeature>();
        if (ToolUtil.isNotEmpty(assetFeatures)) {
            for (AssetFeature data : assetFeatures) {
                // 将数据以key为资产id 查询的信息名称 以及查询方式组合
                assetFeatureMap.put(data.getAssetId() + data.getItem() + data.getSource(), data);
            }
        }

        if (ToolUtil.equals(sshCollect, 1) && ToolUtil.equals(authProtocol, 1)) {
            // 进行ssh采集
            String sshCollectScript = assetType.getSshCollectScript();
            if (StrUtil.isBlank(sshCollectScript)) {
                logger.debug("asset sshCollectScript is empty asset info : %s", JSON.toJSON(asset));
                return;
            }

            Session session = null;
            try {
                // 进行ssh连接
                session = SshUtil.getSshConnection(asset);
                String sshCommondResult = RuntimeUtil.sshCommondResult(session, " /bin/bash " + sshCollectScript);
                this.getSshInfo(assetFeatureMap, results, sshCommondResult, asset);

            } catch (JSchException | IOException e) {
                logger.error(e);
                logger.error("asset feature get data error,assetId : {} info : {}", assetId, e);
                throw new NZException(e.getMessage());
            } finally {
                SshUtil.close(session);
            }

        }
        if (ToolUtil.equals(snmpCollect, 1)) {
            // 进行snmp数据采集
            try {
                SnmpCredential snmpCredential = snmpCredentialService.getById(asset.getSnmpCredentialId());
                if (ToolUtil.isEmpty(snmpCredential)) {
                    logger.debug("asset snmpCredential is empty asset info : %s", JSON.toJSON(asset));
                    return;
                }
                this.getSystemInfoBySnmp(assetFeatureMap, results, asset, snmpCredential);
            } catch (IOException e) {
                logger.error("asset snmp collect error asset info : %s", JSON.toJSON(asset));
                logger.error(e);
            }
        }
        // 存入db 无则新增 有则修改
        if (results != null && results.size() > 0) {
            assetFeatureService.saveOrUpdateBatch(results);
        }
    }

    public void getSshInfo(HashMap<String, AssetFeature> assetFeatureMap, List<AssetFeature> results, String sshCommondResult, AssetAsset asset) {
        Map<String, String> sshResult = JSONObject.parseObject(sshCommondResult, Map.class);
        for (Map.Entry<String, String> entrySet : sshResult.entrySet()) {
            //查询数据库是否已经存在数据
            AssetFeature result = assetFeatureMap.get(asset.getId() + entrySet.getKey() + Constant.ACCOUNT_SSH_PROTOCOL);
            if (ToolUtil.isEmpty(result)) {
                result = new AssetFeature();
            }
            result.setAssetId(asset.getId());
            result.setItem(entrySet.getKey());
            result.setValue(sshResult.get(entrySet.getKey()));
            result.setSource(Constant.ACCOUNT_SSH_PROTOCOL);
            result.setUpdateAt(new Date());
            results.add(result);
        }
    }

    public void getSystemInfoBySnmp(HashMap<String, AssetFeature> assetFeatureMap, List<AssetFeature> results, AssetAsset asset, SnmpCredential snmpCredential) throws IOException {
        Map snmpInfos = JSONObject.parseObject((String) snmpCredential.getConfig(), Map.class);
        Set<String> keySet = Constant.SNMP_OID_MAP.keySet();
        for (String key : keySet) {
            List<Map> snmpResults = SnmpUtil.snmpWalk(asset.getManageIp(), snmpCredential.getPort(), snmpCredential.getType(), (String) snmpInfos.get("readCommunity"), Constant.SNMP_OID_MAP.get(key), snmpInfos);
            if ("ifEntry".equals(key)) {
                List l = new LinkedList();
                if (CollectionUtils.isNotEmpty(snmpResults)) {
                    Map<String, LinkedHashMap> handleMap = new LinkedHashMap<String, LinkedHashMap>();
                    String resultOid;
                    String name;
                    for (Map map : snmpResults) {
                        //截取最后一位oid数
                        resultOid = map.get("oid").toString();
                        //去掉最后一位oid剩下值
                        String first = resultOid.substring(0, resultOid.lastIndexOf("."));
                        //最后一位oid数
                        String last = resultOid.substring(resultOid.lastIndexOf(".") + 1, resultOid.length());
                        name = Constant.SNMP_IFENTRY_MAP.get(first);
                        if (ToolUtil.isEmpty(name)) {
                            continue;
                        }
                        Map map2 = handleMap.get(last);
                        if (ToolUtil.isEmpty(map2)) {
                            Map map3 = new LinkedHashMap();
                            handleMap.put(last, (LinkedHashMap) map3);
                            map3.put(name, map.get("value"));
                        } else {
                            map2.put(name, map.get("value"));
                        }
                    }

                    for (String k : handleMap.keySet()) {
                        l.add(handleMap.get(k));
                    }
                }
                //查询数据库是否已经存在数据
                AssetFeature result = assetFeatureMap.get(asset.getId() + key + Constant.ACCOUNT_SNMP_PROTOCOL);
                if (ToolUtil.isEmpty(result)) {
                    result = new AssetFeature();
                }
                result.setAssetId(asset.getId());
                result.setItem(key);
                result.setValue(JSON.toJSONString(l));
                result.setSource(Constant.ACCOUNT_SNMP_PROTOCOL);
                result.setUpdateAt(new Date());
                results.add(result);
            } else {
                for (Map snmpResult : snmpResults) {
                    String value = (String) snmpResult.get("value");
                    //查询数据库是否已经存在数据
                    AssetFeature result = assetFeatureMap.get(asset.getId() + key + Constant.ACCOUNT_SNMP_PROTOCOL);
                    if (ToolUtil.isEmpty(result)) {
                        result = new AssetFeature();
                    }
                    result.setAssetId(asset.getId());
                    result.setItem(key);
                    if ("sysUpTime".equals(key)) {
                        TimeTicks t = new TimeTicks();
                        t.setValue(value);
                        long milliseconds = t.toMilliseconds();
                        result.setValue(DateUtil.getTime(new Date(new Date().getTime() - milliseconds)));
                    } else {
                        result.setValue(value);
                    }
                    result.setSource(Constant.ACCOUNT_SNMP_PROTOCOL);
                    result.setUpdateAt(new Date());
                    results.add(result);
                }
            }
        }
    }

    private List<Map> getFieldStatByAsset() {
        List<AssetFieldMeta> allMetas = assetFieldMetaService.getAssetFieldMetaListByAssetId(null);
        List<Map> fieldStat = new ArrayList<>();
        Map metaStatMap, childrenMap;

        // 创建一个key为field value  值为使用当前field的资产id集合
        Map<String, Set<Integer>> fieldIdAndAssetIdsMap = new HashMap<String, Set<Integer>>();
        Map<Integer, List<AssetFieldMeta>> fieldIdAndfields = new HashMap<Integer, List<AssetFieldMeta>>();
        for (AssetFieldMeta fieldMeta : allMetas) {

            List<AssetFieldMeta> fields = fieldIdAndfields.get(fieldMeta.getId());
            if (ToolUtil.isEmpty(fields)) {
                fields = new ArrayList<AssetFieldMeta>();
                fields.add(fieldMeta);
                fieldIdAndfields.put(fieldMeta.getId(), fields);
            } else {
                fields.add(fieldMeta);
            }

            List<String> values = fieldMeta.getValue();
            for (String value : values) {
                Set<Integer> assetIds = fieldIdAndAssetIdsMap.get(value);
                if (ToolUtil.isEmpty(assetIds)) {
                    assetIds = new HashSet<Integer>();
                    assetIds.add(fieldMeta.getAssetId());
                    fieldIdAndAssetIdsMap.put(value, assetIds);
                } else {
                    assetIds.add(fieldMeta.getAssetId());
                }
            }
        }

        for (Map.Entry<Integer, List<AssetFieldMeta>> entry : fieldIdAndfields.entrySet()) {
            List<AssetFieldMeta> fields = fieldIdAndfields.get(entry.getKey());
            List children = new ArrayList();
            for (AssetFieldMeta fieldMeta : fields) {
                List<String> values = fieldMeta.getValue();
                for (String valueEntry : values) {
                    if (ToolUtil.isEmpty(fieldIdAndAssetIdsMap.get(valueEntry))) {
                        continue;
                    }
                    childrenMap = new HashMap(4);
                    childrenMap.put("value", valueEntry);
                    childrenMap.put("total", fieldIdAndAssetIdsMap.get(valueEntry).size());
                    children.add(childrenMap);
                    fieldIdAndAssetIdsMap.remove(valueEntry);
                }
            }

            metaStatMap = new HashMap(8);
            metaStatMap.put("id", fields.get(0).getId());
            metaStatMap.put("name", fields.get(0).getName());
            metaStatMap.put("key", fields.get(0).getMetaKey());
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
        PageUtils page = this.queryAssetPage(params);
        List<AssetAsset> list = (List<AssetAsset>) page.getList();
        List<List<String>> exportDataList = this.handleExportDataList(list);

        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_ASSET_ASSET_EXPORT_HEADER);
        String fileName = "Assets";
        String formatStr = params.get("format").toString();
        basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
    }

    @Override
    public Map importAssetsData(MultipartFile multipartFile, Map<String, Object> params) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_ASSET_ASSET_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        // 表头信息 国际化之后的
        List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

        // 返回统计信息
        Map statMap = this.saveAssetsFromImportDataList(importDataList, headerInfos, params);
        return statMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

            // 删除关联的 panel chart  chartElement信息
            List<VisualPanel> panels = panelService.list(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, Constant.PanelType.ASSET.getValue()).in(VisualPanel::getLink, assetIds));
            if (CollectionUtils.isNotEmpty(panels)) {
                Set<Integer> panelIds = panels.stream().map(VisualPanel::getId).collect(Collectors.toSet());
                panelService.delPanelsAndChartInfoByPanelIds(panelIds);
            }

            // 删除 field meta/value
            assetFieldMetaService.remove(new LambdaQueryWrapper<AssetFieldMeta>().eq(AssetFieldMeta::getSeq, seq));
            assetFieldValueService.remove(new LambdaQueryWrapper<AssetFieldValue>().eq(AssetFieldValue::getSeq, seq));
        }
    }

    @Override
    public List<AssetAsset> getBlackboxAssetInfoList() {
        List<AssetAsset> assetList = this.list();

        // 支持 ping 的 state_conf
        List<AssetStateConf> stateConfList = stateConfService.list(new LambdaQueryWrapper<AssetStateConf>().eq(AssetStateConf::getPing, 1));
        List<Integer> enablePingStateConfIdList = stateConfList.stream().map(AssetStateConf::getId).collect(Collectors.toList());

        // 支持 ping 的 asset
        assetList = assetList.stream().filter(asset -> enablePingStateConfIdList.contains(asset.getStateId())).collect(Collectors.toList());

        List<Dc> dcList = dcService.list();
        Map<Integer, Dc> dcIdAndNameMap = dcList.stream().collect(Collectors.toMap(Dc::getId, Function.identity()));

        SysConfigEntity assetPingIntervalConfig = sysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_PING_INTERVAL));
        for (AssetAsset entity : assetList) {
            entity.setDc(dcIdAndNameMap.get(entity.getDcId()));
            entity.setAssetPingInterval(assetPingIntervalConfig.getParamValue());
        }
        return assetList;
    }

    @Override
    public List<AssetAsset> getOpenPingAssets() {
        List<AssetAsset> assetList = this.list();

        // 支持 ping 的 state_conf
        List<AssetStateConf> stateConfList = stateConfService.list(new LambdaQueryWrapper<AssetStateConf>().eq(AssetStateConf::getPing, 1));
        List<Integer> enablePingStateConfIdList = stateConfList.stream().map(AssetStateConf::getId).collect(Collectors.toList());

        // 支持 ping 的 asset
        assetList = assetList.stream().filter(asset -> enablePingStateConfIdList.contains(asset.getStateId())).collect(Collectors.toList());

        return assetList;
    }

    @Override
    public List<VisualChart> getAssetsWithTagByChartIds(List<Integer> chartIds) {
        List<VisualChart> chartIdWithAssetEntity = this.baseMapper.getAssetsByChartIds(chartIds);
        if (CollectionUtils.isEmpty(chartIdWithAssetEntity)) {
            return Collections.emptyList();
        }
        return chartIdWithAssetEntity;
    }

    private Map saveAssetsFromImportDataList(List<Map<Integer, String>> importDataList, List<String> headerInfos, Map<String, Object> params) {
        String seq = StrUtil.uuid();
        params.put("seq", seq);
        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

        String defaultCabinetUsizeStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_DEFAULT_CABINETU);
        Integer defaultCabinetUsize = Integer.parseInt(defaultCabinetUsizeStr);

        // 创建导入校验对象
        ImportValidateUtil validateUtil = ImportValidateUtil.builder()
                .existed(existed).headerInfos(headerInfos)
                .build();

        Integer addNum = 0, updateNum = 0;
        // 记录导入失败内容
        List<Map<Integer, String>> failContentList = ListUtil.list(true);
        // 处理 excel 数据，将含宿主机资产排到前面
        List<Map<Integer, String>> handleImportData = this.handleImportData(importDataList);
        for (int i = 0; i < handleImportData.size(); i++) {
            Integer lineNum = i + 1;
            Map saveEntitys = Tool.MapUtil.newHashMap();
            Map<Integer, String> dataMap = handleImportData.get(i);
            // name， 以 name作为索引 标识该条记录是否存在
            String assetName = dataMap.get(AssetImportEnum.NAME.getIndex());
            validateUtil.validateRequired(assetName, lineNum, AssetImportEnum.NAME.getIndex());
            boolean indexDuplicate = validateUtil.validateIndexDuplicate("asset_asset", "name", assetName, lineNum, AssetImportEnum.NAME.getIndex());
            // 索引列重复 & 忽略标识，提示忽略本行
            if (BooleanUtil.and(indexDuplicate, StrUtil.equals("2", existed))) {
                validateUtil.ignoreThisRow(lineNum, AssetImportEnum.NAME.getIndex());
                failContentList.add(dataMap);
                continue;
            }

            String sn = dataMap.get(AssetImportEnum.SN.getIndex());
            validateUtil.validateRequired(sn, lineNum, AssetImportEnum.SN.getIndex());
            validateUtil.validateDuplicate("asset_asset", "sn", sn, assetName, lineNum, AssetImportEnum.SN.getIndex());

            String manageIp = dataMap.get(AssetImportEnum.MANAGE_IP.getIndex());
            validateUtil.validateRequired(manageIp, lineNum, AssetImportEnum.MANAGE_IP.getIndex());
            validateUtil.validateRegex(manageIp, Constant.REGEX_IP, lineNum, AssetImportEnum.MANAGE_IP.getIndex());
            validateUtil.validateDuplicate("asset_asset", "manage_ip", manageIp, assetName, lineNum, AssetImportEnum.MANAGE_IP.getIndex());

            String ipmiIp = dataMap.get(AssetImportEnum.IPMI_IP.getIndex());
            if(StrUtil.isNotEmpty(ipmiIp))
                validateUtil.validateRegex(ipmiIp, Constant.REGEX_IP, lineNum, AssetImportEnum.IPMI_IP.getIndex());

            // state conf 不自动添加
            String stateName = dataMap.get(AssetImportEnum.STATE.getIndex());
            validateUtil.validateRequired(stateName, lineNum, AssetImportEnum.STATE.getIndex());
            validateUtil.validateRelObjExits("asset_state_conf", "name", stateName, false, lineNum, AssetImportEnum.STATE.getIndex());

            // type conf 不自动添加
            String typeName = dataMap.get(AssetImportEnum.TYPE.getIndex());
            validateUtil.validateRequired(typeName, lineNum, AssetImportEnum.TYPE.getIndex());
            validateUtil.validateRelObjExits("asset_type_conf", "name", typeName, false, lineNum, AssetImportEnum.TYPE.getIndex());

            // brand & model  自动添加
            String brandName = dataMap.get(AssetImportEnum.BRAND.getIndex());
            validateUtil.validateRequired(brandName, lineNum, AssetImportEnum.BRAND.getIndex());

            Boolean brandExist = validateUtil.validateRelObjExits("asset_brand", "name", brandName, true, lineNum, AssetImportEnum.BRAND.getIndex());
            if (!brandExist) {
                AssetBrand brand = new AssetBrand();
                brand.setName(brandName);
                saveEntitys.put("brand", brand);
            }
            String modelName = dataMap.get(AssetImportEnum.MODEL.getIndex());
            validateUtil.validateRequired(modelName, lineNum, AssetImportEnum.MODEL.getIndex());

            String dcName = dataMap.get(AssetImportEnum.DC.getIndex());
            validateUtil.validateRequired(dcName, lineNum, AssetImportEnum.DC.getIndex());

            String parentName = dataMap.get(AssetImportEnum.PARENT.getIndex());
            // asset U size 不得小于关联 model usize ;   default model usize = 1
            Integer modelUsize = 1;
            if (!brandExist) {
                // brand 不存在, model 肯定也不在
                AssetModel model = new AssetModel();
                model.setName(modelName);
                saveEntitys.put("model", model);
            } else {
                // model name: 相同 brand 下 model name 唯一，不同 brand 下 model name 可以重复！！！
                AssetBrand brand = brandService.getOne(new LambdaQueryWrapper<AssetBrand>().eq(AssetBrand::getName, brandName));
                AssetModel model = modelService.getOne(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getName, modelName).eq(AssetModel::getBrandId, brand.getId()));
                if (model == null) {
                    model = new AssetModel();
                    model.setBrandId(brand.getId());
                    model.setName(modelName);
                    saveEntitys.put("model", model);
                } else {
                    modelUsize = model.getUsize();
                    if (StrUtil.isEmpty(parentName)) {
                        // 宿主机 校验 model type 和 asset type 是否一致
                        Integer modelTypeId = model.getTypeId();
                        AssetTypeConf typeConf = typeConfService.getOne(new LambdaQueryWrapper<AssetTypeConf>().eq(AssetTypeConf::getName, typeName));
                        if (typeConf != null && !typeConf.getId().equals(modelTypeId))
                            validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_MATCH.setParam(AssetImportEnum.MODEL.getIndex(), AssetImportEnum.TYPE.getIndex()));
                    }
                }
            }

            if (StrUtil.isNotEmpty(parentName)) {
                AssetAsset parentAsset = this.getOne(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getName, parentName));
                if (parentAsset == null) {
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_RELATED_OBJ_NOT_EXIST.setParam(AssetImportEnum.PARENT.getIndex()));
                } else { // 校验是否宿主机支持挂载
                    AssetTypeConf parentAssetTypeConf = typeConfService.getById(parentAsset.getTypeId());
                    if (parentAssetTypeConf.getVmh().equals(0))
                        validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_SUPPORT.setParam(AssetImportEnum.PARENT.getIndex()));
                }
                // 校验当前填写 asset 是否可以选择宿主机
                AssetTypeConf typeConf = typeConfService.getOne(new LambdaQueryWrapper<AssetTypeConf>().eq(AssetTypeConf::getName, typeName));
                if (typeConf != null && typeConf.getVm().equals(0))
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_SUPPORT.setParam(AssetImportEnum.PARENT.getIndex()));
            }

            String cabinetName = dataMap.get(AssetImportEnum.CABINET.getIndex());
            Integer start = null, end = null;
            if (StrUtil.isEmpty(parentName)) {
                Boolean dcExist = validateUtil.validateRelObjExits("dc", "name", dcName, true, lineNum, AssetImportEnum.DC.getIndex());
                if (!dcExist) {
                    Dc dc = new Dc();
                    dc.setName(dcName);
                    saveEntitys.put("dc", dc);
                }

                // 虚拟机必须选择宿主
                AssetTypeConf typeConf = typeConfService.getOne(new LambdaQueryWrapper<AssetTypeConf>().eq(AssetTypeConf::getName, typeName));
                if (typeConf != null && typeConf.getVm().equals(1))
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_SUPPORT.setParam(AssetImportEnum.PARENT.getIndex()));

                // 当前是 宿主机，校验 cabinet start end
                if (StrUtil.isNotEmpty(cabinetName)) {
                    String cabinetStart = dataMap.get(AssetImportEnum.U_START.getIndex());
                    String cabinetEnd = dataMap.get(AssetImportEnum.U_END.getIndex());

                    if (StrUtil.isAllNotEmpty(cabinetStart, cabinetEnd)) {
                        start = validateUtil.validateInteger(cabinetStart, lineNum, AssetImportEnum.U_START.getIndex());
                        end = validateUtil.validateInteger(cabinetEnd, lineNum, AssetImportEnum.U_END.getIndex());
                        end = end != null ? end + 1 : null; // cabinet end 处理
                    }

                    if (dcExist) {
                        Dc dc = dcService.getOne(new LambdaQueryWrapper<Dc>().eq(Dc::getName, dcName));
                        Cabinet cabinet = this.validateCabinet(cabinetName, dc.getId(), defaultCabinetUsize, saveEntitys);
                        if (Tool.ObjectUtil.isAllNotEmpty(start, end)) {
                            Integer cabinetUsize = cabinet.getuSize();
                            if (start < 0 || end < 0 || start >= end || start > cabinetUsize || end > cabinetUsize + 1) {
                                validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(AssetImportEnum.U_START.getIndex()));
                            } else if ((end - start) < modelUsize) {
                                validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(AssetImportEnum.U_START.getIndex()));
                            } else {
                                boolean flag = this.validateUSizeForImport(start, end, cabinet, assetName, StrUtil.equals("1", existed));
                                if (!flag)
                                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(AssetImportEnum.U_START.getIndex()));
                            }
                        }
                    } else {
                        // dc 不存在, cabinet 肯定也不在
                        Cabinet cabinet = new Cabinet();
                        cabinet.setName(cabinetName);
                        saveEntitys.put("cabinet", cabinet);
                        if (Tool.ObjectUtil.isAllNotEmpty(start, end)) {
                            if (start < 0 || end < 0 || start >= end || start > defaultCabinetUsize || end > defaultCabinetUsize + 1) {
                                validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(AssetImportEnum.U_START.getIndex()));
                            } else if ((end - start) < modelUsize) {
                                validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(AssetImportEnum.U_START.getIndex()));
                            }
                        }
                    }
                }
            }

            String purchaseDate = dataMap.get(AssetImportEnum.PURCHASE_DATE.getIndex());
            validateUtil.validateDateFormat(purchaseDate, "yyyy-MM-dd", lineNum, AssetImportEnum.PURCHASE_DATE.getIndex());

            String fieldsStr = dataMap.get(AssetImportEnum.TAG.getIndex());
            if (StrUtil.isNotEmpty(fieldsStr) && validateUtil.validateJsonArray(fieldsStr, lineNum, AssetImportEnum.TAG.getIndex())) {
                saveEntitys.put("fields", this.getFieldsByFieldsStr(fieldsStr, params.get("seq").toString()));
            }

            String authProtocolPort = dataMap.get(AssetImportEnum.AUTH_PROTOCOL_PORT.getIndex());
            validateUtil.validatePort(authProtocolPort, lineNum, AssetImportEnum.AUTH_PROTOCOL_PORT.getIndex());

            String authType = dataMap.get(AssetImportEnum.AUTH_TYPE.getIndex());
            validateUtil.validateEnumValArr(authType, Tool.ListUtil.of("1", "2"), lineNum, AssetImportEnum.AUTH_TYPE.getIndex());

            String authUsername = dataMap.get(AssetImportEnum.AUTH_USER_NAME.getIndex());
            String authPin = dataMap.get(AssetImportEnum.AUTH_PIN.getIndex());
            String authPriKey = dataMap.get(AssetImportEnum.AUTH_PRI_KEY.getIndex());
            String authUserTip = dataMap.get(AssetImportEnum.AUTH_USER_TIP.getIndex());
            String authPinTip = dataMap.get(AssetImportEnum.AUTH_PIN_TIP.getIndex());
            if (StrUtil.equals("1", authType)) {
                validateUtil.validateRequired(authUsername, lineNum, AssetImportEnum.AUTH_USER_NAME.getIndex());
                validateUtil.validateRequired(authPin, lineNum, AssetImportEnum.AUTH_PIN.getIndex());
            } else if (StrUtil.equals("2", authType)) {
                validateUtil.validateRequired(authPriKey, lineNum, AssetImportEnum.AUTH_PRI_KEY.getIndex());
            }

            String snmpCredentialName = dataMap.get(AssetImportEnum.SNMP_CREDENTIAL.getIndex());
            if (StrUtil.isNotEmpty(snmpCredentialName)) {
                validateUtil.validateRelObjExits("snmp_credential", "name", snmpCredentialName, false, lineNum, AssetImportEnum.SNMP_CREDENTIAL.getIndex());
            }
            String clientUser = dataMap.get(AssetImportEnum.CLIENT_USER.getIndex());
            String clientPin = dataMap.get(AssetImportEnum.CLIENT_PIN.getIndex());
            String clientToken = dataMap.get(AssetImportEnum.CLIENT_TOKEN.getIndex());

            String clientPort = dataMap.get(AssetImportEnum.CLIENT_PORT.getIndex());
            validateUtil.validatePort(clientPort, lineNum, AssetImportEnum.CLIENT_PORT.getIndex());

            String clientEnable = dataMap.get(AssetImportEnum.CLIENT_ENABLE.getIndex());
            validateUtil.validateEnumValArr(clientEnable, ListUtil.of("0", "1"), lineNum, AssetImportEnum.CLIENT_ENABLE.getIndex());
            if (StrUtil.equals("1", clientEnable)) {
                validateUtil.validateRequired(clientToken, lineNum, AssetImportEnum.CLIENT_TOKEN.getIndex());
            }

            // 遇到错误是否继续导入，可选值： 0：不继续    1：继续    默认值：1
            String ignoreError = Tool.ObjectUtil.defaultIfEmpty((String) params.get("ignoreError"), "1");

            List<Map<String, String>> errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
            if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
                // 结束本次导入，自导入行开始 到结尾数据行，提示错误信息
                for (int j = i; j < handleImportData.size(); j++) {
                    validateUtil.recordImportAttrErr(j + 1, RCode.IMPORT_ERROR_STOP);
                    failContentList.add(handleImportData.get(j));
                }
                break;
            }

            if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
                AssetTypeConf typeConf = typeConfService.getOne(new LambdaQueryWrapper<AssetTypeConf>().eq(AssetTypeConf::getName, typeName));
                AssetStateConf stateConf = stateConfService.getOne(new LambdaQueryWrapper<AssetStateConf>().eq(AssetStateConf::getName, stateName));
                saveEntitys.put("typeConf", typeConf);

                AssetAsset asset = new AssetAsset();
                asset.setName(assetName);
                asset.setSn(sn);
                asset.setTypeId(typeConf.getId());
                asset.setStateId(stateConf.getId());
                asset.setPurchaseDate(StringUtils.isEmpty(purchaseDate) ? null : purchaseDate);
                asset.setCreateAt(new Date());
                asset.setCreateBy(ShiroUtils.getUserId());
                asset.setManageIp(manageIp);
                asset.setIpmiIp(ipmiIp);
                if (StrUtil.isNotEmpty(snmpCredentialName)) {
                    SnmpCredential snmpCredential = snmpCredentialService.getOne(new LambdaQueryWrapper<SnmpCredential>().eq(SnmpCredential::getName, snmpCredentialName));
                    asset.setSnmpCredentialId(snmpCredential.getId());
                }
                asset.setCabinetStart(start);
                asset.setCabinetEnd(end);
                asset.setAuthProtocolPort(StringUtils.isNotEmpty(authProtocolPort) ? Integer.valueOf(authProtocolPort) : null);
                asset.setAuthType(StringUtils.isNotEmpty(authType) ? Integer.valueOf(authType) : null);
                asset.setAuthUsername(authUsername);
                asset.setAuthPin(StringUtils.isNotEmpty(authPin) ? Base64.getEncoder().encodeToString(authPin.getBytes()) : null);
                asset.setAuthPriKey(StrUtil.isEmpty(authPriKey) ? null : authPriKey);
                asset.setAuthUserTip(authUserTip);
                asset.setAuthPinTip(authPinTip);
                if (StrUtil.isEmpty(parentName)) {
                    asset.setPid(-1);
                    if (saveEntitys.get("dc") == null) {
                        Dc dc = dcService.getOne(new LambdaQueryWrapper<Dc>().eq(Dc::getName, dcName));
                        asset.setDcId(dc.getId());
                    }
                    if (BooleanUtil.and(StrUtil.isNotEmpty(cabinetName), saveEntitys.get("cabinet") == null)) {
                        Dc dc = dcService.getOne(new LambdaQueryWrapper<Dc>().eq(Dc::getName, dcName));
                        Cabinet cabinet = cabinetService.getOne(new LambdaQueryWrapper<Cabinet>().eq(Cabinet::getName, cabinetName).eq(Cabinet::getDcId, dc.getId()));
                        asset.setCabinetId(cabinet != null ? cabinet.getId() : null);
                    }
                    if (saveEntitys.get("brand") == null) {
                        AssetBrand brand = brandService.getOne(new LambdaQueryWrapper<AssetBrand>().eq(AssetBrand::getName, brandName));
                        asset.setBrandId(brand.getId());
                    }
                    if (saveEntitys.get("model") == null) {
                        AssetBrand brand = brandService.getOne(new LambdaQueryWrapper<AssetBrand>().eq(AssetBrand::getName, brandName));
                        AssetModel model = modelService.getOne(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getName, modelName).eq(AssetModel::getBrandId, brand.getId()));
                        asset.setModelId(model.getId());
                    }
                } else {
                    AssetAsset parentAsset = this.getOne(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getName, parentName));
                    asset.setPid(parentAsset.getId());
                    asset.setDcId(parentAsset.getDcId());
                    asset.setCabinetId(parentAsset.getCabinetId());
                    asset.setCabinetStart(parentAsset.getCabinetStart());
                    asset.setCabinetEnd(parentAsset.getCabinetEnd());
                    asset.setBrandId(parentAsset.getBrandId());
                    asset.setModelId(parentAsset.getModelId());
                }
                asset.setClientUser(ObjectUtil.defaultIfEmpty(clientUser, null));
                asset.setClientPin(ObjectUtil.defaultIfEmpty(clientPin, null));
                asset.setClientAddress(asset.getManageIp());
                asset.setClientPort(StringUtils.isNotEmpty(clientPort) ? Integer.valueOf(clientPort) : null);
                asset.setClientToken(clientToken);
                asset.setClientEnable(Integer.valueOf(StrUtil.emptyToDefault(clientEnable, "0")));
                saveEntitys.put("asset", asset);
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(new DefaultTransactionDefinition());
                try {
                    this.saveOrUpdateRowData(saveEntitys, params);
                    dataSourceTransactionManager.commit(transactionStatus);
                    // save or update
                    Object saveFlag = params.get("save");
                    if (ObjectUtil.isNotNull(saveFlag)) {
                        addNum++;
                    } else {
                        updateNum++;
                    }
                } catch (Exception e) {
                    failContentList.add(dataMap);
                    dataSourceTransactionManager.rollback(transactionStatus);
                    validateUtil.recordImportAttrErr(lineNum, RCode.ERROR);
                    logger.error("Asset import roll back the transaction failure", e);
                }
            } else {
                failContentList.add(dataMap);
            }
        }
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_ASSET_ASSET_EXPORT_HEADER);
        String failedContentFileBase64Str = basicImportAndExportServices.getFailedContentFileBase64Str(failContentList, sysHeaderJsonStr, Constant.ImportFileType.EXCEL.getValue());
        //  返回统计信息
        Map<Object, Object> statMap = Tool.MapUtil.builder()
                .put("seq", seq)
                .put("totalNum", handleImportData.size())
                .put("successNum", addNum + updateNum)
                .put("failNum", failContentList.size())
                .put("addNum", addNum)
                .put("updateNum", updateNum)
                .put("failDetail", validateUtil.getImportErrMsg())
                .put("failContent", failedContentFileBase64Str)
                .build();
        return statMap;
    }

    private List<AssetFieldValue> getFieldsByFieldsStr(String fieldsStr, String seq) {
        List<AssetFieldValue> valueList = new ArrayList<>();

        JSONArray jsonArray = JSONUtil.parseArray(fieldsStr);
        for (int i = 0; i < jsonArray.size(); i++) {
            cn.hutool.json.JSONObject fieldMetaObject = jsonArray.getJSONObject(i);
            String metaKey = fieldMetaObject.getStr("metaKey");
            AssetFieldMeta meta = assetFieldMetaService.getOne(new LambdaQueryWrapper<AssetFieldMeta>().eq(AssetFieldMeta::getMetaKey, metaKey));
            if (meta == null) {
                meta = new AssetFieldMeta();
                meta.setMetaKey(metaKey);
                meta.setName(fieldMetaObject.getStr("name", metaKey));
                meta.setGroupId(fieldMetaObject.getStr("groupId", "1"));
                meta.setParam(fieldMetaObject.getStr("param", "{}"));
                meta.setRemark(fieldMetaObject.getStr("remark", ""));
                meta.setType(fieldMetaObject.getStr("type", null));
                meta.setSearch(fieldMetaObject.getInt("search", 0));
                meta.setDisplay(fieldMetaObject.getInt("display", 0));
                meta.setSeq(seq);
                assetFieldMetaService.save(meta);
            }
            JSONArray fieldValueArr = fieldMetaObject.getJSONArray("value");
            for(Object fieldValue : fieldValueArr){
                AssetFieldValue value = new AssetFieldValue();
                value.setMetaId(meta.getId());
                value.setValue(ToolUtil.toStr(fieldValue));
                value.setSeq(seq);
                valueList.add(value);
            }
        }
        return valueList;
    }

    private Cabinet validateCabinet(String cabinetName, Integer dcId, Integer defaultCabinetUsize, Map saveEntitys) {
        // 返回机柜信息中包含所属的资产 起始位置 数据
        List<Cabinet> cabinetList = cabinetService.getCabinetWithAssetPositionInfo();
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
            saveEntitys.put("cabinet", cabinet);
        }
        return cabinet;
    }

    /**
     * 保存这一行内的需要创建的对象
     *
     * @param saveEntitys
     * @param params
     */
    public void saveOrUpdateRowData(Map saveEntitys, Map<String, Object> params) {
        MapUtil.removeAny(params, "save", "update");
        String seq = params.get("seq").toString();

        Dc dc = (Dc) saveEntitys.get("dc");
        Cabinet cabinet = (Cabinet) saveEntitys.get("cabinet");
        String defaultCabinetUsizeStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_DEFAULT_CABINETU);
        Integer defaultCabinetUsize = Integer.parseInt(defaultCabinetUsizeStr);
        if (dc == null && cabinet != null) {
            cabinet.setSeq(seq);
            cabinet.setuSize(defaultCabinetUsize);
            cabinetService.save(cabinet);
        }
        if (dc != null) {
            String mapCenterConfigJsonStr = sysConfigService.getValue("map_center_config");
            Map<String, Object> map = JSONObject.parseObject(mapCenterConfigJsonStr, Map.class);
            String longitude = (String) map.get("longitude");
            String latitude = (String) map.get("latitude");
            dc.setLongitude(StrUtil.isNotEmpty(longitude) ? Double.valueOf(longitude) : null);
            dc.setLatitude(StrUtil.isNotEmpty(latitude) ? Double.valueOf(latitude) : null);
            dc.setX(1);
            dc.setY(1);
            dc.setState(Constant.SWITCH_OPEN.toUpperCase());
            dc.setSeq(seq);
            dcService.save(dc);
            if (cabinet != null) {
                cabinet.setDcId(dc.getId());
                cabinet.setSeq(seq);
                cabinet.setuSize(defaultCabinetUsize);
                cabinetService.save(cabinet);
            }
        }

        AssetBrand brand = (AssetBrand) saveEntitys.get("brand");
        AssetModel model = (AssetModel) saveEntitys.get("model");
        AssetTypeConf typeConf = (AssetTypeConf) saveEntitys.get("typeConf");

        if (brand == null && model != null) {
            model.setSeq(seq);
            model.setTypeId(typeConf.getId());
            modelService.save(model);
        }
        if (brand != null) {
            brand.setSeq(seq);
            brandService.save(brand);
            if (model != null) {
                model.setBrandId(brand.getId());
                model.setSeq(seq);
                model.setTypeId(typeConf.getId());
                modelService.save(model);
            }
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
        asset.setSeq(seq);

        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore   在此只需要处理更新即可
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");
        AssetAsset oldAsset = this.getOne(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getName, asset.getName()));
        if (StrUtil.equals("1", existed) && oldAsset != null) {
            params.put("update", true);
            // 更新 & 同步
            asset.setId(oldAsset.getId());
            if (asset.getPid() == null || asset.getPid().equals(-1)) {
                // 宿主机
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
            }
            asset.setUpdateAt(new Date());
            asset.setUpdateBy(ShiroUtils.getUserId());
            this.updateById(asset);

            // 如果修改了Model, 处理关联 charts 变化
            if (!oldAsset.getModelId().equals(asset.getModelId())) {
                this.updateModelCharts(asset);
            }

            // 修改后处理 ping & Talon 数据
            this.afterUpdateAsset(asset.getId());

            // 是否同步更新关联的dashboard，可选值： 0：不同步  1：同步  默认值：1
            String syncDashboard = Tool.ObjectUtil.defaultIfEmpty((String) params.get("syncDashboard"), "1");
            if (StrUtil.equals("1", syncDashboard)) {
                // asset info charts && dashboard charts
                List<VisualChart> assetChartList = chartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getVarType, Constant.ChartVariableType.ASSET.getValue()).eq(VisualChart::getVarId, asset.getId()));
                if (Tool.CollUtil.isNotEmpty(assetChartList)) {
                    List<Integer> assetChartIdList = assetChartList.stream().map(VisualChart::getId).collect(Collectors.toList());
                    Map<String, Object> map = Tool.MapUtil.newHashMap(2);
                    map.put("ids", assetChartIdList);
                    chartService.syncTmpl(map);
                }
            }
            // 是否同步更新关联的endpoint，可选值：  0：不同步  1：同步  默认值：1
            String syncEndpoint = Tool.ObjectUtil.defaultIfEmpty((String) params.get("syncEndpoint"), "1");
            if (StrUtil.equals("1", syncEndpoint)) {
                List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getAssetId, asset.getId()));
                if (Tool.CollUtil.isNotEmpty(endpointList)) {
                    monitorEndpointService.syncTmpl(Collections.emptyList(), Collections.emptyList(), ListUtil.of(asset.getId()));
                }
            }
        } else {
            params.put("save", true);
            this.save(asset);
            // asset panel & info chart
            this.saveAssetInfoPanel(asset);
            // 通过 model chart tepmlate 新增 asset charts
            this.saveAssetChartsViaModelChartTmpl(asset);
        }

        // 保存 默认 VSYS 和 Asset 关联关系
        this.saveOrUpdateDefaultVsysAssetRel(asset.getId(), asset.getModelId());

        // 如果没有指定 meta, 则视为删除，先删后增
        assetFieldValueService.remove(new LambdaQueryWrapper<AssetFieldValue>().eq(AssetFieldValue::getAssetId, asset.getId()));
        List<AssetFieldValue> fields = (List<AssetFieldValue>) saveEntitys.get("fields");
        if (CollectionUtils.isNotEmpty(fields)) {
            fields.forEach(assetFieldValue -> assetFieldValue.setAssetId(asset.getId()));
            assetFieldValueService.saveBatch(fields);
        }
    }

    private List<Map<Integer, String>> handleImportData(List<Map<Integer, String>> importDataList) {
        List<Map<Integer, String>> list = importDataList.stream().filter(dataMap -> {
            String parentName = dataMap.get(2);
            if (StringUtils.isEmpty(parentName)) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        importDataList.removeAll(list);
        list.addAll(importDataList);
        return list;
    }

    private List<List<String>> handleExportDataList(List<AssetAsset> list) {
        List<List<String>> exportDataList = new ArrayList<>();
        for (AssetAsset asset : list) {
            List<String> tempList = new ArrayList<>();

            tempList.add(asset.getName());
            tempList.add(asset.getSn());
            tempList.add(ToolUtil.isEmpty(asset.getParent()) ? "" : asset.getParent().getName());
            tempList.add(asset.getManageIp());
            tempList.add(asset.getIpmiIp());
            tempList.add(asset.getBrand().getName());
            tempList.add(asset.getModel().getName());
            tempList.add(asset.getDc().getName());
            tempList.add(ToolUtil.isEmpty(asset.getCabinet()) ? "" : asset.getCabinet().getName());
            tempList.add(ToolUtil.isEmpty(asset.getCabinetStart()) ? "" : String.valueOf(asset.getCabinetStart()));
            tempList.add(ToolUtil.isEmpty(asset.getCabinetEnd()) ? "" : String.valueOf(asset.getCabinetEnd()));
            tempList.add(asset.getState().getName());
            tempList.add(asset.getType().getName());
            tempList.add(asset.getPurchaseDate());
            if (CollectionUtils.isNotEmpty(asset.getFields())) {
                tempList.add(JSONUtil.toJsonStr(asset.getFields()));
            } else {
                tempList.add(StrUtil.EMPTY);
            }

            tempList.add(ToolUtil.isEmpty(asset.getAuthProtocolPort()) ? "" : String.valueOf(asset.getAuthProtocolPort()));
            tempList.add(ToolUtil.isEmpty(asset.getAuthType()) ? "" : String.valueOf(asset.getAuthType()));
            tempList.add(ToolUtil.isEmpty(asset.getAuthUsername()) ? "" : asset.getAuthUsername());
            // asset.getAuthPin()
            tempList.add("");
            // asset.getAuthPriKey()
            tempList.add("");
            tempList.add(ToolUtil.isEmpty(asset.getAuthUserTip()) ? "" : asset.getAuthUserTip());
            tempList.add(ToolUtil.isEmpty(asset.getAuthPinTip()) ? "" : asset.getAuthPinTip());

            String snmpCredentialName = "";
            if (ToolUtil.isNotEmpty(asset.getSnmpCredentialId())) {
                SnmpCredential snmpCredential = snmpCredentialService.getById(asset.getSnmpCredentialId());
                snmpCredentialName = snmpCredential == null ? "" : snmpCredential.getName();
            }
            tempList.add(snmpCredentialName);

            tempList.add(ObjectUtil.defaultIfNull(asset.getClientUser(), ""));
            // client pin
            tempList.add(StrUtil.EMPTY);
            tempList.add(ObjectUtil.defaultIfNull(asset.getClientToken(), ""));
            tempList.add(ObjectUtil.defaultIfNull(asset.getClientPort(), "").toString());
            tempList.add(ObjectUtil.defaultIfNull(asset.getClientEnable(), "").toString());
            exportDataList.add(tempList);
        }
        return exportDataList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bulkEdit(Map<String, Object> params) {
        ValidateUtils.is(params.get("editType")).notNull(RCode.ASSET_EDITTYPE_ISNULL)
                .and(params.get("ids")).notNull(RCode.ASSET_ID_ISNULL);
        Integer editType = (Integer) params.get("editType");
        List<Integer> ids = (List<Integer>) params.get("ids");

        switch (editType) {
            // 1 account 2 label 3 state 4 snmp credentials
            case 1:
                Integer authType = (Integer) params.get("authType");
                String authUsername = (String) params.get("authUsername");
                String authPin = (String) params.get("authPin");
                if (StrUtil.isNotBlank(authPin)) {
                    params.put("authPin", Base64.getEncoder().encodeToString(authPin.getBytes()));
                }
                String authPriKey = (String) params.get("authPriKey");
                if (ObjectUtil.equal(authType, 1)) {
                    ValidateUtils.is(authUsername).notNull(RCode.ASSET_AUTHUSERNAME_ISNULL)
                            .and(authPin).notNull(RCode.ASSET_AUTHPIN_ISNULL);
                } else if (ObjectUtil.equal(authType, 2)) {
                    ValidateUtils.is(authPriKey).notNull(RCode.ASSET_AUTHPRIKEY_ISNULL);
                }
                this.baseMapper.updateAssetStateBatch(ids, params);
                break;
            case 2:
                List<Map<String, Object>> object = (List<Map<String, Object>>) params.get("fields");
                if (ToolUtil.isEmpty(object)) {
                    break;
                }
                // 将标签操作区分 批量删除还是批量新增或者更新
                List<Integer> removeFieldIds = new ArrayList<Integer>();
                List<AssetFieldValue> addOrUpdateFields = new ArrayList<AssetFieldValue>();
                for (Map<String, Object> map : object) {
                    Integer action = (Integer) map.get("action");
                    if (ToolUtil.equals(action, 1)) {
                        removeFieldIds.add((Integer) map.get("id"));
                    } else {
                        List<AssetFieldValue> assetFieldValues = assetFieldValueService.list(new QueryWrapper<AssetFieldValue>().lambda()
                                .eq(AssetFieldValue::getMetaId, map.get("id"))
                                .in(AssetFieldValue::getAssetId, ids));
                        // 分拣出更新数据和新增数据
                        List<Integer> assetIds = assetFieldValues.stream().map(AssetFieldValue::getAssetId).collect(Collectors.toList());
                        List<Integer> addIds = ids.stream().filter(item -> !assetIds.contains(item)).collect(Collectors.toList());

                        for (AssetFieldValue assetFieldValue : assetFieldValues) {
                            assetFieldValue.setValue((String) map.get("value"));
                            addOrUpdateFields.add(assetFieldValue);
                        }

                        for (Integer id : addIds) {
                            AssetFieldValue data = new AssetFieldValue();
                            data.setAssetId(id);
                            data.setMetaId((Integer) map.get("id"));
                            data.setValue((String) map.get("value"));
                            addOrUpdateFields.add(data);
                        }
                    }
                }

                if (ToolUtil.isNotEmpty(removeFieldIds)) {
                    assetFieldValueService.remove(new QueryWrapper<AssetFieldValue>()
                            .lambda().in(AssetFieldValue::getMetaId, removeFieldIds)
                            .in(AssetFieldValue::getAssetId, ids));
                }

                if (ToolUtil.isNotEmpty(addOrUpdateFields)) {
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

    @Override
    public AssetAsset getAssetByIP(String ip) {
        if (Tool.StrUtil.isBlank(ip)) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getManageIp, ip));
    }

    @Override
    public List<MonitorEndpoint> getEndpointListByAssetId(Integer assetId) {
        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL);
        List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getAssetId, assetId));
        return endpointList;
    }

    @Override
    public List<MonitorModule> getModuleListByAssetId(Integer assetId) {
        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL);
        List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getAssetId, assetId));
        if (Tool.CollectionUtil.isEmpty(endpointList)) return Collections.emptyList();

        List<Integer> moduleIds = endpointList.stream().map(MonitorEndpoint::getModuleId).distinct().collect(Collectors.toList());
        List<MonitorModule> moduleList = moduleService.list(new LambdaQueryWrapper<MonitorModule>().in(MonitorModule::getId, moduleIds));
        return moduleList;
    }

    @Override
    public List<MonitorProject> getProjectListByAssetId(Integer assetId) {
        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL);
        List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getAssetId, assetId));
        if (Tool.CollectionUtil.isEmpty(endpointList)) return Collections.emptyList();

        List<Integer> moduleIds = endpointList.stream().map(MonitorEndpoint::getModuleId).distinct().collect(Collectors.toList());
        List<MonitorModule> moduleList = moduleService.list(new LambdaQueryWrapper<MonitorModule>().in(MonitorModule::getId, moduleIds));

        List<Integer> projectIds = moduleList.stream().map(MonitorModule::getProjectId).distinct().collect(Collectors.toList());
        List<MonitorProject> projectList = projectService.list(new LambdaQueryWrapper<MonitorProject>().in(MonitorProject::getId, projectIds));
        return projectList;
    }

    @Override
    public List<AssetAsset> getTsgAssetListbyModel() {
        List<AssetAsset> tsgAssetList = this.baseMapper.getTsgAssetListbyModel();
        return tsgAssetList;
    }
}
