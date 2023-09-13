package com.nis.modules.sys.service.impl;

import SafeNet.AdminApi;
import SafeNet.AdminStatus;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ResponseUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.SoftwareAsset;
import com.nis.modules.asset.entity.SoftwareType;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.SoftwareAssetService;
import com.nis.modules.asset.service.SoftwareTypeService;
import com.nis.modules.sys.service.LicenseManagementService;
import com.nis.modules.sys.utils.AdminStatusEnum;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LicenseManagementServiceImpl implements LicenseManagementService {
    private static final Log log = Log.get();

    @Value("${vendorId:25743}")
    private String vendorId;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private SoftwareTypeService softwareTypeService;

    @Autowired
    private SoftwareAssetService softwareAssetService;

    @Override
    public List<JSONObject> queryLicenseProductList(Integer saId) {
        log.info("[queryLicenseProductList] [begin] [saId: {}]", saId);
        // build adminapi
        AdminApi adminApi = this.buildAdminApi(saId);

        // query haspkey by vendorid
        List<JSONObject> haspKeyList = this.queryHaspKeyByVendorId(adminApi, vendorId);
        log.info("[queryLicenseProductList] [saId: {}] [haspKeyList size: {}]", saId, haspKeyList.size());

        List<JSONObject> productList = Tool.ListUtil.list(true);
        for (JSONObject haspKey : haspKeyList) {
            String haspKeyId = StrUtil.toString(haspKey.get("haspid"));
            if (StrUtil.isEmpty(haspKeyId)) {
                continue;
            }

            // query hasp product list
            List<JSONObject> productListByhaspKeyId = this.queryHaspProductList(adminApi, haspKeyId);
            for (JSONObject product : productListByhaspKeyId) {
                if (ObjectUtil.isEmpty(product)) {
                    continue;
                }

                // hasp
                product.set("hasp", haspKey);

                String productId = StrUtil.toString(product.get("productid"));
                Object featureId = JSONUtil.getByPath(product, "productdetails.feature.id");

                // query product feature list
                List<JSONObject> featureList = this.queryProductFeatureList(adminApi, haspKeyId, productId, featureId);
                product.set("features", featureList);

                // query product session list
                List<JSONObject> sessionList = this.queryProductSessionList(adminApi, haspKeyId, productId);
                product.set("sessions", sessionList);

                productList.add(product);
            }
        }

        log.info("[queryLicenseProductList] [saId: {}] [productList size: {}]", saId, productList.size());

        if (log.isDebugEnabled()) {
            log.debug("[queryLicenseProductList] [saId: {}] [productList: {}]", vendorId, JSONUtil.toJsonStr(productList));
        }

        // filter expired product
        List<JSONObject> activationProductList = this.filterExpiredProduct(productList);
        log.info("[queryLicenseProductList] [finshed] [saId: {}] [activationProductList size: {}]", saId, activationProductList.size());
        return activationProductList;
    }

    @Override
    public void downloadC2V(Integer saId, String haspid, HttpServletResponse response) throws Exception {
        log.info("[downloadC2V] [begin] [saId: {}] [haspid: {}]", saId, haspid);
        // build adminapi
        AdminApi adminApi = this.buildAdminApi(saId);

        String scopeXml = StrUtil.concat(true, "<haspscope>",
                "<hasp><id>", haspid, "</id></hasp>",
                "</haspscope>");
        String formatXml = StrUtil.concat(true, "<admin>",
                "<hasp><element name=\"c2v_file\"/></hasp>",
                "</admin>");

        String configXmlStr = adminApi.getConfig(scopeXml, formatXml);
        int lastApiErrorCode = adminApi.getLastApiError();
        log.info("[downloadC2V] [haspid: {}] [getLastApiError: {}]", haspid, lastApiErrorCode);

        AdminStatusEnum adminStatus = AdminStatusEnum.getAdminStatusByCode(lastApiErrorCode);
        if (ObjectUtil.notEqual(AdminStatusEnum.SNTL_ADMIN_STATUS_OK, adminStatus)) {
            log.error("[downloadC2V] [error] [haspid: {}] [code: {}] [status: {}]", haspid, lastApiErrorCode, adminStatus);
            throw new NZException(RCode.LICENSE_REQUEST_ADMINAPI_ERROR.setParam(adminStatus));
        }

        if (log.isDebugEnabled()) {
            log.debug("[downloadC2V] [haspid: {}] [result: {}]", haspid, configXmlStr);
        }

        JSONObject resultJsonObject = JSONUtil.xmlToJson(configXmlStr);
        Object c2vFileJsonObject = JSONUtil.getByPath(resultJsonObject, "admin_response.hasp.c2v_file");
        String c2vXmlFileStr = JSONUtil.toXmlStr((JSONObject) c2vFileJsonObject);

        String filename = StrUtil.concat(true, haspid.toString(), ".c2v");
        byte[] c2vXmlFileStrBytes = c2vXmlFileStr.getBytes();
        int c2vXmlFileStrBytesLength = c2vXmlFileStrBytes.length;
        ResponseUtil.downloadFile(response, filename, c2vXmlFileStrBytes);

        log.info("[downloadC2V] [finshed] [saId: {}] [haspid: {}] [bytesize: {}]", saId, haspid, c2vXmlFileStrBytesLength);
    }

    @Override
    public void uploadV2C(Integer saId, MultipartFile multipartFile) throws Exception {
        log.info("[uploadV2C] [begin] [saId: {}]", saId);
        // build adminapi
        AdminApi adminApi = this.buildAdminApi(saId);

        File tempFile = null;
        try {
            String filename = multipartFile.getOriginalFilename();

            tempFile = Tool.FileUtil.file(Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, filename));
            FileUtils.copyInputStreamToFile(multipartFile.getInputStream(), tempFile);

            String configXmlStr = adminApi.setConfig(StrUtil.concat(true, "file://", tempFile.getAbsolutePath()));
            int lastApiErrorCode = adminApi.getLastApiError();
            log.info("[uploadV2C] [saId: {}] [getLastApiError: {}]", saId, lastApiErrorCode);

            AdminStatusEnum adminStatus = AdminStatusEnum.getAdminStatusByCode(lastApiErrorCode);
            if (ObjectUtil.notEqual(AdminStatusEnum.SNTL_ADMIN_STATUS_OK, adminStatus)) {
                log.error("[uploadV2C] [error] [saId: {}] [code: {}] [status: {}]", saId, lastApiErrorCode, adminStatus);
                throw new NZException(RCode.LICENSE_REQUEST_ADMINAPI_ERROR.setParam(adminStatus));
            }

            if (log.isDebugEnabled()) {
                log.debug("[uploadV2C] [saId: {}] [result: {}]", saId, configXmlStr);
            }

            log.info("[uploadV2C] [finshed] [saId: {}]", saId);
        } catch (Exception e) {
            log.error(e, "[uploadV2C] [error] [saId: {}]", saId);
            throw e;
        } finally {
            FileUtil.del(tempFile);
        }
    }

    /**
     * build adminapi
     *
     * @param saId
     * @return
     */
    private AdminApi buildAdminApi(Integer saId) {
        log.info("[buildAdminApi] [begin] [saId: {}]", saId);

        SoftwareAsset softwareAsset = softwareAssetService.getById(saId);
        ValidateUtils.is(softwareAsset).notNull(RCode.SOFTWARE_ASSET_NOT_EXIST);

        SoftwareType softwareType = softwareTypeService.getById(softwareAsset.getTypeId());
        if (Tool.ObjectUtil.notEqual("ACC", softwareType.getName())) {
            throw new NZException(RCode.LICENSE_MGMT_NOT_ACC_ASSET);
        }

        Map<String, Object> softwareAssetParam = JSONUtil.toBean(StrUtil.utf8Str(softwareAsset.getParams()), Map.class);
        String accIp = Tool.MapUtil.getStr(softwareAssetParam, "acc_ip", "");
        String accPort = Tool.MapUtil.getStr(softwareAssetParam, "acc_port", "1947");
        String accPin = Tool.MapUtil.getStr(softwareAssetParam, "acc_password", "");

        if (StrUtil.isEmpty(accIp) && ObjectUtil.notEqual(-1, softwareAsset.getAssetId())) {
            log.info("[queryLicenseProductList] [fill acc ip] [saId: {}] [assetId: {}]", saId, softwareAsset.getAssetId());
            AssetAsset asset = assetService.getById(softwareAsset.getAssetId());
            accIp = asset.getManageIp();
        }
        ValidateUtils.is(accIp).notNull(RCode.LICENSE_ACC_IP_ISNULL);

        AdminApi adminApi = null;
        if (StrUtil.isEmpty(accPin)) {
            adminApi = new AdminApi(accIp, Integer.valueOf(accPort));
        } else {
            adminApi = new AdminApi(accIp, Integer.valueOf(accPort), accPin);
        }

        int buildAdminApiReturnStatusCode = adminApi.connect();
        log.info("[buildAdminApi] [saId: {}] [connect code: {}]", saId, buildAdminApiReturnStatusCode);

        if (ObjectUtil.equal(AdminStatus.SNTL_ADMIN_STATUS_OK, buildAdminApiReturnStatusCode)) {
            log.info("[buildAdminApi] [finshed] [saId: {}]", saId);
            return adminApi;
        } else {
            // AdminStatus.SNTL_ADMIN_DLL_BROKEN | AdminStatus.SNTL_ADMIN_DLL_LOADED_IN_ANOTHER_CLASSLOADER
            log.error("[buildAdminApi] [error] [saId: {}] [connect code: {}]", saId, buildAdminApiReturnStatusCode);
            throw new NZException(RCode.LICENSE_BUILD_ADMINAPI_ERROR);
        }
    }


    /**
     * query haspkey by vendorid
     *
     * @param adminApi
     * @param vendorId
     * @return
     */
    private List<JSONObject> queryHaspKeyByVendorId(AdminApi adminApi, String vendorId) {
        log.info("[queryHaspKeyByVendorId] [begin] [vendorId: {}]", vendorId);
        String scopeXml = StrUtil.concat(true, "<haspscope>",
                "<vendor><id>", vendorId, "</id></vendor>",
                "</haspscope>");
        String formatXml = StrUtil.concat(true, "<admin>",
                "<hasp><element name=\"*\"/></hasp>",
                "</admin>");

        String configXmlStr = adminApi.getConfig(scopeXml, formatXml);
        int lastApiErrorCode = adminApi.getLastApiError();
        log.info("[queryHaspKeyByVendorId] [vendorId: {}] [getLastApiError: {}]", vendorId, lastApiErrorCode);

        AdminStatusEnum adminStatus = AdminStatusEnum.getAdminStatusByCode(lastApiErrorCode);
        if (ObjectUtil.notEqual(AdminStatusEnum.SNTL_ADMIN_STATUS_OK, adminStatus)) {
            log.error("[queryHaspKeyByVendorId] [error] [vendorId: {}] [code: {}] [status: {}]", vendorId, lastApiErrorCode, adminStatus);
            throw new NZException(RCode.LICENSE_REQUEST_ADMINAPI_ERROR.setParam(adminStatus));
        }

        if (log.isDebugEnabled()) {
            log.debug("[queryHaspKeyByVendorId] [vendorId: {}] [result: {}]", vendorId, configXmlStr);
        }

        JSONObject resultJsonObject = JSONUtil.xmlToJson(configXmlStr);
        Object haspObject = JSONUtil.getByPath(resultJsonObject, "admin_response.hasp");

        List<JSONObject> haspKeyList = Tool.ListUtil.list(true);
        if (haspObject instanceof JSONObject) {
            haspKeyList.add((JSONObject) haspObject);
        } else if (haspObject instanceof JSONArray) {
            haspKeyList.addAll((List) haspObject);
        }

        log.info("[queryHaspKeyByVendorId] [finshed] [vendorId: {}] [haspKeyList size: {}]", vendorId, haspKeyList.size());
        return haspKeyList;
    }

    /**
     * query hasp product list
     *
     * @param adminApi
     * @param haspKeyId
     * @return
     */
    private List<JSONObject> queryHaspProductList(AdminApi adminApi, String haspKeyId) {
        log.info("[queryHaspProductList] [begin] [haspKeyId: {}]", haspKeyId);
        String scopeXml = StrUtil.concat(true, "<haspscope>",
                "<hasp><id>", haspKeyId, "</id></hasp>",
                "</haspscope>");
        String formatXml = StrUtil.concat(true, "<admin>",
                "<product><element name=\"*\"/></product>",
                "</admin>");

        String configXmlStr = adminApi.getConfig(scopeXml, formatXml);
        int lastApiErrorCode = adminApi.getLastApiError();
        log.info("[queryHaspProductList] [haspKeyId: {}] [getLastApiError: {}]", haspKeyId, lastApiErrorCode);

        AdminStatusEnum adminStatus = AdminStatusEnum.getAdminStatusByCode(lastApiErrorCode);
        if (ObjectUtil.notEqual(AdminStatusEnum.SNTL_ADMIN_STATUS_OK, adminStatus)) {
            log.error("[queryHaspProductList] [error] [haspKeyId: {}] [code: {}] [status: {}]", haspKeyId, lastApiErrorCode, adminStatus);
            throw new NZException(RCode.LICENSE_REQUEST_ADMINAPI_ERROR.setParam(adminStatus));
        }

        if (log.isDebugEnabled()) {
            log.debug("[queryHaspProductList] [haspKeyId: {}] [result: {}]", haspKeyId, configXmlStr);
        }

        JSONObject resultJsonObject = JSONUtil.xmlToJson(configXmlStr);
        Object productObject = JSONUtil.getByPath(resultJsonObject, "admin_response.product");

        List<JSONObject> productList = Tool.ListUtil.list(true);
        if (productObject instanceof JSONObject) {
            productList.add((JSONObject) productObject);
        } else if (productObject instanceof JSONArray) {
            productList.addAll((List) productObject);
        }

        for (Object productNode : productList) {
            JSONObject tempProduct = (JSONObject) productNode;
            tempProduct.set("id", tempProduct.get("productid"));
            tempProduct.set("name", tempProduct.get("productname"));
        }
        log.info("[queryHaspProductList] [finshed] [haspKeyId: {}] [productList size: {}]", haspKeyId, productList.size());
        return productList;
    }

    /**
     * query product feature list
     *
     * @param adminApi
     * @param haspKeyId
     * @param productId
     * @param featureIdObject
     * @return
     */
    private List<JSONObject> queryProductFeatureList(AdminApi adminApi, String haspKeyId, String productId, Object featureIdObject) {
        log.info("[queryProductFeatureList] [begin] [haspKeyId: {}] [productId: {}] [featureId: {}]", haspKeyId, productId, featureIdObject);

        boolean isFeatureNumber = Tool.NumberUtil.isNumber(featureIdObject.toString());
        List featureIdList = Tool.ListUtil.list(true);
        if (isFeatureNumber) {
            featureIdList.add(featureIdObject.toString());
        } else {
            featureIdList.addAll((List) featureIdObject);
        }

        String formatXml = StrUtil.concat(true, "<admin>",
                "<feature><element name=\"*\"/></feature>",
                "</admin>");

        List queryFeatureList = Tool.ListUtil.list(true);
        for (Object featureId : featureIdList) {
            String scopeXml = StrUtil.concat(true, "<haspscope>",
                    "<hasp><id>", haspKeyId, "</id></hasp>",
                    "<product><id>", productId, "</id></product>",
                    "<feature><id>", featureId.toString(), "</id></feature>",
                    "</haspscope>");

            String configXmlStr = adminApi.getConfig(scopeXml, formatXml);
            int lastApiErrorCode = adminApi.getLastApiError();
            log.info("[queryProductFeatureList] [haspKeyId: {}] [productId: {}] [featureId: {}] [getLastApiError: {}]", haspKeyId, productId, featureId, lastApiErrorCode);

            AdminStatusEnum adminStatus = AdminStatusEnum.getAdminStatusByCode(lastApiErrorCode);
            if (ObjectUtil.notEqual(AdminStatusEnum.SNTL_ADMIN_STATUS_OK, adminStatus)) {
                log.error("[queryProductFeatureList] [error] [haspKeyId: {}] [productId: {}] [featureId: {}] [code: {}] [status: {}]", haspKeyId, productId, featureId, lastApiErrorCode, adminStatus);
                throw new NZException(RCode.LICENSE_REQUEST_ADMINAPI_ERROR.setParam(adminStatus));
            }

            if (log.isDebugEnabled()) {
                log.debug("[queryProductFeatureList] [haspKeyId: {}] [productId: {}] [featureId: {}] [result: {}]", haspKeyId, productId, featureId, configXmlStr);
            }

            JSONObject resultJsonObject = JSONUtil.xmlToJson(configXmlStr);
            Object featureObject = JSONUtil.getByPath(resultJsonObject, "admin_response.feature");
            if (featureObject instanceof JSONObject) {
                queryFeatureList.add((JSONObject) featureObject);
            } else if (featureObject instanceof JSONArray) {
                queryFeatureList.addAll((List) featureObject);
            }
        }

        List<JSONObject> featureList = Tool.ListUtil.list(true);
        for (Object featureObject : queryFeatureList) {
            JSONObject tempFeatureNode = (JSONObject) featureObject;
            Object licenserestrictions = JSONUtil.getByPath(tempFeatureNode, "licenserestrictions");
            String licenseType = "Perpetual";
            Object expDate = null;
            if (!StrUtil.equalsIgnoreCase("Perpetual", StrUtil.toString(licenserestrictions))) {
                licenseType = StrUtil.toString(JSONUtil.getByPath((JSON) licenserestrictions, "license_type"));
                expDate = JSONUtil.getByPath((JSON) licenserestrictions, "exp_date");
            }

            JSONObject featureInfo = new JSONObject();
            featureInfo.set("featureid", tempFeatureNode.get("featureid"));
            featureInfo.set("featurename", tempFeatureNode.get("featurename"));
            featureInfo.set("licenseType", licenseType);
            featureInfo.set("expDate", expDate);
            featureList.add(featureInfo);
        }
        featureList = featureList.stream().distinct().collect(Collectors.toList());
        log.info("[queryProductFeatureList] [finshed] [haspKeyId: {}] [productId: {}] [featureList size: {}]", haspKeyId, productId, featureList.size());
        return featureList;
    }


    /**
     * query product session list
     *
     * @param adminApi
     * @param haspKeyId
     * @param productId
     * @return
     */
    private List<JSONObject> queryProductSessionList(AdminApi adminApi, String haspKeyId, String productId) {
        log.info("[queryProductSessionList] [begin] [haspKeyId: {}] [productId: {}]", haspKeyId, productId);
        String scopeXml = StrUtil.concat(true, "<haspscope>",
                "<hasp><id>", haspKeyId, "</id></hasp>",
                "<product><id>", productId, "</id></product>",
                "</haspscope>");
        String formatXml = StrUtil.concat(true, "<admin><session>",
                "<element name=\"machine\"/>",
                "<element name=\"client\"/>",
                "<element name=\"logintime\"/>",
                "</session></admin>");

        String configXmlStr = adminApi.getConfig(scopeXml, formatXml);
        int lastApiErrorCode = adminApi.getLastApiError();
        log.info("[queryProductSessionList] [haspKeyId: {}] [productId: {}] [getLastApiError: {}]", haspKeyId, productId, lastApiErrorCode);

        AdminStatusEnum adminStatus = AdminStatusEnum.getAdminStatusByCode(lastApiErrorCode);
        if (ObjectUtil.notEqual(AdminStatusEnum.SNTL_ADMIN_STATUS_OK, adminStatus)) {
            log.error("[queryProductSessionList] [error] [haspKeyId: {}] [productId: {}] [code: {}] [status: {}]", haspKeyId, productId, lastApiErrorCode, adminStatus);
            throw new NZException(RCode.LICENSE_REQUEST_ADMINAPI_ERROR.setParam(adminStatus));
        }

        if (log.isDebugEnabled()) {
            log.debug("[queryProductSessionList] [haspKeyId: {}] [productId: {}] [result: {}]", haspKeyId, productId, configXmlStr);
        }

        JSONObject resultJsonObject = JSONUtil.xmlToJson(configXmlStr);
        Object sessionObject = JSONUtil.getByPath(resultJsonObject, "admin_response.session");

        List<JSONObject> sessionList = Tool.ListUtil.list(true);
        if (sessionObject instanceof JSONObject) {
            sessionList.add((JSONObject) sessionObject);
        } else if (sessionObject instanceof JSONArray) {
            sessionList.addAll((List) sessionObject);
        }
        log.info("[queryProductSessionList] [finshed] [haspKeyId: {}] [productId: {}] [sessionList size: {}]", haspKeyId, productId, sessionList.size());
        return sessionList;
    }

    /**
     * filter expired product
     *
     * @param productList
     * @return
     */
    private List<JSONObject> filterExpiredProduct(List<JSONObject> productList) {
        log.info("[filterExpiredProduct] [begin] [productList size: {}]", productList.size());
        List<JSONObject> activationProductList = Tool.ListUtil.list(true);

        Map<Object, List<JSONObject>> productListGroupById = productList.stream()
                .collect(Collectors.groupingBy(product -> product.get("productid")));

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
        for (Map.Entry<Object, List<JSONObject>> entry : productListGroupById.entrySet()) {
            Object productId = entry.getKey();
            List<JSONObject> tempProductList = entry.getValue();

            // 永久证书
            List<JSONObject> perpetualTypeProductList = tempProductList.stream().filter(product -> {
                List<String> licenseTypeList = (List<String>) JSONUtil.getByPath(product, "features.licenseType");
                return licenseTypeList.contains("Perpetual");
            }).collect(Collectors.toList());
            log.info("[filterExpiredProduct] [productId: {}] [has perpetual]", productId, Tool.CollUtil.isNotEmpty(perpetualTypeProductList));

            // 不存在永久证书
            if (CollUtil.isEmpty(perpetualTypeProductList)) {
                long currentTimeMillis = System.currentTimeMillis();
                // 获取当前生效范围最大的证书
                JSONObject largestActivationRange = tempProductList.stream().filter(product -> {
                            // 当前时间 大于 锁生效时间
                            String haspProddateStr = StrUtil.toString(JSONUtil.getByPath(product, "hasp.proddate"));
                            DateTime haspProddate = DateUtil.parse(haspProddateStr, dateFormat);
                            return currentTimeMillis > haspProddate.getTime();
                        }).filter(product -> {
                            // 当前时间 小于 锁过期时间
                            List<Integer> expDateList = (List<Integer>) JSONUtil.getByPath(product, "features.expDate");
                            // 最大的过期时间
                            Integer maxExpDateTime = expDateList.stream().sorted(Comparator.comparing(Integer::intValue).reversed()).findFirst().get();
                            return currentTimeMillis < (long) maxExpDateTime * 1000;
                        })
                        .sorted((p1, p2) -> {
                            List<Integer> p1ExpDateList = (List<Integer>) JSONUtil.getByPath(p1, "features.expDate");
                            Integer p1MaxExpDateTime = p1ExpDateList.stream().sorted(Comparator.comparing(Integer::intValue).reversed()).findFirst().get();

                            List<Integer> p2ExpDateList = (List<Integer>) JSONUtil.getByPath(p2, "features.expDate");
                            Integer p2MaxExpDateTime = p2ExpDateList.stream().sorted(Comparator.comparing(Integer::intValue).reversed()).findFirst().get();
                            return Tool.NumberUtil.compare(p1MaxExpDateTime, p2MaxExpDateTime) * -1;
                        })
                        .findFirst()
                        .orElse(null);

                if (ObjectUtil.isNotNull(largestActivationRange)) {
                    log.info("[filterExpiredProduct] [has activation] [productId: {}]", productId);
                    activationProductList.add(largestActivationRange);
                } else {
                    log.warn("[filterExpiredProduct] [no activation] [productId: {}]", productId);
                }
            } else {
                // 获取签发时间最后的一个 永久 证书
                JSONObject last = perpetualTypeProductList.stream()
                        .sorted((p1, p2) -> {
                            String p1HaspProddate = StrUtil.toString(JSONUtil.getByPath(p1, "hasp.proddate"));
                            String p2HaspProddate = StrUtil.toString(JSONUtil.getByPath(p2, "hasp.proddate"));
                            return DateUtil.compare(DateUtil.parse(p1HaspProddate, dateFormat), DateUtil.parse(p2HaspProddate, dateFormat)) * -1;
                        })
                        .findFirst()
                        .get();
                activationProductList.add(last);
            }
        }
        log.info("[filterExpiredProduct] [finshed] [activationProductList size: {}]", activationProductList.size());
        return activationProductList;
    }
}
