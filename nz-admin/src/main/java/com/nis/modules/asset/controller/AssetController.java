package com.nis.modules.asset.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.terminal.entity.Account;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


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
    private SysConfigService sysConfigService;

    @Autowired
    private SysDictService sysDictService;

    /**
     * 资产请求列表
     *
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ASSET)
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
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.ASSET)
    public R save(@RequestBody Asset asset) {
        ValidateUtils.is(asset.getSn()).notNull(RCode.ASSET_SN_ISNULL).maxLength(64, RCode.ASSET_SN_TOO_LONG)
                .and(asset.getModelId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(asset.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(asset.getHost()).notNull(RCode.ASSET_HOST_ISNULL).ip(RCode.ASSET_HOST_FORMAT);

        if (Constant.ASSET_BUILD_NODE_EXPORTER_FLAG.equals(asset.getExporter()) && CollectionUtils.isEmpty(asset.getAccounts())) {
            // 指名了需要安装 node exporter 账号信息不能为空
            throw new NZException(RCode.ASSET_ACCOUNT_ISNULL);
        }


        Account accountCli = null;
        if (CollectionUtils.isNotEmpty(asset.getAccounts())) {
            // 克隆登录服务器的账号信息
            accountCli = JSONObject.parseObject(JSON.toJSONString(asset.getAccounts().get(0)), Account.class);

            assetService.validateAccount(accountCli);

            // 密码登录，密码不能为空，证书登录，证书不能为空
            if (Constant.AUTH_PASSWORD.equals(accountCli.getAuthType())) {
                ValidateUtils.is(accountCli.getPwd()).notNull(RCode.ACCOUNT_PWD_ISNULL);
            } else {
                ValidateUtils.is(accountCli.getPrivateKey()).notNull(RCode.ACCOUNT_KEYFILE_ISNULL);
            }
        }
        // 购买时间格式校验
        if (StringUtils.isNotEmpty(asset.getPurchaseDate())) {
            ValidateUtils.is(asset.getPurchaseDate()).date("yyyy-MM-dd", RCode.ASSET_PURCHASEDATE_FORMAT);
        } else {
            asset.setPurchaseDate(null);
        }

        assetService.saveOrUpdateAsset(asset);

        if (Constant.ASSET_BUILD_NODE_EXPORTER_FLAG.equals(asset.getExporter())) {
            List<SysConfigEntity> sysConfigs = sysConfigService.list(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getStatus, 1));
            Map<String, String> configs = sysConfigs.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

            R result = assetService.uploadBuildExporter(configs, asset, accountCli);
            return result;
        }
        return R.ok();
    }

    /**
     * 修改
     *
     * @param asset
     * @return
     */
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ASSET)
    public R update(@RequestBody Asset asset) {
        ValidateUtils.is(asset.getId()).notNull(RCode.ASSET_ID_ISNULL)
                .and(asset.getSn()).notNull(RCode.ASSET_SN_ISNULL).maxLength(64, RCode.ASSET_SN_TOO_LONG)
                .and(asset.getModelId()).notNull(RCode.ASSET_MODELID_ISNULL)
                .and(asset.getIdcId()).notNull(RCode.IDC_IDCID_ISNULL)
                .and(asset.getHost()).notNull(RCode.ASSET_HOST_ISNULL).ip(RCode.ASSET_HOST_FORMAT);


        if (CollectionUtils.isNotEmpty(asset.getAccounts())) {
            // 目前按照一个asset 对应 一个account实现
            Account account = asset.getAccounts().get(0);
            // 此处取消对 account id 的校验 原因： asset 修改的时候 account 可以是新增 也可以是修改
            // ValidateUtils.is(account.getId()).notNull(RCode.ACCOUNT_ID_ISNULL);
            assetService.validateAccount(account);
        }

        // 购买时间格式校验
        if (StringUtils.isNotEmpty(asset.getPurchaseDate())) {
            ValidateUtils.is(asset.getPurchaseDate()).date("yyyy-MM-dd", RCode.ASSET_PURCHASEDATE_FORMAT);
        } else {
            asset.setPurchaseDate(null);
        }

        assetService.saveOrUpdateAsset(asset);
        return R.ok();
    }

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ASSET)
    public R delete(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_ID_ISNULL);

        assetService.delete(ids);
        return R.ok();
    }

    /**
     * 批量导入
     *
     * @param excelFile
     * @return
     */
    @PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT,type = TypeEnum.ASSET)
    public R importAssetsFromExcel(MultipartFile excelFile) {
        ValidateUtils.is(excelFile).notNull(RCode.ASSET_IMPORT_FILE_ISNULL);
        // 本次导入的 uuid
        String seqUUID = UUID.randomUUID().toString();
        Map map = assetService.importAssetsFromExcel(excelFile, seqUUID);
        return R.ok(map);
    }

    /**
     * 导出
     *
     * @param response
     * @param params
     */
    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT,type = TypeEnum.ASSET)
    public void exportAssetsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) {

        Map<String, Object> queryParam = new HashMap<>(2);
        queryParam.put("type", Constant.DICT_ASSET_STATE_TYPE);
        List<SysDictEntity> assetStates = sysDictService.queryList(queryParam);
        Map<Integer, String> statesMap = new HashMap<>(4);
        for (SysDictEntity dict : assetStates) {
            statesMap.put(dict.getCode(), dict.getValue());
        }

        PageUtils page = assetService.queryPage(params);
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
                if (StringUtils.isNotEmpty(account.getPwd())) {
                    // pwd
                    row1.createCell(14).setCellValue(account.getPwd());
                }
                if (StringUtils.isNotEmpty(account.getPrivateKey())) {
                    // sshKeys
                    row1.createCell(15).setCellValue(account.getPrivateKey());
                }
                // port
                row1.createCell(16).setCellValue(account.getPort());
                if (StringUtils.isNotEmpty(account.getUserTip())) {
                    // userTip
                    row1.createCell(17).setCellValue(account.getUserTip());
                }
                if (StringUtils.isNotEmpty(account.getPasswordTip())) {
                    // passwordTip
                    row1.createCell(18).setCellValue(account.getPasswordTip());
                }
                if (StringUtils.isNotEmpty(account.getReloginPasswordTip())) {
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

    /**
     * asset导入撤销
     *
     * @return
     */
    @DeleteMapping("/cancelImport")
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.ASSET_CANCELIMPORTSEQ_ISNULL);

        assetService.cancelImport(seq);
        return R.ok();
    }
}