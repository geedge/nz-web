package com.nis.modules.asset.job;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.SoftwareAccSession;
import com.nis.modules.asset.entity.SoftwareAsset;
import com.nis.modules.asset.entity.SoftwareType;
import com.nis.modules.asset.service.SoftwareAccSessionService;
import com.nis.modules.asset.service.SoftwareAssetService;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.sys.service.LicenseManagementService;
import com.nis.modules.sys.service.SysConfService;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@DisallowConcurrentExecution
public class SoftwareLicenseStatusJob extends QuartzJobBean {

    private static final Log log = Log.get();

    @Value("${fetureId}")
    private Integer nzFetureId;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private SoftwareAssetService softwareAssetService;

    @Autowired
    private LicenseManagementService licenseManagementService;

    @Autowired
    private SoftwareAccSessionService softwareAccSessionService;

    private static final String TSG_FEATURE_ID = "100";

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Thread.currentThread().setName("SoftwareLicenseStatusJob");

        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        log.info("[SoftwareLicenseStatusJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (Tool.BooleanUtil.negate(isLeader)) return;

        log.info("[SoftwareLicenseStatusJob] [begin]");

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // update license status
            this.updateLicenseStatus();
        } catch (Exception e) {
            log.error(e, "[SoftwareLicenseStatusJob] [error]");
        } finally {
            sw.stop();
        }

        log.info("[SoftwareLicenseStatusJob] [finshed] [Run Time: {}]", sw.toString());
    }

    /**
     * update license status
     */
    private void updateLicenseStatus() {
        String[] licenseTypeNameArr = Arrays.stream(SoftwareLicenseStatusJob.SoftwareLicenseType.values()).map(SoftwareLicenseStatusJob.SoftwareLicenseType::getName).toArray(String[]::new);

        List<SoftwareAsset> licenseAssetList = softwareAssetService.getSoftwareAssetListByTypeName(licenseTypeNameArr);
        log.info("[updateLicenseStatus] [license software asset list] [size: {}]", Tool.CollUtil.size(licenseAssetList));

        for (SoftwareAsset softwareAsset : licenseAssetList) {
            SoftwareType type = softwareAsset.getType();
            SoftwareLicenseType instance = SoftwareLicenseType.getInstance(type.getName());
            switch (instance) {
                // query ACC service
                case TSG_OS_STANDALONE:
                case TSG_OS_CONCENTRATE: {
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
                case NZ: {
                    // NZ License
                     this.updateNZLicenseStatus(softwareAsset);
                    break;
                }
                default:
                    break;
            }
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
            String tsgProductName = StrUtil.str(JSONUtil.getByPath(JSONUtil.parse(featureNameMappingJsonStr), TSG_FEATURE_ID), Tool.CharsetUtil.UTF_8);

            JSONObject tsgProduct = list.stream().filter(product -> StrUtil.equals(tsgProductName, product.getStr("name"))).findFirst().orElse(null);
            log.info("[updateTSGLicenseStatus] [TSG license product info] [data: {}]", JSONUtil.toJsonStr(tsgProduct));
            if (tsgProduct == null) {
                log.warn("[updateTSGLicenseStatus] [TSG active license not found] [said: {}] [acc response: {}]", said, JSONUtil.toJsonStr(list));
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

                // 已授权
                softwareAsset.setStatus(1);

                // session
                sessions = tsgProduct.getJSONArray("sessions");
            }
        } catch (Exception e) {
            log.error(e, "[updateTSGLicenseStatus] [error] [said: {}]", said);
            // 未知错误（网络无法访问）
            softwareAsset.setStatus(2);
        }

        // last_scan_ts
        softwareAsset.setLastScanTs(System.currentTimeMillis());

        // license info
        softwareAssetService.saveOrUpdate(softwareAsset);

        // acc sessions
        if (ObjectUtil.isNotEmpty(sessions)) {
            String softwareAssetName = softwareAsset.getName();
            // remove
            softwareAccSessionService.remove(new LambdaQueryWrapper<SoftwareAccSession>().eq(SoftwareAccSession::getSaid, softwareAssetName));
            // save
            List<SoftwareAccSession> saveBatchList = new ArrayList<>();
            for (Object session : sessions) {
                JSONObject sessionObj = JSONUtil.parseObj(session);
                SoftwareAccSession accSessionEntity = new SoftwareAccSession();
                accSessionEntity.setIp(sessionObj.getStr("client"));
                accSessionEntity.setSaid(softwareAssetName);
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
     */
    private String requestLicenseApi(SoftwareAsset softwareAsset) {
        Map<String, Object> softwareAssetParam = cn.hutool.json.JSONUtil.toBean(StrUtil.utf8Str(softwareAsset.getParams()), Map.class);
        String url = Tool.MapUtil.getStr(softwareAssetParam, "url", "");
        String token = Tool.MapUtil.getStr(softwareAssetParam, "token", "");

        HttpHeaders headers = new HttpHeaders();
        if (StrUtil.isNotEmpty(token)) {
            headers.add(HttpHeaders.AUTHORIZATION, token);
        }

        HttpEntity httpEntity = new HttpEntity(headers);

        String urlStr = UrlBuilder.ofHttp(url).toString();

        // 发送 请求
        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(new URI(urlStr), HttpMethod.GET, httpEntity, String.class);
        } catch (Exception e) {
            log.error(e, "[requestLicenseApi] [error] [url: {}]", urlStr);
        }

        Integer getStatusCodeValue = Opt.ofNullable(responseEntity).map(ResponseEntity::getStatusCodeValue).get();
        String responseBody = Opt.ofNullable(responseEntity).map(ResponseEntity::getBody).get();

        log.info("[requestLicenseApi] [url: {}] [responseStatus: {}] [body: {}]", urlStr, getStatusCodeValue, responseBody);
        if (Tool.ObjectUtil.equal(HttpStatus.OK.value(), getStatusCodeValue)) {
            return responseBody;
        }
        return StrUtil.EMPTY;
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
                JSONObject haspJSONObject = JSONUtil.parseObj(hasp);
                if (haspJSONObject.isEmpty()) {
                    log.warn("[updateNZLicenseStatus] [not found license.hasp object] [said: {}]", said);
                    // 未知错误,接口返回异常，未返回 license 信息
                    softwareAsset.setStatus(2);
                } else {
                    String productionDateTimeSec = haspJSONObject.getStr("production_date");
                    softwareAsset.setDateIssued(Tool.NumberUtil.parseLong(productionDateTimeSec) * 1000);

                    JSONArray feature = haspJSONObject.getJSONArray("feature");
                    List<JSONObject> nzFetureList = feature.stream().map(obj -> JSONUtil.parseObj(obj))
                            // 根据 feature id 过滤
                            .filter(jsonObject -> StrUtil.equals(nzFetureId.toString(), jsonObject.getStr("id"))).toList();
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
        }
        // last_scan_ts
        softwareAsset.setLastScanTs(System.currentTimeMillis());
        // license info
        softwareAssetService.saveOrUpdate(softwareAsset);
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
                JSONObject licenseJSONObject = JSONUtil.parseObj(license);
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
        }
        // last_scan_ts
        softwareAsset.setLastScanTs(System.currentTimeMillis());
        // license info
        softwareAssetService.saveOrUpdate(softwareAsset);
        log.info("[updateCMLicenseStatus] [finshed] [said: {}]", said);
    }

    enum SoftwareLicenseType {
        TSG_OS_CONCENTRATE("TSG-OS Concentrate"),
        TSG_OS_STANDALONE("TSG-OS Stand-alone"),
        CM("TSG-CM"),
        NZ("Network Zodiac");

        private String name;

        SoftwareLicenseType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static SoftwareLicenseType getInstance(String name) {
            for (SoftwareLicenseType type : SoftwareLicenseType.values()) {
                if (StrUtil.equals(type.getName(), name)) {
                    return type;
                }
            }
            return null;
        }
    }
}

