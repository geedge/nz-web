package com.nis.modules.asset.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.agent.util.TalonUtil;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import freemarker.template.Template;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Asset Talon Rest Controller
 *
 * @author shizhendong
 * @date 2023-02-28
 */
@RestController
public class AssetTalonController {

    private static final Log log = Log.get();

    @Value("${server.ssl.enabled}")
    private String sslEnabled;

    @Value("${nezha.installPath:install/}")
    private String installPath;

    @Autowired
    private AssetAssetService assetService;


    /**
     * get Talon Install Script
     *
     * @param request
     * @param assetId
     * @param token
     * @return
     */
    @GetMapping("/asset/talon/{assetId}/install.sh")
    public String getTalonInstallScript(HttpServletRequest request,
                                        @PathVariable("assetId") Integer assetId,
                                        @RequestHeader(name = "Authorization") String token) {
        log.info("[getTalonInstallScript] [begin] [assetId: {}]", assetId);

        // validate
        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL);

        AssetAsset asset = assetService.getById(assetId);
        ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);

        // nz-talon installation script template
        File talonInstallShTmpl = CommonUtils.getResourceFile(installPath + "install-talon.sh.template");
        log.info("[getTalonInstallScript] [nz-talon installation script template] [exist: {}]", Tool.FileUtil.exist(talonInstallShTmpl));

        if (Tool.FileUtil.exist(talonInstallShTmpl)) {
            log.info("[getTalonInstallScript] [generate installation script] [begin] [assetId: {}]", assetId);

            // web component ip
            String localhost = request.getLocalAddr();
            Integer localPort = request.getLocalPort();

            // talon component ip
            String remoteHost = request.getRemoteHost();

            Map<Object, Object> tmplVars = Tool.MapUtil.builder()
                    .put("remoteIP", localhost)
                    .put("remotePort", String.valueOf(localPort))
                    .put("localhost", remoteHost)
                    .put("webToken", token)
                    .put("sslEnabled", sslEnabled)
                    .put("assetId", assetId)
                    .map();
            try {
                String scriptTmplContent = Tool.FileUtil.readUtf8String(talonInstallShTmpl);

                Template template = TemplateUtil.stringToTemplate(scriptTmplContent, "talonInstallTemplate");
                String scriptContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, tmplVars);

                log.info("[getTalonInstallScript] [generate installation script] [finshed] [assetId: {}]", assetId);
                return scriptContent;
            } catch (Exception e) {
                log.error(e, "[getTalonInstallScript] [error] [assetId: {}]", assetId);
                return "echo 'Internal server error'";
            }
        } else {
            log.warn("[getTalonInstallScript] [nz-talon installation script template does not exist]");
            return "load script failed !";
        }
    }


    /**
     * talon Install Package Download
     *
     * @param response
     * @param os
     */
    @GetMapping("/asset/talon/download")
    public void talonInstallPackageDownload(HttpServletResponse response,
                                            @RequestParam(defaultValue = "centos", required = false) String os) {
        log.info("[talonInstallPackageDownload] [begin] [os: {}]", os);

        File rpmFile = TalonUtil.getTalonRpmPackage(os, installPath);
        try {
            Tool.ResponseUtil.downloadFile(response, rpmFile);

            log.info("[talonInstallPackageDownload] [finshed]");
        } catch (Exception e) {
            log.error(e, "[talonInstallPackageDownload] [error]");
            throw new NZException("file download error", 500);
        }
    }


    /**
     * talon Auto Register
     *
     * @param requestBody
     * @return
     */
    @PostMapping("/asset/talon/register")
    @RequiresPermissions({"asset:update"})
    public R talonAutoRegister(@RequestBody Map<String, Object> requestBody) {
        requestBody = Tool.MapUtil.defaultIfEmpty(requestBody, Tool.MapUtil.newHashMap());

        log.info("[talonAutoRegister] [begin] [body: {}]", JSONUtil.toJsonStr(requestBody));

        // validate
        Object port = requestBody.get("port");
        Object token = requestBody.get("token");
        ValidateUtils.is(port).notNull(RCode.ASSET_CLIENT_PROT_ISNULL)
                .and(token).notNull(RCode.ASSET_CLIENT_TOKEN_ISNULL);

        // assetId and manageIp cannot be empty at the same time
        if (BooleanUtil.and(ObjectUtil.isNull(requestBody.get("assetId")), ObjectUtil.isNull(requestBody.get("ip")))) {
            throw new NZException(RCode.ASSET_ID_IP_ISNULL);
        }

        List<AssetAsset> assets = assetService.list(new LambdaUpdateWrapper<AssetAsset>()
                .eq(ObjectUtil.isNotNull(requestBody.get("assetId")), AssetAsset::getId, StrUtil.toString(requestBody.get("assetId")))
                .in(ObjectUtil.isNotNull(requestBody.get("ip")), AssetAsset::getManageIp, Arrays.asList(StrUtil.toString(requestBody.get("ip")).split(",")))
        );

        if (CollectionUtil.isEmpty(assets)){
            throw  new NZException(RCode.ASSET_NOT_EXIST);
        }

        // update asset client info
        for (AssetAsset asset : assets) {
            LambdaUpdateWrapper updateWrapper = new LambdaUpdateWrapper<AssetAsset>()
                    .set(AssetAsset::getClientEnable, 1)
                    .set(AssetAsset::getClientAddress, asset.getManageIp())
                    .set(AssetAsset::getClientPort, Tool.NumberUtil.parseInt(Tool.StrUtil.toString(port)))
                    .set(AssetAsset::getClientToken, Tool.StrUtil.toString(token))
                    .eq(AssetAsset::getId, asset.getId());

            assetService.update(updateWrapper);
        }
        List<Integer> ids = assets.stream().map(o -> o.getId()).collect(Collectors.toList());

        log.info("[talonAutoRegister] [finshed] [assetId: {}]", ids.toString());
        return R.ok().putData("id", ids.toString());
    }


    /**
     * refresh Talon Token
     *
     * @param requestBody
     * @return
     */
    @PostMapping("/asset/talon/token/refresh")
    @RequiresPermissions({"asset:update"})
    public R refreshTalonToken(@RequestBody Map<String, Object> requestBody) {
        requestBody = Tool.MapUtil.defaultIfEmpty(requestBody, Tool.MapUtil.newHashMap());

        log.info("[refreshTalonToken] [begin] [body: {}]", JSONUtil.toJsonStr(requestBody));

        Object token = requestBody.get("token");
        Object assetId = requestBody.get("assetId");

        // validate
        ValidateUtils.is(token).notNull(RCode.ASSET_CLIENT_TOKEN_ISNULL)
                .and(assetId).notNull(RCode.ASSET_ID_ISNULL);

        String assetIdStr = Tool.StrUtil.toString(assetId);

        AssetAsset asset = assetService.getById(assetIdStr);
        ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);

        // ip:port
        String targetToken = Tool.StrUtil.toString(token);
        String targetUrl = String.format("%s:%s", asset.getManageIp(), asset.getClientPort());
        String targetPath = "/auth/refresh";
        Map requestParam = Tool.MapUtil.empty();

        log.info("[refreshTalonToken] [request talon api begin] [url: {}] [path: {}] [Authorization: {}]", targetUrl, targetPath, targetToken);

        String resultObject = TalonUtil.getForObject(targetUrl, targetToken, targetPath, requestParam, String.class);

        log.info("[refreshTalonToken] [request talon api success] [result: {}]", resultObject);
        if (Tool.StrUtil.isNotEmpty(resultObject)) {
            Object code = JSONPath.read(resultObject, "code");
            log.info("[refreshTalonToken] [talon refresh api result] [code: {}]", code);
            if (Tool.ObjectUtil.notEqual(HttpStatus.HTTP_OK, code)) {
                log.warn("[refreshTalonToken] [talon refresh api request error] [msg: {}]", JSONPath.read(resultObject, "msg"));
                return R.error(500, "token refresh failed");
            }

            // 刷新 talon token 后，更新 asset client token
            Object refreshToken = JSONPath.read(resultObject, "data.token");
            LambdaUpdateWrapper updateWrapper = new LambdaUpdateWrapper<AssetAsset>()
                    .set(AssetAsset::getClientToken, Tool.StrUtil.toString(refreshToken))
                    .eq(AssetAsset::getId, asset.getId());
            assetService.update(updateWrapper);

            log.info("[refreshTalonToken] [update asset client token] [assetId: {}] [token: {}]", asset.getId(), Tool.StrUtil.toString(refreshToken));
            return R.ok().putData("token", Tool.StrUtil.toString(refreshToken));
        } else {
            log.error("[refreshTalonToken] [request talon api failed.] [assetId: {}] [Authorization token: {}]", asset.getId(), targetToken);
            return R.error(500, "token refresh failed");
        }
    }

}
