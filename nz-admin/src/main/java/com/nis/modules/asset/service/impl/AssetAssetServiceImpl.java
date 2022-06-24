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
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
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
import org.apache.commons.collections.CollectionUtils;
import org.snmp4j.smi.TimeTicks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    private AssetTypeConfService typeConfService;

    @Autowired
    private AssetStateConfService stateConfService;

    @Autowired
    private AssetFieldMetaService assetFieldMetaService;

    @Autowired
    private AssetFieldValueService assetFieldValueService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private StatService statService;

    @Autowired
    private AssetPingService assetPingService;

    @Autowired
    private MonitorEndpointStateService monitorEndpointStateService;

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

	@Autowired
	private SnmpCredentialService snmpCredentialService;

	@Autowired
    private VisualChartService chartService;

	@Autowired
    private VisualPanelService panelService;

	@Autowired
    private VisualChartElementService chartElementService;

	@Autowired
    private MonitorModuleService moduleService;

	@Autowired
    private MonitorProjectService projectService;

	@Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;
    
    @Autowired
    private AssetTalonStatusService assetTalonStatusService;

    @Autowired
    private SysUserStarredService starredService;

    @SuppressWarnings("unchecked")
    @Override
    public AssetAsset queryAssetInfo(Integer id) {
        AssetAsset asset = this.getById(id);
        if (ToolUtil.isEmpty(asset)) {
            throw new NZException(RCode.ASSET_NOT_EXIST);
        }
        AssetTypeConf type = typeConfService.getById(asset.getTypeId());
        asset.setType(type);

        if(type.getVmh()==1) {
        	List<AssetAsset> children = this.list(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getPid, id));
        	if(ToolUtil.isNotEmpty(children)) {
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
        if(ToolUtil.isNotEmpty(object)) {
        	assetPing = JSON.parseObject((String)object,AssetPing.class);
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

        //收藏标识 0：未收藏 1：已收藏
        int count = starredService.count(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getTid,asset.getId())
                .eq(SysUserStarredEntity::getType,"asset")
                .eq(SysUserStarredEntity::getUid,ShiroUtils.getUserId())
        );
        asset.setStarred(count == 0 ? 0 : 1);
        // 已配置的 module 名单
        asset.setModules(moduleService.queryAssetMoudules(asset.getId()));
        return asset;
    }

    private void handleQueryAssetParam(Map<String, Object> params) {
    	String ids = (String) params.get("ids");
        if(StrUtil.isNotBlank(ids)) {
        	List<String> listIds = Arrays.asList(ids.split(","));
        	params.put("ids",listIds);
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
    }

    @Override
    public PageUtils queryAssetPage(Map<String, Object> params) {
        // 处理请求参数
        this.handleQueryAssetParam(params);

        //当前用户id
        params.put("userId",ShiroUtils.getUserId().intValue());

        IPage page = new Query(AssetAsset.class).getPage(params);
        List<AssetAsset> assetList = assetDao.queryAssetList(page, params);

        // 查询结果不为空时处理返回结果
        if (CollectionUtils.isNotEmpty(assetList)) {
            this.handleResuleList(assetList);
        }

        page.setRecords(assetList);
        return new PageUtils(page);
    }

    @Override
    public Map<String,List> queryAssetGroupBy(Map<String, Object> params){
    	Map<String,List> result = new HashMap<String, List>();
    	QueryWrapper wrapper = new QueryWrapper().select("id","name");
		wrapper.orderByAsc("name");
    	//dc
    	List<Map<String, Object>> dcList = dcService.listMaps(wrapper);
    	List<Map<String,Object>> dcGroupList = assetDao.queryAssetGroupBy("asset.dc_id", params);
    	this.assetGroupBysetNum(dcList, dcGroupList);
    	result.put("dc", dcList);
    	//type
    	List<Map<String, Object>> typeList = typeConfService.listMaps(wrapper);
    	List<Map<String,Object>> typeGroupList = assetDao.queryAssetGroupBy("asset.type_id", params);
    	this.assetGroupBysetNum(typeList, typeGroupList);
    	result.put("type", typeList);
    	//model
    	List<AssetBrand> brandList = brandService.list();
    	Map<String,AssetBrand> brandMap = Tool.MapUtil.newHashMap();
    	brandList.forEach(brand -> {
    		brandMap.put(brand.getId().toString(), brand);
    	});
    	QueryWrapper modelWrapper = new QueryWrapper().select("id","name","brand_id");
    	modelWrapper.orderByAsc("name");
    	List<Map<String, Object>> modelList = modelService.listMaps(modelWrapper);
    	modelList.forEach(model -> {
    		AssetBrand brand = brandMap.get(String.valueOf(model.get("brand_id")));
    		model.put("brand", brand);
    	});
    	List<Map<String,Object>> modelGroupList = assetDao.queryAssetGroupBy("asset.model_id", params);
    	this.assetGroupBysetNum(modelList, modelGroupList);
    	result.put("model", modelList);
    	//ping
    	Map<String, Object> upMap = Tool.MapUtil.newHashMap();
    	upMap.put("id", 1);
    	upMap.put("name", "up");
    	Map<String, Object> downMap = Tool.MapUtil.newHashMap();
    	downMap.put("id", 0);
    	downMap.put("name", "down");
    	List<Map<String, Object>> pingList = Tool.ListUtil.toList(upMap,downMap);
    	List<Map<String,Object>> pingGroupList = assetDao.queryAssetGroupBy("assp.status", params);
    	this.assetGroupBysetNum(pingList, pingGroupList);
    	result.put("ping", pingList);
    	//label
    	List<Map<String,Object>> metaGroupList = assetDao.queryAssetGroupBy("fvm.meta_id", params);
    	metaGroupList = metaGroupList.stream().filter(map -> map.get("id") != null).collect(Collectors.toList());
    	result.put("meta", metaGroupList);
		return result;
    }
    
    private void assetGroupBysetNum(List<Map<String,Object>> allList,List<Map<String,Object>> groupList) {
    	if(Tool.CollUtil.isEmpty(groupList)) {
    		return;
    	}
    	Map<String,Object> groupMap = new HashMap<String, Object>();
    	for(Map<String,Object> m : groupList) {
    		groupMap.put(String.valueOf(m.get("id")), m.get("num"));
    	}
    	for(Map<String,Object> m : allList) {
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
            if(ToolUtil.isNotEmpty(children)) {
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
            if(ToolUtil.isNotEmpty(object)) {
            	assetPing = JSON.parseObject((String)object,AssetPing.class);
            }
            asset.setPingInfo(assetPing);


            // clientstate
//            if (ToolUtil.isNotEmpty(asset.getClientState())) {
//                switch (asset.getClientState()) {
//                    case 1:// ok
//                        break;
//                    case 3: { // 认证失败
//                        asset.setClientState(-1);
//                        break;
//                    }
//                    case 2:  // 超时
//                    case 4: {// 未知错误
//                        asset.setClientState(0); // 认证失败
//                        break;
//                    }
//                    default:
//                        asset.setClientState(null);
//                }
//            }
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

        // account
       /* List<Integer> accountIds = asset.getAccounts();
        if (CollectionUtils.isNotEmpty(accountIds)) {
            List<AssetAccount> assetAccounts = accountService.list();
            List<Integer> idList = assetAccounts.stream().map(AssetAccount::getId).collect(Collectors.toList());

            List<Integer> templateAccountIds = new ArrayList<>(accountIds);
            templateAccountIds.removeAll(idList);
            if (CollectionUtils.isNotEmpty(templateAccountIds)) {
                throw new NZException("The account information was not found. Details: " + templateAccountIds.toString(), RCode.ASSET_ACCOUNT_NOT_FOUND.getCode());
            }
        }*/

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
        if(StrUtil.isNotBlank(authPin)) {
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
        this.validateAssetInfo(asset);

        // 设置属性值
        asset.setCreateAt(new Date());
        asset.setCreateBy(ShiroUtils.getUserId());
        // cabinet end 处理
        if (asset.getCabinetEnd() != null && asset.getCabinetStart() != null) {
            asset.setCabinetEnd(asset.getCabinetEnd() + 1);
        }

        if (asset.getPid() != null && asset.getPid()!=-1) {
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

        // 新增 asset 时，新增 asset panel chart /
        this.saveAssetChartsWhenInsert(asset);

        // 新增 asset 时，新增 关联 Model charts
        this.saveAssetModelChartsWhenInsert(asset);
        return asset.getId();
    }

    /**
     * 新增 asset 时，新增 asset panel chart
     * @param asset
     */
    private void saveAssetChartsWhenInsert(AssetAsset asset) {
        // 配置信息获取
        SysConfigEntity assetChartConfig = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_CHART_TPL));
        // asset config
        VisualChart chartTemplate = JSONObject.parseObject(assetChartConfig.getParamValue(), VisualChart.class);
        // 新增时需要新增 panel chart
        chartService.saveRelPanelAndChart(Constant.PanelType.ASSET.getValue(), asset.getId(), null, chartTemplate);
    }

    /**
     * 新增 asset 时，新增 关联 Model charts
     *
     * @param asset
     */
    private void saveAssetModelChartsWhenInsert(AssetAsset asset) {
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

                saveChart.setVarType(1);
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

        // 修改后处理asset ping数据 删除缓存和数据库中的数据
        assetPingService.remove(new QueryWrapper<AssetPing>().lambda().eq(AssetPing::getAssetId, asset.getId()));
        redisTemplate.opsForHash().delete(Constant.SYSCONFIG_KEY_ASSET_PING, asset.getId().toString());

        // 修改后处理asset talon status数据 删除缓存和数据库中的数据
        assetTalonStatusService.removeById(asset.getId());
        redisTemplate.opsForHash().delete(Constant.ASSET_TALON_STATUS_CACHE_KEY, asset.getId().toString());
        return asset.getId();
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

        // 新增新 model 关联的 charts
        this.saveAssetModelChartsWhenInsert(asset);
    }

    private void saveAssetRelData(AssetAsset asset) {
        // fields
        List<AssetFieldMeta> fields = asset.getFields();
        if (CollectionUtils.isNotEmpty(fields)) {
            // 需要保存的 meta id
            List<AssetFieldValue> values = new ArrayList<AssetFieldValue>();
            for (AssetFieldMeta fieldMeta : fields) {
            	for(String value: fieldMeta.getValue()) {
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

        //删除starred
        starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getType,"asset")
                .in(SysUserStarredEntity::getTid,idList)
        );

        // 删除 endponiet
        List<MonitorEndpoint> endpoints = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().in(MonitorEndpoint::getAssetId, idList));
        if (CollectionUtils.isNotEmpty(endpoints)) {
            List<Integer> endpointIds = endpoints.stream().map(MonitorEndpoint::getId).collect(Collectors.toList());
            monitorEndpointService.deleteEndpointByIds(endpointIds);
        }

        // 删除 alert
        alertMessageService.removeByAssetIds(idList);

        // 删除 assetPing
        List<String> deleteList = idList.stream().map(id-> id.toString()).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(delAssetHosts)) {
            assetPingService.remove(new LambdaQueryWrapper<AssetPing>().in(AssetPing::getHost, delAssetHosts));
            // 删除缓存中的数据
            redisTemplate.opsForHash().delete(Constant.SYSCONFIG_KEY_ASSET_PING, deleteList.toArray());
        }

        // 删除 field
        assetFieldValueService.remove(new LambdaQueryWrapper<AssetFieldValue>().in(AssetFieldValue::getAssetId, idList));

        // 删除关联的 panel chart  chartElement信息
        List<VisualPanel> panels = panelService.list(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, Constant.PanelType.ASSET.getValue()).in(VisualPanel::getLink, ids));
        if (CollectionUtils.isNotEmpty(panels)) {
            Set<Integer> panelIds = panels.stream().map(VisualPanel::getId).collect(Collectors.toSet());
            panelService.delPanelsAndChartInfoByPanelIds(panelIds);
        }
        // 删除 关联通过 chart temp 创建的 charts
        chartService.remove(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getVarType, 1).in(VisualChart::getVarId, idList));
        
        // 删除assetTalonStatus中的数据以及缓存中的数据信息
        assetTalonStatusService.removeByIds(idList);
        redisTemplate.opsForHash().delete(Constant.ASSET_TALON_STATUS_CACHE_KEY, deleteList.toArray());
    }

    /*@Override
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
    }*/

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
    	if(ToolUtil.isEmpty(assetType)) {
    		return;
    	}
    	Integer sshCollect = assetType.getSshCollect();
    	Integer snmpCollect = assetType.getSnmpCollect();
    	Integer authProtocol = assetType.getAuthProtocol();
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
    	
    	if(ToolUtil.equals(sshCollect, 1) && ToolUtil.equals(authProtocol, 1) ) {
    		// 进行ssh采集
    		String sshCollectScript = assetType.getSshCollectScript();
    		if(StrUtil.isBlank(sshCollectScript)) {
    			logger.debug("asset sshCollectScript is empty asset info : %s",JSON.toJSON(asset));
    			return;
    		}
    		
    		Session session =null;
			try {
				// 进行ssh连接
				session=SshUtil.getSshConnection(asset);
				String sshCommondResult = RuntimeUtil.sshCommondResult(session," /bin/bash "+sshCollectScript);
				this.getSshInfo(assetFeatureMap, results, sshCommondResult,asset);
				
			} catch (JSchException | IOException e) {
				logger.error(e);
				logger.error("asset feature get data error,assetId : {} info : {}",assetId,e);
				throw new NZException(e.getMessage());
			} finally {
				SshUtil.close(session);
			}
    		
    	}
    	if(ToolUtil.equals(snmpCollect, 1)) {
    		// 进行snmp数据采集
    		try {
    			SnmpCredential snmpCredential = snmpCredentialService.getById(asset.getSnmpCredentialId());
    			if(ToolUtil.isEmpty(snmpCredential)) {
    				logger.debug("asset snmpCredential is empty asset info : %s",JSON.toJSON(asset));
    				return;
    			}
				this.getSystemInfoBySnmp(assetFeatureMap,results,asset,snmpCredential);
			} catch (IOException e) {
				logger.error("asset snmp collect error asset info : %s",JSON.toJSON(asset));
				logger.error(e);
			}
    	}
    	// 存入db 无则新增 有则修改
		if(results!=null&&results.size()>0) {
			assetFeatureService.saveOrUpdateBatch(results);
		}
    }

    public void getSshInfo(HashMap<String,AssetFeature> assetFeatureMap,List<AssetFeature> results,String sshCommondResult,AssetAsset asset) {
    	Map<String,String> sshResult = JSONObject.parseObject(sshCommondResult, Map.class);
    	for(Map.Entry<String, String> entrySet: sshResult.entrySet()) {
    		//查询数据库是否已经存在数据
        	AssetFeature result = assetFeatureMap.get(asset.getId()+entrySet.getKey()+Constant.ACCOUNT_SSH_PROTOCOL);;
        	if(ToolUtil.isEmpty(result)) {
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
    
    public void getSystemInfoBySnmp(HashMap<String,AssetFeature> assetFeatureMap,List<AssetFeature> results,AssetAsset asset,SnmpCredential snmpCredential) throws IOException {
    	Map snmpInfos = JSONObject.parseObject((String) snmpCredential.getConfig(), Map.class);
    	Set<String> keySet = Constant.SNMP_OID_MAP.keySet();
		for(String key:keySet) {
			List<Map> snmpResults = SnmpUtil.snmpWalk(asset.getManageIp(), snmpCredential.getPort(), snmpCredential.getType(), (String)snmpInfos.get("readCommunity"), Constant.SNMP_OID_MAP.get(key), snmpInfos);
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
				result.setUpdateAt(new Date());
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
						//result.setValue(DateUtil.getTime(DateUtil.getUTCTimeByConfigTimeZone(new Date(new Date().getTime()-milliseconds))));
                        result.setValue(DateUtil.getTime(new Date(new Date().getTime() - milliseconds)));
					}else {
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
        Map<String,Set<Integer>> fieldIdAndAssetIdsMap = new HashMap<String,Set<Integer>>();
        Map<Integer,List<AssetFieldMeta>> fieldIdAndfields = new HashMap<Integer,List<AssetFieldMeta>>();
        for(AssetFieldMeta fieldMeta: allMetas) {
        	
        	List<AssetFieldMeta> fields = fieldIdAndfields.get(fieldMeta.getId());
        	if(ToolUtil.isEmpty(fields)) {
        		fields = new ArrayList<AssetFieldMeta>();
        		fields.add(fieldMeta);
        		fieldIdAndfields.put(fieldMeta.getId(), fields);
        	}else {
        		fields.add(fieldMeta);
        	}
        	
        	List<String> values = fieldMeta.getValue();
        	for(String value : values) {
        		Set<Integer> assetIds = fieldIdAndAssetIdsMap.get(value);
        		if(ToolUtil.isEmpty(assetIds)) {
        			assetIds = new HashSet<Integer>();
        			assetIds.add(fieldMeta.getAssetId());
        			fieldIdAndAssetIdsMap.put(value, assetIds);
        		}else {
        			assetIds.add(fieldMeta.getAssetId());
        		}
        	}
        }

        for(Map.Entry<Integer, List<AssetFieldMeta>> entry : fieldIdAndfields.entrySet()) {
        	List<AssetFieldMeta> fields = fieldIdAndfields.get(entry.getKey());
        	List children = new ArrayList();
        	for(AssetFieldMeta fieldMeta : fields) {
        		List<String> values = fieldMeta.getValue();
    			for (String valueEntry : values) {
    				if(ToolUtil.isEmpty(fieldIdAndAssetIdsMap.get(valueEntry))) {
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
    public Map importAssetsData(MultipartFile multipartFile) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_ASSET_ASSET_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int totalNum = importDataList.size();
        int successNum = this.saveAssetsFromImportDataList(importDataList, failDetails, new ArrayList<>(exportHeaderMap.keySet()), seqUUID);

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
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
            assetFieldMetaService.remove(new LambdaQueryWrapper<AssetFieldMeta>().eq(AssetFieldMeta::getSeq,seq));
            assetFieldValueService.remove(new LambdaQueryWrapper<AssetFieldValue>().eq(AssetFieldValue::getSeq, seq));
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
        List<VisualChart> chartIdWithAssetEntity = assetDao.getAssetsByChartIds(chartIds);
        if(CollectionUtils.isEmpty(chartIdWithAssetEntity)){
            return Collections.emptyList();
        }
        return chartIdWithAssetEntity;
    }

    private int saveAssetsFromImportDataList(List<Map<Integer, String>> importDataList, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
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

        // 配置信息获取  asset chart template
        SysConfigEntity assetChartConfig = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_CHART_TPL));
        VisualChart chartTemplate = JSONObject.parseObject(assetChartConfig.getParamValue(), VisualChart.class);

        int successNum = 0;
        // 处理 excel 数据，将含宿主机资产排到前面
        List<Map<Integer, String>> handleImportData = this.handleImportData(importDataList);
        for (int i = 0; i < handleImportData.size(); i++) {
            int dataFromExcelRowNum = i + 1;
            Map saveEntitys = new HashMap();
            boolean saveFlag = true;
            Map<Integer, String> dataMap = handleImportData.get(i);
            // name
            String name = dataMap.get(0);
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
            String sn = dataMap.get(1);
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
            String host = dataMap.get(3);
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

            // type
            AssetTypeConf typeConf = null;
            String typeName = dataMap.get(11);
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

            // brand  model
            Integer brandId = null, modelId = null;
            String brandName = dataMap.get(4);
            if (StringUtils.isEmpty(brandName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ASSET_BRAND_NAME_ISNULL.getMsg());
            } else {
                brandId = this.validateBrand(brandName, brandNameAndEntityMap, seqUUID, saveEntitys);
            }
            String modelName = dataMap.get(5);
            if (StringUtils.isEmpty(modelName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.ASSET_MODELNAME_ISNULL.getMsg());
            } else {
                modelId = this.validateModel(brandId, modelName, brandIdAndModelListMap, seqUUID, saveEntitys, Tool.ObjectUtil.isNull(typeConf) ? null : typeConf.getId());
            }

            // state
            String stateName = dataMap.get(10);
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

            // 虚拟机宿主
            String parentName = dataMap.get(2);
            Integer start = null, end = null, cabinetId = null, dcId = null;
            AssetAsset parentAsset = null;
            if (StringUtils.isNotEmpty(parentName)) {// 虚拟机 parentname 不为空
                parentAsset = assetNameAndEntityMap.get(parentName);
                if (parentAsset == null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.ASSET_PARENT_NOT_EXIST.getMsg());
                } else {
                    // 校验是否宿主机支持挂载
                    AssetTypeConf parentAssetTypeConf = typeIdAndEntityMap.get(parentAsset.getTypeId());
                    if (parentAssetTypeConf.getVmh().equals(0)) {
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
                String dcName = dataMap.get(6);
                if (StringUtils.isEmpty(dcName)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.DC_NAME_ISNULL.getMsg());
                } else {
                    dcId = this.validateDc(dcName, dcNameAndEntityMap, seqUUID, saveEntitys);
                }

                // 虚拟机 vm info 校验 / 虚拟机必须选择宿主
                if (typeConf !=null && typeConf.getVm().equals(1) && StringUtils.isEmpty(parentName)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.ASSET_NOT_SELECT_PARENT.getMsg());
                }
                // parent Name 为 null , 当前是 宿主机 ， 并且前面没有发生错误，校验 cabinet start end 相关数据
                if (saveFlag) {
                    String cabinetName = dataMap.get(7);
                    String cabinetStart = dataMap.get(8);
                    String cabinetEnd = dataMap.get(9);
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
                    if (dcId != null && StringUtils.isNotEmpty(cabinetName)) { // dc已存在，不需要添加
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
                    } else if (dcId == null && StringUtils.isNotEmpty(cabinetName)) { // dc不存在
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

            String purchaseDate = dataMap.get(12);
            if (StringUtils.isNotEmpty(purchaseDate)) {
                if (!DateUtil.isValidDate(purchaseDate, "yyyy-MM-dd")) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(12), RCode.ASSET_PURCHASEDATE_FORMAT.getMsg());
                }
            }

            String fieldsStr = dataMap.get(13);
            if (StringUtils.isNotEmpty(fieldsStr)) {
                saveEntitys.put("fields", this.getFieldsByFieldsStr(fieldsStr, seqUUID));
            }
            
            
            String authProtocolPort = dataMap.get(14);
            if(StringUtils.isNotEmpty(authProtocolPort)){
                if (!CommonUtils.checkPort(authProtocolPort)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(14), RCode.ACCOUNT_PORT_ERROR.getMsg());
                }
            }
            String authType = dataMap.get(15);
            if(StringUtils.isNotEmpty(authType)){
                if (!"1".equalsIgnoreCase(authType) && !"2".equalsIgnoreCase(authType)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(15), RCode.ACCOUNT_AUTHTYPE_INVALIDE.getMsg());
                }
            }
            String authUsername = dataMap.get(16);
            String authPin = dataMap.get(17);
            String authPriKey = dataMap.get(18);
            String authUserTip = dataMap.get(19);
            String authPinTip = dataMap.get(20);
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

            Integer snmpCredentialId = null;
            String snmpCredentialName = dataMap.get(21);
            if(StringUtils.isNotEmpty(snmpCredentialName)){
                SnmpCredential snmpCredential = snmpCredentialService.getOne(new LambdaQueryWrapper<SnmpCredential>().eq(SnmpCredential::getName, snmpCredentialName));
                if(snmpCredential == null){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(21), RCode.SNMP_CREDENTIAL_NOT_FOUND.getMsg());
                }else{
                    snmpCredentialId = snmpCredential.getId();
                }
            }

            String clientUser = dataMap.get(22);
            String clientPin = dataMap.get(23);
            String clientToken = dataMap.get(24);
            String clientPort = dataMap.get(25);
            if (StringUtils.isNotEmpty(clientPort)) {
                if (!CommonUtils.checkPort(clientPort)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(25), RCode.ASSET_CLIENTPORT_ERROR.getMsg());
                }
            }
            String clientEnable = dataMap.get(26);
            if (StrUtil.isNotEmpty(clientEnable)) {
                if (!StrUtil.equals("0", clientEnable) && !StrUtil.equals("1", clientEnable)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(26), RCode.TYPECONF_CLIENTENABLE_INCORRECT.getMsg());
                }
                // client enable 为 1 时 token 必填
                if (StrUtil.equals("1", clientEnable) && StrUtil.isEmpty(clientToken)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(26), RCode.ASSET_CLIENT_TOKEN_ISNULL.getMsg());
                }
            }

            // 前面没有报错
            if (saveFlag) {
                AssetAsset asset = new AssetAsset();
                asset.setName(name);
                asset.setSn(sn);
                asset.setTypeId(typeConf.getId());
                asset.setStateId(stateConf.getId());
                asset.setPurchaseDate(StringUtils.isEmpty(purchaseDate) ? null : purchaseDate);
                asset.setCreateAt(new Date());
                asset.setCreateBy(ShiroUtils.getUserId());
                asset.setManageIp(host);
                asset.setSeq(seqUUID);
                asset.setDcId(dcId);
                asset.setBrandId(brandId);
                asset.setModelId(modelId);
                asset.setCabinetId(cabinetId);
                asset.setCabinetStart(start);
                asset.setCabinetEnd(end);
                asset.setAuthProtocolPort(StringUtils.isNotEmpty(authProtocolPort) ? Integer.valueOf(authProtocolPort) : null);
                asset.setAuthType(StringUtils.isNotEmpty(authType) ? Integer.valueOf(authType) : null);
                asset.setAuthUsername(authUsername);
                asset.setAuthPin(StringUtils.isNotEmpty(authPin) ? Base64.getEncoder().encodeToString(authPin.getBytes()) : null);
                asset.setAuthPriKey(authPriKey);
                asset.setAuthUserTip(authUserTip);
                asset.setAuthPinTip(authPinTip);
                asset.setSnmpCredentialId(snmpCredentialId);
                if (parentAsset == null) {
                    asset.setPid(-1);
                } else {
                    asset.setPid(parentAsset.getId());
                    asset.setDcId(parentAsset.getDcId());
                    asset.setCabinetId(parentAsset.getCabinetId());
                    asset.setCabinetStart(parentAsset.getCabinetStart());
                    asset.setCabinetEnd(parentAsset.getCabinetEnd());
                }
                asset.setClientUser(ObjectUtil.defaultIfEmpty(clientUser, null));
                asset.setClientPin(ObjectUtil.defaultIfEmpty(clientPin, null));
                asset.setClientAddress(asset.getManageIp());
                asset.setClientPort(StringUtils.isNotEmpty(clientPort) ? Integer.valueOf(clientPort) : null);
                asset.setClientToken(clientToken);
                asset.setClientEnable(Integer.valueOf(StrUtil.emptyToDefault(clientEnable, "0")));
                saveEntitys.put("asset", asset);
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                try {
                    // 调用入库方法
                    Map updateMap = this.saveRowData(saveEntitys,chartTemplate);
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
                    meta.setSeq(seqUUID);
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

    public Map saveRowData(Map saveEntitys,VisualChart chartTemplate) {
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
        // 新增时需要新增 panel chart
		chartService.saveRelPanelAndChart(Constant.PanelType.ASSET.getValue(), asset.getId(), asset.getSeq(), chartTemplate);
		// 新增 asset 时，新增 关联 Model charts
        this.saveAssetModelChartsWhenInsert(asset);

		// fields
		List<AssetFieldValue> fields = (List<AssetFieldValue>) saveEntitys.get("fields");
		if (CollectionUtils.isNotEmpty(fields)) {
		    fields.forEach(assetFieldValue -> assetFieldValue.setAssetId(asset.getId()));
		    assetFieldValueService.saveBatch(fields);
		}
        updateMap.put("asset", asset);
        return updateMap;
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


    private Integer validateModel(Integer brandId, String modelName, Map<Integer, List<AssetModel>> brandIdAndModelListMap, String seqUUID, Map saveEntitys, Integer typeId) {
        // brand 不存在,model肯定也不在
        if (brandId == null) {
            AssetModel model = new AssetModel();
            model.setName(modelName);
            model.setSeq(seqUUID);
            model.setUsize(1);
            model.setTypeId(typeId);
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
                model.setTypeId(typeId);
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

    private List<List<String>> handleExportDataList(List<AssetAsset> list) {
        List<List<String>> exportDataList = new ArrayList<>();
        for (AssetAsset asset : list) {
            List<String> tempList = new ArrayList<>();

           tempList.add(asset.getName());
           tempList.add(asset.getSn());
           tempList.add(ToolUtil.isEmpty(asset.getParent()) ? "" : asset.getParent().getName());
           tempList.add(asset.getManageIp());
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
                String fieldStr = "";
                for (AssetFieldMeta meta : asset.getFields()) {
                    fieldStr += meta.getMetaKey() + "=" + meta.getValue().get(0) + "\n";
                }
                tempList.add(fieldStr);
            } else {
                tempList.add(StrUtil.EMPTY);
            }
            
            tempList.add(ToolUtil.isEmpty(asset.getAuthProtocolPort()) ? "" : String.valueOf(asset.getAuthProtocolPort()));
            tempList.add(ToolUtil.isEmpty(asset.getAuthType()) ? "" : String.valueOf(asset.getAuthType()));
            tempList.add(ToolUtil.isEmpty(asset.getAuthUsername()) ? "" : asset.getAuthUsername());
//            row.createCell(17).setCellValue(ToolUtil.isEmpty(asset.getAuthPin()) ? "" : asset.getAuthPin());
//            row.createCell(18).setCellValue(ToolUtil.isEmpty(asset.getAuthPriKey()) ? "" : asset.getAuthPriKey());
            tempList.add("");
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
//            row.createCell(23).setCellValue(asset.getClientPin());
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
		
		switch(editType) {
			// 1 account 2 label 3 state 4 snmp credentials
			case 1:
				Integer authType = (Integer) params.get("authType");
				String authUsername	= (String) params.get("authUsername");
				String authPin	= (String) params.get("authPin");
				if(StrUtil.isNotBlank(authPin)) {
					params.put("authPin",Base64.getEncoder().encodeToString(authPin.getBytes()));
		        }
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

	public AssetAsset getAssetByIP(String ip) {
		if(Tool.StrUtil.isBlank(ip)) {
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
        if(Tool.CollectionUtil.isEmpty(endpointList)) return Collections.emptyList();

        List<Integer> moduleIds = endpointList.stream().map(MonitorEndpoint::getModuleId).distinct().collect(Collectors.toList());
        List<MonitorModule> moduleList = moduleService.list(new LambdaQueryWrapper<MonitorModule>().in(MonitorModule::getId, moduleIds));
        return moduleList;
    }

    @Override
    public List<MonitorProject> getProjectListByAssetId(Integer assetId) {
        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL);
        List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getAssetId, assetId));
        if(Tool.CollectionUtil.isEmpty(endpointList)) return Collections.emptyList();

        List<Integer> moduleIds = endpointList.stream().map(MonitorEndpoint::getModuleId).distinct().collect(Collectors.toList());
        List<MonitorModule> moduleList = moduleService.list(new LambdaQueryWrapper<MonitorModule>().in(MonitorModule::getId, moduleIds));

        List<Integer> projectIds = moduleList.stream().map(MonitorModule::getProjectId).distinct().collect(Collectors.toList());
        List<MonitorProject> projectList = projectService.list(new LambdaQueryWrapper<MonitorProject>().in(MonitorProject::getId, projectIds));
        return projectList;
    }
}
