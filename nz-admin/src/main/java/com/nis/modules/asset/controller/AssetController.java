package com.nis.modules.asset.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.romote.entity.Account;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 资产  controller
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@RestController
@RequestMapping("/asset")
public class AssetController {

    @Autowired
    private AssetService assetService;

    @Autowired
    private SftpUtils sftpUtils;

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 资产请求列表
     *
     * @param params
     * @return
     */
    @GetMapping
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = assetService.queryPage(params);
        return R.ok(page);
    }

    /**
     * 保存
     *
     * @param asset
     * @return
     */
    @PostMapping
    @SysLog("save asset")
    public R save(Asset asset, @RequestParam(value = "cert", required = false) MultipartFile[] sshKeys) {
        ValidateUtils.is(asset.getSn()).notNull(RCode.ASSET_SN_ISNULL).maxLength(64, RCode.ASSET_SN_TOO_LONG)
                .and(asset.getModelId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(asset.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(asset.getHost()).notNull(RCode.ASSET_HOST_ISNULL).ip(RCode.ASSET_HOST_FORMAT);

        Integer exporterFlag = 1;
        if (exporterFlag.equals(asset.getExporter()) && CollectionUtils.isEmpty(asset.getAccounts())) {
            // 指名了需要安装 node exporter 账号信息不能为空
            throw new NZException(RCode.ASSET_ACCOUNT_ISNULL);
        }

        if (CollectionUtils.isNotEmpty(asset.getAccounts())) {

            // 目前按照一个asset 对应 一个account实现
            Account account = asset.getAccounts().get(0);
            assetService.validateAccount(account);

            // 密码登录，密码不能为空，证书登录，证书不能为空
            if (Constant.AUTH_PASSWORD.equals(account.getAuthType())) {
                ValidateUtils.is(account.getPwd()).notNull(RCode.ACCOUNT_PWD_ISNULL);
            } else {
                if (sshKeys.length < 1) {
                    // 文件数量不足
                    throw new NZException(RCode.ACCOUNT_FILE_INSUFFICIENT);
                }
                ValidateUtils.is(sshKeys[0]).notNull(RCode.ACCOUNT_KEYFILE_ISNULL);
            }
        }
        // 购买时间格式校验
        if (StringUtils.isNotEmpty(asset.getPurchaseDate())) {
            String format = "yyyy-MM-dd";
            ValidateUtils.is(asset.getPurchaseDate()).date(format, RCode.ASSET_PURCHASEDATE_FORMAT);

            SimpleDateFormat sd = new SimpleDateFormat(format);
            try {
                sd.setLenient(false);
                sd.parse(asset.getPurchaseDate());
            } catch (Exception e) {
                // 日期不合法
                throw new NZException(RCode.ASSET_PURCHASEDATE_UNAVAILABLE);
            }
        } else {
            asset.setPurchaseDate(null);
        }

        // 克隆登录服务器的账号信息
        Account accountCli = JSONObject.parseObject(JSON.toJSONString(asset.getAccounts().get(0)), Account.class);
        assetService.saveOrUpdateAsset(asset, sshKeys);
        String msg = "The asset and account are saved successfully";

        if (exporterFlag.equals(asset.getExporter())) {
            List<SysConfigEntity> sysConfigs = sysConfigService.list(new QueryWrapper<SysConfigEntity>().eq("status", 1));
            Map<String, String> configs = new HashMap<>();
            for (SysConfigEntity config : sysConfigs) {
                configs.put(config.getParamKey(), config.getParamValue());
            }

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

                // 登录
                sftpUtils.loginIn(asset.getHost(), accountCli.getPort(), accountCli.getUser(), accountCli.getAuthType(), accountCli.getPwd(), accountCli.getPrivateKey());

                // 上传
                sftpUtils.upload(targetExporterPath, buildNodeExporterPath, buildNodeExporterName, shellName);

                // 执行脚本让exporter运行
                String execCommand = "chmod +x " + targetExporterPath + "/" + shellName + " && " + targetExporterPath + "/" + shellName;
                sftpUtils.exec(execCommand);
            } catch (Exception e) {
                // asset account 保存成功 但是 文件上传失败
                return R.error(RCode.ASSET_UPLOAD_FAIL);
            } finally {
                // 退出
                sftpUtils.logout();
            }
            msg += " and the file is uploaded successfully.";
        }
        return R.ok(msg);
    }

    /**
     * 修改
     *
     * @param asset
     * @return
     */
    @PutMapping
    @SysLog("update asset")
    public R update(Asset asset, @RequestParam(value = "cert", required = false) MultipartFile[] sshKeys) {
        ValidateUtils.is(asset.getId()).notNull(RCode.ASSET_ID_ISNULL)
                .and(asset.getSn()).notNull(RCode.ASSET_SN_ISNULL).maxLength(64, RCode.ASSET_SN_TOO_LONG)
                .and(asset.getModelId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(asset.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(asset.getHost()).notNull(RCode.ASSET_HOST_ISNULL).ip(RCode.ASSET_HOST_FORMAT);


        if (CollectionUtils.isNotEmpty(asset.getAccounts())) {
            // 记录当前获取的下标
            /*int currSshKeys = 0;

            for (int i = 0; i < asset.getAccounts().size(); i++) {
                Account account = asset.getAccounts().get(i);
                // 修改 id 不能为null
                ValidateUtils.is(account.getId()).notNull(RCode.ACCOUNT_ID_ISNULL);
                // 证书登录 ssh
                if (Constant.AUTH_CERTIFICATE.equals(account.getAuthType())) {
                    if (sshKeys.length < 1 || currSshKeys >= sshKeys.length) {
                        // 文件数量不足
                        throw new NZException(RCode.ACCOUNT_FILE_INSUFFICIENT);
                    } else {
                        assetService.validateAccount(account, sshKeys[currSshKeys++]);
                    }
                } else {
                    // 用户名 密码登录
                    assetService.validateAccount(asset.getAccounts().get(i), null);
                }
            }*/

            // 目前按照一个asset 对应 一个account实现
            Account account = asset.getAccounts().get(0);
            ValidateUtils.is(account.getId()).notNull(RCode.ACCOUNT_ID_ISNULL);
            assetService.validateAccount(account);
        }

        // 购买时间格式校验
        if (StringUtils.isNotEmpty(asset.getPurchaseDate())) {
            String format = "yyyy-MM-dd";
            ValidateUtils.is(asset.getPurchaseDate()).date(format, RCode.ASSET_PURCHASEDATE_FORMAT);

            SimpleDateFormat sd = new SimpleDateFormat(format);
            try {
                sd.setLenient(false);
                sd.parse(asset.getPurchaseDate());
            } catch (Exception e) {
                // 日期不合法
                throw new NZException(RCode.ASSET_PURCHASEDATE_UNAVAILABLE);
            }
        } else {
            asset.setPurchaseDate(null);
        }

        assetService.saveOrUpdateAsset(asset, sshKeys);
        return R.ok();
    }

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog("delete asset")
    public R delete(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_ID_ISNULL);

        assetService.delete(ids);
        return R.ok();
    }
}