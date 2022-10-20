package com.nis.modules.panel.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.panel.entity.VisualExpressionTmpl;
import com.nis.modules.panel.service.VisualExpreTmplService;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 表达式模板 controller
 *
 * @author shizhendong
 * @date 2021-03-11
 */
@RestController
@RequestMapping("/expression/tmpl")
public class ExpreTmplController {

    @Autowired
    private VisualExpreTmplService expreTmplService;

    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.EXPRESSIONTMPL)
    public R queryTmplInfo(@PathVariable("id") Integer id) {
        return R.ok(expreTmplService.queryTmplInfoById(id));
    }

    @GetMapping
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.EXPRESSIONTMPL)
    public R queryTmplPage(@RequestParam Map<String, Object> params) {
        PageUtils page = expreTmplService.queryPage(params);
        return R.ok(page);
    }

    @GetMapping("/gname")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.EXPRESSIONTMPL)
    public R queryTmplsByGname(@RequestParam(value = "gname", required = false) String gname) {
        List<String> list = expreTmplService.queryTmplsByGname(gname);
        Map map = new HashMap(2);
        map.put("list", list);
        return R.ok(map);
    }

    @PostMapping
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.EXPRESSIONTMPL)
    @RequiresPermissions({"expression:tmpl:save"})
    public R saveTmpl(@RequestBody VisualExpressionTmpl tmpl) {
        ValidateUtils.is(tmpl.getName()).notNull(RCode.EXPRETMPL_NAME_ISNULL).maxLength(64)
                .and(tmpl.getExpression()).notNull(RCode.EXPRETMPL_EXPRESSION_ISNULL).maxLength(4096)
                .and(tmpl.getRemark()).maxLength(256);

        Map map = new HashMap(2);
        map.put("id", expreTmplService.saveTmpl(tmpl));
        return R.ok(map);
    }

    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.EXPRESSIONTMPL)
    @RequiresPermissions({"expression:tmpl:update"})
    public R editTmpl(@RequestBody VisualExpressionTmpl tmpl) {
        ValidateUtils.is(tmpl.getId()).notNull(RCode.EXPRETMPL_ID_ISNULL)
                .and(tmpl.getName()).notNull(RCode.EXPRETMPL_NAME_ISNULL).maxLength(64)
                .and(tmpl.getExpression()).notNull(RCode.EXPRETMPL_EXPRESSION_ISNULL).maxLength(4096)
                .and(tmpl.getRemark()).maxLength(256);

        Map map = new HashMap(2);
        map.put("id", expreTmplService.updateTmpl(tmpl));
        return R.ok(map);
    }

    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.EXPRESSIONTMPL)
    @RequiresPermissions({"expression:tmpl:delete"})
    public R deleteTmpls(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.EXPRETMPL_ID_ISNULL);

        expreTmplService.delete(ids);
        return R.ok();
    }

    @PostMapping("/render")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.EXPRESSIONTMPL)
    public R renderExpressionContent(@RequestBody VisualExpressionTmpl tmpl) {
        ValidateUtils.is(tmpl.getExpression()).notNull(RCode.EXPRETMPL_EXPRESSION_ISNULL)
                .and(tmpl.getVarsVal()).notNull(RCode.EXPRETMPL_RENDER_DATA_ISNULL);

        Map map = new HashMap(2);
        map.put("expression", expreTmplService.renderExpression(tmpl));
        return R.ok(map);
    }

    @GetMapping("/template")
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

        expreTmplService.getTemplateByLanguage(response, format);
    }

    @PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.EXPRESSIONTMPL)
    @RequiresPermissions({"expression:tmpl:save"})
    public R importTmpls(@RequestParam(value = "file") MultipartFile excelFile,
                         HttpServletRequest request,
                         @RequestParam Map<String, Object> params) throws IOException {

        String language = request.getHeader(Constant.LANGUAGE);
        ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);
        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }

        Map info = expreTmplService.importTmpls(excelFile, params);
        return R.ok(info);
    }

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.EXPRESSIONTMPL)
    public void exportDataCenters(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
        Object format = params.get("format");
        String formatStr = format == null ? "1" : format.toString();
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(formatStr)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }
        params.put("format", formatStr);

        expreTmplService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.EXPRESSIONTMPL)
    public R cancelImport(@RequestParam(value = "seq", required = false) String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);

        expreTmplService.cancelImport(seq);
        return R.ok();
    }
}
