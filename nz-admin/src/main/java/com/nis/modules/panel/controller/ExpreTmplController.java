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

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        if (ToolUtil.isEmpty(language)) {
            throw new NZException(RCode.EXPRETMPL_EXPORT_HEADER_LANGUAGE_ISNULL);
        }

        expreTmplService.getExportTemplateByLanguage(response, language);
    }

    @PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.EXPRESSIONTMPL)
    @RequiresPermissions({"expression:tmpl:save"})
    public R importTmplsFromExcel(MultipartFile excelFile, String language) {
        ValidateUtils.is(excelFile).notNull(RCode.EXPRETMPL_IMPORT_FILE_ISNULL)
                .and(language).notNull(RCode.EXPRETMPL_EXPORT_HEADER_LANGUAGE_ISNULL);

        Map info = expreTmplService.importExpreTmplsFromExcel(excelFile, language);
        return R.ok(info);
    }

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.EXPRESSIONTMPL)
    public void exportExpreTmpls(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        expreTmplService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.EXPRESSIONTMPL)
    public R cancelImport(@RequestParam(value = "seq", required = false) String seq) {
        ValidateUtils.is(seq).notNull(RCode.EXPRETMPL_SEQ_ISNULL);

        expreTmplService.cancelImport(seq);
        return R.ok();
    }
}
