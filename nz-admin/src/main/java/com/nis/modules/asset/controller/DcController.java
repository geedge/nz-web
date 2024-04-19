package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.DcService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 数据中心  controller
 *
 * @author shizhendong
 * @date 2021-4-6
 */
@RestController
@RequestMapping("/dc")
public class DcController {

    @Autowired
    private DcService dcService;

    @Autowired
    private CabinetService cabinetService;

    /**
     * dc详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.DATACENTER)
    public R queryDcDetails(@PathVariable("id") Integer id) {
        return R.ok(dcService.queryDcDetails(id));
    }

    /**
     * 数据中心请求列表
     *
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.DATACENTER)
    public R queryDcPage(@RequestParam Map<String, Object> params) {
        PageUtils page = dcService.queryPage(params);
        return R.ok(page);
    }


    /**
     * 保存
     *
     * @param dc
     * @return
     */
    @PostMapping
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.DATACENTER)
    @RequiresPermissions({"dc:save"})
    public R saveDc(@RequestBody Dc dc) {
        ValidateUtils.is(dc.getName()).notNull(RCode.DC_NAME_ISNULL).maxLength(64)
                .and(dc.getState()).notNull(RCode.DC_STATE_ISNULL)
                .and(dc.getY()).min(1, RCode.DC_Y_MINIMUM)
                .and(dc.getX()).min(1, RCode.DC_X_MINIMUM)
                .and(dc.getLocation()).maxLength(256)
                .and(dc.getTel()).maxLength(128)
                .and(dc.getRemark()).maxLength(256);

        return R.ok().putData("id", dcService.saveDc(dc));
    }

    /**
     * 修改
     *
     * @param dc
     * @return
     */
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.DATACENTER)
    @RequiresPermissions({"dc:update"})
    public R updateDc(@RequestBody Dc dc) {
        ValidateUtils.is(dc.getId()).notNull(RCode.DC_ID_ISNULL)
                .and(dc.getName()).notNull(RCode.DC_NAME_ISNULL).maxLength(64)
                .and(dc.getState()).notNull(RCode.DC_STATE_ISNULL)
                .and(dc.getY()).min(1, RCode.DC_Y_MINIMUM)
                .and(dc.getX()).min(1, RCode.DC_X_MINIMUM)
                .and(dc.getLocation()).maxLength(256)
                .and(dc.getTel()).maxLength(128)
                .and(dc.getRemark()).maxLength(256);

        return R.ok().putData("id", dcService.updateDc(dc));
    }


    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.DATACENTER)
    @RequiresPermissions({"dc:delete"})
    public R deleteDcByIds(@RequestParam(required = false, defaultValue = "0") Integer force, Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.DC_ID_ISNULL);

        return dcService.delete(force, ids);
    }


    @GetMapping("/template")
    @SysLog(operation = OperationEnum.DOWNLOAD, type = TypeEnum.DATACENTER)
    public void getTemplateByLanguage(HttpServletResponse response,
                                      @RequestParam(required = false, defaultValue = "en") String language,
                                      @RequestParam(required = false, defaultValue = "1") String format) throws IOException {

        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(format)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }

        dcService.getTemplateByLanguage(response, format);
    }


    @PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.DATACENTER)
    @RequiresPermissions({"dc:save"})
    public R importDcData(@RequestParam(value = "file") MultipartFile excelFile, HttpServletRequest request, @RequestParam Map<String, Object> params) throws IOException {
        String language = request.getHeader(Constant.LANGUAGE);
        ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);
        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }

        Map info = dcService.importDcData(excelFile, params);
        return R.ok(info);
    }

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.DATACENTER)
    public void exportDataCenters(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
        Object format = params.get("format");
        String formatStr = format == null ? "1" : format.toString();
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(formatStr)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }
        params.put("format", formatStr);

        dcService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.CANCEL, type = TypeEnum.DATACENTER)
    public R cancelImport(@RequestParam(value = "seq", required = false) String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);

        dcService.cancelImport(seq);
        return R.ok();
    }

    /**
     * 机柜详情
     *
     * @param id
     * @return
     */
    @GetMapping("/cabinet/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.CABINET)
    public R queryCabinetDetails(@PathVariable("id") Integer id) {
        return R.ok(cabinetService.queryCabinetDetails(id));
    }


    /**
     * 机柜请求列表
     *
     * @param params
     * @return
     */
    @GetMapping("/cabinet")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.CABINET)
    public R queryCabinetPage(@RequestParam Map<String, Object> params) {
        PageUtils page = cabinetService.queryCabinetPage(params);
        return R.ok(page);
    }


    /**
     * 保存
     *
     * @param cabinet
     * @return
     */
    @PostMapping("/cabinet")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:save"})
    public R saveCabinet(@RequestBody Cabinet cabinet) {
        ValidateUtils.is(cabinet.getName()).notNull(RCode.DC_CABINETNAME_ISNULL).maxLength(64)
        		.and(cabinet.getRemark()).maxLength(256)
                .and(cabinet.getDcId()).notNull(RCode.DC_ID_ISNULL)
                .and(cabinet.getY()).min(1, RCode.CABINET_Y_MINIMUM)
                .and(cabinet.getX()).min(1, RCode.CABINET_X_MINIMUM);

        return R.ok().putData("id", cabinetService.saveCabinet(cabinet));
    }


    /**
     * 修改
     *
     * @param cabinet
     * @return
     */
    @PutMapping("/cabinet")
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:update"})
    public R updateCabinet(@RequestBody Cabinet cabinet) {
        ValidateUtils.is(cabinet.getId()).notNull(RCode.DC_CABINETID_ISNULL)
                .and(cabinet.getName()).notNull(RCode.DC_CABINETNAME_ISNULL).maxLength(64)
        		.and(cabinet.getRemark()).maxLength(256)
                .and(cabinet.getDcId()).notNull(RCode.DC_ID_ISNULL)
                .and(cabinet.getY()).min(1, RCode.CABINET_Y_MINIMUM)
                .and(cabinet.getX()).min(1, RCode.CABINET_X_MINIMUM);

        return R.ok().putData("id", cabinetService.updateCabinet(cabinet));
    }



    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/cabinet")
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:delete"})
    public R deleteCabinetByIds(@RequestParam(required = false, defaultValue = "0") Integer force, Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.DC_CABINETID_ISNULL);

        return cabinetService.deleteByIds(force, ids);
    }



    /**
     * 机柜u位查询
     * @param id
     * @return
     */
    @GetMapping("/cabinet/u")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.CABINET)
    public R getCabinetUsizeDetails(Integer id) {
        ValidateUtils.is(id).notNull(RCode.DC_CABINETID_ISNULL);

        Map uInfo = cabinetService.getCabinetUsizeDetails(id);
        return R.ok(uInfo);
    }


    @GetMapping("/cabinet/template")
    @SysLog(operation = OperationEnum.DOWNLOAD, type = TypeEnum.CABINET)
    public void getCabinetTemplateByLanguage(HttpServletResponse response,
                                             @RequestParam(required = false, defaultValue = "en") String language,
                                             @RequestParam(required = false, defaultValue = "1") String format) throws IOException {

        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(format)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }

        cabinetService.getCabinetTemplateByLanguage(response, format);
    }

    @PostMapping("/cabinet/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.CABINET)
    @RequiresPermissions({"cabinet:save"})
    public R importCabinetsFromExcel(@RequestParam(value = "file") MultipartFile excelFile, HttpServletRequest request, @RequestParam Map<String, Object> params) throws IOException {
        String language = request.getHeader(Constant.LANGUAGE);
        ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);

        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }

        Map info = cabinetService.importCabinetData(excelFile, params);
        return R.ok(info);
    }

    @GetMapping("/cabinet/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.CABINET)
    public void exportCabinets(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
        Object format = params.get("format");
        String formatStr = format == null ? "1" : format.toString();
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(formatStr)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }
        params.put("format", formatStr);

        cabinetService.export(response, params);
    }

    @DeleteMapping("/cabinet/cancelImport")
    @SysLog(operation = OperationEnum.CANCEL, type = TypeEnum.CABINET)
    public R cancelCabinetImport(@RequestParam(value = "seq", required = false) String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);

        cabinetService.cancelImport(seq);
        return R.ok();
    }

}
