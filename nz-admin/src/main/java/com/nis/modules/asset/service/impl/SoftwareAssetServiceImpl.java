package com.nis.modules.asset.service.impl;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.SoftwareAssetDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.SoftwareAccSession;
import com.nis.modules.asset.entity.SoftwareAsset;
import com.nis.modules.asset.entity.SoftwareType;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.SoftwareAccSessionService;
import com.nis.modules.asset.service.SoftwareAssetService;
import com.nis.modules.asset.service.SoftwareTypeService;
import com.nis.modules.sys.service.LicenseManagementService;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Software Asset Service Implement
 *
 * @author shizhendong
 * @date 2023-08-10
 */
@Service
public class SoftwareAssetServiceImpl extends ServiceImpl<SoftwareAssetDao, SoftwareAsset> implements SoftwareAssetService {

    private static final Log log = Log.get();

    @Value("${fetureId}")
    private Integer nzFetureId;

    @Value("${cn.fetureId:30000}")
    private Integer cnFetureId;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private SoftwareTypeService softwareTypeService;

    @Autowired
    private SoftwareAccSessionService softwareAccSessionService;

    @Autowired
    private LicenseManagementService licenseManagementService;

    private static final String TSG_FEATURE_ID = "100";

    @Override
    public SoftwareAsset querySoftwareAssetInfo(Integer id) {
        SoftwareAsset softwareAsset = this.getById(id);
        ValidateUtils.is(softwareAsset).notNull(RCode.SOFTWARE_ASSET_NOT_EXIST);

        AssetAsset refAsset = assetService.getById(softwareAsset.getAssetId());
        if (Tool.ObjectUtil.isNotNull(refAsset)) {
            softwareAsset.setAsset(refAsset);
        }

        SoftwareType refType = softwareTypeService.getById(softwareAsset.getTypeId());
        if (Tool.ObjectUtil.isNotNull(refType)) {
            softwareAsset.setType(refType);
        }

        Object softwareAssetParams = softwareAsset.getParams();
        if (Tool.ObjectUtil.isNotEmpty(softwareAssetParams)) {
            try {
                softwareAsset.setParams(JSONUtil.toBean(StrUtil.utf8Str(softwareAssetParams), LinkedHashMap.class));
            } catch (Exception e) {
                log.error(e, "[querySoftwareAssetInfo] [json format error] [id: {}]", id);
            }
        }
        return softwareAsset;
    }

    @Override
    public PageUtils querySoftwareAssetPage(Map<String, Object> params) {
        IPage page = new Query(SoftwareAsset.class).getPage(params);
        List<SoftwareAsset> softwareAssetList = this.getBaseMapper().querySoftwareAssetInfoList(page, params);

        for (SoftwareAsset softwareAsset : softwareAssetList) {
            Object softwareAssetParams = softwareAsset.getParams();
            if (Tool.ObjectUtil.isNotEmpty(softwareAssetParams)) {
                try {
                    softwareAsset.setParams(JSONUtil.toBean(StrUtil.utf8Str(softwareAssetParams), LinkedHashMap.class));
                } catch (Exception e) {
                    log.error(e, "[querySoftwareAssetPage] [json format error] [id: {}]", softwareAsset.getId());
                }
            }
        }

        page.setRecords(softwareAssetList);
        return new PageUtils(page);
    }

    /**
     * validate software asset
     *
     * @param softwareAsset
     */
    private void validateSoftwareAsset(SoftwareAsset softwareAsset) {
        // name duplicate
        String name = softwareAsset.getName();
        SoftwareAsset tempSoftwareAssetForValidateNameDuplicate = this.getOne(new LambdaQueryWrapper<SoftwareAsset>()
                .eq(SoftwareAsset::getName, name)
                .ne(ObjectUtil.isNotNull(softwareAsset.getId()), SoftwareAsset::getId, softwareAsset.getId())
                .last("limit 1"));
        if (ToolUtil.isNotEmpty(tempSoftwareAssetForValidateNameDuplicate)) {
            throw new NZException(RCode.SOFTWARE_ASSET_NAME_DUPLICATE);
        }

        Integer assetId = softwareAsset.getAssetId();
        if (assetId != null && assetId != -1) {
            AssetAsset asset = assetService.getById(assetId);
            ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);
        }

        Integer typeId = softwareAsset.getTypeId();
        if (typeId != null && typeId != -1) {
            SoftwareType type = softwareTypeService.getById(typeId);
            ValidateUtils.is(type).notNull(RCode.SOFTWARE_TYPE_NOT_EXIST);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer saveOrUpdateSoftwareAsset(SoftwareAsset softwareAsset) {
        // validate software asset
        this.validateSoftwareAsset(softwareAsset);

        // set default value
        softwareAsset.setAssetId(ObjectUtil.defaultIfNull(softwareAsset.getAssetId(), -1));
        softwareAsset.setTypeId(ObjectUtil.defaultIfNull(softwareAsset.getTypeId(), -1));
        softwareAsset.setParams(ObjectUtil.defaultIfNull(softwareAsset.getParams(), StrUtil.EMPTY_JSON));

        // params
        if (ObjectUtil.isNotEmpty(softwareAsset.getParams())) {
            softwareAsset.setParams(JSONUtil.toJsonStr(softwareAsset.getParams()));
        }

        Integer id = softwareAsset.getId();
        if (ObjectUtil.isNull(id)) {
            softwareAsset.setCid(ShiroUtils.getUserId().intValue());
            softwareAsset.setCts(System.currentTimeMillis());
            softwareAsset.setConcurrency(null);
            softwareAsset.setDateExpires(null);
            softwareAsset.setDateIssued(null);
            softwareAsset.setLogins(null);
            softwareAsset.setLastScanTs(null);
            softwareAsset.setStatus(null);
            softwareAsset.setLastScanMsg(null);
        } else {
            SoftwareAsset tempSoftwareAssetForJudge = this.getById(id);
            if (ObjectUtil.isNull(tempSoftwareAssetForJudge)) {
                softwareAsset.setCid(ShiroUtils.getUserId().intValue());
                softwareAsset.setCts(System.currentTimeMillis());
            }
        }

        // uid&uts
        softwareAsset.setUid(ShiroUtils.getUserId().intValue());
        softwareAsset.setUts(System.currentTimeMillis());

        // save or update
        this.saveOrUpdate(softwareAsset);
        return softwareAsset.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R deleteSoftwareAsset(Integer force, List<Integer> idList) {
        List<SoftwareAsset> delSoftwareAssetList = this.list(new LambdaQueryWrapper<SoftwareAsset>().in(SoftwareAsset::getId, idList));
        if (Tool.CollUtil.isEmpty(delSoftwareAssetList)) {
            return R.ok().putData("list", Tool.ListUtil.empty());
        }

        // validate build-in data
        SoftwareAsset findFirstBuildInSoftwareAsset = delSoftwareAssetList.stream().filter(t -> ObjectUtil.equal(t.getBuildIn(), 1)).findFirst().orElse(null);
        if (ObjectUtil.isNotNull(findFirstBuildInSoftwareAsset)) {
            throw new NZException(RCode.SOFTWARE_ASSET_BUILTIN_CANNOT_DELETED);
        }

        List<Map> resultList = Tool.ListUtil.list(true);
        for (SoftwareAsset softwareAsset : delSoftwareAssetList) {
            Map<Object, Object> typeRefObjectInfo = Tool.MapUtil.builder()
                    .put("id", softwareAsset.getId())
                    .put("name", softwareAsset.getName())
                    .put("relates", Tool.ListUtil.empty())
                    .build();
            resultList.add(typeRefObjectInfo);
        }

        // remove
        this.removeByIds(idList);

        // acc session
        softwareAccSessionService.remove(new LambdaQueryWrapper<SoftwareAccSession>().in(SoftwareAccSession::getSaid, idList));

        return R.ok().putData("list", resultList);
    }

    @Override
    public List<SoftwareAsset> getSoftwareAssetListByTypeName(String... typeNames) {
        if (Tool.ArrayUtil.isEmpty(typeNames)) {
            return Tool.ListUtil.empty();
        }

        List<SoftwareType> softwareTypeList = softwareTypeService.list(new LambdaQueryWrapper<SoftwareType>().in(SoftwareType::getName, typeNames));
        Integer[] idArr = softwareTypeList.stream().map(SoftwareType::getId).toArray(Integer[]::new);
        if (Tool.ArrayUtil.isEmpty(idArr)) {
            return Tool.ListUtil.empty();
        }

        return this.getSoftwareAssetListByTypeId(idArr);
    }

    @Override
    public List<SoftwareAsset> getSoftwareAssetListByTypeId(Integer... typeIds) {
        if (Tool.ArrayUtil.isEmpty(typeIds)) {
            return Tool.ListUtil.empty();
        }

        List<SoftwareAsset> softwareAssetList = this.list(new LambdaQueryWrapper<SoftwareAsset>().in(SoftwareAsset::getTypeId, typeIds));
        if (Tool.CollUtil.isNotEmpty(softwareAssetList)) {
            for (SoftwareAsset softwareAsset : softwareAssetList) {
                AssetAsset refAsset = assetService.getById(softwareAsset.getAssetId());
                if (Tool.ObjectUtil.isNotNull(refAsset)) {
                    softwareAsset.setAsset(refAsset);
                }
                SoftwareType refType = softwareTypeService.getById(softwareAsset.getTypeId());
                if (Tool.ObjectUtil.isNotNull(refType)) {
                    softwareAsset.setType(refType);
                }
            }
        }
        return softwareAssetList;
    }

    /**
     * update license status
     */
    @Override
    public void updateLicenseStatus(String saIds) {
        String[] licenseTypeNameArr = Arrays.stream(Constant.SoftwareLicenseType.values()).map(Constant.SoftwareLicenseType::getName).toArray(String[]::new);

        List<SoftwareAsset> licenseAssetList = this.getSoftwareAssetListByTypeName(licenseTypeNameArr);
        if (StrUtil.isNotEmpty(saIds)) {
            List<String> saIdList = StrUtil.split(saIds, ",");
            licenseAssetList = licenseAssetList.stream().filter(asset -> saIdList.contains(asset.getId().toString())).toList();
        }
        log.info("[updateLicenseStatus] [license software asset list] [saIds: {}] [size: {}]", saIds, Tool.CollUtil.size(licenseAssetList));

        for (SoftwareAsset softwareAsset : licenseAssetList) {
            Thread.ofVirtual().start(() -> {
                SoftwareType type = softwareAsset.getType();
                Constant.SoftwareLicenseType instance = Constant.SoftwareLicenseType.getInstance(type.getName());
                switch (instance) {
                    // query ACC service
                    case TSG_OS_HL_SEATS:
                    case TSG_OS_HL: {
                        // TSG License
                        this.updateTSGLicenseStatus(softwareAsset);
                        break;
                    }
                    // query license service
                    case CM: {
                        // CM License
                        this.updateCMLicenseStatus(softwareAsset);
                        break;
                    }
                    case NZ:
                    case CN: {
                        // NZ|CN License
                        this.updateNZLicenseStatus(softwareAsset);
                        break;
                    }
                    default:
                        break;
                }
            });
        }
    }

    /**
     * TSG License
     *
     * @param softwareAsset
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTSGLicenseStatus(SoftwareAsset softwareAsset) {
        Integer said = softwareAsset.getId();
        JSONArray sessions = new JSONArray();
        try {
            List<JSONObject> list = licenseManagementService.queryLicenseProductList(softwareAsset.getId(), 1);
            log.info("[updateTSGLicenseStatus] [query ACC get license product list] [said: {}] [size: {}]", said, Tool.CollUtil.size(list));

            // feature id&name map
            String featureNameMappingJsonStr = sysConfService.getValue("license_feature_name_mapping");
            String tsgProductName = StrUtil.str(com.nis.common.utils.JSONUtil.getByPath(com.nis.common.utils.JSONUtil.parse(featureNameMappingJsonStr), TSG_FEATURE_ID), Tool.CharsetUtil.UTF_8);

            JSONObject tsgProduct = list.stream().filter(product -> StrUtil.equals(tsgProductName, product.getStr("name"))).findFirst().orElse(null);
            log.info("[updateTSGLicenseStatus] [TSG license product info] [data: {}]", com.nis.common.utils.JSONUtil.toJsonStr(tsgProduct));
            if (tsgProduct == null) {
                log.warn("[updateTSGLicenseStatus] [TSG active license not found] [said: {}] [acc response: {}]", said, com.nis.common.utils.JSONUtil.toJsonStr(list));
                // 未授权
                softwareAsset.setStatus(0);
            } else {
                String productionDateTimeStamp = tsgProduct.getStr("production_date");
                softwareAsset.setDateIssued(Tool.NumberUtil.parseLong(productionDateTimeStamp) * 1000);

                String expirationDateTimeStamp = tsgProduct.getStr("expiration_date");
                softwareAsset.setDateExpires(Tool.NumberUtil.parseLong(expirationDateTimeStamp) * 1000);

                String loginlimit = tsgProduct.getStr("loginlimit");
                softwareAsset.setConcurrency(Tool.NumberUtil.parseInt(loginlimit));

                String logincount = tsgProduct.getStr("logincount");
                softwareAsset.setLogins(Tool.NumberUtil.parseInt(logincount));

                String haspId = tsgProduct.getStr("haspid");
                softwareAsset.setHaspId(haspId);

                // 已授权
                softwareAsset.setStatus(1);

                // session
                sessions = tsgProduct.getJSONArray("sessions");
            }
        } catch (Exception e) {
            log.error(e, "[updateTSGLicenseStatus] [error] [said: {}]", said);
            // 未知错误（网络无法访问）
            softwareAsset.setStatus(2);
            softwareAsset.setLastScanMsg(Tool.StrUtil.subWithLength(e.getMessage(), 0, 256));
        }

        // last_scan_ts
        softwareAsset.setLastScanTs(System.currentTimeMillis());

        // license info
        this.saveOrUpdate(softwareAsset);

        // acc sessions
        if (ObjectUtil.isNotEmpty(sessions)) {
            // remove
            softwareAccSessionService.remove(new LambdaQueryWrapper<SoftwareAccSession>().eq(SoftwareAccSession::getSaid, said));
            // save
            List<SoftwareAccSession> saveBatchList = new ArrayList<>();
            for (Object session : sessions) {
                JSONObject sessionObj = com.nis.common.utils.JSONUtil.parseObj(session);
                SoftwareAccSession accSessionEntity = new SoftwareAccSession();
                accSessionEntity.setIp(sessionObj.getStr("client"));
                accSessionEntity.setSaid(said.toString());
                accSessionEntity.setStatus(softwareAsset.getStatus());
                saveBatchList.add(accSessionEntity);
            }
            softwareAccSessionService.saveBatch(saveBatchList);
        }
    }

    /**
     * request License Api
     *
     * @param softwareAsset
     * @return
     * @throws Exception 
     */
    private String requestLicenseApi(SoftwareAsset softwareAsset) throws Exception {
        JSONObject softwareAssetParam = JSONUtil.parseObj(StrUtil.utf8Str(softwareAsset.getParams()));
        JSONArray connectorAttributeKeys = softwareAssetParam.getJSONArray("connectorAttributeKeys");
        connectorAttributeKeys = ObjectUtil.defaultIfNull(connectorAttributeKeys, new JSONArray());

        String url = connectorAttributeKeys.stream()
                .filter(obj -> StrUtil.equals("url", ((JSONObject) obj).getStr("key")))
                .findFirst()
                .map(obj -> ((JSONArray) ((JSONObject) obj).get("value")).getFirst().toString())
                .orElse("");
        String token = connectorAttributeKeys.stream()
                .filter(obj -> StrUtil.equals("token", ((JSONObject) obj).getStr("key")))
                .findFirst()
                .map(obj -> ((JSONArray) ((JSONObject) obj).get("value")).getFirst().toString())
                .orElse("");

        HttpHeaders headers = new HttpHeaders();
        if (StrUtil.isNotEmpty(token)) {
            SoftwareType type = softwareAsset.getType();
            if (StrUtil.equals(type.getName(), Constant.SoftwareLicenseType.CN.getName())) {
                headers.add(sysConfService.getValueOrDefault("cn_api_token_header_name", "Cn-Authorization"), token);
            } else {
                headers.add(HttpHeaders.AUTHORIZATION, token);
            }
        }

        HttpEntity httpEntity = new HttpEntity(headers);
        String urlStr = UrlBuilder.ofHttp(url).toString();

        // 发送 请求
        ResponseEntity<String> responseEntity = restTemplate.exchange(new URI(urlStr), HttpMethod.GET, httpEntity, String.class);
        Integer getStatusCodeValue = Opt.ofNullable(responseEntity).map(ResponseEntity::getStatusCodeValue).get();
        String responseBody = Opt.ofNullable(responseEntity).map(ResponseEntity::getBody).get();
        log.info("[requestLicenseApi] [url: {}] [responseStatus: {}] [body: {}]", urlStr, getStatusCodeValue, responseBody);
        if (!Tool.ObjectUtil.equal(HttpStatus.OK.value(), getStatusCodeValue)) {
            throw new NZException(getStatusCodeValue.toString());
        }
        return responseBody;
    }

    /**
     * NZ License
     *
     * @param softwareAsset
     */
    private void updateNZLicenseStatus(SoftwareAsset softwareAsset) {
        Integer said = softwareAsset.getId();
        log.info("[updateNZLicenseStatus] [begin] [said: {}]", said);
        try {
            String responseBody = this.requestLicenseApi(softwareAsset);
            if (StrUtil.isEmpty(responseBody)) {
                log.warn("[updateNZLicenseStatus] [license api responseBody is empty] [said: {}]", said);
                // 未知错误
                softwareAsset.setStatus(2);
            } else {
                Object hasp = JSONPath.read(responseBody, "data.license.hasp");
                JSONObject haspJSONObject = com.nis.common.utils.JSONUtil.parseObj(hasp);
                if (haspJSONObject.isEmpty()) {
                    log.warn("[updateNZLicenseStatus] [not found license.hasp object] [said: {}]", said);
                    // 未知错误,接口返回异常，未返回 license 信息
                    softwareAsset.setStatus(2);
                } else {
                    String productionDateTimeSec = haspJSONObject.getStr("production_date");
                    softwareAsset.setDateIssued(Tool.NumberUtil.parseLong(productionDateTimeSec) * 1000);

                    JSONArray featureArray = new JSONArray();
                    Object featureObject = haspJSONObject.get("feature");
                    if (featureObject instanceof JSONObject) {
                        featureArray.add((JSONObject) featureObject);
                    } else if (featureObject instanceof JSONArray) {
                        featureArray = (JSONArray) featureObject;
                    }
                    List<JSONObject> nzFetureList = featureArray.stream().map(obj -> com.nis.common.utils.JSONUtil.parseObj(obj))
                            // 根据 feature id 过滤
                            .filter(jsonObject -> {
                                String typeName = softwareAsset.getType().getName();
                                if (StrUtil.equals(typeName, Constant.SoftwareLicenseType.CN.getName())) {
                                    return StrUtil.equals(cnFetureId.toString(), jsonObject.getStr("id"));
                                }
                                return StrUtil.equals(nzFetureId.toString(), jsonObject.getStr("id"));
                            })
                            .toList();
                    // 永久的
                    JSONObject perpetualFeture = nzFetureList.stream().filter(jsonObject -> {
                        JSONObject license = jsonObject.getJSONObject("license");
                        String licenseType = license.getStr("license_type");
                        return StrUtil.equalsIgnoreCase("perpetual", licenseType);
                    }).findFirst().orElse(null);
                    log.info("[updateNZLicenseStatus] [said: {}] [has perpetual feture: {}]", said, ObjectUtil.isNotNull(perpetualFeture));

                    if (ObjectUtil.isNotNull(perpetualFeture)) {
                        // 已授权
                        softwareAsset.setStatus(1);
                        softwareAsset.setDateExpires(-1L);
                    } else {
                        // 过期时间最大的 feture
                        JSONObject largestFeature = nzFetureList.stream().filter(jsonObject -> {
                            JSONObject license = jsonObject.getJSONObject("license");
                            String licenseType = license.getStr("license_type");
                            return StrUtil.equalsIgnoreCase("expiration", licenseType);
                        }).sorted((f1, f2) -> {
                            // 按照 exp_date 排序，过期时间最大的 feture
                            JSONObject f1License = f1.getJSONObject("license");
                            String f1LicenseExpDateStr = f1License.getStr("exp_date");

                            JSONObject f2License = f2.getJSONObject("license");
                            String f2LicenseExpDateStr = f2License.getStr("exp_date");
                            return Tool.NumberUtil.compare(Long.valueOf(f1LicenseExpDateStr), Long.valueOf(f2LicenseExpDateStr)) * -1;
                        }).findFirst().orElse(null);

                        if (ObjectUtil.isNull(largestFeature)) {
                            log.warn("[updateNZLicenseStatus] [not found expiration type feture] [said: {}]", said);
                            // 未授权
                            softwareAsset.setStatus(0);
                        } else {
                            JSONObject license = largestFeature.getJSONObject("license");
                            String licenseExpDateStr = license.getStr("exp_date");
                            long currentTimeMillis = System.currentTimeMillis();

                            log.info("[updateNZLicenseStatus] [largestFeature] [currentTimeMillis: {}] [license.exp_date: {}]", currentTimeMillis, licenseExpDateStr);

                            long licenseExpDateTimeMillis = TimeUnit.SECONDS.toMillis(Long.valueOf(licenseExpDateStr));
                            if (currentTimeMillis > licenseExpDateTimeMillis) {
                                // 未授权
                                softwareAsset.setStatus(0);
                            } else {
                                // 已授权
                                softwareAsset.setStatus(1);
                            }
                            softwareAsset.setDateExpires(licenseExpDateTimeMillis);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e, "[updateNZLicenseStatus] [error] [said: {}]", said);
            softwareAsset.setStatus(2);
            softwareAsset.setLastScanMsg(Tool.StrUtil.subWithLength(e.getMessage(), 0, 256));
        }
        // last_scan_ts
        softwareAsset.setLastScanTs(System.currentTimeMillis());
        // license info
        this.saveOrUpdate(softwareAsset);
        log.info("[updateNZLicenseStatus] [finshed] [said: {}]", said);
    }

    /**
     * CM License
     *
     * @param softwareAsset
     */
    private void updateCMLicenseStatus(SoftwareAsset softwareAsset) {
        Integer said = softwareAsset.getId();
        log.info("[updateCMLicenseStatus] [begin] [said: {}]", said);
        try {
            String responseBody = this.requestLicenseApi(softwareAsset);
            if (StrUtil.isEmpty(responseBody)) {
                log.warn("[updateCMLicenseStatus] [license api responseBody is empty] [said: {}]", said);
                // 未知错误
                softwareAsset.setStatus(2);
            } else {
                Object license = JSONPath.read(responseBody, "data.license");
                JSONObject licenseJSONObject = com.nis.common.utils.JSONUtil.parseObj(license);
                if (licenseJSONObject.isEmpty()) {
                    log.warn("[updateCMLicenseStatus] [not found license object] [said: {}]", said);
                    // 未知错误,接口返回异常，未返回 license 信息
                    softwareAsset.setStatus(2);
                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

                    String issuedDateStr = licenseJSONObject.getStr("issued_date");
                    Date issuedDate = dateFormat.parse(issuedDateStr);
                    softwareAsset.setDateIssued(issuedDate.getTime());

                    String expiresDateStr = licenseJSONObject.getStr("expires_date");
                    log.info("[updateCMLicenseStatus] [issued_date: {}] [expires_date: {}]", issuedDateStr, expiresDateStr);

                    // 如为空，表示Never, 永不过期
                    if (StrUtil.isEmpty(expiresDateStr)) {
                        // 已授权
                        softwareAsset.setStatus(1);
                        softwareAsset.setDateExpires(-1L);
                    } else {
                        Date expiresDate = dateFormat.parse(expiresDateStr);
                        long expiresTimeMillis = expiresDate.getTime();
                        if (System.currentTimeMillis() > expiresTimeMillis) {
                            // 未授权
                            softwareAsset.setStatus(0);
                        } else {
                            // 已授权
                            softwareAsset.setStatus(1);
                        }
                        softwareAsset.setDateExpires(expiresTimeMillis);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e, "[updateCMLicenseStatus] [error] [said: {}]", said);
            softwareAsset.setStatus(2);
            softwareAsset.setLastScanMsg(Tool.StrUtil.subWithLength(e.getMessage(), 0, 256));
        }
        // last_scan_ts
        softwareAsset.setLastScanTs(System.currentTimeMillis());
        // license info
        this.saveOrUpdate(softwareAsset);
        log.info("[updateCMLicenseStatus] [finshed] [said: {}]", said);
    }

}
